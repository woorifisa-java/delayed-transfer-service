package consumer;

import queue.TransferQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import domain.Transfer;
import lock.UserLockManager;

/**
 * Queue에서 Transfer를 꺼내 실행하는 Consumer (userId 단위 "대기" 락)
 *
 * 요구사항 반영:
 * - "이미 락이 걸려 있어서 대기가 발생하는 경우"에만
 *   'userLock 실패 -> 대기' 로그가 찍히도록 한다.
 *
 * 구현 방식:
 * 1) tryLock(userId)로 먼저 즉시 시도
 * 2) 실패(false)면 그때만 실패 로그 출력 후 lock(userId)로 블로킹 대기
 * 3) 성공했으면 바로 실행
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
            boolean locked = false; // 이 루프에서 lock을 실제로 획득했는지 표시

            try {
                // 1) 큐에서 작업 가져오기
                transfer = queue.take();
                if (transfer == null) {
                    break; // 종료 신호로 null을 쓰는 구조라고 가정
                }

                userId = transfer.getUserId();

                // 2) user 단위 lock
                //    먼저 즉시 시도해서 "대기 발생 여부"를 판별
                locked = lockManager.tryLock(userId);
                
                if (locked) {
                	log.info("거래 {} (고객 {}) userLock 획득",
                			transfer.getTransferId(), userId);
                }

                if (!locked) {
                    // 진짜로 "이미 누가 잡고 있어서 기다려야 하는 상황"에서만 찍힘
                    log.info("거래 {} (고객 {}) userLock 실패 -> userLock 얻을 때까지 대기",
                            transfer.getTransferId(), userId);

                    // 여기서 블로킹(대기). 대기가 끝나 lock을 얻으면 다음으로 진행
                    lockManager.lock(userId);
                    log.info("거래 {} (고객 {}) userLock 획득",
                			transfer.getTransferId(), userId);
                    locked = true;
                }


                try {
                    // 3) 실행
                    log.info("거래 {} (고객 {}) 실행 시작", transfer.getTransferId(), userId);

                    simulateTransfer();
                    transfer.markDone();

                    log.info("거래 {} (고객 {}) 상태 변경: PREPARING -> DONE",
                            transfer.getTransferId(), userId);

                } finally {
                    // 4) 반드시 lock 해제 (lock을 잡았을 때만)
                    if (locked) {
                        lockManager.unlock(userId);
                    }
                }

            } catch (InterruptedException e) {
                // take(), lock(), sleep()에서 인터럽트가 걸리면 종료
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
