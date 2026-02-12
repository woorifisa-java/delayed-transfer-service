
package consumer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// userId 별 현재 동시에 실행 중인 Transfer 개수 count하는 객체
public class TransferConsumerMonitor {
	
	//AtomicInteger : 증감 원자적 처리
	private static final ConcurrentHashMap<Long, AtomicInteger> inFlight = new ConcurrentHashMap<>();
	
	//실행 시작 시 호출
	public static int enter(Long userId) {
        return inFlight
                .computeIfAbsent(userId, k -> new AtomicInteger(0))
                .incrementAndGet(); //실행 시작 시점에 +1하고 증가된 값 바로 반환
    }

	// 실행 종료 시 호출
    public static void exit(Long userId) {
        AtomicInteger c = inFlight.get(userId);
        // 값이 0이 되면 더 이상 실행중인 작업 없음으로 간주 -> Map에서 제거
        if (c != null && c.decrementAndGet() == 0) {
        	// 실행 종료 시 -1
            inFlight.remove(userId);
        }
    }
}
