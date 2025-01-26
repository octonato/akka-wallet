package demo.wallet.domain;

import akka.javasdk.annotations.TypeName;

public sealed interface WalletEvent {

  @TypeName("wallet-created")
  record WalletCreated() implements WalletEvent {
  }

  @TypeName("deposit-initiated")
  record DepositInitiated(long amount, String transactionId) implements WalletEvent {
  }

  @TypeName("withdraw-initiated")
  record WithdrawInitiated(long amount, String transactionId) implements WalletEvent {
  }

  @TypeName("deposited")
  record Deposited(long amount, String transactionId) implements WalletEvent {
  }

  @TypeName("withdrawn")
  record Withdrawn(long amount, String transactionId) implements WalletEvent {
  }

  @TypeName("tx-cancelled")
  record TransactionCancelled(String transactionId) implements WalletEvent {
  }

  @TypeName("tx-completed")
  record TransactionCompleted(String transactionId) implements WalletEvent {
  }
}
