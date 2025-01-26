package demo.transfer.domain;

public record TransferWorkflowState(Transfer transfer, Status status) {



  public enum Status {
    INITIATED,
    WITHDRAW_EXECUTED,
    DEPOSIT_EXECUTED,
    CANCELLED,
    COMPLETED
  }

  public boolean isWithdrawExecuted() {
    return status == Status.WITHDRAW_EXECUTED;
  }

  public boolean isDepositExecuted() {
    return status == Status.DEPOSIT_EXECUTED;
  }

  public TransferWorkflowState complete() {
    return new TransferWorkflowState(transfer, Status.COMPLETED);
  }

  public TransferWorkflowState withdrawExecuted() {
    return new TransferWorkflowState(transfer, Status.WITHDRAW_EXECUTED);
  }

  public TransferWorkflowState depositExecuted() {
    return new TransferWorkflowState(transfer, Status.DEPOSIT_EXECUTED);
  }

  public TransferWorkflowState cancelled() {
    return new TransferWorkflowState(transfer, Status.CANCELLED);
  }

}