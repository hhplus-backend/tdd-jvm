package io.hhplus.tdd

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

@Component
class LockManager {
    private val locks: MutableMap<String, ReentrantLock> = ConcurrentHashMap()

    /**
     * 명시적으로 락을 획득합니다.
     * 락 획득 후 수행이 다되면 락을 해제
     * return lockManager.withLock(key) {
     * ...
    }
     */
    fun <T> lock(key: String, timeout: Long, unit: TimeUnit, action: () -> T): T {
        val lock = locks.computeIfAbsent(key) { ReentrantLock(true) }
        lock.tryLock(timeout, unit)
        try {
            return action()
        } finally {
            lock.unlock()
        }
    }

    /**
     * 명시적으로 락을 획득합니다.
     * time 시간 동안 락 획득을 시도하며, 주어진 시간 내에 성공적으로 락을 획득하면
     * true, 시간이 초과되면 false를 return
     */
    fun lock(key: String, timeout: Long, unit: TimeUnit): Boolean {
        val lock = locks.computeIfAbsent(key) { ReentrantLock(true) }
        return lock.tryLock(timeout, unit)
    }

    /**
     * 명시적으로 락을 획득합니다.
     * leasetime 후에 자동으로 락을 해제합니다.
     */
    fun lockWithLeaseTime(key: String, timeout: Long, leaseTime: Long, unit: TimeUnit): Boolean {
        val lock = locks.computeIfAbsent(key) { ReentrantLock(true) }

        val lockAcquired = lock.tryLock(timeout, unit)
        if (lockAcquired) {
            // leaseTime 후 락을 해제
            Executors.newSingleThreadScheduledExecutor().schedule({
                unlock(key)
            }, leaseTime, unit)
        }
        return lockAcquired
    }

    /**
     * 명시적으로 락을 획득합니다.
     */
    fun lock(key: String) {
        val lock = locks.computeIfAbsent(key) { ReentrantLock(true) }
        lock.lock()
    }

    /**
     * 명시적으로 락을 해제합니다.
     * 락이 존재하지 않거나, 현재 스레드가 락을 보유하고 있지 않은 경우에도 오류를 발생시키지 않습니다.
     */
    fun unlock(key: String) {
        val lock = locks[key]
        if (lock != null && lock.isHeldByCurrentThread) {
            lock.unlock()
        }
        // 락이 없거나 다른 스레드가 보유 중이어도 그냥 무시
    }
}