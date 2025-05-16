package demo.transfer.domain;

public record Participant(String id, boolean joined, boolean executed) {

  public Participant(String id) {
    this(id, false, false);
  }
}
