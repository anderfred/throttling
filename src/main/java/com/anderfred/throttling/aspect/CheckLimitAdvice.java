package com.anderfred.throttling.aspect;

import com.anderfred.throttling.annotations.ThrottleHandler;
import com.anderfred.throttling.config.ThrottlingConfig;
import com.anderfred.throttling.handler.LimitHandler;
import java.lang.reflect.Method;
import javax.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class CheckLimitAdvice {
  final LimitHandler limitHandler;
  final ThrottlingConfig config;
  private final String[] IP_HEADER_CANDIDATES = {
    "X-Forwarded-For",
    "Proxy-Client-IP",
    "WL-Proxy-Client-IP",
    "HTTP_X_FORWARDED_FOR",
    "HTTP_X_FORWARDED",
    "HTTP_X_CLUSTER_CLIENT_IP",
    "HTTP_CLIENT_IP",
    "HTTP_FORWARDED_FOR",
    "HTTP_FORWARDED",
    "HTTP_VIA",
    "REMOTE_ADDR"
  };

  @Autowired
  public CheckLimitAdvice(LimitHandler limitHandler, ThrottlingConfig config) {
    this.limitHandler = limitHandler;
    this.config = config;
  }

  @Before("@annotation(com.anderfred.throttling.annotations.ThrottleHandler)")
  public void check(JoinPoint joinPoint) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = signature.getMethod();
    ThrottleHandler annotation = method.getAnnotation(ThrottleHandler.class);
    long period = annotation.period() > 0 ? annotation.period() : config.getPeriod();
    long limit = annotation.limit() > 0 ? annotation.limit() : config.getLimit();
    limitHandler.checkLimit(
        getClientIpAddressIfServletRequestExist() + "_" + joinPoint.getSignature(), limit, period);
  }

  public String getClientIpAddressIfServletRequestExist() {

    if (RequestContextHolder.getRequestAttributes() == null) {
      return "0.0.0.0";
    }

    HttpServletRequest request =
        ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    for (String header : IP_HEADER_CANDIDATES) {
      String ipList = request.getHeader(header);
      if (ipList != null && ipList.length() != 0 && !"unknown".equalsIgnoreCase(ipList)) {
        return ipList.split(",")[0];
      }
    }

    return request.getRemoteAddr();
  }
}
