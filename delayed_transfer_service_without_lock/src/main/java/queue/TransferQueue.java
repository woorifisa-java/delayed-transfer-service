package queue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import domain.Transfer;

public class TransferQueue {
	private final BlockingQueue<Transfer> queue = new LinkedBlockingQueue<>();

	public void put(Transfer transfer) {
		try {
			queue.put(transfer);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public Transfer take() {
		Transfer transfer = null;
		
		try {
			transfer = queue.take();
		} catch (InterruptedException e) {
			return null;
		}
		
		return transfer;
	}

	public int size() {
		return queue.size();
	}
}
