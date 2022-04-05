package com.anderfred.throttling.handler;

import static java.util.Objects.requireNonNull;

import com.anderfred.throttling.exceptions.RequestCountExceedsException;
import com.anderfred.throttling.handler.timer.ClearCountTimesTask;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Limit {
  private final String marker;
  private final AtomicLong count;
  private final Timer timer;
  private final AtomicLong limit;
  private final AtomicLong period;
  Logger log = LoggerFactory.getLogger(Limit.class);

  public Limit(String marker, long limit, long period) {
    requireNonNull(marker);
    this.marker = marker;
    this.limit = new AtomicLong(limit);
    this.period = new AtomicLong(period);
    this.count = new AtomicLong(1L);
    log.debug(String.format("Creating new Limit: of marker:[%s], limit:[%d], period:[%d]", marker, limit, period));
    timer = new Timer();
    timer.schedule(new ClearCountTimesTask(this), period, period);
  }

  public void increment() {
    this.count.getAndIncrement();
    if (count.get() >= limit.get()) {
      log.warn(String.format("WARNING | Limit [%d] for marker:[%s], exceeds", limit.get(), marker));
      throw new RequestCountExceedsException();
    }
  }

  public void setPeriod(long period) {
    clearTimer();
    this.period.set(period);
    timer.schedule(new ClearCountTimesTask(this), 0, this.period.get());
  }

  public void clearTimer(){
    timer.cancel();
  }

  public void setLimit(long limit) {
    this.limit.set(limit);
  }

  public String getMarker() {
    return this.marker;
  }

  public void clearCount() {
    this.count.set(0L);
  }
}
