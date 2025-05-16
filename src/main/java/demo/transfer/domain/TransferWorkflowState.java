package demo.transfer.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record TransferWorkflowState(Transfer transfer, Status status) {

  public TransferWorkflowState(Transfer transfer) {
    this(transfer, Status.CREATED);
  }

  public enum Status {
    CREATED,
    INITIATED,
    PAUSED,
    CANCELLED,
    COMPLETED
  }

  public TransferWorkflowState initiated() {
    return new TransferWorkflowState(transfer, Status.INITIATED);
  }

  public TransferWorkflowState completed() {
    return new TransferWorkflowState(transfer, Status.COMPLETED);
  }

  public TransferWorkflowState cancelled() {
    return new TransferWorkflowState(transfer, Status.CANCELLED);
  }

  public TransferWorkflowState paused() {
    return new TransferWorkflowState(transfer, Status.PAUSED);
  }

  @JsonIgnore
  public boolean isPaused() {
    return status == Status.PAUSED;
  }
}
