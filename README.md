# Zeebe Test Container

[![](https://img.shields.io/badge/Community%20Extension-An%20open%20source%20community%20maintained%20project-FF4700)](https://github.com/camunda-community-hub/community)
[![](https://img.shields.io/badge/Lifecycle-Incubating-blue)](https://github.com/Camunda-Community-Hub/community/blob/main/extension-lifecycle.md#incubating-)

Easily test your application against a containerized, configurable Zeebe instance.

Please refer to [testcontainers.org](https://testcontainers.org) for general documentation on how to
use containers for your tests, as well as general prerequisites.

- [Zeebe Test Container](#zeebe-test-container)
  - [Supported Zeebe versions](#supported-zeebe-versions)
  - [Installation](#installation)
    - [Requirements](#requirements)
  - [Compatibility guarantees](#compatibility-guarantees)
  - [Quickstart](#quickstart)
  - [Using with junit4](#using-with-junit4)
  - [Using with junit5](#using-with-junit5)
- [Usage](#usage)
  - [Broker with embedded gateway](#broker-with-embedded-gateway)
  - [Standalone broker without gateway](#standalone-broker-without-gateway)
  - [Standalone gateway](#standalone-gateway)
  - [Cluster](#cluster)
    - [Usage](#usage-1)
    - [Example](#example)
  - [Configuring your container](#configuring-your-container)
  - [Examples](#examples)
  - [Continuous Integration](#continuous-integration)
- [Tips](#tips)
  - [Tailing your container's logs during development](#tailing-your-containers-logs-during-development)
  - [Configuring GenericContainer specific properties with a Zeebe*Node interface](#configuring-genericcontainer-specific-properties-with-a-zeebenode-interface)
  - [Advanced Docker usage via the DockerClient](#advanced-docker-usage-via-the-dockerclient)
    - [Gracefully restarting a container](#gracefully-restarting-a-container)
  - [Limiting container resources](#limiting-container-resources)
- [Contributing](#contributing)
  - [Build from source](#build-from-source)
    - [Prerequisites](#prerequisites)
    - [Building](#building)
  - [Backwards compatibility](#backwards-compatibility)
  - [Report issues or contact developers](#report-issues-or-contact-developers)
  - [Create a Pull Request](#create-a-pull-request)
  - [Commit Message Guidelines](#commit-message-guidelines)
  - [Contributor License Agreement](#contributor-license-agreement)

## Supported Zeebe versions

> **NOTE**: version 1.0 is incompatible with Zeebe versions pre 0.23.x

- 0.23.x
- 0.24.x
- 0.25.x
- 0.26.x

## Installation

Add the project to your dependencies:

```xml

<dependency>
  <groupId>io.zeebe</groupId>
  <artifactId>zeebe-test-container</artifactId>
  <version>1.0.1</version>
</dependency>
```

```groovy
testImplementation 'io.zeebe:zeebe-test-container:1.0.1'
```

### Requirements

Zeebe Test Container is built for Java 8+, and will probably not work on lower Java versions.

Additionally, you will need to comply to all of the Testcontainers requirements, as defined
[here](https://www.testcontainers.org/#prerequisites).

## Compatibility guarantees

As there is currently only a single maintainer, only the latest major version will be maintained and
supported.

`zeebe-test-container` uses [API guardian](https://github.com/apiguardian-team/apiguardian) to
declare the stability and guarantees of its API.

Every class/interface/etc. is annotated with an `@API` annotation describing its status. Additionally,
at times, inner members of a class/interface/etc. may be annotated with an overriding `@API`
annotation. In that case, it would take precedence over its parent annotation. For example, if you
have the following:

```java
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

@API(status = Status.STABLE)
public class MyClass {
  @API(status = Status.EXPERIMENTAL)
  public void myExperimentalMethod() {

  }

  public void myStableMethod() {

  }
}
```

Then we can assume the contract for `MyClass` is stable and will not be changed until the next major
version, __except__ for its method `myExperimentalMethod`, which is an experimental addition which
may change at any time, at least until it is marked as stable or dropped.

> NOTE: for contributors, please remember to annotate new additions, and to maintain the
> compatibility guarantees of pre-annotated entities.

## Quickstart

## Using with junit4

If you're using junit4, you can add the container as a rule: it will be started and closed around
each test execution. You can read more about Testcontainers and
junit4 [here](https://www.testcontainers.org/test_framework_integration/junit_4/).

```java
package com.acme.zeebe;

import io.zeebe.client.ZeebeClient;
import io.zeebe.client.api.response.DeploymentEvent;
import io.zeebe.client.api.response.WorkflowInstanceResult;
import io.zeebe.containers.ZeebeContainer;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.BpmnModelInstance;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;

public class MyFeatureTest {

  @Rule
  public ZeebeContainer zeebeContainer = new ZeebeContainer();

  @Test
  public void shouldConnectToZeebe() {
    // given
    final ZeebeClient client =
      ZeebeClient.newClientBuilder()
        .gatewayAddress(zeebeContainer.getExternalGatewayAddress())
        .usePlaintext()
        .build();
    final BpmnModelInstance process =
      Bpmn.createExecutableProcess("process").startEvent().endEvent().done();

    // when
    // do something (e.g. deploy a workflow)
    final DeploymentEvent deploymentEvent =
      client.newDeployCommand().addWorkflowModel(process, "process.bpmn").send().join();

    // then
    // verify (e.g. we can create an instance of the deployed workflow)
    final WorkflowInstanceResult workflowInstanceResult =
      client
        .newCreateInstanceCommand()
        .bpmnProcessId("process")
        .latestVersion()
        .withResult()
        .send()
        .join();
    Assertions.assertThat(workflowInstanceResult.getWorkflowKey())
      .isEqualTo(deploymentEvent.getWorkflows().get(0).getWorkflowKey());
  }
}
```

## Using with junit5

If you're using junit5, you can use the `Testcontainers` extension. It will manage the container
lifecycle for you. You can read more about the
extension [here](https://www.testcontainers.org/test_framework_integration/junit_5/).

```java
package com.acme.zeebe;

import io.zeebe.client.ZeebeClient;
import io.zeebe.client.api.response.DeploymentEvent;
import io.zeebe.client.api.response.WorkflowInstanceResult;
import io.zeebe.containers.ZeebeContainer;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.BpmnModelInstance;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class MyFeatureTest {

  @Container
  private final ZeebeContainer zeebeContainer = new ZeebeContainer();

  @Test
  void shouldConnectToZeebe() {
    // given
    final ZeebeClient client =
      ZeebeClient.newClientBuilder()
        .gatewayAddress(zeebeContainer.getExternalGatewayAddress())
        .usePlaintext()
        .build();
    final BpmnModelInstance process =
      Bpmn.createExecutableProcess("process").startEvent().endEvent().done();

    // when
    // do something (e.g. deploy a workflow)
    final DeploymentEvent deploymentEvent =
      client.newDeployCommand().addWorkflowModel(process, "process.bpmn").send().join();

    // then
    // verify (e.g. we can create an instance of the deployed workflow)
    final WorkflowInstanceResult workflowInstanceResult =
      client
        .newCreateInstanceCommand()
        .bpmnProcessId("process")
        .latestVersion()
        .withResult()
        .send()
        .join();
    Assertions.assertThat(workflowInstanceResult.getWorkflowKey())
      .isEqualTo(deploymentEvent.getWorkflows().get(0).getWorkflowKey());
  }
}
```

# Usage

`zeebe-test-container` provides three different containers:

- `ZeebeContainer`: a Zeebe broker with an embedded gateway
- `ZeebeBrokerContainer`: a Zeebe broker without an embedded gateway
- `ZeebeGatewayContainer`: a standalone Zeebe gateway
- `ZeebeCluster`: an experimental cluster builder API which lets you manage a Zeebe cluster made of
  various containers

> If you're unsure which one you should use, then you probably want to use `ZeebeContainer`, as it
> is the quickest way to test your application against Zeebe.

## Broker with embedded gateway

`ZeebeContainer` will start a new Zeebe broker with embedded gateway. For most tests, this is what
you will want to use. It provides all the functionality of a Zeebe single node deployment, which for
testing purposes should be enough.

The container is considered started if and only if:

1. The monitoring, command, cluster, and gateway ports are open and accepting connections (read more about the ports [here](https://docs.zeebe.io/operations/network-ports.html)).
1. The broker ready check returns a 204 (see more about this check [here](https://docs.zeebe.io/operations/health.html#ready-check)).
1. The gateway topology is considered complete.

> A topology is considered complete if there is a leader for all partitions.

Once started, the container is ready to accept commands, and a client can connect to it by setting
its `gatewayAddress` to `ZeebeContainer#getExternalGatewayAddress()`.

## Standalone broker without gateway

`ZeebeBrokerContainer` will start a new Zeebe broker with no embedded gateway. As it contains no
gateway, the use case for this container is to test Zeebe in clustered mode. As such, it will
typically be combined with a `ZeebeGatewayContainer` or a `ZeebeContainer`.

The container is considered started if and only if:

1. The monitoring, command, and cluster ports are open and accepting connections (read more about the ports [here](https://docs.zeebe.io/operations/network-ports.html)).
1. The broker ready check returns a 204 (see more about this check [here](https://docs.zeebe.io/operations/health.html#ready-check)).

Once started, the container is ready to accept commands via the command port; you should therefore
link a gateway to it if you wish to use it.

## Standalone gateway

`ZeebeGatewayContainer` will start a new Zeebe standalone gateway. As it is only a gateway, it
should be linked to at least one broker - a `ZeebeContainer` or `ZeebeBrokerContainer`. By default,
it will not expose the monitoring port, as monitoring is not enabled in the gateway by default. If
you enable monitoring, remember to expose the port as well via
`GenericContainer#addExposedPort(int)`.

The container is considered started if and only if:

1. The cluster and gateway ports are open and accepting connections (read more about the ports [here](https://docs.zeebe.io/operations/network-ports.html)).
1. The gateway topology is considered complete.

> A topology is considered complete if there is a leader for all partitions.

Once started, the container is ready to accept commands, and a client can connect to it by setting
its `gatewayAddress` to `ZeebeContainer#getExternalGatewayAddress()`.

## Cluster

> NOTE: the cluster API is currently an experimental API. You're encouraged to use it and give
> feedback, as this is how we can validate it. Keep in mind however that it is subject to change in
> the future.

A typical production Zeebe deployment will be a cluster of nodes, some brokers, and possibly some
standalone gateways. It can be useful to test against such deployments for acceptance or E2E tests.

While it's not too hard to manually link several containers, it can become tedious and error prone
if you want to test many different configurations. The cluster API provides you with an easy way to
programmatically build Zeebe deployments while minimizing the surface of configuration errors.

> NOTE: if you have a static deployment that you don't need to change programmatically per test,
> then you might want to consider setting up a static Zeebe cluster in your CI pipeline (either via
> Helm or docker-compose), or even using Testcontainer's
> [docker-compose](https://www.testcontainers.org/modules/docker_compose/) feature.

### Usage

The main entry point is the `ZeebeClusterBuilder`, which can be instantiated via
`ZeebeCluster#builder()`. The builder can be used to configure the topology of your cluster. You can
configure the following:

- the number of brokers in the cluster (by default 1)
- whether or not brokers should be using the embedded gateway (by default true)
- the number of standalone gateways in the cluster (by default 0)
- the number of partitions in the cluster (by default 1)
- the replication factor of each partition (by default 1)
- the network the containers should use (by default `Network#SHARED`)

Container instances (e.g. `ZeebeBrokerContainer` or `ZeebeContainer`) are only instantiated and
configured once you call `#build()`. At this point, your cluster is configured in a valid way, but
isn't started yet - that is, no real Docker containers have been created/started.

Once your cluster is built, you can access its brokers and gateways via `ZeebeCluster#getBrokers`
and `ZeebeCluster#getGateways`. This allows you to further configure each container as you wish
before actually starting the cluster. For example, if you want to create a large cluster with many
brokers and need to increase the startup time:

```java
import io.zeebe.containers.cluster.ZeebeCluster;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class ZeebeHugeClusterTest {
  private final ZeebeCluster cluster =
      ZeebeCluster.builder()
          .withEmbeddedGateway(false)
          .withGatewaysCount(3)
          .withBrokersCount(6)
          .withPartitionsCount(6)
          .withReplicationFactor(3)
          .build();

  @AfterEach
  void tearDown() {
    cluster.stop();
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.MINUTES)
  void shouldStartCluster() {
    // given
    // configure each container to have a high start up time as they get started in parallel
    cluster.getBrokers().values().forEach(broker -> broker.self().withStartupTimeout(Duration.ofMinutes(5)));
    cluster.getGateways().values().forEach(gateway -> gateway.self().withStartupTimeout(Duration.ofMinutes(5)));
    cluster.start();

    // test more things
  }
}
```

### Example

Here is a short example on how to set up a cluster for testing with junit5.

```java
import io.zeebe.client.ZeebeClient;
import io.zeebe.client.api.response.BrokerInfo;
import io.zeebe.client.api.response.Topology;
import io.zeebe.containers.cluster.ZeebeCluster;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Showcases how you can create a test with a cluster of two brokers and one standalone gateway.
 * Configuration is kept to minimum, as the goal here is only to showcase how to connect the
 * different nodes together.
 */
class ZeebeClusterWithGatewayExampleTest {
  private final ZeebeCluster cluster =
      ZeebeCluster.builder()
          .withEmbeddedGateway(false)
          .withGatewaysCount(1)
          .withBrokersCount(2)
          .withPartitionsCount(2)
          .withReplicationFactor(1)
          .build();

  @AfterEach
  void tearDown() {
    cluster.stop();
  }

  @Test
  @Timeout(value = 15, unit = TimeUnit.MINUTES)
  void shouldStartCluster() {
    // given
    cluster.start();

    // when
    final Topology topology;
    try (final ZeebeClient client = cluster.newClientBuilder().build()) {
      topology = client.newTopologyRequest().send().join(5, TimeUnit.SECONDS);
    }

    // then
    final List<BrokerInfo> brokers = topology.getBrokers();
    Assertions.assertThat(topology.getClusterSize()).isEqualTo(3);
    Assertions.assertThat(brokers)
        .hasSize(2)
        .extracting(BrokerInfo::getAddress)
        .containsExactlyInAnyOrder(
            cluster.getBrokers().get(0).getInternalCommandAddress(),
            cluster.getBrokers().get(1).getInternalCommandAddress());
  }
}
```

You can find more examples by looking at the
[test/java/io/zeebe/containers/examples/cluster](test/java/io/zeebe/containers/examples/cluster)
package.

## Configuring your container

Configuring your Zeebe container of choice is done exactly as you normally would - via environment
variables or via configuration file. You can find out more about it on the
[Zeebe documentation website](https://docs.zeebe.io/operations/configuration.html).

> Zeebe 0.23.x and upwards use Spring Boot for configuration - refer to their documentation on how
> environment variables are mapped to configuration settings. You can read more about this
> [here](https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-external-config)

> Testcontainers provide mechanisms through which
> [environment variables can be injected](https://www.javadoc.io/doc/org.testcontainers/testcontainers/1.14.3/org/testcontainers/containers/GenericContainer.html#withEnv-java.lang.String-java.lang.String-),
> or [configuration files mounted](https://www.testcontainers.org/features/files/). Refer to their
> documentation for more.

## Examples

A series of examples are included as part of the tests, see
[test/java/io/zeebe/containers/examples](test/java/io/zeebe/containers/examples).

> Note that these are written for junit5.

## Continuous Integration

If you wish to use this with your continous integration pipeline (e.g. Jenkins, CircleCI), the
[Testcontainers](https://www.testcontainers.org/supported_docker_environment/) has a section
explaining how to use it, how volumes can be shared, etc.

# Tips

## Tailing your container's logs during development

As containers are somewhat opaque by nature (though Testcontainers already does a great job of
making this more seamless), it's very useful to add a `LogConsumer` to a container. This will
consume your container logs and pipe them out to your consumer. If you're using SLF4J, you can use
the `Slf4jLogConsumer` and give it a logger, which makes it much easier to debug if anything goes
wrong. Here's an example of how this would look like:

```java
package com.acme.zeebe;

import io.zeebe.containers.ZeebeContainer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class MyFeatureTest {
  private static final Logger LOGGER =
    LoggerFactory.getLogger("com.acme.zeebe.MyFeatureTest.zeebeContainer");

  @Container
  private final ZeebeContainer zeebeContainer =
    new ZeebeContainer().withLogConsumer(new Slf4jLogConsumer(LOGGER));

  @Test
  void shouldTestProperty() {
    // test...
  }
}
```

## Configuring GenericContainer specific properties with a Zeebe*Node interface

There are three container types in this module: `ZeebeContainer`, `ZeebeBrokerContainer`,
`ZeebeGatewayContainer`. `ZeebeContainer` is a special case which can be both a gateway and a
broker. To support a more generic handling of broker/gateway concept, there are two types introduced
which are `ZeebeBrokerNode` and `ZeebeGatewayNode`. These types, however, are not directly extending
`GenericContainer`.

At times, when you have a `ZeebeBrokerNode` or `ZeebeGatewayNode`, you may want to additionally
configure it in ways that are only available to `GenericContainer` instances. You can use
`Container#self()` to get the node's `GenericContainer` representation.

For example:

```java
import io.zeebe.containers.cluster.ZeebeCluster;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class ZeebeHugeClusterTest {
  private final ZeebeCluster cluster =
      ZeebeCluster.builder()
          .withBrokersCount(1)
          .build();

  @AfterEach
  void tearDown() {
    cluster.stop();
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void shouldStartCluster() {
    // given
    final ZeebeBrokerNode<?> broker = cluster.getBrokers().get(0);
    broker.self().withStartupTimeout(Duration.ofMinutes(2));
    cluster.start();

    // test more things
  }
}
```

## Advanced Docker usage via the DockerClient

Remember that you have access to the raw docker client via `GenericContainer#getDockerClient()`.
This lets you do all kinds of things at runtime, such as fiddling with volumes, networks, etc.,
which is a great way to test failure injection.

### Gracefully restarting a container

> NOTE: this is an advanced feature which is experimental and may still cause issues, so use at your
> own risk.

`Testcontainers` itself does not provide a convenience method to stop or pause a container. Using
`GenericContainer#stop()` will actually kill and remove the container. To do these things, you can
use the `DockerClient` directly, and send standard docker commands (e.g. stop, pause, etc.).

Alternatively, we provide a convenience method as `ZeebeNode#shutdownGracefully()`
(e.g. `ZeebeContainer#shutdownGracefully()`, `ZeebeBrokerContainer#shutdownGracefully()`). This will
only stop the container with a grace period, after which it will kill the container if not stopped.
However, the container is not removed, and can be restarted later.

Keep in mind that to restart it you need to use a `DockerClient#startContainerCmd(String)`, as just
calling `GenericContainer#start()` will not start your container again.

## Limiting container resources

When starting many containers, you can use `GenericContainer#withCreateContainerCmdModifier()` on creation to
limit the resources available to them. This can be useful when testing locally on a
development machine and having to start multiple containers.

# Contributing

Contributions are more than welcome! Please make sure to read and adhere to the
[Code of Conduct](CODE_OF_CONDUCT.md). Additionally, in order to have your contributions accepted,
you will need to sign the [Contributor License Agreement](https://cla-assistant.io/camunda/).

## Build from source

### Prerequisites

In order to build from source, you will need to install maven 3.6+. You can find more about it on
the [maven homepage](https://maven.apache.org/users/index.html).

You will also need a JDK targeting Java 8+. We recommend installing any flavour of OpenJDK such as
[AdoptOpenJDK](https://adoptopenjdk.net/).

Finally, you will need to [install Docker](https://docs.docker.com/get-docker/) on your local machine.

### Building

With all requirements ready, you can now simply [clone the repository](https://docs.github.com/en/github/creating-cloning-and-archiving-repositories/cloning-a-repository),
and from its root, run the following command:

```shell
mvn clean install
```

This will build the project and run all tests locally.

Should you wish to only build without running the tests, you can run:

```shell
mvn clean package
```

## Backwards compatibility

Zeebe Test Container uses a [Semantic Versioning](https://semver.org/) scheme for its versions, and
[revapi](https://revapi.org/) to enforce backwards compatibility according to its specification.

Additionally, we also use [apiguardian](https://github.com/apiguardian-team/apiguardian) to specify
backwards compatibility guarantees on a more granular level. As such, only APIs marked as `STABLE`
are considered when enforcing backwards compatibility.

If you wish to incubate a new feature, or if you're unsure about a new API type/method, please use
the `EXPERIMENTAL` status for it. This will give us flexibility to test out new features and change
them easily if we realize they need to be adapted.

## Report issues or contact developers

Work on Zeebe Test Container is done entirely through the Github repository. If you want to report
a bug or request a new feature feel free to open a new issue on [GitHub][issues].

## Create a Pull Request

To work on an issue, follow the following steps:

1. Check that a [GitHub issue][issues] exists for the task you want to work on. If one does not,
   create one.
1. Checkout the `master` branch and pull the latest changes.
   ```
   git checkout develop
   git pull
   ```
1. Create a new branch with the naming scheme `issueId-description`.
   ```
   git checkout -b 123-my-new-feature
   ```
1. Follow the [Google Java Format](https://github.com/google/google-java-format#intellij-android-studio-and-other-jetbrains-ides)
   and [Zeebe Code Style](https://github.com/zeebe-io/zeebe/wiki/Code-Style) while coding.
1. Implement the required changes on your branch, and make sure to build and test your changes locally
   before opening a pull requests for review.
1. If you want to make use of the CI facilities before your feature is ready for review, feel free
   to open a draft PR.
1. If you think you finished the issue please prepare the branch for reviewing.
   In general the commits should be squashed into meaningful commits with a
   helpful message. This means cleanup/fix etc commits should be squashed into
   the related commit.
1. Finally, be sure to check on the CI results and fix any reported errors.

## Commit Message Guidelines

Commit messages use [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/#summary)
format.

## Contributor License Agreement

You will be asked to sign our Contributor License Agreement when you open a Pull Request. We are not
asking you to assign copyright to us, but to give us the right to distribute your code without
restriction. We ask this of all contributors in order to assure our users of the origin and
continuing existence of the code. You only need to sign the CLA once.

Note that this is a general requirement of any Camunda Community Hub project.
