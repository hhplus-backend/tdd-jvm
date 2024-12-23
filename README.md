# tdd-jvm
this is tdd-jvm

## 요구사항

- PATCH  `/point/{id}/charge` : 포인트를 충전한다.
- PATCH `/point/{id}/use` : 포인트를 사용한다.
- *GET `/point/{id}` : 포인트를 조회한다.*
- *GET `/point/{id}/histories` : 포인트 내역을 조회한다.*
- *잔고가 부족할 경우, 포인트 사용은 실패하여야 합니다.*
- *동시에 여러 건의 포인트 충전, 이용 요청이 들어올 경우 순차적으로 처리되어야 합니다.*

## 동시성 제어 방식에 대한 분석
본 프로젝트에서 **동시에 여러 건의 포인트 충전, 이용 요청이 들어올 경우 순차적으로 처리**되어하는 요구사항을 충족시키기 위한 동시성 제어에 대한 분석입니다.

### 1. 동시성 제어의 필요성
- 왜 동시성 제어가 필요한가?  
    여러 스레드 또는 프로세스가 동시에 같은 자원(예: 데이터베이스, 파일)에 접근할 때 발생할 수 있는 문제(경쟁 상태, 데이터 불일치)를 방지하기 위함입니다.  
    예를 들어 하나의 자원에 여러 사용자가 동시에 쓰기 작업을 수행하는 경우 데이터 무결성이 깨질 수 있고, 동일한 작업이 중복으로 수행되어 잘못된 결과가 저장될 수 있습니다.  
    **본 프로젝트에서는 포인트 충전 및 이용에 대해서 순차처리를 위해 동시성 제어가 필요합니다.**

### 2. 동시성 제어 방식의 종류

- 낙관적 동시성 제어 (Optimistic Concurrency Control)  
    충돌이 자주 발생하지 않을 것으로 예상하고, 먼저 작업을 수행한 후 충돌이 감지되면 롤백하는 방식입니다.  
    JPA 사용시 `@Version` 키워드를 사용하여 쉽게 구현 가능합니다.  
    ex) 데이터베이스의 `version` 또는 `timestamp`를 사용한 충돌 감지.  
    - 장점: 락을 사용하지 않아 성능이 좋고 리소스 낭비가 적음.  
    - 단점: 충돌이 빈번하면 롤백 비용이 커짐.  
    ```kotlin
    @Entity
    data class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
        val name: String,
        var quantity: Int, 
        @Version // 버전 필드를 사용하여 낙관적 락 적용
        val version: Long? = null
    )
    
    @Service
    class ProductService(private val productRepository: ProductRepository) {
        @Transactional
        fun updateProduct(productId: Long, updatedQuantity: Int) {
            val product = productRepository.findById(productId)
                .orElseThrow { IllegalArgumentException("Product not found") }          
            product.quantity = updatedQuantity
            productRepository.save(product) // JPA가 자동으로 version 필드를 검사
        }
    }
    ```
    
- 비관적 동시성 제어 (Pessimistic Concurrency Control)  
    충돌이 발생할 가능성이 높다고 가정하고, 먼저 락을 획득하여 다른 작업의 접근을 제한합니다.  
    `LockModeType.PESSIMISTIC_WRITE`을 사용해 쓰기 락을 걸어 다른 트랜잭션이 읽거나 쓰는 것을 방지합니다.  
    ex)데이터베이스의 SELECT ... FOR UPDATE, ReentrantLock 등을 사용.  
    - 장점: 충돌을 미리 방지할 수 있어 데이터 무결성이 보장됨.  
    - 단점: 락 획득 및 해제에 따른 비용이 발생하고, 데드락의 위험이 있음.  
    ```kotlin
    import org.springframework.data.jpa.repository.Lock
    interface ProductRepository : CrudRepository<Product, Long> {
        @Lock(LockModeType.PESSIMISTIC_WRITE) // 비관적 락 적용
        @Query("SELECT p FROM Product p WHERE p.id = :id")
        fun findByIdWithLock(id: Long): Product?
    }
  
    @Service
    class ProductService(private val productRepository: ProductRepository) {
        @Transactional
        fun updateProductWithLock(productId: Long, updatedQuantity: Int) {
            val product = productRepository.findByIdWithLock(productId)
                ?: throw IllegalArgumentException("Product not found")
            product.quantity = updatedQuantity
            // 트랜잭션 종료 시 락 해제
        }
    }
    ```

