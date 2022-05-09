package com.atguigu.common.log.event;

import com.atguigu.common.log.DTO.SysLogDTO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import sun.security.util.SecurityConstants;

/**
 * @author CC
 * @date 2021/1/15异步监听日志事件
 */
@Slf4j
@AllArgsConstructor
public class SysLogListener {

    //private final RemoteLogService remoteLogService;

    @Async
    @Order
    @EventListener(SysLogEvent.class)
    public void saveSysLog(SysLogEvent event) {
        SysLogDTO sysLog = event.getSysLog();
        //remoteLogService.saveLog(sysLog, SecurityConstants.FROM_IN);
        log.info("保存日志到数据库");
    }

}

