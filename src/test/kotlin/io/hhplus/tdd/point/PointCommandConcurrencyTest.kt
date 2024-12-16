package io.hhplus.tdd.point

import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


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
    }

    @AfterEach
    fun tearDown() {
        userPointTable.insertOrUpdate(1L, 0L)
        userPointTable.insertOrUpdate(2L, 0L)
    }

    @Test
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
}
