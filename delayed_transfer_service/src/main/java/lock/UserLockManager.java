package lock;

/**
 * user 단위 동시 실행을 제어하기 위한 Lock 관리자 인터페이스
 *
 * 핵심 포인트
 * - tryLock(userId): "지금 바로" lock을 얻을 수 있나? 즉시 true/false로 알려줌
 * - lock(userId): lock을 얻을 때까지 기다림(블로킹)
 * - unlock(userId): 반드시 해제
 */
public interface UserLockManager {

    // 대기 여부 판단을 위해 즉시 lock시도
    boolean tryLock(Long userId);

    /**
     * userId에 대한 lock 획득할 때까지 대기(블로킹)
     *
     * InterruptedException을 던지게 해두면
     * 대기 중 인터럽트가 들어왔을 때 상위(Consumer)에서 종료 처리 가능
     */
    void lock(Long userId) throws InterruptedException;

    /**
     * userId에 대한 lock 해제
     */
    void unlock(Long userId);
}
