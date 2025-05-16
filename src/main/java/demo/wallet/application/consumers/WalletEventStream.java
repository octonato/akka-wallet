package demo.wallet.application.consumers;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.annotations.Produce;
import akka.javasdk.consumer.Consumer;
import demo.wallet.api.PublicWalletEvent;
import demo.wallet.application.WalletEntity;
import demo.wallet.domain.WalletEvent;

@ComponentId("wallet-event-stream")
@Consume.FromEventSourcedEntity(WalletEntity.class)
@Produce.ServiceStream(id = " wallet-events")
public class WalletEventStream extends Consumer {

  public Effect onEvent(WalletEvent event) {
    if (messageContext().eventSubject().isEmpty()) {
      return effects().ignore();
    } else {
      var walletId = messageContext().eventSubject().get();
      return switch (event) {
        case WalletEvent.WalletCreated __ ->
            effects().produce(new PublicWalletEvent.WalletCreated(walletId));
        case WalletEvent.Deposited evt ->
            effects().produce(new PublicWalletEvent.Deposited(walletId, evt.amount()));
        case WalletEvent.Withdrawn evt ->
            effects().produce(new PublicWalletEvent.Withdrawn(walletId, evt.amount()));
        default -> effects().ignore();
      };
    }
  }
}
