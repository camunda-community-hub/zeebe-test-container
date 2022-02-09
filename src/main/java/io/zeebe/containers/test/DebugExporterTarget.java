package io.zeebe.containers.test;

import feign.Request;
import feign.RequestTemplate;
import feign.Target;
import io.zeebe.containers.ZeebeBrokerNode;
import io.zeebe.containers.ZeebeNode;

/**
 * A {@link Target} implementation for a {@link DebugExporterClient} which targets a {@link
 * ZeebeNode}'s debug HTTP exporter.
 */
final class DebugExporterTarget implements Target<DebugExporterClient> {
  private final ZeebeBrokerNode<?> zeebe;
  private final String scheme;
  private final int port;

  DebugExporterTarget(final ZeebeBrokerNode<?> zeebe, final int port) {
    this(zeebe, port, "http");
  }

  DebugExporterTarget(final ZeebeBrokerNode<?> zeebe, final int port, final String scheme) {
    this.zeebe = zeebe;
    this.scheme = scheme;
    this.port = port;
  }

  @Override
  public Class<DebugExporterClient> type() {
    return DebugExporterClient.class;
  }

  @Override
  public String name() {
    return String.format("%s debug http exporter server", zeebe.getInternalHost());
  }

  @Override
  public String url() {
    return String.format("%s://%s", scheme, zeebe.getExternalAddress(port));
  }

  @Override
  public Request apply(final RequestTemplate input) {
    input.target(url());
    return input.request();
  }
}
