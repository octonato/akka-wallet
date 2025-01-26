package demo.transfer.domain;

import java.util.List;

public record TransferStatus(String transferId, List<Participant> participants, TransferState.Status status) {
  public static TransferStatus of(TransferState state) {
    return new TransferStatus(state.transferId(), List.copyOf(state.participants().values()), state.status());
  }
}
