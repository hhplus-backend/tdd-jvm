package io.hhplus.tdd.point

import io.hhplus.tdd.point.UserPoint.Companion.MAX_BALANCE
import io.hhplus.tdd.point.UserPoint.Companion.MIN_AMOUNT
import io.hhplus.tdd.point.fixture.UserPointFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class PointServiceTest {

    private lateinit var pointCommand: PointCommand
    private lateinit var pointQuery: PointQuery
    private lateinit var pointService: PointService

    private val userId = 1L
    private val amount = 100L
    private val userPointFixture = UserPointFixture()

    @BeforeEach
    fun setUp() {
        pointCommand = mock(PointCommand::class.java)
        pointQuery = mock(PointQuery::class.java)

        pointService = PointService(pointCommand, pointQuery)
    }

    @Test
    @DisplayName("chargePoint 성공하면 충전 후 UserPoint가 return 된다.")
    fun `chargePoint should call pointCommand and return updated user point`() {
        // given
        val initialPoint = 1000L
        val updatedPoint = initialPoint + amount
        val userPoint = UserPoint(id = userId, point = updatedPoint, updateMillis = System.currentTimeMillis())

        given(pointCommand.chargePoint(userId, amount)).willReturn(userPoint)

        // when
        val result = pointService.chargePoint(userId, amount)

        // then
        verify(pointCommand).chargePoint(userId, amount)
        assertThat(result).isEqualTo(userPoint)
    }

    @Test
    @DisplayName("chargePoint 성공하면 충전 후 UserPoint가 return 된다.")
    fun `usePoint should call pointCommand and return updated user point`() {
        // given
        val initialPoint = 1000L
        val updatedPoint = initialPoint - amount
        val userPoint = UserPoint(id = userId, point = updatedPoint, updateMillis = System.currentTimeMillis())

        given(pointCommand.usePoint(userId, amount)).willReturn(userPoint)

        // when
        val result = pointService.usePoint(userId, amount)

        // then
        verify(pointCommand).usePoint(userId, amount)
        assertThat(result).isEqualTo(userPoint)
    }

    @Test
    @DisplayName("getUserPoint 성공하면 해당 ID의 UserPoint가 return 된다.")
    fun `getUserPoint should call pointQuery and return user point`() {
        // given
        val userPoint = UserPoint(id = userId, point = 1000L, updateMillis = System.currentTimeMillis())

        given(pointQuery.getUserPoint(userId)).willReturn(userPoint)

        // when
        val result = pointService.getUserPoint(userId)

        // then
        verify(pointQuery).getUserPoint(userId)
        assertThat(result).isEqualTo(userPoint)
    }

    @Test
    @DisplayName("getPointHistories 성공하면 해당 ID의 UserPoint history list가 return 된다.")
    fun `getPointHistories should call pointQuery and return point histories`() {
        // given
        val pointHistories = listOf(
            PointHistory(
                id = 1L,
                userId = userId,
                amount = 100L,
                type = TransactionType.CHARGE,
                timeMillis = System.currentTimeMillis()
            ),
            PointHistory(
                id = 2L,
                userId = userId,
                amount = -50L,
                type = TransactionType.USE,
                timeMillis = System.currentTimeMillis()
            )
        )

        given(pointQuery.getPointHistories(userId)).willReturn(pointHistories)

        // when
        val result = pointService.getPointHistories(userId)

        // then
        verify(pointQuery).getPointHistories(userId)
        assertThat(result).isEqualTo(pointHistories)
    }

    @Test
    @DisplayName("increasePoints 최소값 이하의 포인트 충전 요청이 들어오면 IllegalArgumentException이 발생한다.")
    fun `increasePoints should throw exception when amount is less than MIN_AMOUNT`() {
        // given
        val userPoint = userPointFixture.createUserPointWith_500_000()

        // when
        val exception = assertThrows<IllegalArgumentException> {
            userPoint.increasePoints(MIN_AMOUNT - 1L)
        }

        // then
        assertThat(exception.message).isEqualTo("충전/사용 금액은 최소 $MIN_AMOUNT 이상이어야 합니다.")
    }

    @Test
    @DisplayName("increasePoints 최대 잔고 이상의 포인트 충전 요청이 들어오면 IllegalArgumentException이 발생한다.")
    fun `increasePoints should throw exception when new balance exceeds MAX_BALANCE`() {
        // given
        val userPoint = userPointFixture.createUserPointWith_500_000()

        // when
        val exception = assertThrows<IllegalArgumentException> {
            userPoint.increasePoints(600_000L)
        }

        // then
        assertThat(exception.message).isEqualTo("잔고는 최대 $MAX_BALANCE 포인트를 초과할 수 없습니다.")
    }

    @Test
    @DisplayName("deductPoints 최소값 이하의 포인트 사용 요청이 들어오면 IllegalArgumentException이 발생한다.")
    fun `deductPoints should throw exception when amount is less than MIN_AMOUNT`() {
        // given
        val userPoint = userPointFixture.createUserPointWith_500_000()

        // when
        val exception = assertThrows<IllegalArgumentException> {
            userPoint.deductPoints(MIN_AMOUNT - 1L)
        }

        // then
        assertThat(exception.message).isEqualTo("충전/사용 금액은 최소 $MIN_AMOUNT 이상이어야 합니다.")
    }

    @Test
    @DisplayName("deductPoints 현재 잔고보다 많은 포인트 사용 요청이 들어오면 IllegalArgumentException이 발생한다.")
    fun `deductPoints should throw exception when balance is insufficient`() {
        // given
        val userPoint = userPointFixture.createUserPointWith_500_000()

        // when
        val exception = assertThrows<IllegalArgumentException> {
            userPoint.deductPoints(600_000L)
        }

        // then
        assertThat(exception.message).isEqualTo("포인트가 부족합니다. 현재 잔고: ${userPoint.point}")
    }
}
