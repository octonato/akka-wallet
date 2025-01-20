package demo.wallet.domain;

public record Wallet(long balance) {

  public long balance() {
    return balance;
  }

  public Wallet increaseBalance(long amount) {
    return new Wallet(balance + amount);
  }

  public Wallet decreaseBalance(long amount) {
    return new Wallet(balance - amount);
  }
}
