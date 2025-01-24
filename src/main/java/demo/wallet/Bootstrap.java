package demo.wallet;

import akka.javasdk.ServiceSetup;
import akka.javasdk.annotations.Setup;
import demo.wallet.application.consumers.WalletEventsToTopic;

import java.util.Set;

@Setup
public class Bootstrap implements ServiceSetup {
  @Override
  public Set<Class<?>> disabledComponents() {
    return Set.of(
      WalletEventsToTopic.class // disable the topic producer for demo purposes
    );
  }
}
