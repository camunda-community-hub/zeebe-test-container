package io.zeebe.containers;

import org.testcontainers.containers.Container;
import org.testcontainers.utility.TestcontainersConfiguration;

public interface ZeebeContainer<SELF extends ZeebeContainer<SELF>> extends Container<SELF> {
  String DEFAULT_CLUSTER_NAME = "zeebe";
  String ZEEBE_CONTAINER_IMAGE_PROPERTY = "zeebe.container.image";
  String DEFAULT_ZEEBE_CONTAINER_IMAGE = "camunda/zeebe";
  String DEFAULT_ZEEBE_VERSION = "0.20.0";

  static String getDefaultImage() {
    return TestcontainersConfiguration.getInstance()
        .getProperties()
        .getOrDefault(ZEEBE_CONTAINER_IMAGE_PROPERTY, DEFAULT_ZEEBE_CONTAINER_IMAGE)
        .toString();
  }

  void applyDefaultConfiguration();
}
