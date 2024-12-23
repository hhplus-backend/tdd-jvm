package io.hhplus.tdd.point

import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import io.hhplus.tdd.point.UserPoint.Companion.MAX_BALANCE
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


@SpringBootTest
class PointCommandConcurrencyTest {

    @Autowired
    lateinit var pointCommand: PointCommand

    @Autowired
    lateinit var userPointTable: UserPointTable

    @Autowired
    lateinit var pointHistoryTable: PointHistoryTable


    @BeforeEach
    fun setUp() {
        val userPoint1 = UserPoint(id = 1L, point = 10000L, updateMillis = System.currentTimeMillis())
        userPointTable.insertOrUpdate(1L, userPoint1.point)

        val userPoint2 = UserPoint(id = 2L, point = 10000L, updateMillis = System.currentTimeMillis())
        userPointTable.insertOrUpdate(2L, userPoint2.point)

        val userPoint3 = UserPoint(id = 3L, point = 10000L, updateMillis = System.currentTimeMillis())
        userPointTable.insertOrUpdate(3L, userPoint3.point)

        val userPoint4 = UserPoint(id = 4L, point = 0L, updateMillis = System.currentTimeMillis())
        userPointTable.insertOrUpdate(4L, userPoint4.point)

        val userPoint5 = UserPoint(id = 5L, point = 1000L, updateMillis = System.currentTimeMillis())
        userPointTable.insertOrUpdate(5L, userPoint5.point)
    }

