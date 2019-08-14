package com.zccs.assets_management.scheduled;

import com.zccs.assets_management.pojo.SysJobModel;
import com.zccs.assets_management.utils.BaseUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * @Description: JobDetail的concurrent设为true，支持多个job并发运行
 * @Author gaoh
 * @Date 2019/8/13
 */
public class JobFactoryConcurrent implements Job {

	/**
	 * 定时任务最终就是根据cron时间表达式 不断的调用 execute方法  然后通过反射执行具体的方法
	 * @param context
	 * @throws JobExecutionException
	 */
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		SysJobModel task = (SysJobModel) context.getMergedJobDataMap().get(SysJobModel.class.getSimpleName());
		BaseUtils.invokJobMethod(task);
	}
}