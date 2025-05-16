package demo.transfer.application.consumers;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.consumer.Consumer;
import akka.javasdk.timer.TimerScheduler;
import demo.transfer.application.TransferMediatorEntity;
import demo.transfer.domain.TransferEvent;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ComponentId("transaction-timeout")
@Consume.FromEventSourcedEntity(TransferMediatorEntity.class)
public class TransferTimeoutConsumer extends Consumer {

  private final ComponentClient componentClient;
  private final TimerScheduler timerScheduler;
  private final Logger logger = LoggerFactory.getLogger(getClass());

  public TransferTimeoutConsumer(ComponentClient componentClient, TimerScheduler timerScheduler) {
    this.componentClient = componentClient;
    this.timerScheduler = timerScheduler;
  }

  private String genTimerId(String transferId) {
    return "timeout-transfer-timer:" + transferId;
  }

  public Effect onEvent(TransferEvent event) {
    return switch (event) {
      case TransferEvent.Created evt -> {
        String timerId = genTimerId(evt.transferId());
        logger.info(
            "Scheduling cancellation for transfer [{}], timer id [{}]", evt.transferId(), timerId);
        var cancellationCall =
            componentClient
                .forEventSourcedEntity(evt.transferId())
                .method(TransferMediatorEntity::cancel)
                .deferred();

        var scheduledCancellation =
            timerScheduler.startSingleTimer(timerId, Duration.ofSeconds(20), cancellationCall);

        yield effects().asyncDone(scheduledCancellation);
      }

      case TransferEvent.Initiated evt -> cancelTimer(evt.transferId());
      case TransferEvent.Completed evt -> cancelTimer(evt.transferId());
      case TransferEvent.Cancelled evt -> cancelTimer(evt.transferId());

      default -> effects().ignore();
    };
  }

  private Effect cancelTimer(String txId) {
    var timerId = genTimerId(txId);
    logger.info("Cancelling transfer timer [{}]", timerId);
    return effects().asyncDone(timerScheduler.cancel(timerId));
  }
}
