package io.hhplus.tdd.point.dto

data class PointUseRequest(
    val amount: Long
) {
    constructor() : this(0)
}