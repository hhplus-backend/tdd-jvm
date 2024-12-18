package io.hhplus.tdd.point

import io.hhplus.tdd.point.UserPoint.Companion.MAX_BALANCE
import io.hhplus.tdd.point.UserPoint.Companion.MIN_AMOUNT
import io.hhplus.tdd.point.fixture.UserPointFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UserPointTest {

    private val userPointFixture = UserPointFixture()

    @Test
    @DisplayName("increasePoints 포인트가 정상적으로 충전된다.")
    fun `increasePoints should return a new UserPoint with increased points`() {
        // given
        val amount = 100_000L
        val userPoint = userPointFixture.createUserPointWith_500_000()

        // when
        val updatedUserPoint = userPoint.increasePoints(amount)

        // then
        assertThat(updatedUserPoint.id).isEqualTo(userPoint.id)
        assertThat(updatedUserPoint.point).isEqualTo(userPoint.point + amount)
    }

    @Test
    @DisplayName("increasePoints 최소값 이하의 포인트 충전 요청이 들어오면 IllegalArgumentException이 발생한다.")
    fun `increasePoints should throw an exception when amount is less than MIN_AMOUNT`() {
        // given
        val amount = 0L
        val userPoint = userPointFixture.createUserPointWith_500_000()

        // when
        val exception = assertThrows<IllegalArgumentException> {
            userPoint.increasePoints(amount)
        }

        // then
        assertThat(exception.message).isEqualTo("충전/사용 금액은 최소 $MIN_AMOUNT 이상이어야 합니다.")
    }

    @Test
    @DisplayName("increasePoints 최대 잔고 이상의 포인트 충전 요청이 들어오면 IllegalArgumentException이 발생한다.")
    fun `increasePoints should throw an exception when the balance exceeds MAX_BALANCE`() {
        // given
        val amount = 600_000L
        val userPoint = userPointFixture.createUserPointWith_500_000()

        // when
        val exception = assertThrows<IllegalArgumentException> {
            userPoint.increasePoints(amount)
        }

        // then
        assertThat(exception.message).isEqualTo("잔고는 최대 $MAX_BALANCE 포인트를 초과할 수 없습니다.")
    }

    @Test
    @DisplayName("deductPoints 포인트가 정상적으로 사용된다.")
    fun `deductPoints should return a new UserPoint with decreased points`() {
        // given
        val amount = 100_000L
        val userPoint = userPointFixture.createUserPointWith_500_000()

        // when
        val updatedUserPoint = userPoint.deductPoints(amount)

        // then
        assertThat(updatedUserPoint.id).isEqualTo(userPoint.id)
        assertThat(updatedUserPoint.point).isEqualTo(userPoint.point - amount)
    }

    @Test
    @DisplayName("deductPoints 최소값 이하의 포인트 사용 요청이 들어오면 IllegalArgumentException이 발생한다.")
    fun `deductPoints should throw an exception when amount is less than MIN_AMOUNT`() {
        // given
        val amount = 0L
        val userPoint = userPointFixture.createUserPointWith_500_000()

        // when
        val exception = assertThrows<IllegalArgumentException> {
            userPoint.deductPoints(amount)
        }

        assertThat(exception.message).isEqualTo("충전/사용 금액은 최소 $MIN_AMOUNT 이상이어야 합니다.")
    }

    @Test
    @DisplayName("deductPoints 현재 잔고보다 많은 포인트 사용 요청이 들어오면 IllegalArgumentException이 발생한다.")
    fun `deductPoints should throw an exception when there are insufficient points`() {
        // given
        val amount = 600_000L
        val userPoint = userPointFixture.createUserPointWith_500_000()

        // when
        val exception = assertThrows<IllegalArgumentException> {
            userPoint.deductPoints(amount)
        }

        // then
        assertThat(exception.message).isEqualTo("포인트가 부족합니다. 현재 잔고: ${userPoint.point}")
    }
}
