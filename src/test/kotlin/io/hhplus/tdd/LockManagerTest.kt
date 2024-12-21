package io.hhplus.tdd

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class LockManagerTest {

    private val lockManager = LockManager()

    @Test
    @DisplayName("락과 액션이 성공적으로 실행되는지 테스트")
    fun `lock with key and action executes successfully`() {
        val result = lockManager.lock("testKey", 1, TimeUnit.SECONDS) {
            "Success"
        }
        assertThat(result).isEqualTo("Success")
    }

    @Test
    @DisplayName("락 획득 시 true 반환 확인")
    fun `lock with timeout returns true if lock acquired`() {
        val isLocked = lockManager.lock("testKey", 1, TimeUnit.SECONDS)
        assertThat(isLocked).isTrue()

        lockManager.unlock("testKey")
    }

    @Test
    @DisplayName("락 획득 실패 시 false 반환 확인")
    fun `lock with timeout returns false if lock not acquired`() {
        val executor = Executors.newSingleThreadExecutor()
        val key = "testKey"

        // 다른 스레드에서 락 점유
        val future = executor.submit {
            lockManager.lock(key)
        }
        future.get(1, TimeUnit.SECONDS) // 락 점유를 기다림

        // 현재 스레드에서 락 획득 시도
        val isLocked = lockManager.lock(key, 500, TimeUnit.MILLISECONDS)
        assertThat(isLocked).isFalse()

        lockManager.unlock(key)
        executor.shutdown()
    }

    @Test
    @DisplayName("Lease Time이 지난 후 락이 해제되는지 테스트")
    fun `lockWithLeaseTime releases lock after lease time`() {
        val isLocked = lockManager.lockWithLeaseTime("testKey", 1, 500, TimeUnit.MILLISECONDS)
        assertTrue(isLocked)

        Thread.sleep(1000)

        val isRelocked = lockManager.lock("testKey", 500, TimeUnit.MILLISECONDS)
        assertThat(isRelocked).isTrue()
        lockManager.unlock("testKey")
    }

    @Test
    @DisplayName("락이 존재하지 않을 때 unlock 호출 시 예외 발생하지 않음")
    fun `unlock does not throw exception if lock does not exist`() {
        assertDoesNotThrow {
            lockManager.unlock("nonExistingKey")
        }
    }

    @Test
    @DisplayName("현재 스레드가 락을 소유하지 않을 때 unlock 호출 시 예외 발생하지 않음")
    fun `unlock does not throw exception if current thread does not own the lock`() {
        val executor = Executors.newSingleThreadExecutor()

        lockManager.lock("testKey")

        val future = executor.submit<Boolean> {
            assertDoesNotThrow {
                lockManager.unlock("testKey")
            }
            true
        }

        assertThat(future.get(2, TimeUnit.SECONDS)).isTrue()

        lockManager.unlock("testKey")
        executor.shutdown()
    }

    @Test
    @DisplayName("다수의 스레드가 락을 올바르게 처리하는지 테스트")
    fun `multiple threads handle lock properly`() {
        val executor = Executors.newFixedThreadPool(3)
        val key = "sharedKey"
        val results = mutableListOf<String>()

        val tasks = (1..3).map { id ->
            executor.submit {
                lockManager.lock(key, 1, TimeUnit.SECONDS) {
                    results.add("Thread $id")
                    Thread.sleep(100)
                }
            }
        }

        tasks.forEach { it.get() }

        assertThat(results.size).isEqualTo(3)
        assertThat(results).containsAll(listOf("Thread 1", "Thread 2", "Thread 3"))

        executor.shutdown()
    }
}