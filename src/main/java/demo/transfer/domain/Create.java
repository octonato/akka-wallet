package demo.transfer.domain;

import java.util.List;

public record Create(List<String> participants) {

  public static Create of(String participantId) {
    return new Create(List.of(participantId));
  }

  public static Create of(String participantId1, String participantId2) {
    return new Create(List.of(participantId1, participantId2));
  }
}
