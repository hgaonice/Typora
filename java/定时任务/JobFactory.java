package com.zccs.assets_management.scheduled;

import com.zccs.assets_management.pojo.SysJobModel;
import com.zccs.assets_management.utils.BaseUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * @Description: JobDetail的concurrent设为false，禁止并发执行多个相同定义的JobDetail，标签@DisallowConcurrentExecution控制禁止并发
 * @Author gaoh
 * @Date 2019/8/13
 * @see DisallowConcurrentExecution   设置不允许并发
 */
@DisallowConcurrentExecution
public class JobFactory implements Job {

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		SysJobModel task = (SysJobModel) context.getMergedJobDataMap().get(SysJobModel.class.getSimpleName());
		BaseUtils.invokJobMethod(task);
	}
}