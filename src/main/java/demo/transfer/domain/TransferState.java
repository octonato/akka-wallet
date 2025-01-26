package demo.transfer.domain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static demo.transfer.domain.TransferState.Status.PENDING;

public record TransferState(String transferId, Map<String, Participant> participants, Status status) {

  // constructor accepting list of participants
    public TransferState(String transferId, List<Participant> participants) {
        this(
          transferId,
          participants.stream().collect(Collectors.toMap(Participant::id, Function.identity())),
          PENDING);
    }



  public enum Status {
    PENDING,
    INITIATED,
    CANCELLED,
    COMPLETED;

    public boolean isTerminated() {
      return this == CANCELLED || this == COMPLETED;
    }
  }


  public Set<String> allParticipantsIds() {
    return participants.keySet();
  }

  public TransferState initiate() {
    return new TransferState(transferId, participants, Status.INITIATED);
  }

  public boolean isPending() {
    return status == Status.PENDING;
  }

  public boolean isInProgress() {
    return status == Status.INITIATED;
  }

  public TransferState complete() {
    return new TransferState(transferId, participants, Status.COMPLETED);
  }

  public boolean isCompleted() {
    return status == Status.COMPLETED;
  }

  public TransferState cancel() {
    return new TransferState(transferId, participants, Status.CANCELLED);
  }

  public boolean isCancelled() {
    return status == Status.CANCELLED;
  }


  public TransferState participantJoined(String participantId) {
    var participant = participants.get(participantId);
    if (participant != null) {
      var newParticipant = new Participant(participant.id(), true, participant.executed());
      participants.put(participantId, newParticipant);
    }
    return this;
  }


  public boolean allJoined() {
    return allJoined(participants.values());
  }

  private boolean allJoined(Collection<Participant> participants) {
    return participants.stream().allMatch(Participant::joined);
  }
  public boolean isLastToJoin(String participantId) {
    var allOtherParticipants =  participants.values().stream().filter(p -> !p.id().equals(participantId));
    return allJoined(allOtherParticipants.toList());
  }

  public boolean isLastToExecute(String participantId) {
    var allOtherParticipants =  participants.values().stream().filter(p -> !p.id().equals(participantId));
    return allOtherParticipants.allMatch(Participant::executed);
  }

  public TransferState participantExecuted(String participantId) {
    var participant = participants.get(participantId);
    if (participant != null) {
      var newParticipant = new Participant(participant.id(), participant.joined(), true);
      participants.put(participantId, newParticipant);
    }
    return this;
  }

  public boolean hasJoined(String participantId) {
    var participant = participants.get(participantId);
    return participant != null && participant.joined();
  }

  public boolean hasExecuted(String participantId) {
    var participant = participants.get(participantId);
    return participant != null && participant.executed();
  }
}
