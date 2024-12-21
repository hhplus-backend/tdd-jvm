package io.hhplus.tdd.aspect

import io.hhplus.tdd.LockManager
import io.hhplus.tdd.annotation.SyncLock
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.expression.ExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Component

@Aspect
@Component
class LockAspect(
    private val lockManager: LockManager,
    private val parser: ExpressionParser
) {

    private val logger = LoggerFactory.getLogger(javaClass)


    @Around("@annotation(syncLock)")
    fun aroundDistributedLock(joinPoint: ProceedingJoinPoint, syncLock: SyncLock): Any? {
        val lockKey = resolveLockKey(syncLock.key, joinPoint)

        val acquired = lockManager.lock(lockKey, syncLock.waitTime, syncLock.timeUnit)

        if (!acquired) {
            logger.error("lock 획득 실패: $lockKey")
            throw IllegalStateException("Failed to acquire lock: $lockKey within ${syncLock.waitTime} ${syncLock.timeUnit}")
        }

        return try {
            logger.info("lock 획득 성공: $lockKey")
            joinPoint.proceed()
        } finally {
            lockManager.unlock(lockKey)
            logger.info("lock 해제 성공: $lockKey")
        }
    }

    private fun resolveLockKey(lockKeyExpression: String, joinPoint: ProceedingJoinPoint): String {
        val methodSignature = joinPoint.signature as MethodSignature
        val method = methodSignature.method
        val args = joinPoint.args
        val parameterNames = methodSignature.parameterNames

        val context = StandardEvaluationContext()
        for (i in parameterNames.indices) {
            context.setVariable(parameterNames[i], args[i])
        }

        val expression = parser.parseExpression(lockKeyExpression)
        val value = expression.getValue(context, String::class.java)

        return value ?: throw IllegalArgumentException("lock 키를 만드는데 실패하였습니다.")
    }
}
