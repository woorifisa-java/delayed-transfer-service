package lock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * userId 별로 ReentrantLock을 관리하는 간단한 LockManager 구현체
 * ReentrantLock()
 */
public class SimpleUserLockManager implements UserLockManager {

    private final ConcurrentHashMap<Long, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    @Override
    public void lock(Long userId) {
        ReentrantLock lock =
                lockMap.computeIfAbsent(userId, id -> new ReentrantLock());
        lock.lock();
    }

    @Override
    public void unlock(Long userId) {
        ReentrantLock lock = lockMap.get(userId);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}