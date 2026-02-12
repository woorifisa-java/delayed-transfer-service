package lock;

/**
 * user 단위 동시 실행을 제어하기 위한 Lock 관리자 인터페이스
 */
public interface UserLockManager {

	/**
	 * userId에 대한 lock 획득 시도
	 *
	 * @param userId 사용자 ID
	 * @return true : lock 획득 성공 false : 이미 다른 스레드가 해당 user 실행 중
	 */
	boolean tryLock(Long userId);

	/**
	 * userId에 대한 lock 해제
	 *
	 * @param userId 사용자 ID
	 */
	void unlock(Long userId);
}