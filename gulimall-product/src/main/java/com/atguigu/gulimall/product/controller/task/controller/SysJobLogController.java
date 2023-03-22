package com.atguigu.gulimall.product.controller.task.controller;


import com.atguigu.gulimall.product.jobtask.domain.SysJobLog;
import com.atguigu.gulimall.product.service.task.service.ISysJobLogService;
import com.atguigu.common.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 调度日志操作处理
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/job/log")
public class SysJobLogController
{
    @Autowired
    private ISysJobLogService jobLogService;

    /**
     * 查询定时任务调度日志列表
     */
    @GetMapping("/list")
    public R list(SysJobLog sysJobLog)
    {
        List<SysJobLog> list = jobLogService.selectJobLogList(sysJobLog);
        return R.ok().put("data",list);
    }

    /**
     * 导出定时任务调度日志列表
     */
    /*@PostMapping("/export")
    public void export(HttpServletResponse response, SysJobLog sysJobLog)
    {
        List<SysJobLog> list = jobLogService.selectJobLogList(sysJobLog);
        ExcelUtil<SysJobLog> util = new ExcelUtil<SysJobLog>(SysJobLog.class);
        util.exportExcel(response, list, "调度日志");
    }*/

    /**
     * 根据调度编号获取详细信息
     */
    @GetMapping(value = "/{configId}")
    public R getInfo(@PathVariable Long jobLogId)
    {
        return R.ok().put("data",jobLogService.selectJobLogById(jobLogId));
    }

    /**
     * 删除定时任务调度日志
     */
    @DeleteMapping("/{jobLogIds}")
    public R remove(@PathVariable Long[] jobLogIds)
    {
        return R.ok().put("data",jobLogService.deleteJobLogByIds(jobLogIds));
    }

    /**
     * 清空定时任务调度日志
     */
    @DeleteMapping("/clean")
    public R clean()
    {
        jobLogService.cleanJobLog();
        return R.ok();
    }
}
