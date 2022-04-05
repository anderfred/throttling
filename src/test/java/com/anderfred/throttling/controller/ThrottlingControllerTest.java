package com.anderfred.throttling.controller;

import static java.util.Objects.nonNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.anderfred.throttling.config.ThrottlingConfig;
import com.anderfred.throttling.handler.LimitHandler;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ThrottlingControllerTest {
  private static final String testMethodPath = "/test";
  private static final int limitExceedCode = 502;
  @Autowired ThrottlingController controller;

  @Autowired LimitHandler handler;

  @MockBean ThrottlingConfig config;

  @Autowired MockMvc mockMvc;

  @BeforeEach
  void clearContext() {
    handler.resetLimits();
  }

  @Test
  void shouldPassWithCustomConfigValues() throws Exception {
    final long requestsLimit = 10L;
    final long period = 1000 * 60;
    when(config.getLimit()).thenReturn(requestsLimit);
    when(config.getPeriod()).thenReturn(period);

    makeNumberOfRequests(testMethodPath, requestsLimit - 2, null);
    this.mockMvc.perform(get(testMethodPath)).andExpect(status().isOk());
  }

  @Test
  void shouldPassWithChangeLimitOnRuntime() throws Exception {
    final long requestsLimit = 10L;
    final long period = 1000 * 60;
    when(config.getLimit()).thenReturn(requestsLimit);
    when(config.getPeriod()).thenReturn(period);
    makeNumberOfRequests(testMethodPath, requestsLimit, null);
    this.mockMvc.perform(get(testMethodPath)).andExpect(status().is(limitExceedCode));
    handler.getLimits().values().stream()
        .findAny()
        .ifPresent(limit -> limit.setLimit(requestsLimit * 3));
    makeNumberOfRequests(testMethodPath, requestsLimit, null);
    this.mockMvc.perform(get(testMethodPath)).andExpect(status().isOk());
    makeNumberOfRequests(testMethodPath, requestsLimit, null);
    this.mockMvc.perform(get(testMethodPath)).andExpect(status().is(limitExceedCode));
  }

  @Test
  void shouldFailWithRequestsExceedsNumber() throws Exception {
    final long requestsLimit = 100L;
    final long period = 1000 * 60;
    when(config.getLimit()).thenReturn(requestsLimit);
    when(config.getPeriod()).thenReturn(period);

    makeNumberOfRequests(testMethodPath, requestsLimit, null);
    this.mockMvc.perform(get(testMethodPath)).andExpect(status().is(limitExceedCode));
  }

  @Test
  void shouldPassWithWaitingPeriod() throws Exception {
    final long requestsLimit = 10L;
    final long period = 1000;
    when(config.getLimit()).thenReturn(requestsLimit);
    when(config.getPeriod()).thenReturn(period);
    makeNumberOfRequests(testMethodPath, requestsLimit - 2, null);
    this.mockMvc.perform(get(testMethodPath)).andExpect(status().isOk());
  }

  @Test
  void shouldFailWithTimeLimitThenPassWithCleared() throws Exception {
    final long requestsLimit = 10L;
    final long period = 1000;
    when(config.getLimit()).thenReturn(requestsLimit);
    when(config.getPeriod()).thenReturn(period);
    makeNumberOfRequests(testMethodPath, requestsLimit + 1, null);
    this.mockMvc.perform(get(testMethodPath)).andExpect(status().is(limitExceedCode));
    Thread.sleep(period + 10);
    this.mockMvc.perform(get(testMethodPath)).andExpect(status().isOk());
  }

  @Test
  void shouldPassForTwoIpAddressesLimit() throws Exception {
    final long requestsLimit = 10L;
    final long period = 1000;
    final String firstIpAddress = getRandomIpAddress();
    final String secondIpAddress = getRandomIpAddress();
    when(config.getLimit()).thenReturn(requestsLimit);
    when(config.getPeriod()).thenReturn(period);
    makeNumberOfRequests(testMethodPath, requestsLimit - 2, firstIpAddress);
    this.mockMvc
        .perform(
            get(testMethodPath)
                .with(
                    request -> {
                      request.setRemoteAddr(firstIpAddress);
                      return request;
                    }))
        .andExpect(status().isOk());
    this.mockMvc
        .perform(
            get(testMethodPath)
                .with(
                    request -> {
                      request.setRemoteAddr(firstIpAddress);
                      return request;
                    }))
        .andExpect(status().is(limitExceedCode));
    makeNumberOfRequests(testMethodPath, requestsLimit - 2, secondIpAddress);
    this.mockMvc
        .perform(
            get(testMethodPath)
                .with(
                    request -> {
                      request.setRemoteAddr(secondIpAddress);
                      return request;
                    }))
        .andExpect(status().isOk());
    this.mockMvc
        .perform(
            get(testMethodPath)
                .with(
                    request -> {
                      request.setRemoteAddr(secondIpAddress);
                      return request;
                    }))
        .andExpect(status().is(limitExceedCode));
  }

  @Test
  void shouldPassWithMultipleThreadsExecution() throws Exception {

    final long requestsLimit = 100L;
    final long period = 10000;
    final int parts = 4;
    final long part = (requestsLimit / parts);
    ThreadPoolExecutor poolExecutor = new ScheduledThreadPoolExecutor(parts);
    when(config.getLimit()).thenReturn(requestsLimit);
    when(config.getPeriod()).thenReturn(period);

    for (int i = 0; i < parts; i++)
      poolExecutor.execute(
          () -> {
            try {
              makeNumberOfRequests(testMethodPath, part, null);
            } catch (Exception e) {
              e.printStackTrace();
            }
          });
    awaitTerminationAfterShutdown(poolExecutor);
    this.mockMvc.perform(get(testMethodPath)).andExpect(status().isOk());
    makeNumberOfRequests(testMethodPath, parts, null);
    this.mockMvc.perform(get(testMethodPath)).andExpect(status().is(limitExceedCode));
  }

  public void awaitTerminationAfterShutdown(ExecutorService threadPool) {
    threadPool.shutdown();
    try {
      if (!threadPool.awaitTermination(100, TimeUnit.MILLISECONDS)) {
        threadPool.shutdownNow();
      }
    } catch (InterruptedException ex) {
      threadPool.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  private void makeNumberOfRequests(String path, long count, String address) throws Exception {
    for (long i = 0; i < count; i++) {
      if (nonNull(address))
        this.mockMvc.perform(
            get(path)
                .with(
                    request -> {
                      request.setRemoteAddr(address);
                      return request;
                    }));
      else this.mockMvc.perform(get(path));
    }
  }

  private String getRandomIpAddress() {
    Random r = new Random();
    return r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(256);
  }
}
