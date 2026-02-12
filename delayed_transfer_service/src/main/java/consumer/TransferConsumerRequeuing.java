package consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import domain.Transfer;
import lock.UserLockManager;
import queue.TransferQueue;

public class TransferConsumerRequeuing implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(TransferConsumer.class);

	private final String name;
	private final TransferQueue queue;
	private final UserLockManager lockManager;

	public TransferConsumerRequeuing(String name, TransferQueue queue, UserLockManager lockManager) {
		this.name = name;
		this.queue = queue;
		this.lockManager = lockManager;
	}

	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			Transfer transfer = null;
			Long userId = null;
			boolean locked = false;

			try {
				// 1) 큐에서 작업 가져오기
				transfer = queue.take();
				if (transfer == null)
					break;

				userId = transfer.getUserId();

				// 2) userLock 즉시 시도
				locked = lockManager.tryLock(userId);

				if (!locked) {
					// 3) 락 실패 → 재큐잉
					log.info("거래 {} (고객 {}) userLock 실패 -> 재큐잉", transfer.getTransferId(), userId);

					queue.put(transfer);

					// 바쁜루프 방지(너무 빨리 재시도하면 CPU만 태움)
					Thread.sleep(20);
					continue;
				}

				// 4) 락 성공 → 실행
				log.info("거래 {} (고객 {}) userLock 획득", transfer.getTransferId(), userId);

				log.info("거래 {} 실행 시작", transfer.getTransferId());

				simulateTransfer();
				transfer.markDone();

				log.info("거래 {} (고객 {}) 상태 변경: PREPARING -> DONE", transfer.getTransferId(), userId);

			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			} finally {
				// 5) 락을 잡았을 때만 해제
				if (locked && userId != null) {
					lockManager.unlock(userId);
				}
			}
		}
	}

	private void simulateTransfer() throws InterruptedException {
		Thread.sleep(200);
	}
}
