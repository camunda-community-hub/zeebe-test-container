Zeebe Test Container
====================

Easily test your application against a containerized, configurable Zeebe instance.

Features
========

- [x] Start a Zeebe broker container with configurable environment
- [x] Start a Zeebe gateway container with configurable environment

Example usage
=============

Import `zeebe-container` to your project, and you can use the containers in your
tests as:

### Simple broker with embedded gateway
```java
class MyFeatureTest {
  @Rule
  public BrokerContainer zeebe = new BrokerContainer();

  @Test
  public void shouldTestMyFeature() {
    // create a client to connect to the gateway
    final ZeebeClient client =
        ZeebeClient.newClientBuilder()
            .brokerContactPoint(broker.getExternalAddress(ZeebePort.GATEWAY))
            .build();

    // test stuff
    // ...
  }
}
```

### Configured broker with embedded gateway
```java
class MyFeatureTest {
  private final BrokerEnvironment zeebeEnv = new BrokerEnvironment()
    .withPartitionCount(3)
    .withReplicationFactor(1);
  
  @Rule
  public BrokerContainer zeebe = new BrokerContainer(zeebeEnv);

  @Test
  public void shouldTestMyFeature() {
    // create a client to connect to the gateway
    final ZeebeClient client =
        ZeebeClient.newClientBuilder()
            .brokerContactPoint(broker.getExternalAddress(ZeebePort.GATEWAY))
            .build();

    // test stuff
    // ...
  }
}
```

### Standalone Gateway
```java
class MyFeatureTest {
  @Test
  public void shouldTestMyFeature() {
    // create a broker and a standalone gateway
    final BrokerContainer broker = new BrokerContainer();
    final GatewayContainer gateway =
        new GatewayContainer()
            .withNetwork(broker.getNetwork()); // make sure they are on the same network

    // configure broker so it doesn't start an embedded gateway
    broker.getEnvironment().withEmbeddedGateway(false).withHost("zeebe-0");
    gateway.getEnvironment().withContactPoint(broker.getInternalAddress(ZeebePort.INTERNAL_API));

    // start both containers
    Stream.of(gateway, broker).parallel().forEach(GenericContainer::start);

    // create a client to connect to the gateway
    final ZeebeClient client =
        ZeebeClient.newClientBuilder()
            .brokerContactPoint(gateway.getExternalAddress(ZeebePort.GATEWAY))
            .build();

    // test stuff
    // ...

    Stream.of(gateway, broker).parallel().forEach(GenericContainer::stop);
  }
}
```
