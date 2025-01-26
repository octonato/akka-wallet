package demo.wallet.api;

public record WithdrawRequest(long amount, String transactionId) {
}
