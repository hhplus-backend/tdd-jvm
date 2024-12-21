package io.hhplus.tdd.point.fixture

import io.hhplus.tdd.point.UserPoint

class UserPointFixture {

    fun createUserPointWith_500_000(id: Long = 1L, point: Long = 500_000L): UserPoint {
        return UserPoint(id = id, point = point, updateMillis = System.currentTimeMillis())
    }
}