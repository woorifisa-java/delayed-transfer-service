package lock;

/**
 * user 단위 동시 실행을 제어하기 위한 Lock 관리자 인터페이스
 */
public interface UserLockManager {

	/**
	 * userId에 대한 lock 획득 시도
	 *
	 * boolean->void를 사용하는 이유
	 * boolean : lock동작 여부가 확실하지 않음, void lock()일 경우 해당 메서드는 리턴되면 lock이 정확히 동작하고 있음을 의미
	 */
	void lock(Long userId);

	/**
	 * userId에 대한 lock 해제
	 *
	 * @param userId 사용자 ID
	 */
	void unlock(Long userId);
}