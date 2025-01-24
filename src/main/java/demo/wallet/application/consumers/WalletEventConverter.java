package demo.wallet.application.consumers;

import demo.wallet.api.PublicWalletEvent;
import demo.wallet.domain.WalletEvent;

public class WalletEventConverter {

  public static PublicWalletEvent convert(String walletId, WalletEvent event) {
    return switch (event) {
      case WalletEvent.WalletCreated created -> new PublicWalletEvent.WalletCreated(walletId);
      case WalletEvent.Deposited deposited -> new PublicWalletEvent.Deposited(walletId, deposited.amount());
      case WalletEvent.Withdrawn withdrawn -> new PublicWalletEvent.Withdrawn(walletId, withdrawn.amount());
    };
  }
}
