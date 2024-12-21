package io.hhplus.tdd.database

import io.hhplus.tdd.point.TransactionType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PointHistoryTableTest {

    private lateinit var pointHistoryTable: PointHistoryTable

    @BeforeEach
    fun setUp() {
        pointHistoryTable = PointHistoryTable()
    }

    @Test
    @DisplayName("insert history")
    fun `insert should correctly insert a point history record`() {
        // given
        val userId = 1L
        val amount = 100L
        val transactionType = TransactionType.CHARGE
        val updateMillis = System.currentTimeMillis()

        // when
        val pointHistory = pointHistoryTable.insert(userId, amount, transactionType, updateMillis)

        // then
        assertThat(pointHistory).isNotNull()
        assertThat(pointHistory.id).isEqualTo(userId)
        assertThat(pointHistory.amount).isEqualTo(amount)
        assertThat(pointHistory.type).isEqualTo(transactionType)
        assertThat(pointHistory.timeMillis).isEqualTo(updateMillis)
    }

    @Test
    @DisplayName("selectAllByUserId 해당 유저의 point history 내역을 리스트로 조회한다.")
    fun `selectAllByUserId should return correct point histories for given userId`() {
        // given
        val userId = 1L
        val transactionTypeCharge = TransactionType.CHARGE
        val transactionTypeUse = TransactionType.USE
        val updateMillis = System.currentTimeMillis()

        pointHistoryTable.insert(userId, 100L, transactionTypeCharge, updateMillis)
        pointHistoryTable.insert(userId, -50L, transactionTypeUse, updateMillis)

        // when
        val histories = pointHistoryTable.selectAllByUserId(userId)

        // then
        assertThat(histories).hasSize(2)
    }

    @Test
    @DisplayName("selectAllByUserId 유저가 없는 id 조회 요청하면 빈 리스트가 반환된다.")
    fun `selectAllByUserId should return an empty list for non-existing userId`() {
        // given
        val nonExistingUserId = 999L

        // when
        val histories = pointHistoryTable.selectAllByUserId(nonExistingUserId)

        // then
        assertThat(histories).isEmpty()
    }
}
