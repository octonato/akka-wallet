package demo.wallet;

import akka.javasdk.ServiceSetup;
import akka.javasdk.annotations.Setup;

import java.util.Set;

@Setup
public class Bootstrap implements ServiceSetup {
  @Override
  public Set<Class<?>> disabledComponents() {
    return Set.of();
  }
}
