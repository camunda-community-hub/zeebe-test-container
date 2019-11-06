/*
 * Copyright © 2019 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zeebe.containers;

public interface ZeebeGatewayContainer<SELF extends ZeebeGatewayContainer<SELF>>
    extends ZeebeContainer<SELF> {
  default SELF withHost(final String host) {
    return withEnv(ZeebeGatewayEnvironment.HOST, host);
  }

  default SELF withPort(final int port) {
    return withEnv(ZeebeGatewayEnvironment.PORT, port);
  }

  default SELF withContactPoint(final String contactPoint) {
    return withEnv(ZeebeGatewayEnvironment.CONTACT_POINT, contactPoint);
  }

  default SELF withTransportBuffer(final int transportBuffer) {
    return withEnv(ZeebeGatewayEnvironment.TRANSPORT_BUFFER, transportBuffer);
  }

  default SELF withRequestTimeout(final int requestTimeout) {
    return withEnv(ZeebeGatewayEnvironment.REQUEST_TIMEOUT, requestTimeout);
  }

  default SELF withManagementThreadCount(final int managementThreadCount) {
    return withEnv(ZeebeGatewayEnvironment.MANAGEMENT_THREAD_COUNT, managementThreadCount);
  }

  default SELF withSecurityEnabled(final boolean securityEnabled) {
    return withEnv(ZeebeGatewayEnvironment.SECURITY_ENABLED, securityEnabled);
  }

  default SELF withCertificatePath(final String certificatePath) {
    return withEnv(ZeebeGatewayEnvironment.CERTIFICATE_PATH, certificatePath);
  }

  default SELF withPrivateKeyPath(final String privateKeyPath) {
    return withEnv(ZeebeGatewayEnvironment.PRIVATE_KEY_PATH, privateKeyPath);
  }

  default SELF withMonitoringEnabled(final boolean monitoringEnabled) {
    return withEnv(ZeebeGatewayEnvironment.MONITORING_ENABLED, monitoringEnabled);
  }

  default SELF withMonitoringHost(final String monitoringHost) {
    return withEnv(ZeebeGatewayEnvironment.MONITORING_HOST, monitoringHost);
  }

  default SELF withMonitoringPort(final int monitoringPort) {
    return withEnv(ZeebeGatewayEnvironment.MONITORING_PORT, monitoringPort);
  }
}
