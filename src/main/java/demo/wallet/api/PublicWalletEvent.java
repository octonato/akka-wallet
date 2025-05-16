package demo.wallet.api;

import akka.javasdk.annotations.TypeName;

public sealed interface PublicWalletEvent {
  @TypeName("wallet-created")
  record WalletCreated(String walletId) implements PublicWalletEvent {}

  @TypeName("balance-increased")
  record Deposited(String walletId, long amount) implements PublicWalletEvent {}

  @TypeName("balance-decreased")
  record Withdrawn(String walletId, long amount) implements PublicWalletEvent {}
}
