package io.zeebe.containers;

import org.testcontainers.containers.ContainerState;

public interface ZeebeNetworkable extends ContainerState {

  String getInternalHost();

  default String getInternalAddress(final ZeebePort port) {
    return String.format("%s:%d", getInternalHost(), port.getPort());
  }

  default String getExternalAddress(final ZeebePort port) {
    return String.format("%s:%d", getContainerIpAddress(), getMappedPort(port.getPort()));
  }
}
