package consumer;

import domain.Transfer;
import queue.TransferQueue;

public class TransferConsumerWithoutLock implements Runnable {

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
            boolean entered = false; // monitor enter 성공 여부

            try {
                // 1. 큐에서 작업 가져오기
                transfer = queue.take();
                if (transfer == null) {
                    break;
                }

                userId = transfer.getUserId();

                // 2. 실행 진입 감지: 동시 실행 카운트 체크
                int inflight = TransferConsumerMonitor.enter(userId);
                entered = true;

                

                // 3. 실행
                System.out.printf("[%s] T%d(%s) 실행 시작%n",
                        name, transfer.getTransferId(), userId);
                
                if (inflight > 1) {
                    System.out.println("---------------------------");
                    System.out.printf(" ❗❗ userId = %d 동시 실행 %d건%n", userId, inflight);
                    System.out.println("---------------------------");
                }

                simulateTransfer();

                transfer.markDone();

                System.out.printf("[%s] T%d(%s) DONE%n",
                        name, transfer.getTransferId(), userId);

            } catch (InterruptedException e) {
                // 인터럽트 복구 후 종료
                Thread.currentThread().interrupt();
                break;
            } finally {
                // 4. monitor 정리
                if (entered && userId != null) {
                    TransferConsumerMonitor.exit(userId);
                }
            }
        }
    }

    // 송금 실행 시뮬레이션
    private void simulateTransfer() throws InterruptedException {
        Thread.sleep(200);
    }
}