package io.hhplus.tdd.point

import java.util.concurrent.CompletableFuture

data class PointRequest(
    val userId: Long,
    val amount: Long,
    val type: TransactionType,
    var result: CompletableFuture<UserPoint> = CompletableFuture()
)