package io.hhplus.tdd.point

import io.hhplus.tdd.LockManager
import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import org.springframework.stereotype.Component

@Component
class PointCommand(
    private val userPointTable: UserPointTable,
    private val pointHistoryTable: PointHistoryTable,
    private val lockManager: LockManager
) {
    fun chargePoint(userId: Long, amount: Long): UserPoint {
        return lockManager.withLock(userId) {
            val userPoint = userPointTable.selectById(userId)
            val updatedUserPoint = userPoint.increasePoints(amount)

            val savedUserPoint = userPointTable.insertOrUpdate(userId, updatedUserPoint.point)
            pointHistoryTable.insert(updatedUserPoint.id, amount, TransactionType.CHARGE, System.currentTimeMillis())
            savedUserPoint
        }
    }

    fun usePoint(userId: Long, amount: Long): UserPoint {
        return lockManager.withLock(userId) {
            val userPoint = userPointTable.selectById(userId)
            val updatedUserPoint = userPoint.deductPoints(amount)

            val savedUserPoint = userPointTable.insertOrUpdate(userId, updatedUserPoint.point)
            pointHistoryTable.insert(updatedUserPoint.id, -amount, TransactionType.USE, System.currentTimeMillis())
            savedUserPoint
        }
    }
}
