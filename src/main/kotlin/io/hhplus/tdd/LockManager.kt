package io.hhplus.tdd

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

@Component
class LockManager {
    private val locks: MutableMap<Long, ReentrantLock> = ConcurrentHashMap()

    fun <T> withLock(userId: Long, action: () -> T): T {
        val lock = locks.computeIfAbsent(userId) { ReentrantLock() }
        lock.lock()
        try {
            return action()
        } finally {
            lock.unlock()
        }
    }
}