package lock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * userId 별로 ReentrantLock을 관리하는 간단한 LockManager 구현체
 *
 * - userId마다 ReentrantLock 1개를 생성/보관
 * - tryLock()으로 "즉시 성공/실패" 판단 가능
 * - lock()은 lockInterruptibly()로 대기 중 인터럽트에 반응 가능
 */
public class SimpleUserLockManager implements UserLockManager {

    private final ConcurrentHashMap<Long, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    private ReentrantLock getLock(Long userId) {
        // userId에 해당하는 lock이 없으면 새로 생성해서 넣고, 있으면 기존 lock 사용
        return lockMap.computeIfAbsent(userId, id -> new ReentrantLock());
    }

    // 즉시 lock시도
    @Override
    public boolean tryLock(Long userId) {
        // 즉시 시도: 이미 누가 잡고 있으면 false
        return getLock(userId).tryLock();
    }

    @Override
    public void lock(Long userId) throws InterruptedException {
        // 블로킹 대기(인터럽트 가능)
        getLock(userId).lockInterruptibly(); 
        // lockInterruptibly : lock 기다리면서 interrupt가 오면 즉시 대기를 중단하고 빠져나옴
    }

    @Override
    public void unlock(Long userId) {
        ReentrantLock lock = lockMap.get(userId);

        // 현재 스레드가 잡고 있을 때만 unlock (실수 방지)
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
