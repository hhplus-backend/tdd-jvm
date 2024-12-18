package io.hhplus.tdd.point

import org.springframework.stereotype.Service

@Service
class PointService(
    private val pointCommand: PointCommand,
    private val pointQuery: PointQuery
) {
    /**
     * 포인트 충전 요청
     */
    fun chargePoint(userId: Long, amount: Long): UserPoint {
        return pointCommand.chargePoint(userId, amount)
    }

    /**
     * 포인트 사용 요청
     */
    fun usePoint(userId: Long, amount: Long): UserPoint {
        return pointCommand.usePoint(userId, amount)
    }

    /**
     * 특정 유저의 포인트 조회
     */
    fun getUserPoint(userId: Long): UserPoint {
        return pointQuery.getUserPoint(userId)
    }

    /**
     * 특정 유저의 포인트 내역 조회
     */
    fun getPointHistories(userId: Long): List<PointHistory> {
        return pointQuery.getPointHistories(userId)
    }

}