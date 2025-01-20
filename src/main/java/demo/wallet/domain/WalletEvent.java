package demo.wallet.domain;

import akka.javasdk.annotations.TypeName;

public sealed interface WalletEvent {

  @TypeName("wallet-created")
  record WalletCreated() implements WalletEvent {
  }

  @TypeName("balance-increased")
  record Deposited(long amount) implements WalletEvent {
  }

  @TypeName("balance-decreased")
  record Withdrawn(long amount) implements WalletEvent {
  }
}
