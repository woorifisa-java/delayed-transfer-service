package consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import domain.Transfer;
import queue.TransferQueue;

public class TransferConsumerWithoutLock implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(TransferConsumerWithoutLock.class);

    private final String name;
    private final TransferQueue queue;

    public TransferConsumerWithoutLock(String name, TransferQueue queue) {
        this.name = name;
        this.queue = queue;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            Transfer transfer = null;
            Long userId = null;
            boolean entered = false;

            try {
                // 1. 큐에서 작업 가져오기
                transfer = queue.take();
                if (transfer == null) {
                    break;
                }

                userId = transfer.getUserId();

                int inflight = TransferConsumerMonitor.enter(userId);
                entered = true;


                log.info("거래 " + transfer.getTransferId() + "(고객 " + userId + ")" + " 실행 시작");
                
                if (inflight > 1) {
                    log.warn("❗❗ 거래 " + transfer.getTransferId() + "(고객 " + userId + ")" 
                            + " 동시 실행 " + inflight + "건 발생");
                }

                simulateTransfer();
                transfer.markDone();

                log.info("거래 " + transfer.getTransferId() + "(고객 " + userId + ")"
                        + " 상태 변경: PREPARING -> DONE");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } finally {
                if (entered && userId != null) {
                    TransferConsumerMonitor.exit(userId);
                }
            }
        }
    }

    private void simulateTransfer() throws InterruptedException {
        Thread.sleep(200);
    }
}
