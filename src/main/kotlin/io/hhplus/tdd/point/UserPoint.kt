package io.hhplus.tdd.point

data class UserPoint(
    val id: Long,
    val point: Long,
    val updateMillis: Long,
) {
    companion object {
        const val MAX_BALANCE = 1_000_000L // 최대 잔고
        const val MIN_AMOUNT = 1L // 최소 금액
    }

    fun increasePoints(amount: Long): UserPoint {
        require(amount >= MIN_AMOUNT) { "충전/사용 금액은 최소 $MIN_AMOUNT 이상이어야 합니다." }
        require(this.point + amount <= MAX_BALANCE) { "잔고는 최대 $MAX_BALANCE 포인트를 초과할 수 없습니다." }
        return this.copy(point = this.point + amount)
    }

    fun deductPoints(amount: Long): UserPoint {
        require(amount >= MIN_AMOUNT) { "충전/사용 금액은 최소 $MIN_AMOUNT 이상이어야 합니다." }
        require(this.point >= amount) { "포인트가 부족합니다. 현재 잔고: ${this.point}" }
        return this.copy(point = this.point - amount)
    }
}