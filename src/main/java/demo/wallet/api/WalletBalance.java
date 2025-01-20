package demo.wallet.api;

public record WalletBalance(String id, long balance) {

  public WalletBalance increase(long amount) {
    return new WalletBalance(id, balance + amount);
  }

  public WalletBalance decrease(long amount) {
    return new WalletBalance(id, balance - amount);
  }
}
