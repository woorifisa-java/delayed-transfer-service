package service;

import domain.Transfer;
import repository.TransferRepository;

import java.util.concurrent.atomic.AtomicLong;

public class TransferCreator {

	private final TransferRepository repository; // 생성된 요청 객체를 저장하기 위함
	private final AtomicLong idGenerator = new AtomicLong(1); //

	public TransferCreator(TransferRepository repository) {
		this.repository = repository;
	}

	// 지연 이체 하나를 생성
	public Transfer create(Long userId, Long delayMillis) {
		long transferId = idGenerator.getAndIncrement(); // transferId를 자동으로 1 증가
		long bankOpenAt = System.currentTimeMillis() + delayMillis;

		Transfer transfer = new Transfer(transferId, userId, bankOpenAt);

		repository.save(transfer);

		return transfer;
	}
}
