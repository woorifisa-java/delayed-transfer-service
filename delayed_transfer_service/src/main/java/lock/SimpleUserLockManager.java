package lock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * userId 별로 ReentrantLock을 관리하는 간단한 LockManager 구현체
 */
public class SimpleUserLockManager implements UserLockManager {

    private final ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    @Override
    public boolean tryLock(String userId) {
        ReentrantLock lock =
                lockMap.computeIfAbsent(userId, id -> new ReentrantLock());
        return lock.tryLock();
    }

    @Override
    public void unlock(String userId) {
        ReentrantLock lock = lockMap.get(userId);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}