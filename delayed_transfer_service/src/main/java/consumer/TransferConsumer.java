package consumer;

import queue.TransferQueue;
import domain.Transfer;
import lock.UserLockManager;

/**
 * Queue에서 Transfer를 꺼내 실행하는 Consumer (userId 단위 "대기" 락)
 */
public class TransferConsumer implements Runnable {

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
                System.out.printf("[%s] T%d(%s) lock 대기%n",
                        name, transfer.getTransferId(), userId);

                lockManager.lock(userId); // 여기서 블로킹(대기)

                System.out.printf("[%s] T%d(%s) lock 획득%n",
                        name, transfer.getTransferId(), userId);

                try {
                    // 3. 실행
                    System.out.printf("[%s] T%d(%s) 실행 시작%n",
                            name, transfer.getTransferId(), userId);

                    simulateTransfer();
                    transfer.markDone();

                    System.out.printf("[%s] T%d(%s) DONE%n",
                            name, transfer.getTransferId(), userId);

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
