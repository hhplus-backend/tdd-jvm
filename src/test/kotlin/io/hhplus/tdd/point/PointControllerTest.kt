package io.hhplus.tdd.point

import com.fasterxml.jackson.databind.ObjectMapper
import io.hhplus.tdd.point.UserPoint.Companion.MAX_BALANCE
import io.hhplus.tdd.point.UserPoint.Companion.MIN_AMOUNT
import io.hhplus.tdd.point.dto.PointChargeRequest
import io.hhplus.tdd.point.fixture.UserPointFixture
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(PointController::class)
class PointControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var pointService: PointService

    // 공통 given
    private val userId = 1L
    private val userPointFixture = UserPointFixture()
//    private val userPoint = UserPoint(id = userId, point = 1000L, updateMillis = System.currentTimeMillis())

    @Test
    @DisplayName("getUserPoint 성공하면 200 return")
    fun `test getUserPoint returns user point`() {
        // given
        val userPoint = userPointFixture.createUserPointWith_500_000()
        given(pointService.getUserPoint(userId)).willReturn(userPoint)

        // when then
        mockMvc.perform(get("/point/$userId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.point").value(500_000L))
    }

    @Test
    @DisplayName("getPointHistories 성공하면 200 return")
    fun `test getPointHistories returns user point histories`() {
        // given
        val pointHistory = PointHistory(
            id = 1L,
            userId = userId,
            amount = 100L,
            type = TransactionType.CHARGE,
            timeMillis = System.currentTimeMillis()
        )
        val pointHistory2 = PointHistory(
            id = 1L,
            userId = userId,
            amount = 100L,
            type = TransactionType.CHARGE,
            timeMillis = System.currentTimeMillis()
        )
        given(pointService.getPointHistories(userId)).willReturn(listOf(pointHistory, pointHistory2))

        // when then
        mockMvc.perform(get("/point/$userId/histories"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(pointHistory.id))
            .andExpect(jsonPath("$[0].amount").value(pointHistory.amount))
            .andExpect(jsonPath("$[1].id").value(pointHistory2.id))
            .andExpect(jsonPath("$[1].amount").value(pointHistory2.amount))
    }

    @Test
    @DisplayName("chargePoint 성공하면 200 return 잔액 return")
    fun `test chargePoint increases user point`() {
        // given
        val amount = 500L
        val chargeRequest = PointChargeRequest(amount)
        val userPoint = userPointFixture.createUserPointWith_500_000()
        given(pointService.chargePoint(userId, amount)).willReturn(userPoint.copy(point = userPoint.point + amount))

        // when then
        mockMvc.perform(
            patch("/point/$userId/charge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(chargeRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.point").value(userPoint.point + amount))
    }

    @Test
    @DisplayName("chargePoint 음수를 요청하면 500 return")
    fun `test chargePoint with negative amount`() {
        val amount = -500L
        val negativeChargeRequest = PointChargeRequest(amount)
        given(
            pointService.chargePoint(
                1L,
                amount
            )
        ).willThrow(IllegalArgumentException("충전/사용 금액은 최소 $MIN_AMOUNT 이상이어야 합니다."))

        mockMvc.perform(
            patch("/point/{id}/charge", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(negativeChargeRequest))
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    @DisplayName("chargePoint 최대 잔고여서 더이상 충전이 불가능하면 500 return")
    fun `test chargePoint with max amount`() {
        val amount = 1_000_001L
        val negativeChargeRequest = PointChargeRequest(amount)
        given(
            pointService.chargePoint(
                1L,
                amount
            )
        ).willThrow(IllegalArgumentException("잔고는 최대 $MAX_BALANCE 포인트를 초과할 수 없습니다."))

        mockMvc.perform(
            patch("/point/{id}/charge", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(negativeChargeRequest))
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    @DisplayName("usePoint 성공하면 200 return 잔액 return")
    fun `test usePoint deduct user point`() {
        // given
        val amount = 100L
        val useRequest = PointChargeRequest(amount)
        val userPoint = userPointFixture.createUserPointWith_500_000()
        given(pointService.usePoint(userId, amount)).willReturn(userPoint.copy(point = userPoint.point - amount))

        // when then
        mockMvc.perform(
            patch("/point/$userId/use")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(useRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.point").value(userPoint.point - amount))
    }

    @Test
    @DisplayName("usePoint 음수를 요청하면 500 return")
    fun `test usePoint has not enough point`() {
        // given
        val amount = -500L
        val useRequest = PointChargeRequest(amount)
        given(
            pointService.usePoint(
                1L,
                amount
            )
        ).willThrow(IllegalArgumentException("충전/사용 금액은 최소 $MIN_AMOUNT 이상이어야 합니다."))

        // when then
        mockMvc.perform(
            patch("/point/$userId/use")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(useRequest))
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    @DisplayName("usePoint 잔고가 부족하면 500 return")
    fun `test usePoint with negative amount`() {
        // given
        val currentPoint = 100L
        val amount = 1_000_000L
        val useRequest = PointChargeRequest(amount)
        given(
            pointService.usePoint(
                1L,
                amount
            )
        ).willThrow(IllegalArgumentException("포인트가 부족합니다. 현재 잔고: $currentPoint"))

        // when then
        mockMvc.perform(
            patch("/point/$userId/use")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(useRequest))
        )
            .andExpect(status().isInternalServerError)
    }

    @Throws(Exception::class)
    private fun asJsonString(obj: Any?): String {
        val objectMapper = ObjectMapper()
        return objectMapper.writeValueAsString(obj)
    }
}