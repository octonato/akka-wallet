package demo.wallet.application.consumers;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.annotations.Produce;
import akka.javasdk.consumer.Consumer;
import demo.wallet.application.WalletEntity;
import demo.wallet.domain.WalletEvent;

@ComponentId("wallet-event-stream")
@Consume.FromEventSourcedEntity(WalletEntity.class)
@Produce.ServiceStream(id = "wallet-events")
public class WalletEventStream extends Consumer {

  public Effect onEvent(WalletEvent event) {
    if (messageContext().eventSubject().isPresent()) {
      var walletId = messageContext().eventSubject().get();
      var publishedEvent = WalletEventConverter.convert(walletId, event);
      return effects().produce(publishedEvent);
    } else {
      // should not happen, event subject is always present
      return effects().ignore();
    }
  }
}
