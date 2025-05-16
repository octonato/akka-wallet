package demo.wallet.domain;

public record WithdrawCommand(long amount, String transactionId) {}
