package com.anderfred.throttling.handler;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class LimitHandler {
  private final Map<String, Limit> limits = new ConcurrentHashMap<>();

  public void checkLimit(String marker, long limit, long period) {
    requireNonNull(marker);
    limits.keySet().stream()
        .filter(s -> s.equals(marker))
        .findAny()
        .ifPresentOrElse(
            s -> limits.get(s).increment(),
            () -> limits.put(marker, new Limit(marker, limit, period)));
  }

  public void setLimit(String marker, long limit) {
    limits.keySet().stream()
        .filter(s -> s.equals(marker))
        .findAny()
        .ifPresent(s -> limits.get(s).setLimit(limit));
  }

  public void setPeriod(String marker, long period) {
    limits.keySet().stream()
        .filter(s -> s.equals(marker))
        .findAny()
        .ifPresent(s -> limits.get(s).setPeriod(period));
  }

  public Map<String, Limit> getLimits(){
    return new ConcurrentHashMap<>(limits);
  }

  public void resetLimits() {
    limits.values().forEach(Limit::clearTimer);
    limits.clear();
  }
}
