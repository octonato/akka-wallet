package demo.wallet.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record Wallet(long balance,
                     List<String> executedTransactions,
                     Map<String, Transaction> pendingTransactions) {

  public Wallet(long balance) {
    this(balance, List.of(), Map.of());
  }

  private Wallet addPendingTransaction(Transaction transaction) {
    var tmpMap = new HashMap<>(pendingTransactions);
    tmpMap.put(transaction.transactionId, transaction);
    return new Wallet(balance, executedTransactions, Map.copyOf(tmpMap));
  }

  private Wallet removePendingTransaction(String transactionId) {
    var tmpMap = new HashMap<>(pendingTransactions);
    tmpMap.remove(transactionId);
    return new Wallet(balance, executedTransactions, Map.copyOf(tmpMap));
  }

  public boolean alreadySeen(String transactionId) {
    return executedTransactions.contains(transactionId) || isPendingTransaction(transactionId);
  }

  public boolean isPendingTransaction(String transactionId) {
    return pendingTransactions.containsKey(transactionId);
  }

  public Transaction getTransaction(String transactionId) {
    return pendingTransactions.get(transactionId);
  }

  private Wallet addExecutedTransaction(String txId) {
    var tmpList = new ArrayList<>(executedTransactions);
    tmpList.add(txId);
    return new Wallet(balance, List.copyOf(tmpList), pendingTransactions);
  }

  private Wallet removeExecutedTransaction(String transactionId) {
    var otherTx =
      executedTransactions.stream()
        .filter(t -> !t.equals(transactionId))
        .toList();
    return new Wallet(balance, otherTx, pendingTransactions);
  }

  public Wallet addPendingDeposit(long amount, String txId) {
    return addPendingTransaction(new Transaction(amount, txId, Transaction.TransactionType.DEPOSIT));
  }

  public Wallet addPendingWithdraw(long amount, String txId) {
    return decreaseBalance(amount).
      addPendingTransaction(new Transaction(amount, txId, Transaction.TransactionType.WITHDRAW));
  }

  public Wallet cancelTransaction(String txId) {

    // first find the transaction
    var transaction = pendingTransactions.get(txId);

    if (transaction != null) {
      if (transaction.isWithdraw()) {
        // 'unreserve' funds if it's a withdrawn
        // remove the transaction from pending
        return increaseBalance(transaction.amount).removePendingTransaction(txId);
      } else {
        return removePendingTransaction(txId);
      }
    } else {
      return this;
    }
  }

  public Wallet completeTransaction(String txId) {
    return removeExecutedTransaction(txId);
  }

  public long reservedFunds() {
    return pendingTransactions.values().stream()
      .filter(Transaction::isWithdraw)
      .mapToLong(Transaction::amount)
      .sum();
  }

  public boolean isExecutedTransaction(String transactionId) {
    return executedTransactions.contains(transactionId);
  }

  public Wallet executeDeposit(long amount, String txId) {
    if (isPendingTransaction(txId)) {
      return increaseBalance(amount).removePendingTransaction(txId).addExecutedTransaction(txId);
    } else {
      return this;
    }
  }

  public Wallet executeWithdraw(long amount, String txId) {
    if (isPendingTransaction(txId)) {
      // note: we don't need to decrease balance as it was already done when the transaction was created
      return removePendingTransaction(txId).addExecutedTransaction(txId);
    } else {
      return this;
    }
  }


  public record Transaction(long amount, String transactionId, TransactionType type) {
    public enum TransactionType {
      DEPOSIT,
      WITHDRAW
    }

    public boolean isWithdraw() {
      return type == TransactionType.WITHDRAW;
    }

    public boolean isDeposit() {
      return !isWithdraw();
    }
  }

  public Wallet increaseBalance(long amount) {
    return new Wallet(balance + amount);
  }

  public Wallet decreaseBalance(long amount) {
    return new Wallet(balance - amount);
  }
}
