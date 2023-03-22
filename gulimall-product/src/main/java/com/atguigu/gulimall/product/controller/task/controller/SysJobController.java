package com.atguigu.gulimall.product.controller.task.controller;


import com.atguigu.gulimall.product.jobtask.domain.SysJob;
import com.atguigu.gulimall.product.service.task.service.ISysJobService;
import com.atguigu.gulimall.product.jobtask.util.TaskException;
import com.atguigu.common.utils.R;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 调度任务信息操作处理
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/job")
public class SysJobController
{
    @Autowired
    private ISysJobService jobService;

    /**
     * 查询定时任务列表
     */
    @GetMapping("/list")
    public R list(SysJob sysJob)
    {
        List<SysJob> list = jobService.selectJobList(sysJob);

        return R.ok().put("data",list);
    }

    /**
     * 导出定时任务列表
     */
    /*@PostMapping("/export")
    public void export(HttpServletResponse response, SysJob sysJob)
    {
        List<SysJob> list = jobService.selectJobList(sysJob);
        ExcelUtil<SysJob> util = new ExcelUtil<SysJob>(SysJob.class);
        util.exportExcel(response, list, "定时任务");
    }*/

    /**
     * 获取定时任务详细信息
     */
    @GetMapping(value = "/{jobId}")
    public R getInfo(@PathVariable("jobId") Long jobId)
    {
        return R.ok().put("data",jobService.selectJobById(jobId));
    }

    /**
     * 新增定时任务
     */
    @PostMapping
    public R add(@RequestBody SysJob job) throws SchedulerException, TaskException
    {

        return R.ok().put("data",jobService.insertJob(job));
    }

    /**
     * 修改定时任务
     */
    @PutMapping
    public R edit(@RequestBody SysJob job) throws SchedulerException, TaskException
    {

        return R.ok().put("data",jobService.updateJob(job));
    }

    /**
     * 定时任务状态修改
     */
    @PutMapping("/changeStatus")
    public R changeStatus(@RequestBody SysJob job) throws SchedulerException
    {
        SysJob newJob = jobService.selectJobById(job.getJobId());
        newJob.setStatus(job.getStatus());
        return R.ok().put("data",jobService.changeStatus(newJob));
    }

    /**
     * 定时任务立即执行一次
     */
    @PutMapping("/run")
    public R run(@RequestBody SysJob job) throws SchedulerException
    {
        jobService.run(job);
        return R.ok();
    }

    /**
     * 删除定时任务
     */
    @DeleteMapping("/{jobIds}")
    public R remove(@PathVariable Long[] jobIds) throws SchedulerException, TaskException
    {
        jobService.deleteJobByIds(jobIds);
        return R.ok();
    }
}
