package io.zeebe.containers.clock;

import feign.Request;
import feign.RequestTemplate;
import feign.Target;
import io.zeebe.containers.ZeebeNode;

final class ZeebeClockTarget implements Target<ZeebeClockClient> {
  private final ZeebeNode<?> zeebe;
  private final String scheme;

  ZeebeClockTarget(final ZeebeNode<?> zeebe) {
    this(zeebe, "http");
  }

  ZeebeClockTarget(final ZeebeNode<?> zeebe, final String scheme) {
    this.zeebe = zeebe;
    this.scheme = scheme;
  }

  @Override
  public Class<ZeebeClockClient> type() {
    return ZeebeClockClient.class;
  }

  @Override
  public String name() {
    return String.format("%s clock", zeebe.getInternalHost());
  }

  @Override
  public String url() {
    return String.format("%s://%s", scheme, zeebe.getExternalMonitoringAddress());
  }

  @Override
  public Request apply(final RequestTemplate input) {
    input.target(url());
    return input.request();
  }
}
