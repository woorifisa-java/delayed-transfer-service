package delayed_transfer_service;

import consumer.TransferConsumer;
import consumer.TransferConsumerRequeuing;
import lock.SimpleUserLockManager;
import lock.UserLockManager;
import queue.TransferQueue;
import repository.TransferRepository;
import scheduler.TransferScheduler;
import service.TransferCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
	public static final String RESET = "\u001B[0m";
	public static final String CYAN = "\u001B[36m";
	public static final String GREEN = "\u001B[32m";
	public static final String PURPLE = "\u001B[35m";

	
	private static final Logger log = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) throws InterruptedException {

		TransferRepository repository = new TransferRepository();
		TransferQueue queue = new TransferQueue();
		TransferCreator creator = new TransferCreator(repository);

		UserLockManager lockManager = new SimpleUserLockManager();

		// ------ 시나리오: 유저 3명 ------
		// userA: 3건, userB: 1건, userC: 1건
		Long userA = 1L;
		Long userB = 2L;
		Long userC = 3L;

		System.out.println("---------------------------------------------------------------------------------------------------");
		log.info("지연이체 요청 생성 시작");
		log.info("2초 후 은행 점검 끝\n---------------------------------------------------------------------------------------------------");

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

		log.info("요청 생성 완료 (userA: 3건, userB: 1건, userC: 1건)\n");

		// ------ 스케줄러 시작 ------
		// 200ms마다 실행 가능한 요청 찾아서 큐에 넣음
		TransferScheduler scheduler = new TransferScheduler(repository, queue, 200L);
		Thread schedulerThread = new Thread(scheduler, CYAN + "scheduler" + RESET);
		schedulerThread.start();

		// ------ 컨슈머 2개 시작 ------
		Thread consumer1 = new Thread(new TransferConsumer("consumer-1", queue, lockManager), GREEN + "consumer-1" + RESET);
		Thread consumer2 = new Thread(new TransferConsumer("consumer-2", queue, lockManager), PURPLE + "consumer-2" + RESET);

		// ------ 컨슈머 2개 시작 (requeuing) ------
//		Thread consumer1 = new Thread(new TransferConsumerRequeuing("consumer-1", queue, lockManager), GREEN + "consumer-1" + RESET);
//		Thread consumer2 = new Thread(new TransferConsumerRequeuing("consumer-2", queue, lockManager), PURPLE + "consumer-2" + RESET);

		
		consumer1.start();
		consumer2.start();

		// ------ 요청이 모두 처리될 시간을 주기 위한 대기 ------
		Thread.sleep(8000);

		// ------ 종료 처리 ------
		System.out.println("---------------------------------------------------------------------------------------------------");
		log.info("종료 처리 시작");

		scheduler.stop(); // 스케줄러 루프 종료
		schedulerThread.interrupt(); // 혹시 sleep 중이면 깨우기

		consumer1.interrupt(); // 컨슈머 종료
		consumer2.interrupt();

		// join으로 깔끔하게 종료 대기
		schedulerThread.join();
		consumer1.join();
		consumer2.join();

		log.info("지연 이체 종료\n---------------------------------------------------------------------------------------------------");
	}
}
