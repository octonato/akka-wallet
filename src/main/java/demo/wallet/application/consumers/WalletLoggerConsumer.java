package demo.wallet.application.consumers;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.consumer.Consumer;
import demo.wallet.application.WalletEntity;
import demo.wallet.domain.WalletEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ComponentId("wallet-logger")
@Consume.FromEventSourcedEntity(WalletEntity.class)
public class WalletLoggerConsumer extends Consumer {

  private Logger logger = LoggerFactory.getLogger(WalletLoggerConsumer.class);

  public Effect onEvent(WalletEvent event) {
    logger.info("Received event: {}", event);
    return effects().done();
  }

}
