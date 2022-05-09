package com.atguigu.common.log.aspect;

import com.atguigu.common.log.DTO.SysLogDTO;
import com.atguigu.common.log.annotation.SysLog;
import com.atguigu.common.log.event.SysLogEvent;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationEventPublisher;

@Slf4j
@Aspect
@RequiredArgsConstructor
public class SysLogAspect {

    private final ApplicationEventPublisher publisher;

    //private final KeyStrResolver hospitalKeyStrResolver;

    @SneakyThrows
    @Around("@annotation(sysLog)")
    public Object around(ProceedingJoinPoint point, SysLog sysLog) {
        String strClassName = point.getTarget().getClass().getName();
        String strMethodName = point.getSignature().getName();
        log.debug("[类名]:{},[方法]:{}", strClassName, strMethodName);

        //SysLogDTO logDTO = SysLogUtils.getSysLog();
        //logDTO.setTitle(sysLog.value());
        // 发送异步日志事件
        Long startTime = System.currentTimeMillis();
        Object obj = point.proceed();
        Long endTime = System.currentTimeMillis();
        //logDTO.setTime(endTime - startTime);
        //logDTO.setHospitalId(Integer.parseInt(hospitalKeyStrResolver.key()));

        //例子
        SysLogDTO logDTO = new SysLogDTO();
        publisher.publishEvent(new SysLogEvent(logDTO));
        return obj;
    }

}
