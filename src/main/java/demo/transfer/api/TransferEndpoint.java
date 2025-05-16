package demo.transfer.api;

import akka.Done;
import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpResponses;
import demo.transfer.application.TransferMediatorEntity;
import demo.transfer.application.TransferWorkflow;
import demo.transfer.domain.Create;
import demo.transfer.domain.Transfer;
import demo.transfer.domain.TransferStatus;
import demo.transfer.domain.TransferWorkflowState;
import demo.wallet.application.WalletEntity;
import demo.wallet.domain.DepositCommand;
import demo.wallet.domain.WithdrawCommand;

// Opened up for access from the public internet to make the service easy to try out.
// For actual services meant for production this must be carefully considered and often set more
// limited
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@HttpEndpoint("/transfer")
public class TransferEndpoint {

  private final ComponentClient componentClient;

  public TransferEndpoint(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  @Get("/{transferId}")
  public TransferStatus transfer(String transferId) {
    var prefixedTransferId = TransferId.prefixForMediator(transferId);
    return componentClient
        .forEventSourcedEntity(prefixedTransferId)
        .method(TransferMediatorEntity::getState)
        .invoke();
  }

  @Post("/{transferId}")
  public HttpResponse transfer(String transferId, TransferRequest request) {

    var createTxCmd = Create.of(request.from(), request.to());
    var prefixedTransferId = TransferId.prefixForMediator(transferId);

    // first create a transfer
    componentClient
        .forEventSourcedEntity(prefixedTransferId)
        .method(TransferMediatorEntity::init)
        .invoke(createTxCmd);

    // then withdraw from the sender
    componentClient
        .forEventSourcedEntity(request.from())
        .method(WalletEntity::withdraw)
        .invoke(new WithdrawCommand(request.amount(), prefixedTransferId));

    // then deposit to the receiver
    componentClient
        .forEventSourcedEntity(request.to())
        .method(WalletEntity::deposit)
        .invoke(new DepositCommand(request.amount(), prefixedTransferId));

    return HttpResponses.ok(transferId);
  }

  @Post("/{transferId}/workflow")
  public Done transferWorkflow(String transferId, TransferRequest request) {

    var prefixedTransferId = TransferId.prefixForWorkflow(transferId);
    var transfer = new Transfer(request.amount(), request.from(), request.to());
    return componentClient
        .forWorkflow(prefixedTransferId)
        .method(TransferWorkflow::startTransfer)
        .invoke(transfer);
  }

  @Get("/{transferId}/workflow")
  public TransferWorkflowState getWorkflowState(String transferId) {

    var prefixedTransferId = TransferId.prefixForWorkflow(transferId);
    return componentClient
        .forWorkflow(prefixedTransferId)
        .method(TransferWorkflow::getState)
        .invoke();
  }
}
