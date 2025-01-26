package demo.wallet.api;

public record DepositRequest(long amount, String transactionId) {
}
