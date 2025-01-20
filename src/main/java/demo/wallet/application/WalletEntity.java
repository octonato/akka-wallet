package demo.wallet.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.eventsourcedentity.EventSourcedEntity;
import demo.wallet.domain.Wallet;
import demo.wallet.domain.WalletEvent;

@ComponentId("wallet")
public class WalletEntity extends EventSourcedEntity<Wallet, WalletEvent> {


  public Effect<Done> create() {
    return effects()
      .persist(new WalletEvent.WalletCreated())
      .thenReply(__ -> Done.getInstance());
  }

  public Effect<Done> createAndDeposit(long balance) {
    return effects()
      .persist(new WalletEvent.WalletCreated(), new WalletEvent.Deposited(balance))
      .thenReply(__ -> Done.getInstance());
  }

  public Effect<Done> deposit(long amount) {
    return effects()
      .persist(new WalletEvent.Deposited(amount))
      .thenReply(__ -> Done.getInstance());
  }

  public Effect<Done> withdraw(long amount) {
    return effects()
      .persist(new WalletEvent.Withdrawn(amount))
      .thenReply(__ -> Done.getInstance());
  }

  public ReadOnlyEffect<Long> balance() {
    if (currentState() == null) return effects().error("Wallet does not exist");
    else return effects().reply(currentState().balance());
  }


  @Override
  public Wallet applyEvent(WalletEvent event) {
    return switch (event) {
      case WalletEvent.WalletCreated() -> new Wallet(0);
      case WalletEvent.Deposited(long amount) -> currentState().increaseBalance(amount);
      case WalletEvent.Withdrawn(long amount) -> currentState().decreaseBalance(amount);
    };
  }
}
