package delayed_transfer_service;

import consumer.TransferConsumer;
import consumer.TransferConsumerWithoutLock;
import lock.SimpleUserLockManager;
import lock.UserLockManager;
import queue.TransferQueue;
import repository.TransferRepository;
import scheduler.TransferScheduler;
import service.TransferCreator;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        // ====== 준비물 ======
        TransferRepository repository = new TransferRepository();
        TransferQueue queue = new TransferQueue();
        TransferCreator creator = new TransferCreator(repository);

        UserLockManager lockManager = new SimpleUserLockManager();

        // ====== 시나리오: 유저 3명 ======
        // userA: 3건, userB: 1건, userC: 1건
        Long userA = 1L;
        Long userB = 2L;
        Long userC = 3L;

        System.out.println("\n==============================");
        System.out.println("[메인] 지연이체 요청 생성 시작");
        System.out.println("==============================");

        // 은행 점검이 "2초 후" 끝난다고 가정 → 2초 뒤부터 실행 가능
        long delayToBankOpen = 2000L;

        // userA 3건
        creator.create(userA, delayToBankOpen);
        creator.create(userA, delayToBankOpen);
        creator.create(userA, delayToBankOpen);

        // userB 1건
        creator.create(userB, delayToBankOpen);

        // userC 1건
        creator.create(userC, delayToBankOpen);

        System.out.println("[메인] 요청 생성 완료 (userA:3건, userB:1건, userC:1건)\n");

        // ====== 스케줄러 시작 ======
        // 200ms마다 실행 가능한 요청 찾아서 큐에 넣음
        TransferScheduler scheduler = new TransferScheduler(repository, queue, 200L);
        Thread schedulerThread = new Thread(scheduler, "스케줄러-1");
        schedulerThread.start();

     // ====== 컨슈머 (withLock) 2개 시작 ======
//		Thread consumer1 = new Thread(new TransferConsumer("컨슈머-1", queue, lockManager), "컨슈머-1");
//		Thread consumer2 = new Thread(new TransferConsumer("컨슈머-2", queue, lockManager), "컨슈머-2");

		// ====== 컨슈머 (withoutLock) 2개 시작 ======
		Thread consumer1 = new Thread(new TransferConsumerWithoutLock("컨슈머-1", queue), "컨슈머-1");
		Thread consumer2 = new Thread(new TransferConsumerWithoutLock("컨슈머-2", queue), "컨슈머-2");

        consumer1.start();
        consumer2.start();

        // ====== 시연 시간 잠깐 대기 ======
        // (요청이 모두 처리될 시간을 줌)
        Thread.sleep(8000);

        // ====== 종료 처리 ======
        System.out.println("\n==============================");
        System.out.println("[메인] 종료 처리 시작");
        System.out.println("==============================");

        scheduler.stop();               // 스케줄러 루프 종료
        schedulerThread.interrupt();    // 혹시 sleep 중이면 깨우기

        consumer1.interrupt();          // 컨슈머 종료
        consumer2.interrupt();

        // join으로 깔끔하게 종료 대기
        schedulerThread.join();
        consumer1.join();
        consumer2.join();

        System.out.println("[메인] 프로그램 종료");
    }
}

