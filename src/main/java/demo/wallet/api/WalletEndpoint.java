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

// Opened up for access from the public internet to make the service easy to try out.
// For actual services meant for production this must be carefully considered and often set more
// limited
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@HttpEndpoint("/wallet")
public class WalletEndpoint {

  private final ComponentClient componentClient;

  public WalletEndpoint(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  @Get("/{walletId}")
  public WalletStatus state(String walletId) {

    var wallet =
        componentClient.forEventSourcedEntity(walletId).method(WalletEntity::getState).invoke();

    return WalletStatus.of(walletId, wallet);
  }

  @Get("/{walletId}/balance")
  public WalletBalance balance(String walletId) {

    var balance =
        componentClient.forEventSourcedEntity(walletId).method(WalletEntity::getBalance).invoke();

    return new WalletBalance(walletId, balance);
  }

  @Get("/balance/higher-than/{amount}")
  public WalletsList findBalanceHigherThan(long amount) {
    return componentClient.forView().method(WalletView::getWallets).invoke(amount);
  }

  @Post("/{walletId}/create")
  public WalletStatus create(String walletId) {

    var wallet =
        componentClient.forEventSourcedEntity(walletId).method(WalletEntity::create).invoke();

    return WalletStatus.of(walletId, wallet);
  }

  @Post("/{walletId}/deposit")
  public HttpResponse deposit(String walletId, DepositRequest request) {

    var prefixedTransferId = TransferId.prefixForMediator(request.transactionId());
    componentClient
        .forEventSourcedEntity(prefixedTransferId)
        .method(TransferMediatorEntity::init)
        .invoke(Create.of(walletId));

    var wallet =
        componentClient
            .forEventSourcedEntity(walletId)
            .method(WalletEntity::deposit)
            .invoke(new DepositCommand(request.amount(), prefixedTransferId));

    var walletStatus = WalletStatus.of(walletId, wallet);
    return HttpResponses.ok(walletStatus);
  }

  @Post("/{walletId}/withdraw")
  public HttpResponse withdraw(String walletId, WithdrawRequest request) {

    var prefixedTransferId = TransferId.prefixForMediator(request.transactionId());

    componentClient
        .forEventSourcedEntity(prefixedTransferId)
        .method(TransferMediatorEntity::init)
        .invoke(Create.of(walletId));

    var wallet =
        componentClient
            .forEventSourcedEntity(walletId)
            .method(WalletEntity::withdraw)
            .invoke(new WithdrawCommand(request.amount(), prefixedTransferId));

    var walletStatus = WalletStatus.of(walletId, wallet);
    return HttpResponses.ok(walletStatus);
  }
}
