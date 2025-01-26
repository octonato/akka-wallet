package demo.transfer.application.consumers;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.consumer.Consumer;
import demo.transfer.application.TransferMediatorEntity;
import demo.transfer.domain.TransferEvent;
import demo.wallet.application.WalletEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@ComponentId("transaction-to-wallet")
@Consume.FromEventSourcedEntity(TransferMediatorEntity.class)
public class TransferToWalletConsumer extends Consumer {


  final private Logger logger = LoggerFactory.getLogger(getClass());
  final private ComponentClient componentClient;

  public TransferToWalletConsumer(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public Effect onCreated(TransferEvent event) {

    return switch (event) {
      // only react on those three events
      case TransferEvent.Initiated evt -> initiate(evt);
      case TransferEvent.Completed evt -> complete(evt);
      case TransferEvent.Cancelled evt -> cancel(evt);
      default -> effects().done();
    };
  }

  private Effect allOrNothing(List<CompletionStage<Done>> allFutures) {
    return effects().asyncDone(CompletableFuture
      .allOf(allFutures.toArray(CompletableFuture[]::new))
      .thenApply(__ -> Done.getInstance()));
  }

  private Effect initiate(TransferEvent.Initiated evt) {
    logger.info("Executing transaction [{}]", evt.transferId());

    var allFutures =
      evt.participantsIds().stream()
        .map(walletId ->
          componentClient.forEventSourcedEntity(walletId)
          .method(WalletEntity::executeTransaction)
          .invokeAsync(evt.transferId())).toList();

    return allOrNothing(allFutures);
  }

  private Effect complete(TransferEvent.Completed evt) {
    logger.info("Completing transaction [{}]", evt.transferId());

    var allFutures =
      evt.participantsIds().stream()
        .map(walletId ->
          componentClient.forEventSourcedEntity(walletId)
            .method(WalletEntity::completeTransaction)
            .invokeAsync(evt.transferId())).toList();

    return allOrNothing(allFutures);
  }

  private Effect cancel(TransferEvent.Cancelled evt) {

    logger.info("Cancelling transaction [{}]", evt.transferId());

    var allFutures =
      evt.participantsIds().stream()
        .map(walletId ->
          componentClient.forEventSourcedEntity(walletId)
            .method(WalletEntity::cancelTransaction)
            .invokeAsync(evt.transferId())).toList();

    return allOrNothing(allFutures);
  }
}
