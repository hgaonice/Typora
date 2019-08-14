package com.zccs.assets_management.scheduled.task;

import com.alibaba.fastjson.JSON;
import com.zccs.assets_management.dao.SysJobMapper;
import com.zccs.assets_management.pojo.SysJobModel;
import com.zccs.assets_management.utils.BaseUtils;
import com.zccs.assets_management.utils.SpringBeanFactoryUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * @author gaoh
 * @version 1.0
 * @date 2019/8/13 14:42
 */
@Component
public class TestJob {

    //每十秒执行一次
//    @Scheduled(cron = "0/10 * * * * ? *")
    @Transactional(rollbackFor = {Exception.class})
    public void execute() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.CHINA);// 输出北京时间
        BaseUtils.loggerDebug("定时任务：" + sdf.format(new Date()) + "");
        BaseUtils.loggerDebug("TestJob-定时任务！");
        SysJobMapper bean = SpringBeanFactoryUtils.getApplicationContext().getBean(SysJobMapper.class);
        List<SysJobModel> sysJobModels = bean.selectAll();
        for (SysJobModel jobModel : sysJobModels) {
            BaseUtils.loggerDebug(JSON.toJSONString(jobModel));
        }
    }
}