    @Test
    @DisplayName("포인트 충전 동시성 테스트")
    fun `test chargePoint with 100 concurrent requests`() {
        // given
        val userId = 1L
        val amount = 100L
        val initialPoint = 10000L
        val threadCount = 100
        val latch = CountDownLatch(threadCount)
        val executor: ExecutorService = Executors.newFixedThreadPool(10)

        // when
        for (i in 1..threadCount) {
            executor.submit {
                try {
                    pointCommand.chargePoint(userId, amount)
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        // then
        val finalUserPoint = userPointTable.selectById(userId)
        val expectedPoint = initialPoint + (amount * threadCount)
        val histories = pointHistoryTable.selectAllByUserId(userId)

        assertThat(finalUserPoint.point).isEqualTo(expectedPoint)
        assertThat(histories).hasSize(threadCount)
    }

    @Test
    @DisplayName("포인트 사용 동시성 테스트")
    fun `test usePoint with 100 concurrent requests`() {
        // given
        val userId = 2L
        val amount = 100L
        val initialPoint = 10000L
        val threadCount = 100
        val latch = CountDownLatch(threadCount)
        val executor: ExecutorService = Executors.newFixedThreadPool(10)

        // when
        for (i in 1..threadCount) {
            executor.submit {
                try {
                    pointCommand.usePoint(userId, amount)
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        // then
        val finalUserPoint = userPointTable.selectById(userId)
        val expectedPoint = initialPoint - (amount * threadCount)
        val histories = pointHistoryTable.selectAllByUserId(userId)

        assertThat(finalUserPoint.point).isEqualTo(expectedPoint)
        assertThat(histories).hasSize(threadCount)
    }

    @Test
    @DisplayName("포인트 사용을 100건만 처리 할 수 있을 때, 포인트 사용 요청이 101건 들어오면 마지막 요청은 실패한다.")
    fun `test usePoint with 101 concurrent requests but insufficient points`() {
        // given
        val userId = 3L
        val amount = 100L
        val threadCount = 101 // 101개의 요청 중 100개만 성공해야 함
        val latch = CountDownLatch(threadCount)
        val executor: ExecutorService = Executors.newFixedThreadPool(10)

        val successCount = AtomicInteger(0) // 성공 횟수 추적
        val failureCount = AtomicInteger(0) // 실패 횟수 추적

        // when
        for (i in 1..threadCount) {
            executor.submit {
                try {
                    pointCommand.usePoint(userId, amount)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failureCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        // then
        val finalUserPoint = userPointTable.selectById(userId)
        val expectedPoint = 0L // 10000포인트에서 최대 100번 차감되므로 최종 포인트는 0
        val histories = pointHistoryTable.selectAllByUserId(userId)

        assertThat(finalUserPoint.point).isEqualTo(expectedPoint)
        assertThat(successCount.get()).isEqualTo(100) // 100개의 요청만 성공
        assertThat(failureCount.get()).isEqualTo(1)  // 1개의 요청은 실패
        assertThat(histories).hasSize(100) // 기록도 100개만 남아야 함
    }

    @Test
    @DisplayName("포인트 충전을 100건만 처리 할 수 있을 때(최대잔고에 도달할 경우), 포인트 사용 적립 101건 들어오면 마지막 요청은 실패한다.")
    fun `test chargePoint with 101 concurrent requests but insufficient points`() {
        // given
        val userId = 4L
        val amount = 10_000L
        val threadCount = 101 // 101개의 요청 중 100개만 성공해야 함
        val latch = CountDownLatch(threadCount)
        val executor: ExecutorService = Executors.newFixedThreadPool(10)

        val successCount = AtomicInteger(0) // 성공 횟수 추적
        val failureCount = AtomicInteger(0) // 실패 횟수 추적

        // when
        for (i in 1..threadCount) {
            executor.submit {
                try {
                    pointCommand.chargePoint(userId, amount)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failureCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        // then
        val finalUserPoint = userPointTable.selectById(userId)
        val histories = pointHistoryTable.selectAllByUserId(userId)

        assertThat(finalUserPoint.point).isEqualTo(MAX_BALANCE)
        assertThat(successCount.get()).isEqualTo(100) // 100개의 요청만 성공
        assertThat(failureCount.get()).isEqualTo(1)  // 1개의 요청은 실패
        assertThat(histories).hasSize(100) // 기록도 100개만 남아야 함
    }

    @Test
    @DisplayName("포인트 충전과 사용 복합적인 테스트")
    fun `test concurrent charge and usage`() {
        // given
        val executor = Executors.newFixedThreadPool(4)

        val tasks = listOf(
            { pointCommand.chargePoint(1L, 5000L) },
            { pointCommand.usePoint(1L, 3000L) },
            { pointCommand.chargePoint(2L, 2000L) },
            { pointCommand.usePoint(2L, 1500L) }
        )

        // when
        val futures = tasks.map { task -> // task가 순서대로 실행되는 것은 아님.
            executor.submit {
                try {
                    task()
                } catch (e: Exception) {
                    println(e.message)
                }
            }
        }

        futures.forEach { it.get() }

        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)

        // then
        val user1Point = userPointTable.selectById(1L)
        assertThat(user1Point.point).isEqualTo(12000L)
        val user2Point = userPointTable.selectById(2L)
        assertThat(user2Point.point).isEqualTo(10500L)
    }

    @Test
    @DisplayName("충전 및 사용 작업이 동시 실행될 때 결과 검증. 충전 및 사용을 서로 다른 스레드에서 동시에 실행하고, 작업 완료 후 결과 상태를 검증")
    fun `test concurrent charge and usage at the same time`() {
//        충전 먼저 성공:
//        •	충전: 성공 → 잔고 2000원.
//        •	사용: 성공 → 잔고 0원.
//        •	사용 먼저 실행:
//        •	사용: 실패 → 잔고 부족.
//        •	충전: 성공 → 잔고 2000원.
        // given
        val executor = Executors.newFixedThreadPool(2)
        val userId = 5L

        // when
        // 충전 작업 (1000원 충전)
        val chargeTask = executor.submit<Boolean> {
            try {
                pointCommand.chargePoint(userId, 1000L)
                true
            } catch (e: Exception) {
                false
            }
        }

        // 사용 작업 (2000원 사용)
        val useTask = executor.submit<Boolean> {
            try {
                pointCommand.usePoint(userId, 2000L)
                true
            } catch (e: Exception) {
                false
            }
        }

        // 작업 완료 대기
        val chargeResult = chargeTask.get(2, TimeUnit.SECONDS)
        val useResult = useTask.get(2, TimeUnit.SECONDS)

        // then
        val finalUserPoint = userPointTable.selectById(userId)

        if (!useResult) {
            // 2000원 사용 실패 -> 잔고 부족, 충전은 성공했을 것
            assertThat(finalUserPoint.point).isEqualTo(2000L)
        } else if (chargeResult) {
            // 충전 먼저 성공 -> 충전과 사용 모두 성공
            assertThat(finalUserPoint.point).isEqualTo(0L)
        } else {
            fail("예상하지 못한 상태입니다.")
        }

        executor.shutdown()
        executor.awaitTermination(2, TimeUnit.SECONDS)
    }
}
