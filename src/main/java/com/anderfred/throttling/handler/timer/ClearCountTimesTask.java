package com.anderfred.throttling.handler.timer;

import com.anderfred.throttling.handler.Limit;
import java.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClearCountTimesTask extends TimerTask {
  private final Limit limit;
  Logger log = LoggerFactory.getLogger(ClearCountTimesTask.class);

  public ClearCountTimesTask(Limit limit) {
    this.limit = limit;
  }

  @Override
  public void run() {
    limit.clearCount();
    log.debug(String.format("Cleared count on marker:[%s]", limit.getMarker()));
  }
}
