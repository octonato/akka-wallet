package demo.transfer.api;

import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpResponses;
import demo.transfer.application.TransferMediatorEntity;
import demo.transfer.domain.Create;
import demo.transfer.domain.TransferStatus;
import demo.wallet.application.WalletEntity;
import demo.wallet.domain.DepositCommand;
import demo.wallet.domain.WithdrawCommand;

import java.util.concurrent.CompletionStage;

// Opened up for access from the public internet to make the service easy to try out.
// For actual services meant for production this must be carefully considered, and often set more limited
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@HttpEndpoint("/transfer")
public class TransferEndpoint {

  final private ComponentClient componentClient;

  public TransferEndpoint(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  @Get("/{transferId}")
  public CompletionStage<TransferStatus> transfer(String transferId) {
    return componentClient
      .forEventSourcedEntity(transferId)
      .method(TransferMediatorEntity::getState)
      .invokeAsync();
  }

  @Post("/{transferId}")
  public CompletionStage<HttpResponse> transfer(String transferId, TransferRequest request) {

    var createTxCmd = Create.of(request.from(), request.to());
    return
      // first create a transfer
      componentClient
        .forEventSourcedEntity(transferId)
        .method(TransferMediatorEntity::init).invokeAsync(createTxCmd)
        .thenApply(HttpResponses::ok)

        // then withdraw from the sender
        .thenCompose(res ->
          componentClient.forEventSourcedEntity(request.from())
            .method(WalletEntity::withdraw).invokeAsync(new WithdrawCommand(request.amount(), transferId))
            .thenApply(__ -> res)
        )
        // then deposit to the receiver
        .thenCompose(res ->
          componentClient
            .forEventSourcedEntity(request.to())
            .method(WalletEntity::deposit).invokeAsync(new DepositCommand(request.amount(), transferId))
            .thenApply(__ -> res)
        );

  }
}
