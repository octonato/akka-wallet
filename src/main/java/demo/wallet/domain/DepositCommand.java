package demo.wallet.domain;

public record DepositCommand(long amount, String transactionId) {}