- 분산 락 (Distributed Lock)  
    분산 시스템에서 여러 노드 또는 서버가 동일한 자원에 접근하는 경우 공유 락을 통해 동시성을 제어하는 방식입니다.  
    - 예시: Redis, ZooKeeper를 이용한 분산 락 구현.  
    - 장점: 여러 인스턴스 간의 동기화가 가능함.  
    - 단점: 락 획득 시 네트워크 오버헤드가 발생할 수 있고, 설정이 복잡할 수 있음.    
    ```kotlin
    // redis 사용 예시
    @Service
    class RedisLockService(private val redisTemplate: StringRedisTemplate) {
        fun acquireLock(lockKey: String, leaseTime: Long): Boolean {
            val result = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "locked", Duration.ofSeconds(leaseTime))
            return result == true
        }
    
        fun releaseLock(lockKey: String) {
            redisTemplate.delete(lockKey)
        }
    }
  
    @Service
    class ProductService(
    private val redisLockService: RedisLockService,
    private val productRepository: ProductRepository
    ) {
    private val lockKeyPrefix = "lock:product:"
    
        fun updateProductWithDistributedLock(productId: Long, updatedQuantity: Int) {
            val lockKey = "$lockKeyPrefix$productId"
            // 락 획득
            val acquired = redisLockService.acquireLock(lockKey, leaseTime = 10)
    
            if (!acquired) {
                throw IllegalStateException("Failed to acquire lock for product $productId")
            }
    
            try {
                val product = productRepository.findById(productId)
                    .orElseThrow { IllegalArgumentException("Product not found") }
                
                product.quantity = updatedQuantity
                productRepository.save(product)
            } finally {
                // 락 해제
                redisLockService.releaseLock(lockKey)
            }
        }
    }
    ```

### 3. 고려사항
락을 걸시에 아래의 사항을 고려해야합니다.  

- 데드락 (Deadlock) 방지  
    여러 락을 사용할 경우 락 획득 순서를 정하거나 타임아웃을 설정해 데드락을 방지해야 합니다.  
    ex) tryLock 사용 시 시간 제한을 두어 데드락 방지.  
    ```kotlin
    if (lock.tryLock(5, TimeUnit.SECONDS)) { // 락 획득까지 대기하는 시간 설정
        try {
            // 작업 수행
        } finally {
            lock.unlock()
        }
    }
    ```

- 락 타임아웃 (Timeout)  
락 획득 시 타임아웃과 임대 시간(leaseTime)을 적절히 설정해 불필요한 락 점유를 방지합니다.  

- 분산 환경에서의 락 보장  
분산 락을 사용할 때는 네트워크 장애, 서버 재시작 등의 상황에서 락이 정상적으로 해제될 수 있도록 보장해야 합니다.  


### 4. 구현 방식 설명
DB, 외부 infra를 사용 하지 않고, application level에서 동시성을 제어 하기 위해서는 임계영역전에 락을 획득합니다.

`synchronized`를 사용한다면 타임아웃을 설정할 수 없어 스레드가 무한정 락이 해제될 때까지 대기하는 데드락이 발생할 수 있기에 타임아웃을 지정할 수 있는
`java.util.concurrent.locks.ReentrantLock` 패키지에서 제공하는 **`ReentrantLock`**를 사용하여 구현하였습니다.


- `synchronized` 와 `ReentrantLock` 비교

| **특징**               | **synchronized**           | **ReentrantLock**             |
|------------------------|----------------------------|-------------------------------|
| **락 획득/해제**       | 자동 관리                  | 명시적 관리 (lock()/unlock()) |
| **타임아웃**           | 지원하지 않음              | `tryLock()`으로 지원          |
| **인터럽트 처리**      | 지원하지 않음              | `lockInterruptibly()` 지원    |
| **공정성**             | 지원하지 않음              | 공정성 설정 가능              |
| **조건 변수**          | `wait()/notify()` 사용     | `newCondition()` 사용         |
| **재진입성**           | 지원                      | 지원                         |
| **성능**               | 간단한 경우 성능 유리      | 복잡한 상황에서 유리          |

