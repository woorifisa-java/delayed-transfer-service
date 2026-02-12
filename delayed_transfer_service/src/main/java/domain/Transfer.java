package domain;

public class Transfer {

    private final long transferId;
    private final long userId;
    private final long bankOpenAt;
    private TransferStatus status;

    public Transfer(long transferId, long userId, long bankOpenAt) {
        this.transferId = transferId;
        this.userId = userId;
        this.bankOpenAt = bankOpenAt;
        this.status = TransferStatus.DELAYED;
    }

    public long getTransferId() {
        return transferId;
    }

    public long getUserId() {
        return userId;
    }

    public long getBankOpenAt() {
        return bankOpenAt;
    }

    // 최신값 보장을 위한 synchronized
    public synchronized TransferStatus getStatus() {
        return status;
    }

    // Scheduler가 실행 대상으로 선택할 때 호출
    public synchronized boolean markPreparing() {
        if (status != TransferStatus.DELAYED) {
            return false;
        }
        status = TransferStatus.PREPARING;
        return true;
    }

    // Consumer가 실행 완료 후 호출
    public synchronized void markDone() {
        status = TransferStatus.DONE;
    }
}
