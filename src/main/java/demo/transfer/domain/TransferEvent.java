package demo.transfer.domain;

import akka.javasdk.annotations.TypeName;
import java.util.List;
import java.util.Set;

public sealed interface TransferEvent {

  @TypeName("created")
  record Created(String transferId, List<Participant> participants) implements TransferEvent {}

  @TypeName("participant-joined")
  record ParticipantJoined(String transferId, String participantId) implements TransferEvent {}

  @TypeName("participant-executed")
  record ParticipantExecuted(String transferId, String participantId) implements TransferEvent {}

  @TypeName("initiated")
  record Initiated(String transferId, Set<String> participantsIds) implements TransferEvent {}

  @TypeName("completed")
  record Completed(String transferId, Set<String> participantsIds) implements TransferEvent {}

  @TypeName("cancelled")
  record Cancelled(String transferId, Set<String> participantsIds) implements TransferEvent {}
}
