package consumer;

import queue.TransferQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import domain.Transfer;
import lock.UserLockManager;

/**
 * Queue에서 Transfer를 꺼내 실행하는 Consumer (userId 단위 "대기" 락)
 */

public class TransferConsumer implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(TransferConsumer.class);

	private final String name;
	private final TransferQueue queue;
	private final UserLockManager lockManager;

	public TransferConsumer(String name, TransferQueue queue, UserLockManager lockManager) {
		this.name = name;
		this.queue = queue;
		this.lockManager = lockManager;
	}

	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			Transfer transfer = null;
			Long userId = null;

			try {
				// 1. 큐에서 작업 가져오기
				transfer = queue.take();
				if (transfer == null) {
					break;
				}

				userId = transfer.getUserId();

				// 2. user 단위 lock: 얻을 때까지 "대기"
				log.info("거래 " + transfer.getTransferId() + "(고객 " + userId + ")" + " userLock 실패 -> userLock 얻을 때까지 대기");

				lockManager.lock(userId); // 여기서 블로킹(대기)

				log.info("거래 " + transfer.getTransferId() + "(고객 " + userId + ")" + " userLock 획득");
				
				try {
					// 3. 실행
					log.info("거래 " + transfer.getTransferId() + " 실행 시작");

					simulateTransfer();
					transfer.markDone();

					log.info("거래 " + transfer.getTransferId() +  "(고객 " + userId + ")" + " 상태 변경: PREPARING -> DONE");

				} finally {
					// 4. 반드시 lock 해제
					lockManager.unlock(userId);
				}

			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

	// 송금 실행 시뮬레이션
	private void simulateTransfer() throws InterruptedException {
		Thread.sleep(200);
	}
}
