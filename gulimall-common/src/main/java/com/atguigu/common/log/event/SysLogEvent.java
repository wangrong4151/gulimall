package com.atguigu.common.log.event;

import com.atguigu.common.log.DTO.SysLogDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author CC
 * @date 2021/1/15 系统日志事件
 */
@Getter
@AllArgsConstructor
public class SysLogEvent {
    private final SysLogDTO sysLog;
}
