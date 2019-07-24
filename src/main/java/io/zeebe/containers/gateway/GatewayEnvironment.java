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

public class GatewayEnvironment extends ZeebeEnvironment<GatewayEnvironment> {
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

  public GatewayEnvironment withHost(String host) {
    this.envMap.put(GatewayEnvVar.HOST, host);
    this.host = host;
    return this;
  }

  public int getPort() {
    return port;
  }

  public GatewayEnvironment withPort(int port) {
    this.envMap.put(GatewayEnvVar.PORT, port);
    this.port = port;
    return this;
  }

  public String getContactPoint() {
    return contactPoint;
  }

  public GatewayEnvironment withContactPoint(String contactPoint) {
    this.envMap.put(GatewayEnvVar.CONTACT_POINT, contactPoint);
    this.contactPoint = contactPoint;
    return this;
  }

  public int getTransportBuffer() {
    return transportBuffer;
  }

  public GatewayEnvironment withTransportBuffer(int transportBuffer) {
    this.envMap.put(GatewayEnvVar.TRANSPORT_BUFFER, transportBuffer);
    this.transportBuffer = transportBuffer;
    return this;
  }

  public int getRequestTimeout() {
    return requestTimeout;
  }

  public GatewayEnvironment withRequestTimeout(int requestTimeout) {
    this.envMap.put(GatewayEnvVar.REQUEST_TIMEOUT, requestTimeout);
    this.requestTimeout = requestTimeout;
    return this;
  }

  public String getClusterName() {
    return clusterName;
  }

  public GatewayEnvironment withClusterName(String clusterName) {
    this.envMap.put(GatewayEnvVar.CLUSTER_NAME, clusterName);
    this.clusterName = clusterName;
    return this;
  }

  public String getClusterMemberId() {
    return clusterMemberId;
  }

  public GatewayEnvironment withClusterMemberId(String clusterMemberId) {
    this.envMap.put(GatewayEnvVar.CLUSTER_MEMBER_ID, clusterMemberId);
    this.clusterMemberId = clusterMemberId;
    return this;
  }

  public String getClusterHost() {
    return clusterHost;
  }

  public GatewayEnvironment withClusterHost(String clusterHost) {
    this.envMap.put(GatewayEnvVar.CLUSTER_HOST, clusterHost);
    this.clusterHost = clusterHost;
    return this;
  }

  public int getClusterPort() {
    return clusterPort;
  }

  public GatewayEnvironment withClusterPort(int clusterPort) {
    this.envMap.put(GatewayEnvVar.CLUSTER_PORT, clusterPort);
    this.clusterPort = clusterPort;
    return this;
  }

  public int getManagementThreadCount() {
    return managementThreadCount;
  }

  public GatewayEnvironment withManagementThreadCount(int managementThreadCount) {
    this.envMap.put(GatewayEnvVar.MANAGEMENT_THREAD_COUNT, managementThreadCount);
    this.managementThreadCount = managementThreadCount;
    return this;
  }

  public boolean isSecurityEnabled() {
    return securityEnabled;
  }

  public GatewayEnvironment withSecurityEnabled(boolean securityEnabled) {
    this.envMap.put(GatewayEnvVar.SECURITY_ENABLED, securityEnabled);
    this.securityEnabled = securityEnabled;
    return this;
  }

  public String getCertificatePath() {
    return certificatePath;
  }

  public GatewayEnvironment withCertificatePath(String certificatePath) {
    this.envMap.put(GatewayEnvVar.CERTIFICATE_PATH, certificatePath);
    this.certificatePath = certificatePath;
    return this;
  }

  public String getPrivateKeyPath() {
    return privateKeyPath;
  }

  public GatewayEnvironment withPrivateKeyPath(String privateKeyPath) {
    this.envMap.put(GatewayEnvVar.PRIVATE_KEY_PATH, privateKeyPath);
    this.privateKeyPath = privateKeyPath;
    return this;
  }

  public boolean isMonitoringEnabled() {
    return monitoringEnabled;
  }

  public GatewayEnvironment withMonitoringEnabled(boolean monitoringEnabled) {
    this.envMap.put(GatewayEnvVar.MONITORING_ENABLED, monitoringEnabled);
    this.monitoringEnabled = monitoringEnabled;
    return this;
  }

  public String getMonitoringHost() {
    return monitoringHost;
  }

  public GatewayEnvironment withMonitoringHost(String monitoringHost) {
    this.envMap.put(GatewayEnvVar.MONITORING_HOST, monitoringHost);
    this.monitoringHost = monitoringHost;
    return this;
  }

  public int getMonitoringPort() {
    return monitoringPort;
  }

  public GatewayEnvironment withMonitoringPort(int monitoringPort) {
    this.envMap.put(GatewayEnvVar.MONITORING_PORT, monitoringPort);
    this.monitoringPort = monitoringPort;
    return this;
  }
}
