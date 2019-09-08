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
package io.zeebe.containers.gateway;

import io.zeebe.containers.ZeebeEnvironment;

public class ZeebeGatewayEnvironment extends ZeebeEnvironment<ZeebeGatewayEnvironment> {
  private String host;
  private int port;
  private String contactPoint;
  private int transportBuffer;
  private int requestTimeout;

  private String clusterName;
  private String clusterMemberId;
  private String clusterHost;
  private int clusterPort;

  private int managementThreadCount;

  private boolean securityEnabled;
  private String certificatePath;
  private String privateKeyPath;

  private boolean monitoringEnabled;
  private String monitoringHost;
  private int monitoringPort;

  public String getHost() {
    return host;
  }

  public ZeebeGatewayEnvironment withHost(String host) {
    this.envMap.put(ZeebeGatewayEnvVar.HOST, host);
    this.host = host;
    return this;
  }

  public int getPort() {
    return port;
  }

  public ZeebeGatewayEnvironment withPort(int port) {
    this.envMap.put(ZeebeGatewayEnvVar.PORT, port);
    this.port = port;
    return this;
  }

  public String getContactPoint() {
    return contactPoint;
  }

  public ZeebeGatewayEnvironment withContactPoint(String contactPoint) {
    this.envMap.put(ZeebeGatewayEnvVar.CONTACT_POINT, contactPoint);
    this.contactPoint = contactPoint;
    return this;
  }

  public int getTransportBuffer() {
    return transportBuffer;
  }

  public ZeebeGatewayEnvironment withTransportBuffer(int transportBuffer) {
    this.envMap.put(ZeebeGatewayEnvVar.TRANSPORT_BUFFER, transportBuffer);
    this.transportBuffer = transportBuffer;
    return this;
  }

  public int getRequestTimeout() {
    return requestTimeout;
  }

  public ZeebeGatewayEnvironment withRequestTimeout(int requestTimeout) {
    this.envMap.put(ZeebeGatewayEnvVar.REQUEST_TIMEOUT, requestTimeout);
    this.requestTimeout = requestTimeout;
    return this;
  }

  public String getClusterName() {
    return clusterName;
  }

  public ZeebeGatewayEnvironment withClusterName(String clusterName) {
    this.envMap.put(ZeebeGatewayEnvVar.CLUSTER_NAME, clusterName);
    this.clusterName = clusterName;
    return this;
  }

  public String getClusterMemberId() {
    return clusterMemberId;
  }

  public ZeebeGatewayEnvironment withClusterMemberId(String clusterMemberId) {
    this.envMap.put(ZeebeGatewayEnvVar.CLUSTER_MEMBER_ID, clusterMemberId);
    this.clusterMemberId = clusterMemberId;
    return this;
  }

  public String getClusterHost() {
    return clusterHost;
  }

  public ZeebeGatewayEnvironment withClusterHost(String clusterHost) {
    this.envMap.put(ZeebeGatewayEnvVar.CLUSTER_HOST, clusterHost);
    this.clusterHost = clusterHost;
    return this;
  }

  public int getClusterPort() {
    return clusterPort;
  }

  public ZeebeGatewayEnvironment withClusterPort(int clusterPort) {
    this.envMap.put(ZeebeGatewayEnvVar.CLUSTER_PORT, clusterPort);
    this.clusterPort = clusterPort;
    return this;
  }

  public int getManagementThreadCount() {
    return managementThreadCount;
  }

  public ZeebeGatewayEnvironment withManagementThreadCount(int managementThreadCount) {
    this.envMap.put(ZeebeGatewayEnvVar.MANAGEMENT_THREAD_COUNT, managementThreadCount);
    this.managementThreadCount = managementThreadCount;
    return this;
  }

  public boolean isSecurityEnabled() {
    return securityEnabled;
  }

  public ZeebeGatewayEnvironment withSecurityEnabled(boolean securityEnabled) {
    this.envMap.put(ZeebeGatewayEnvVar.SECURITY_ENABLED, securityEnabled);
    this.securityEnabled = securityEnabled;
    return this;
  }

  public String getCertificatePath() {
    return certificatePath;
  }

  public ZeebeGatewayEnvironment withCertificatePath(String certificatePath) {
    this.envMap.put(ZeebeGatewayEnvVar.CERTIFICATE_PATH, certificatePath);
    this.certificatePath = certificatePath;
    return this;
  }

  public String getPrivateKeyPath() {
    return privateKeyPath;
  }

  public ZeebeGatewayEnvironment withPrivateKeyPath(String privateKeyPath) {
    this.envMap.put(ZeebeGatewayEnvVar.PRIVATE_KEY_PATH, privateKeyPath);
    this.privateKeyPath = privateKeyPath;
    return this;
  }

  public boolean isMonitoringEnabled() {
    return monitoringEnabled;
  }

  public ZeebeGatewayEnvironment withMonitoringEnabled(boolean monitoringEnabled) {
    this.envMap.put(ZeebeGatewayEnvVar.MONITORING_ENABLED, monitoringEnabled);
    this.monitoringEnabled = monitoringEnabled;
    return this;
  }

  public String getMonitoringHost() {
    return monitoringHost;
  }

  public ZeebeGatewayEnvironment withMonitoringHost(String monitoringHost) {
    this.envMap.put(ZeebeGatewayEnvVar.MONITORING_HOST, monitoringHost);
    this.monitoringHost = monitoringHost;
    return this;
  }

  public int getMonitoringPort() {
    return monitoringPort;
  }

  public ZeebeGatewayEnvironment withMonitoringPort(int monitoringPort) {
    this.envMap.put(ZeebeGatewayEnvVar.MONITORING_PORT, monitoringPort);
    this.monitoringPort = monitoringPort;
    return this;
  }
}
