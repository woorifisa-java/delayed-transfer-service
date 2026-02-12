// DB 역할
package repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import domain.Transfer;
import domain.TransferStatus;

public class TransferRepository {
	private final Map<Long, Transfer> store = new ConcurrentHashMap<>();

	// 시나리오(TransferCreator)에서 생성된 이체 저장
	public void save(Transfer transfer) {
		store.put(transfer.getTransferId(), transfer);
	}

	// Consumer가 특정 이체를 가져올 때 호출 (Transfer 조회)
	public Transfer findById(long id) {
		return store.get(id);
	}

	// Scheduler가 실행 가능한 Transfer 조회할 때 호출
	public List<Transfer> findExecutable(long now) {
		return store.values().stream() // 데이터를 하나씩 흘려보내면서 처리하는 방식
				.filter(t -> t.getStatus() == TransferStatus.DELAYED) // 딜레이 상태에 있는 것만 남도록 필터
				.filter(t -> t.getBankOpenAt() <= now) // 은행 점검 시간 이후인지 확인
				.collect(Collectors.toList()); // 스트림 결과를 리스트로 저장
	}
}
