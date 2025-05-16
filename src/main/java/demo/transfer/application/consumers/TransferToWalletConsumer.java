package demo.transfer.application.consumers;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.consumer.Consumer;
import demo.transfer.application.TransferMediatorEntity;
import demo.transfer.domain.TransferEvent;
import demo.wallet.application.WalletEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ComponentId("transaction-to-wallet")
@Consume.FromEventSourcedEntity(TransferMediatorEntity.class)
public class TransferToWalletConsumer extends Consumer {

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final ComponentClient componentClient;

  public TransferToWalletConsumer(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public Effect onCreated(TransferEvent event) {

    return switch (event) {
        // only react to those three events
      case TransferEvent.Initiated evt -> initiate(evt);
      case TransferEvent.Completed evt -> complete(evt);
      case TransferEvent.Cancelled evt -> cancel(evt);
      default -> effects().done();
    };
  }

  private Effect initiate(TransferEvent.Initiated evt) {
    logger.info("Executing transaction [{}]", evt.transferId());

    evt.participantsIds()
        .forEach(
            walletId ->
                componentClient
                    .forEventSourcedEntity(walletId)
                    .method(WalletEntity::executeTransaction)
                    .invoke(evt.transferId()));

    return effects().done();
  }

  private Effect complete(TransferEvent.Completed evt) {
    logger.info("Completing transaction [{}]", evt.transferId());

    evt.participantsIds()
        .forEach(
            walletId ->
                componentClient
                    .forEventSourcedEntity(walletId)
                    .method(WalletEntity::completeTransaction)
                    .invoke(evt.transferId()));

    return effects().done();
  }

  private Effect cancel(TransferEvent.Cancelled evt) {

    logger.info("Cancelling transaction [{}]", evt.transferId());

    evt.participantsIds()
        .forEach(
            walletId ->
                componentClient
                    .forEventSourcedEntity(walletId)
                    .method(WalletEntity::cancelTransaction)
                    .invoke(evt.transferId()));

    return effects().done();
  }
}
