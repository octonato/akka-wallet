package demo.wallet.api;

import akka.javasdk.annotations.TypeName;

public sealed interface PublicWalletEvent {
  @TypeName("wallet-created-pub")
  record WalletCreated(String walletId) implements PublicWalletEvent {}

  @TypeName("balance-increased-pub")
  record Deposited(String walletId, long amount) implements PublicWalletEvent {}

  @TypeName("balance-decreased-pub")
  record Withdrawn(String walletId, long amount) implements PublicWalletEvent {}
}
