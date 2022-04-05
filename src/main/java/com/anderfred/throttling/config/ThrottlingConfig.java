package com.anderfred.throttling.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "throttling")
public class ThrottlingConfig {

  private long limit;
  private long period;

  public long getLimit() {
    long DEFAULT_LIMIT = 100L;
    return limit > 0 ? limit : DEFAULT_LIMIT;
  }

  public void setLimit(long limit) {
    this.limit = limit;
  }

  public long getPeriod() {
    long DEFAULT_PERIOD = 1000 * 60;
    return period > 0 ? period : DEFAULT_PERIOD;
  }

  public void setPeriod(long period) {
    this.period = period;
  }
}
