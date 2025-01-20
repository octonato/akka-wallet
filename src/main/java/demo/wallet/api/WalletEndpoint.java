package demo.wallet.api;

import akka.Done;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.client.ComponentClient;
import demo.wallet.application.WalletEntity;
import demo.wallet.application.WalletView;

import java.util.concurrent.CompletionStage;

// Opened up for access from the public internet to make the service easy to try out.
// For actual services meant for production this must be carefully considered, and often set more limited
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@HttpEndpoint("/wallet")
public class WalletEndpoint {


  private final ComponentClient componentClient;

  public WalletEndpoint(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  @Get("/{id}")
  public CompletionStage<WalletBalance> balance(String id) {
    return componentClient
      .forEventSourcedEntity(id)
      .method(WalletEntity::balance)
      .invokeAsync()
      .thenApply(balance -> new WalletBalance(id, balance));
  }

  @Get("/balance/higher-than/{amount}")
  public CompletionStage<WalletsList> findBalanceHigherThan(long amount) {
    return componentClient
      .forView().method(WalletView::getWallets)
      .invokeAsync(amount);
  }

  @Post("/{id}/create")
  public CompletionStage<Done> create(String id) {
    return componentClient
      .forEventSourcedEntity(id)
      .method(WalletEntity::create)
      .invokeAsync();
  }

  @Post("/{id}/create/{amount}")
  public CompletionStage<Done> create(String id, long amount) {
    return componentClient
      .forEventSourcedEntity(id)
      .method(WalletEntity::createAndDeposit)
      .invokeAsync(amount);
  }

  @Post("/{id}/deposit/{amount}")
  public CompletionStage<Done> deposit(String id, long amount) {
    return componentClient
      .forEventSourcedEntity(id)
      .method(WalletEntity::deposit)
      .invokeAsync(amount);
  }

  @Post("/{id}/withdraw/{amount}")
  public CompletionStage<Done> withdraw(String id, long amount) {
    return componentClient
      .forEventSourcedEntity(id)
      .method(WalletEntity::withdraw)
      .invokeAsync(amount);
  }

}
