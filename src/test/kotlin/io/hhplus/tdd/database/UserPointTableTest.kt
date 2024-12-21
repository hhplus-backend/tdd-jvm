package io.hhplus.tdd.database

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test


class UserPointTableTest {

    private lateinit var userPointTable: UserPointTable

    @BeforeEach
    fun setUp() {
        userPointTable = UserPointTable()
    }

    @Test
    @DisplayName("selectById 유저 id를 조회하면 해당 유저의 포인트를 리턴한다.")
    fun `selectById should return UserPoint with the given id`() {
        // given
        val userId = 1L
        val initialPoint = 500L
        userPointTable.insertOrUpdate(userId, initialPoint)

        // when
        val userPoint = userPointTable.selectById(userId)

        // then
        assertThat(userPoint.id).isEqualTo(userId)
        assertThat(userPoint.point).isEqualTo(initialPoint)
    }

    @Test
    @DisplayName("selectById 없는 유저id를 조회하면 포인트는 0을 리턴한다.")
    fun `selectById should return UserPoint with 0 points if not found`() {
        // given
        val userId = 999L

        // when
        val userPoint = userPointTable.selectById(userId)

        // then
        assertThat(userPoint.id).isEqualTo(userId)
        assertThat(userPoint.point).isEqualTo(0L)
    }

    @Test
    @DisplayName("insertOrUpdate 이미 있는 아이디라면 point가 update 된다.")
    fun `insertOrUpdate should insert new UserPoint or update existing UserPoint`() {
        // given
        val userId = 1L
        val initialPoint = 500L
        val updatedPoint = 1000L
        userPointTable.insertOrUpdate(userId, initialPoint)

        // when
        val updatedUserPoint = userPointTable.insertOrUpdate(userId, updatedPoint)

        // then
        assertThat(updatedUserPoint.id).isEqualTo(userId)
        assertThat(updatedUserPoint.point).isEqualTo(updatedPoint)
    }
}