- 공정성 : 먼저 대기한 스레드가 반드시 먼저 락을 획득하는 것인지에 대한 여부  
    `ReentrantLock`는 생성자에 true를 전달하면 FIFO 순서로 락을 획득하지만 `synchronized`는 먼저 대기한 스레드가 반드시 먼저 락을 획득하는 것은 아님(JVM이 관리)
```kotlin
val fairLock = ReentrantLock(true)
```

- 재진입성 : 동일한 스레드가 이미 획득한 락을 다시 획득할 수 있는지에 대한 여부

### 4.1 구현 방식 코드
락을 수행하는 LockManager, @SyncLock 어노테이션을 통해 메서드 단위에서 락을 제어하는 방식으로 구현하였습니다.

또한 `fun <T> lock(key: String, action: () -> T): T`를 사용하여 메소드 단위가 아닌 특정 구간의 lock도 가능합니다.

deadlock을 방지하기위해 lock 획득 waitTime을 받아서 `trylock`을 시도합니다.

- LockManager : 락을 획득하고 해제하는 manager 구현. userId를 키로 가지고 있는 `ConcurrentHashMap`를 활용하여 user별 로 관리.
```kotlin
@Component
class LockManager {
    private val locks: MutableMap<String, ReentrantLock> = ConcurrentHashMap()

    fun <T> lock(key: String, timeout: Long, unit: TimeUnit, action: () -> T): T {
        val lock = locks.computeIfAbsent(key) { ReentrantLock() }
        val acquired = lock.tryLock(timeout, unit)
        if (!acquired) {
            throw IllegalStateException("Failed to acquire lock for key: $key within $timeout ${unit.name}")
        }
        try {
            return action()
        } finally {
            lock.unlock()
        }
    }
    
    fun lock(key: String, time: Long, unit: TimeUnit): Boolean {
        val lock = locks.computeIfAbsent(key) { ReentrantLock() }
        return lock.tryLock(time, unit)
    }

    fun unlock(key: String) {
        val lock = locks[key]
        if (lock != null && lock.isHeldByCurrentThread) {
            lock.unlock()
        }
    }
}
```

- aspect : SyncLock 어노테이션에 대한 aspect
```kotlin
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
```

- 실제 적용 코드 (PointCommand)
```kotlin
@SyncLock(key = "#userId")
fun chargePoint(userId: Long, amount: Long): UserPoint {
    val userPoint = userPointTable.selectById(userId)
    val updatedUserPoint = userPoint.increasePoints(amount)

    val savedUserPoint = userPointTable.insertOrUpdate(userId, updatedUserPoint.point)
    pointHistoryTable.insert(updatedUserPoint.id, amount, TransactionType.CHARGE, System.currentTimeMillis())

    return savedUserPoint
}
```

### 5. 테스트 및 검증
아래의 시나리오대로 동시성 테스트를 진행하였습니다.  

스레드를 만들고 지정된 횟수만큼 수행하여 성공횟수와 실패 횟수 및 포인트 조회를 통해 검증하는 절차로 진행하였습니다.
```kotlin
val latch = CountDownLatch(threadCount)
val executor: ExecutorService = Executors.newFixedThreadPool(10)
val successCount = AtomicInteger(0) // 성공 횟수 추적
val failureCount = AtomicInteger(0) // 실패 횟수 추적

// when
for (i in 1..threadCount) {
    executor.submit {
        try {
            pointCommand.usePoint(userId, amount)
            successCount.incrementAndGet()
        } catch (e: Exception) {
            failureCount.incrementAndGet()
        } finally {
            latch.countDown()
        }
    }
}

latch.await()
executor.shutdown()

```

`executor: ExecutorService = Executors.newFixedThreadPool(10)`

![img.png](img.png)

1. 포인트 충전 동시성 테스트
2. 포인트 사용 동시성 테스트
3. 포인트 사용을 100건만 처리 할 수 있을 때, 포인트 사용 요청이 101건 들어오면 마지막 요청은 실패한다.
4. 포인트 충전을 100건만 처리 할 수 있을 때(최대잔고에 도달할 경우), 포인트 사용 적립 101건 들어오면 마지막 요청은 실패한다.


### 6. 개선사항
현재는 lock 획득시에 timeout에 걸리면 재시도 하지 않지만 추후 MAX_RETRY 까지 재시도 가능하게 수정