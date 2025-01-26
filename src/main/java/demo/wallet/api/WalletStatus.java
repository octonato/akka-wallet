package demo.wallet.api;

import demo.wallet.domain.Wallet;

import java.util.List;

public record WalletStatus( String walletId,
  long balance,
                           long reservedFunds,
                           List<Transaction> pendingTransactions) {

  record Transaction(String transactionId, long amount, String type) {
  }
  public static WalletStatus of(String walletId, Wallet wallet) {

    var tx =
      wallet.pendingTransactions().values().stream()
        .map(pt -> new Transaction(pt.transactionId(), pt.amount(), pt.type().toString()))
        .toList();
    return new WalletStatus(walletId, wallet.balance(), wallet.reservedFunds(), tx);
  }
}
