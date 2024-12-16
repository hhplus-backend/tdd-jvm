package io.hhplus.tdd.point.dto

data class PointChargeRequest(
    val amount: Long
) {
    constructor() : this(0)
}
