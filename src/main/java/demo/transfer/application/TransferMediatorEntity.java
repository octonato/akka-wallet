package demo.transfer.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.eventsourcedentity.EventSourcedEntity;
import akka.javasdk.eventsourcedentity.EventSourcedEntityContext;
import demo.transfer.api.TransferId;
import demo.transfer.domain.Create;
import demo.transfer.domain.Participant;
import demo.transfer.domain.TransferEvent;
import demo.transfer.domain.TransferState;
import demo.transfer.domain.TransferStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ComponentId("transfer-mediator")
public class TransferMediatorEntity extends EventSourcedEntity<TransferState, TransferEvent> {

  private Logger logger = LoggerFactory.getLogger(getClass());

  private final String transferId;

  private final Effect<Done> doneEffect = effects().reply(Done.getInstance());

  public TransferMediatorEntity(EventSourcedEntityContext context) {
    this.transferId = context.entityId();
  }

  public ReadOnlyEffect<TransferStatus> getState() {
    return effects().reply(TransferStatus.of(currentState()));
  }

  public Effect<TransferStatus> init(Create cmd) {

    if (!TransferId.isMediatorId(transferId)) {
      return effects().error("Transfer [" + transferId + "] is not a valid mediator id");
    }

    if (currentState() == null) {
      logger.info("Creating transfer: [{}] for [{}]", transferId, cmd);

      var participants = cmd.participants().stream().map(Participant::new).toList();
      return effects()
          .persist(new TransferEvent.Created(transferId, participants))
          .thenReply(TransferStatus::of);

    } else if (currentState().isCancelled()) {
      logger.info("Cancelled transfer: [{}]", transferId);
      return effects().error("Transfer [" + transferId + "] exists but was cancelled");

    } else if (currentState().isCompleted()) {
      logger.info("Completed transfer: [{}]", transferId);
      return effects().error("Transfer [" + transferId + "] exists but is already completed");

    } else if (currentState().isInProgress()) {
      return effects().error("Transfer [" + transferId + "] already in progress");

    } else {
      logger.info("Transfer already created: [{}]", transferId);
      return effects().reply(TransferStatus.of(currentState()));
    }
  }

  public Effect<Done> participantJoined(String participantId) {

    if (currentState().isCancelled()) {
      // needs to ignore the join as it can trigger an Initialized event
      // ultimately, the entity joining it will receive a cancel commands as well
      logger.info(
          "Joining after cancelling: transfer [{}], participant [{}]", transferId, participantId);
      return doneEffect;

    } else if (currentState().participants().containsKey(participantId)) {

      if (currentState().hasJoined(participantId)) {
        logger.info(
            "Participant already joined: transfer [{}], participant [{}]",
            transferId,
            participantId);
        // just ignore if already joined
        return doneEffect;

      } else {

        var joinedEvent = new TransferEvent.ParticipantJoined(transferId, participantId);
        logger.info(
            "Participant joined: transfer [{}], participant [{}]", transferId, participantId);
        // if last to join, we should also mark the transfer as initiated
        if (currentState().isLastToJoin(participantId)) {
          logger.info("All participants joined: transfer [{}]", transferId);

          var allParticipantsIds = currentState().allParticipantsIds();
          var initiatedEvent = new TransferEvent.Initiated(transferId, allParticipantsIds);

          return effects().persist(joinedEvent, initiatedEvent).thenReply(__ -> Done.getInstance());

        } else {
          return effects().persist(joinedEvent).thenReply(__ -> Done.getInstance());
        }
      }
    } else {
      // joined by unknown should be ignored to not block the flow
      return doneEffect;
    }
  }

  public Effect<Done> confirmExecution(String participantId) {

    if (currentState().isCancelled()) {
      logger.info(
          "Transfer [{}]: cancelled, but was executed by participant [{}]. THIS IS A BUG!",
          transferId,
          participantId);
      return doneEffect;

    } else if (currentState().participants().containsKey(participantId)) {

      var participant = currentState().participants().get(participantId);
      if (currentState().hasExecuted(participantId)) {
        logger.info("Transfer [{}]: participant [{}] already executed", transferId, participantId);
        // just ignore if already joined
        return doneEffect;

      } else {

        logger.info("Transfer [{}]: participant [{}] executed", transferId, participantId);

        var executedEvent = new TransferEvent.ParticipantExecuted(transferId, participant.id());
        // if last to join, we should also mark the transfer as completed
        if (currentState().isLastToExecute(participantId)) {
          logger.info("Transfer [{}]: all participants executed", transferId);

          var allParticipants = currentState().allParticipantsIds();
          var completedEvent = new TransferEvent.Completed(transferId, allParticipants);

          return effects()
              .persist(executedEvent, completedEvent)
              .thenReply(__ -> Done.getInstance());

        } else {
          return effects().persist(executedEvent).thenReply(__ -> Done.getInstance());
        }
      }
    } else {
      // confirmation by unknown should be ignored to not block the flow
      return doneEffect;
    }
  }

  public Effect<Done> cancel() {
    if (currentState() == null) {
      return doneEffect;

    } else if (currentState().isPending()) {
      var allParticipants = currentState().allParticipantsIds();
      return effects()
          .persist(new TransferEvent.Cancelled(transferId, allParticipants))
          .thenReply(__ -> Done.getInstance());
    } else {
      logger.info(
          "Attempt to cancel transfer [{}] with status [{}]. Transfer can't be cancelled.",
          transferId,
          currentState().status());
      return doneEffect;
    }
  }

  @Override
  public TransferState applyEvent(TransferEvent event) {
    return switch (event) {
      case TransferEvent.Created created ->
          new TransferState(created.transferId(), created.participants());

      case TransferEvent.ParticipantJoined evt ->
          currentState().participantJoined(evt.participantId());
      case TransferEvent.ParticipantExecuted evt ->
          currentState().participantExecuted(evt.participantId());

      case TransferEvent.Initiated evt -> currentState().initiate();
      case TransferEvent.Completed evt -> currentState().complete();
      case TransferEvent.Cancelled evt -> currentState().cancel();
    };
  }
}
