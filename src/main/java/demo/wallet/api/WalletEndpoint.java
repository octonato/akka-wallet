package demo.wallet.api;

import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpResponses;
import demo.transfer.api.TransferId;
import demo.transfer.application.TransferMediatorEntity;
import demo.transfer.domain.Create;
import demo.wallet.application.WalletEntity;
import demo.wallet.application.WalletView;
import demo.wallet.domain.DepositCommand;
import demo.wallet.domain.WithdrawCommand;
import java.util.concurrent.CompletionStage;

// Opened up for access from the public internet to make the service easy to try out.
// For actual services meant for production this must be carefully considered, and often set more
// limited
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@HttpEndpoint("/wallet")
public class WalletEndpoint {

  private final ComponentClient componentClient;

  public WalletEndpoint(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  @Get("/{walletId}")
  public CompletionStage<WalletStatus> state(String walletId) {
    return componentClient
        .forEventSourcedEntity(walletId)
        .method(WalletEntity::getState)
        .invokeAsync()
        .thenApply(wallet -> WalletStatus.of(walletId, wallet));
  }

  @Get("/{walletId}/balance")
  public CompletionStage<WalletBalance> balance(String walletId) {
    return componentClient
        .forEventSourcedEntity(walletId)
        .method(WalletEntity::getBalance)
        .invokeAsync()
        .thenApply(balance -> new WalletBalance(walletId, balance));
  }

  @Get("/balance/higher-than/{amount}")
  public CompletionStage<WalletsList> findBalanceHigherThan(long amount) {
    return componentClient.forView().method(WalletView::getWallets).invokeAsync(amount);
  }

  @Post("/{walletId}/create")
  public CompletionStage<WalletStatus> create(String walletId) {
    return componentClient
        .forEventSourcedEntity(walletId)
        .method(WalletEntity::create)
        .invokeAsync()
        .thenApply(wallet -> WalletStatus.of(walletId, wallet));
  }

  @Post("/{walletId}/deposit")
  public CompletionStage<HttpResponse> deposit(String walletId, DepositRequest request) {

    var prefixedTransferId = TransferId.prefixForMediator(request.transactionId());
    var transactionStatus =
        componentClient
            .forEventSourcedEntity(prefixedTransferId)
            .method(TransferMediatorEntity::init)
            .invokeAsync(Create.of(walletId));

    return transactionStatus.thenCompose(
        txStatus ->
            componentClient
                .forEventSourcedEntity(walletId)
                .method(WalletEntity::deposit)
                .invokeAsync(new DepositCommand(request.amount(), prefixedTransferId))
                .thenApply(
                    wallet -> {
                      var walletStatus = WalletStatus.of(walletId, wallet);
                      return HttpResponses.ok(walletStatus);
                    }));
  }

  @Post("/{walletId}/withdraw")
  public CompletionStage<HttpResponse> withdraw(String walletId, WithdrawRequest request) {

    var prefixedTransferId = TransferId.prefixForMediator(request.transactionId());
    var transactionStatus =
        componentClient
            .forEventSourcedEntity(prefixedTransferId)
            .method(TransferMediatorEntity::init)
            .invokeAsync(Create.of(walletId));

    return transactionStatus.thenCompose(
        txStatus -> {
          return componentClient
              .forEventSourcedEntity(walletId)
              .method(WalletEntity::withdraw)
              .invokeAsync(new WithdrawCommand(request.amount(), prefixedTransferId))
              .thenApply(
                  wallet -> {
                    var walletStatus = WalletStatus.of(walletId, wallet);
                    return HttpResponses.ok(walletStatus);
                  });
        });
  }
}
