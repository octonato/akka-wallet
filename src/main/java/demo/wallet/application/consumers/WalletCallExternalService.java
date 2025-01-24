package demo.wallet.application.consumers;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.consumer.Consumer;
import demo.wallet.application.WalletEntity;
import demo.wallet.domain.WalletEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;


@ComponentId("wallet-events-to-external")
@Consume.FromEventSourcedEntity(WalletEntity.class)
public class WalletCallExternalService extends Consumer {

  private Logger logger = LoggerFactory.getLogger(WalletLoggerConsumer.class);
  public Effect onEvent(WalletEvent event) {
    return effects().asyncDone(makeCallToOtherService(event));
  }

  private CompletionStage<Done> makeCallToOtherService(WalletEvent event) {
    logger.info("making a call to fake external service: {}", event);
    return CompletableFuture.completedFuture(Done.getInstance());
  }
}
