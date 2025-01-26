package demo.wallet.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.eventsourcedentity.EventSourcedEntity;
import demo.wallet.domain.DepositCommand;
import demo.wallet.domain.Wallet;
import demo.wallet.domain.WalletEvent;
import demo.wallet.domain.WithdrawCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.function.Function.identity;

@ComponentId("wallet")
public class WalletEntity extends EventSourcedEntity<Wallet, WalletEvent> {

  final private Logger logger = LoggerFactory.getLogger(getClass());

  final private Effect<Done> doneEffect = effects().reply(Done.getInstance());



  public Effect<Wallet> create() {
    if (currentState() == null)
    return effects()
      .persist(new WalletEvent.WalletCreated())
      .thenReply(identity());
    else
      return effects().reply(currentState());
  }


  private <T> ReadOnlyEffect<T> walletDoesNotExist() {
    return effects().error("Wallet [" + commandContext().entityId() + "] does not exist");
  }

  public Effect<Wallet> deposit(DepositCommand cmd) {

    if (currentState() == null) {
      return walletDoesNotExist();
    } else if (currentState().alreadySeen(cmd.transactionId())) {
      logger.info("Wallet [{}]: received deposit request, but transaction [{}] is already in progress",
        commandContext().entityId(),
        cmd.transactionId());
      return effects().reply(currentState());
    } else {
      logger.info("Wallet [{}]: received deposit request: {}", commandContext().entityId(), cmd);
      return effects()
        .persist(new WalletEvent.DepositInitiated(cmd.amount(), cmd.transactionId()))
        .thenReply(identity());
    }
  }

  public Effect<Wallet> withdraw(WithdrawCommand cmd) {
    if (currentState() == null) {
      return walletDoesNotExist();
    } else if (currentState().alreadySeen(cmd.transactionId())) {
      logger.info("Wallet [{}]: received withdraw request, but transaction [{}] is already in progress",
        commandContext().entityId(),
        cmd.transactionId());
      return effects().reply(currentState());
    } else if (currentState().balance() < cmd.amount()) {
      return effects().error("Insufficient balance");
    } else {
      logger.info("Wallet [{}]: received withdraw request: {}", commandContext().entityId(), cmd);
      return effects()
        .persist(new WalletEvent.WithdrawInitiated(cmd.amount(), cmd.transactionId()))
        .thenReply(identity());
    }
  }

  public ReadOnlyEffect<Long> getBalance() {
    if (currentState() == null) return walletDoesNotExist();
    else return effects().reply(currentState().balance());
  }

  public ReadOnlyEffect<Wallet> getState() {
    if (currentState() == null) return walletDoesNotExist();
    else return effects().reply(currentState());
  }

  public Effect<Done> executeTransaction(String transactionId) {

    if (currentState().isPendingTransaction(transactionId)) {
      var transaction = currentState().getTransaction(transactionId);
      var walletId = commandContext().entityId();
      logger.info("Wallet [{}]: transaction [{}] executed", walletId, transaction);

      if (transaction.isDeposit()) {
        return effects()
          .persist(new WalletEvent.Deposited(transaction.amount(), transaction.transactionId()))
          .thenReply(__ -> Done.getInstance());
      } else {
        return effects()
          .persist(new WalletEvent.Withdrawn(transaction.amount(), transaction.transactionId()))
          .thenReply(__ -> Done.getInstance());
      }
    }

    return doneEffect;
  }

  public Effect<Done> completeTransaction(String transactionId) {
    var walletId = commandContext().entityId();

    if (currentState().isExecutedTransaction(transactionId)) {
      logger.info("Wallet [{}]: transaction [{}] completed", walletId, transactionId);
      return effects()
        .persist(new WalletEvent.TransactionCompleted(transactionId))
        .thenReply(__ -> Done.getInstance());
    }

    return doneEffect;
  }

  public Effect<Done> cancelTransaction(String transactionId) {
    var walletId = commandContext().entityId();

    if (currentState() != null && currentState().isPendingTransaction(transactionId)) {
      var transaction = currentState().getTransaction(transactionId);
      logger.info("Wallet [{}]: transaction [{}] cancelled", walletId, transaction);
      return effects()
        .persist(new WalletEvent.TransactionCancelled(transactionId))
        .thenReply(__ -> Done.getInstance());
    }

    return doneEffect;
  }


  @Override
  public Wallet applyEvent(WalletEvent event) {
    return switch (event) {
      case WalletEvent.WalletCreated() -> new Wallet(0);

      case WalletEvent.DepositInitiated(long amount, String txId) -> currentState().addPendingDeposit(amount, txId);
      case WalletEvent.WithdrawInitiated(long amount, String txId) -> currentState().addPendingWithdraw(amount, txId);

      case WalletEvent.Deposited(long amount, String txId) -> currentState().executeDeposit(amount, txId);
      case WalletEvent.Withdrawn(long amount, String txId) -> currentState().executeWithdraw(amount, txId);

      case WalletEvent.TransactionCancelled(String txId) -> currentState().cancelTransaction(txId);
      case WalletEvent.TransactionCompleted(String txId) -> currentState().completeTransaction(txId);
    };
  }
}
