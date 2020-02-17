Zeebe Test Container
====================

Easily test your application against a containerized, configurable Zeebe instance.

> **NOTE**: the module was, up to now, where matching Zeebe's versions; this however makes us less flexible in
offering extra features. So from 0.30.x on, we will be using our own release cycle.

Please refer to [testcontainers.org](https://testcontainers.org) for general documentation on how to
use containers for your tests.

Features
========

- [x] Start a Zeebe broker container with configurable environment
- [x] Start a Zeebe gateway container with configurable environment

Planned
=======

Current plans are to add more tests and QoL features:

- [ ] Gateway tests
- [ ] Verification API
- [ ] Client builder
- [ ] Cluster rule

Supported Zeebe versions
========================

> Supported versions are those we actively test against; in general the library should be
compatible with most Zeebe versions `>= 0.20.x`.

- 0.20.1
- 0.21.1
- 0.22.1

Quickstart
==========

Add the project to your dependencies:

```xml
<dependency>
  <groupId>io.zeebe</groupId>
  <artifactId>zeebe-test-container</artifactId>
  <version>0.31.1</version>
</dependency>
```

You can then use `ZeebeBrokerContainer` and `ZeebeGatewayContainer` as you would any `GenericContainer`.

Example usage
=============

Import `zeebe-test-container` to your project, and you can use the containers in your
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
  @Rule
  public ZeebeBrokerContainer zeebe = new ZeebeBrokerContainer()
    .withPartitionCount(3)
    .withReplicationFactor(1);

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
    broker.withEmbeddedGateway(false).withHost("zeebe-0");
    gateway.withContactPoint(broker.getInternalAddress(ZeebePort.INTERNAL_API));

    // start both containers
    Stream.of(gateway, broker).parallel().forEach(Startable::start);

    // create a client to connect to the gateway
    final ZeebeClient client =
        ZeebeClient.newClientBuilder()
            .brokerContactPoint(gateway.getExternalAddress(ZeebePort.GATEWAY))
            .build();

    // test stuff
    // ...

    Stream.of(gateway, broker).parallel().forEach(Startable::stop);
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
    Stream.of(zeebe0, zeebe1, zeebe2).parallel().forEach(Startable::start);

    // Run your tests
    // ...

    // stop all brokers
    Stream.of(zeebe0, zeebe1, zeebe2).parallel().forEach(Startable::stop);
  }
}
```
