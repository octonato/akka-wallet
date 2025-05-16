package demo.transfer.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.workflow.Workflow;
import akka.javasdk.workflow.WorkflowContext;
import demo.transfer.api.TransferId;
import demo.transfer.domain.Transfer;
import demo.transfer.domain.TransferWorkflowState;
import demo.wallet.application.WalletEntity;
import demo.wallet.domain.DepositCommand;
import demo.wallet.domain.WithdrawCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ComponentId("transfer-workflow")
public class TransferWorkflow extends Workflow<TransferWorkflowState> {

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final ComponentClient componentClient;
  private final String transferId;

  public static final String prefix = "tw:";

  public static String prefix(String id) {
    return prefix + id;
  }

  // step names
  private final String INITIATE_TRANSFER = "initiate-transfer";
  private final String EXECUTE = "execute";
  private final String CANCEL = "cancel";

  public TransferWorkflow(WorkflowContext context, ComponentClient componentClient) {
    this.componentClient = componentClient;
    this.transferId = context.workflowId();
  }

  public Effect<TransferWorkflowState> getState() {
    if (currentState() == null)
      return effects().error("Workflow [" + transferId + "] not initialized");
    else return effects().reply(currentState());
  }

  public Effect<Done> startTransfer(Transfer transfer) {
    if (!TransferId.isWorkflowId(transferId)) {
      return effects().error("Transfer [" + transferId + "] is not a valid workflow id");
    }

    if (transfer.amount() <= 0) return effects().error("Transfer amount must be greater than zero");
    else if (currentState() != null)
      return effects().error("Workflow [" + transferId + "] already started");
    else {

      var initialState = new TransferWorkflowState(transfer);
      logger.info("Starting transfer [{}], initial state [{}]", transferId, initialState);

      return effects()
          .updateState(initialState)
          .transitionTo(INITIATE_TRANSFER)
          .thenReply(Done.getInstance());
    }
  }

  @Override
  public WorkflowDef<TransferWorkflowState> definition() {
    return workflow()
        .addStep(initiateTransfer(), maxRetries(3).failoverTo(CANCEL))
        .addStep(executeStep())
        .addStep(cancelStep());
  }

  private Step initiateTransfer() {
    return step(INITIATE_TRANSFER)
        .call(
            () -> {
              var amount = currentState().transfer().amount();

              var fromWallet = currentState().transfer().from();
              logger.info(
                  "Transfer [{}]: withdrawing [{}] to wallet [{}]", transferId, amount, fromWallet);
              componentClient
                  .forEventSourcedEntity(fromWallet)
                  .method(WalletEntity::withdraw)
                  .invoke(new WithdrawCommand(amount, transferId));

              var toWallet = currentState().transfer().to();
              logger.info(
                  "Transfer [{}]: depositing [{}] to wallet [{}]", transferId, amount, toWallet);

              componentClient
                  .forEventSourcedEntity(toWallet)
                  .method(WalletEntity::deposit)
                  .invoke(new DepositCommand(amount, transferId));
            })
        .andThen(() -> effects().updateState(currentState().initiated()).transitionTo(EXECUTE));
  }

  private Step executeStep() {
    return step(EXECUTE)
        .call(
            () -> {
              var fromWallet = currentState().transfer().from();
              executeTransfer(fromWallet);

              var toWallet = currentState().transfer().to();
              executeTransfer(toWallet);
            })
        .andThen(() -> effects().updateState(currentState().completed()).end());
  }

  private Step cancelStep() {
    return step(CANCEL)
        .call(
            () -> {
              logger.info("Cancelling transfer workflow [{}]", transferId);

              var fromWallet = currentState().transfer().from();
              cancelTransfer(fromWallet);

              var toWallet = currentState().transfer().to();
              cancelTransfer(toWallet);
            })
        .andThen(() -> effects().updateState(currentState().cancelled()).end());
  }

  private Done cancelTransfer(String walletId) {
    logger.info("Transfer [{}]: cancelling transaction on wallet [{}]", transferId, walletId);
    return componentClient
        .forEventSourcedEntity(walletId)
        .method(WalletEntity::cancelTransaction)
        .invoke(transferId);
  }

  private Done executeTransfer(String walletId) {
    logger.info("Transfer [{}]: executing transaction on wallet [{}]", transferId, walletId);
    return componentClient
        .forEventSourcedEntity(walletId)
        .method(WalletEntity::executeTransaction)
        .invoke(transferId);
  }
}
