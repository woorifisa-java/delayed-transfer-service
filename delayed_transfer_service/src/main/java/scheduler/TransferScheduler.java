package scheduler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import domain.Transfer;
import queue.TransferQueue;
import repository.TransferRepository;

public class TransferScheduler implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(TransferScheduler.class);

	private volatile boolean running = true;

	private final TransferRepository transferRepository;
	private final TransferQueue transferQueue;
	private final long intervalMillis;

	public TransferScheduler(TransferRepository transferRepository,
	                         TransferQueue transferQueue,
	                         long intervalMillis) {
		this.transferRepository = transferRepository;
		this.transferQueue = transferQueue;
		this.intervalMillis = intervalMillis;
	}

	@Override
	public void run() {
		while (running) {
			long now = System.currentTimeMillis();

			// 실행 대상 조회
			List<Transfer> executables = transferRepository.findExecutable(now);

			if (!executables.isEmpty()) {
				log.info("실행 대상 건수: " + executables.size() + "\n");
			}

			for (Transfer t : executables) {
				enqueueOnce(t, now);
			}

			sleep(intervalMillis);
		}
	}

	/**
	 * 중복 큐 적재 방지
	 * DELAYED → PREPARING 상태 변경 후 큐에 넣기
	 */
    private void enqueueOnce(Transfer transfer, long now) {

        // 실행 시간 안 됐으면 패스
        if (transfer.getBankOpenAt() > now) {
            return;
        }

        // DELAYED -> PREPARING
        boolean isMarked = transfer.markPreparing();

        if (!isMarked) {
            return;
        }

        log.info("거래 " + transfer.getTransferId()
                +  " (고객 " + transfer.getUserId() + ")" + " 상태 변경: DELAYED -> PREPARING");

        // 큐에 넣기
        transferQueue.put(transfer);

        log.info("거래 " + transfer.getTransferId()
                + " (고객 " + transfer.getUserId() + ")" + " 큐에 추가 완료");
    }

	public void stop() {
		running = false;
	}

	public static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
