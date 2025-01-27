package demo.wallet.application.consumers;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.consumer.Consumer;
import demo.transfer.api.TransferId;
import demo.transfer.application.TransferMediatorEntity;
import demo.wallet.application.WalletEntity;
import demo.wallet.domain.WalletEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ComponentId("wallet-to-transfer")
@Consume.FromEventSourcedEntity(WalletEntity.class)
public class WalletToTransferMediatorConsumer extends Consumer {

  final private Logger logger = LoggerFactory.getLogger(getClass());

  final private ComponentClient componentClient;

  public WalletToTransferMediatorConsumer(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public Effect onEvent(WalletEvent event) {

    if (messageContext().eventSubject().isEmpty()) {
      return effects().done();

    } else {
      var walletId = messageContext().eventSubject().get();
      logger.info("Received event [{}] from wallet [{}]", event, walletId);
      return switch (event) {
        case WalletEvent.DepositInitiated evt -> join(evt.transactionId(), walletId);
        case WalletEvent.WithdrawInitiated evt -> join(evt.transactionId(), walletId);
        case WalletEvent.Deposited evt -> execute(evt.transactionId(), walletId);
        case WalletEvent.Withdrawn evt -> execute(evt.transactionId(), walletId);
        default -> effects().done();
      };
    }
  }

  private Effect join(String transactionId, String walletId) {
    if (TransferId.isMediatorId(transactionId)) {
    var done =
      componentClient
        .forEventSourcedEntity(transactionId)
        .method(TransferMediatorEntity::participantJoined)
        .invokeAsync(walletId);
      return effects().asyncDone(done);
    }
    // a wallet can participate in transfers that are not managed by the TransferMediatorEntity
    // in such case we just ignore the event
    return effects().ignore();
  }

  private Effect execute(String transactionId, String walletId) {
    if (TransferId.isMediatorId(transactionId)) {
      var done =
        componentClient
          .forEventSourcedEntity(transactionId)
          .method(TransferMediatorEntity::confirmExecution)
          .invokeAsync(walletId);

      return effects().asyncDone(done);
    }
    // a wallet can participate in transfers that are not managed by the TransferMediatorEntity
    // in such case we just ignore the event
    return effects().ignore();
  }
}
