Zeebe Test Container
====================

Easily test your application against a containerized, configurable Zeebe instance.

Features
========

- [x] Start a Zeebe broker container with configurable environment
- [x] Start a Zeebe gateway container with configurable environment

Planned
=======

Current plans are to add more tests and QoL features:

- [ ] Gateway tests
- [ ] Client builder
- [ ] Cluster rule
- [ ] Helper client (wrapper around ZBClient with QoL)

Supported Zeebe versions
========================

- 0.20.0
- 0.21.0-alpha1
- 0.21.0-alpha2

Example usage
=============

Import `zeebe-container` to your project, and you can use the containers in your
tests as:

### Simple broker with embedded gateway
```java
class MyFeatureTest {
  @Rule
  public ZeebeBrokerContainer zeebe = new ZeebeBrokerContainer();

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
  public ZeebeBrokerContainer zeebe = new ZeebeBrokerContainer(zeebeEnv);

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
    final ZeebeBrokerContainer broker = new ZeebeBrokerContainer();
    final ZeebeGatewayContainer gateway =
        new ZeebeGatewayContainer()
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

### Cluster of 3 brokers
```java
class MyClusteredTest {
  @Test
  public void shouldTestWithCluster() {
    final Network network = Network.newNetwork();
    final ZeebeBrokerContainer zeebe0 = new ZeebeBrokerContainer().withNetwork(network).withNodeId(0).withHost("zeebe-0");
    final ZeebeBrokerContainer zeebe1 = new ZeebeBrokerContainer().withNetwork(network).withNodeId(1).withHost("zeebe-1");
    final ZeebeBrokerContainer zeebe2 = new ZeebeBrokerContainer().withNetwork(network).withNodeId(2).withHost("zeebe-2");
    final Collection<String> contactPoints =
        Stream.of(zeebe0, zeebe1, zeebe2)
            .map(ZeebeBrokerContainer::getContactPoint)
            .collect(Collectors.toList());

    // set contact points for all
    Stream.of(zeebe0, zeebe1, zeebe2).forEach(node -> node.withContactPoints(contactPoints));

    // start all brokers
    // it's important to start the brokers in parallel as they will not be ready until a Raft is formed
    Stream.of(zeebe0, zeebe1, zeebe2).parallel().forEach(GenericContainer::start);

    // Run your tests
    // ...

    // stop all brokers
    Stream.of(zeebe0, zeebe1, zeebe2).parallel().forEach(GenericContainer::stop);
  }
}
```
