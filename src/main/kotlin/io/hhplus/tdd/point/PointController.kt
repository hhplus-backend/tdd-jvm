package io.hhplus.tdd.point

import io.hhplus.tdd.point.dto.PointChargeRequest
import io.hhplus.tdd.point.dto.PointUseRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/point")
class PointController(
    private val pointService: PointService
) {

    private val logger: Logger = LoggerFactory.getLogger(PointController::class.java)

    @GetMapping("{id}")
    fun getUserPoint(@PathVariable id: Long): UserPoint {
        logger.info("[GET] /point/$id - 유저 포인트 조회 요청")
        return pointService.getUserPoint(id)
    }

    @GetMapping("{id}/histories")
    fun getPointHistories(@PathVariable id: Long): List<PointHistory> {
        logger.info("[GET] /point/$id/histories - 유저 포인트 내역 조회 요청")
        return pointService.getPointHistories(id)
    }

    @PatchMapping("{id}/charge")
    fun chargePoint(@PathVariable id: Long, @RequestBody pointChargeRequest: PointChargeRequest): UserPoint {
        logger.info("[PATCH] /point/$id/charge - 유저 포인트 충전 요청: $pointChargeRequest.amount")
        return pointService.chargePoint(id, pointChargeRequest.amount)
    }

    @PatchMapping("{id}/use")
    fun usePoint(@PathVariable id: Long, @RequestBody pointUseRequest: PointUseRequest): UserPoint {
        logger.info("[PATCH] /point/$id/use - 유저 포인트 사용 요청: $pointUseRequest.amount")
        return pointService.usePoint(id, pointUseRequest.amount)
    }
}