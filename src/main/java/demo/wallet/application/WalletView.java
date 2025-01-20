package demo.wallet.application;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.annotations.Query;
import akka.javasdk.view.TableUpdater;
import akka.javasdk.view.View;
import demo.wallet.api.WalletBalance;
import demo.wallet.api.WalletsList;
import demo.wallet.domain.WalletEvent;

@ComponentId("wallet-view")
public class WalletView extends View {

  @Query("SELECT * as wallets FROM wallet_view WHERE balance > :amount")
  public View.QueryEffect<WalletsList> getWallets(long amount) {
    return queryResult();
  }

  @Consume.FromEventSourcedEntity(WalletEntity.class)
  public static class WalletsByBalance extends TableUpdater<WalletBalance> {

    public Effect<WalletBalance> onEvent(WalletEvent event) {
      if (updateContext().eventSubject().isEmpty()) {
        return effects().ignore();
      } else {
        var walletId = updateContext().eventSubject().get();
      return switch (event) {
        case WalletEvent.WalletCreated evt -> effects().updateRow(new WalletBalance(walletId, 0));

        case WalletEvent.Deposited evt -> effects().updateRow(rowState().increase(evt.amount()));

        case WalletEvent.Withdrawn evt -> effects().updateRow(rowState().decrease(evt.amount()));
      };
      }
    }
  }
}
