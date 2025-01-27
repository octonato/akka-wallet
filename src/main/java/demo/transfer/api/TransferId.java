package demo.transfer.api;

public class TransferId {

  private static final String mediatorPrefix = "m:";
  private static final String workflowPrefix = "w:";

  public static String prefixForMediator(String id) {
    if (isMediatorId(id)) {
      return id;
    }
    return mediatorPrefix + id;
  }

  public static String prefixForWorkflow(String id) {
    if (isWorkflowId(id)) {
      return id;
    }
    return workflowPrefix + id;
  }

  public static boolean isMediatorId(String id) {

    return id.startsWith(mediatorPrefix);
  }

  public static boolean isWorkflowId(String transferId) {
    return transferId.startsWith(workflowPrefix);
  }
}
