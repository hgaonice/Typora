package com.zccs.assets_management.service;

import com.zccs.assets_management.dao.PictureDao;
import com.zccs.assets_management.dao.SysJobMapper;
import com.zccs.assets_management.exception.BusinessException;
import com.zccs.assets_management.pojo.SysJobModel;
import com.zccs.assets_management.scheduled.JobFactory;
import com.zccs.assets_management.scheduled.JobFactoryConcurrent;
import com.zccs.assets_management.utils.BaseUtils;
import com.zccs.assets_management.utils.DateFormatUtils;
import com.zccs.assets_management.utils.SysParameter;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.shiro.SecurityUtils;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.entity.Example.Criteria;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * @Description: 定时任务业务层
 * @author gaoh
 * @date 20190813
 *
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class SysJobService {

    @Resource
	private SqlSessionFactory sqlSessionFactory;

    @Resource
    private SysJobMapper sysJobMapper;

    @Resource
    private SchedulerFactoryBean schedulerFactoryBean; // 在quartz-model.xml配置注入

    private static Scheduler scheduler = null;

    public static String[] TASK_GROUP; //定时任务分组

    @Resource
    private PictureDao pictureDao;

    /**
     * 查询所有
     * @return
     */
    public List<SysJobModel> selectList() {
        return sysJobMapper.selectAll();
    }
    /**
     * 单条记录查询
     * @param model
     * @return
     */
    public SysJobModel selectOne(SysJobModel model) {
        return sysJobMapper.selectOne(model);
    }

    /**
     * 新增
     *
     * @param request
     * @throws Exception
     */
    @Transactional(rollbackFor = {Exception.class})
    public void add(HttpServletRequest request) throws Exception {
        String userName = SecurityUtils.getSubject().getSession().getAttribute(SysParameter.USER_NAME).toString();
        String taskJob = request.getParameter("taskJob");
        SysJobModel model = new SysJobModel();
        BaseUtils.serializeArray2Model(model, taskJob);

        if (!validCron(model.getCron().trim())) {
            throw new BusinessException("不是有效的cron表达式!");
        }

//        model.setId(CryptographyUtils.getUUID16());
        model.setCron(model.getCron().trim());
        model.setClassName(model.getClassName().trim());
        model.setMethod("execute");
        model.setIsEnable("0");

        model.setCreateBy(userName);
        model.setCreateDate(new Date());
        model.setUpdateBy(userName);
        model.setUpdateDate(new Date());

        sysJobMapper.insertSelective(model);
    }

    /**
     * 修改
     *
     * @param request
     * @throws Exception
     */
    @Transactional(rollbackFor = {Exception.class})
    public void update(HttpServletRequest request) throws Exception {
        String userName = SecurityUtils.getSubject().getSession().getAttribute(SysParameter.USER_NAME).toString();
        String taskJob = request.getParameter("taskJob");
        SysJobModel model = new SysJobModel();
        BaseUtils.serializeArray2Model(model, taskJob);

        if (!validCron(model.getCron().trim())) {
            throw new BusinessException("不是有效的cron表达式!");
        }

        model.setCron(model.getCron().trim());
        model.setClassName(model.getClassName().trim());
        model.setUpdateBy(userName);
        model.setUpdateDate(new Date());

        sysJobMapper.updateByPrimaryKeySelective(model);

    }


    /**
     * 删除
     * @param request
     * @throws Exception
     */
    @Transactional(rollbackFor = { Exception.class })
    public void delete(HttpServletRequest request) throws Exception {
        String ids = request.getParameter("ids");

        if (StringUtils.isNotBlank(ids)) {
            SqlSession sqlSession = null;
            try {
                sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
                // 必须用sqlSession获得mapper
                SysJobMapper mapper = sqlSession.getMapper(SysJobMapper.class);
                String[] idsTemp = ids.split("~");
                for (int i = 0; i < idsTemp.length; i++) {
                    SysJobModel model = new SysJobModel();
                    model.setId(Integer.parseInt(idsTemp[i]));
                    mapper.deleteByPrimaryKey(model);
                    // 批量提交 每50条提交一次
                    if ((i + 1) % 50 == 0) {
                        sqlSession.flushStatements();
                    }
                }
                sqlSession.flushStatements();
            }catch (Exception e){
                throw new BusinessException(e.getMessage());
            }finally {
                if(sqlSession != null){
                    sqlSession.close();
                }
            }
        } else {
            throw new BusinessException("请选择需要删除的数据！");
        }
    }

    /**
     * 启用定时任务
     * @param ids
     * @throws Exception
     */
    @Transactional(rollbackFor = { Exception.class })
    public void startJob(String ids) throws Exception {
        // 只有配置了job_group的服务才能进行修改时间表达式操作
        if (TASK_GROUP == null || TASK_GROUP.length == 0) {
            throw new BusinessException("系统未配置定时任务分组[TASK_GROUP]，没有操作权限!");
        }
        if (scheduler == null) {
            throw new BusinessException("数据库模式的定时任务初始化失败，请检查定时任务配置！");
        }

//        String ids = request.getParameter("ids");
        String[] idsTemp = ids.split("~");
        for (int i = 0; i < idsTemp.length; i++) {
            SysJobModel model = new SysJobModel();
            model.setId(Integer.parseInt(idsTemp[i]));
            model = sysJobMapper.selectByPrimaryKey(model);
            if(model == null){
                throw new BusinessException("定时任务[ID："+idsTemp[i]+"]不存在，请检查!");
            }
            if("0".equals(model.getIsEnable())) {
                // 开启定时任务
                // 反射检查定时任务类是否存在
                try{
                    Class.forName(model.getClassName());
                }catch(Exception e){
                    throw new BusinessException("定时任务["+model.getClassName()+"]不存在，请检查!");
                }
                operJob(model, 1);
                BaseUtils.loggerDebug(model.getName() + "已成功启动！");
            }else{
                continue;
            }

            model.setIsEnable("1");
            sysJobMapper.updateByPrimaryKeySelective(model);
        }
    }

    /**
     * 停用定时任务
     * @param request
     * @throws Exception
     */
    @Transactional(rollbackFor = { Exception.class })
    public void stopJob(HttpServletRequest request) throws Exception {
        // 只有配置了job_group的服务才能进行修改时间表达式操作
        if (TASK_GROUP == null || TASK_GROUP.length == 0) {
            throw new BusinessException("系统未配置定时任务分组[TASK_GROUP]，没有操作权限!");
        }
        if (scheduler == null) {
            throw new BusinessException("数据库模式的定时任务初始化失败，请检查定时任务配置！");
        }

        String ids = request.getParameter("ids");
        String[] idsTemp = ids.split("~");
        for (int i = 0; i < idsTemp.length; i++) {
            SysJobModel model = new SysJobModel();
            model.setId(Integer.parseInt(idsTemp[i]));
            model = sysJobMapper.selectByPrimaryKey(model);
            if(model == null){
                throw new BusinessException("定时任务(ID："+idsTemp[i]+")不存在，请检查!");
            }
            if("1".equals(model.getIsEnable())) {
                // 停用定时任务
                operJob(model, 0);
                BaseUtils.loggerDebug(model.getName() + "以停用！");
            }else{
                continue;
            }

            model.setIsEnable("0");
            sysJobMapper.updateByPrimaryKeySelective(model);
        }
    }

    /**
     * 更新时间规则
     *
     * @param jobModel
     * @throws Exception
     */
    @Transactional(rollbackFor = {Exception.class})
    public void editRuleExecute(SysJobModel jobModel) throws Exception {
      /*  String id = request.getParameter("id");
        String cron = request.getParameter("cron").trim();*/
        Integer id = jobModel.getId();
        String cron = jobModel.getCron();

        if (id==null) {
            throw new BusinessException("定时任务ID不能为空!");
        }
        if (StringUtils.isBlank(cron)) {
            throw new BusinessException("时间规则不能为空!");
        } else {
            if (!validCron(cron)) {
                throw new BusinessException("不是有效的cron表达式!");
            }
        }
        SysJobModel model = new SysJobModel();

        model.setId(id);
        model = sysJobMapper.selectByPrimaryKey(model);

        if (model == null) {
            throw new BusinessException("未找到该定时任务!");
        }
        // 只有配置了job_group的服务才能进行修改时间表达式操作
        if (TASK_GROUP == null || TASK_GROUP.length == 0) {
            throw new BusinessException("系统未配置定时任务分组[TASK_GROUP]，没有操作权限!");
        }
        if (scheduler == null) {
            throw new BusinessException("数据库模式的定时任务初始化失败，请检查定时任务配置！");
        }

        model.setCron(cron);
        sysJobMapper.updateByPrimaryKeySelective(model);

        if ("1".equals(model.getIsEnable())) { // 只有正在启用的定时任务才重新生成trigger
            TriggerKey triggerKey = TriggerKey.triggerKey(model.getName(), model.getGroupNo());
            CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
            if (trigger == null) {
                return;
            }

            // 重置时间
            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(model.getCron());
            trigger = trigger.getTriggerBuilder().withIdentity(triggerKey).withSchedule(scheduleBuilder).build();

            scheduler.rescheduleJob(triggerKey, trigger);
        }
    }

    /**
     * 检查定时任务名称是否重复
     * @param request
     * @return
     * @throws Exception
     * @throws Exception
     */
    public void sysJobCheckName(HttpServletRequest request) throws Exception {
        SysJobModel model = new SysJobModel();

        if (StringUtils.isBlank(request.getParameter("name"))) {
            return;
        } else {
                 model.setName(BaseUtils.encodeUTF8(request.getParameter("name")));
        }

        if (sysJobMapper.selectCount(model) > 0) {
            throw new BusinessException("名称已存在");
        }
    }

    /**
     * 检查定时任务调用类是否重复
     * @param request
     * @return
     * @throws Exception
     * @throws Exception
     */
    public void sysJobCheckClassName(HttpServletRequest request) throws Exception {
        SysJobModel model = new SysJobModel();

        if (StringUtils.isBlank(request.getParameter("className"))) {
            return;
        } else {
            model.setClassName(BaseUtils.encodeUTF8(request.getParameter("className")));
        }
        if (sysJobMapper.selectCount(model) > 0) {
            throw new BusinessException("调用类已存在");
        }
    }

    /**
     * 项目启动时调用的初始化job方法
     * 要在quartz.xml中配置
     */
    public void initJob() throws Exception {

        //加载配置文件中的分组信息
        String taskConfig = "";
        Object task_group = BaseUtils.getConfig().get("TASK_GROUP");
        if (task_group != null) {
             taskConfig = task_group.toString();
        }

        TASK_GROUP = StringUtils.isNotBlank(taskConfig) ? taskConfig.split(",") : new String[0];

        //获取分组任务信息数据，如果存在分组，再找分组里对应开启的任务
        if(TASK_GROUP == null || TASK_GROUP.length == 0 ){
            BaseUtils.loggerDebug("系统未配置定时任务分组[TASK_GROUP]...");
            return;
        }
        //初始化scheduler
        scheduler = schedulerFactoryBean.getScheduler();
        if (scheduler == null) {
            BaseUtils.loggerDebug("数据库模式的定时任务初始化失败，请检查定时任务配置！");
            return;
        }

        List<SysJobModel> jobList = new ArrayList();
        for(int i = 0; i < TASK_GROUP.length; i++){
            Example ex = new Example(SysJobModel.class);
            Criteria criteria = ex.createCriteria();
            criteria.andEqualTo("groupNo", TASK_GROUP[i]);
            List<SysJobModel> listJob = sysJobMapper.selectByExample(ex);
            jobList.addAll(listJob);
        }

        BaseUtils.loggerDebug("系统当前任务组中定时任务总数："+jobList.size()+"");

        for(int j = 0; j < jobList.size(); j++){
            SysJobModel model = jobList.get(j);
            if ("0".equals(model.getIsEnable())) {
                continue;
            }
            operJob(model, 1);
        }
    }

    /**
     * 操作job
     * @param model
     * @param sign 0停用 1启用
     * @throws SchedulerException
     */
    public void operJob(SysJobModel model, int sign) throws SchedulerException {
        if (model == null) {
            return;
        }
        //获取分组任务信息数据，如果存在分组，再找分组里对应开启的任务
        if(TASK_GROUP == null || TASK_GROUP.length == 0 ){
            throw new BusinessException("系统未配置定时任务分组[TASK_GROUP]...");
        }
        //初始化scheduler
        scheduler = schedulerFactoryBean.getScheduler();
        if (scheduler == null) {
            throw new BusinessException("数据库模式的定时任务初始化失败，请检查定时任务配置！");
        }

        //停用
        if(sign == 0){
            JobKey jobKey = JobKey.jobKey(model.getName(), model.getGroupNo());
            scheduler.deleteJob(jobKey);
        }else if(sign == 1){//启用
            // 往TriggerKey中插入name与Group,TriggerKey继承Key类,key类中有name与group两个字段,group默认为default
            TriggerKey triggerKey = TriggerKey.triggerKey(model.getName(), model.getGroupNo());
            // 创建trigger对象
            CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);

            //若trigger对象不存在，则创建一个
            if (trigger == null) {
                //判断job并发还是等待
                Class clazz = "0".equals(model.getIsConcurrent()) ? JobFactory.class : JobFactoryConcurrent.class;
                JobDetail jobDetail = JobBuilder.newJob(clazz).withIdentity(model.getName(), model.getGroupNo()).build();
                /**
                 * @see  JobFactoryConcurrent
                 * @see  JobFactory
                 * 将任务信息 存到任务详情里面  供反射调用
                 */
                jobDetail.getJobDataMap().put(SysJobModel.class.getSimpleName(), model);
                CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(model.getCron());
                trigger = TriggerBuilder.newTrigger().withIdentity(model.getName(), model.getGroupNo()).withSchedule(scheduleBuilder).startNow().build();

                scheduler.scheduleJob(jobDetail, trigger);
            } else {
                //Trigger已存在，那么更新相应的定时设置
                CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(model.getCron());
                //按新的cronExpression表达式重新构建trigger
                trigger = trigger.getTriggerBuilder().withIdentity(triggerKey).withSchedule(scheduleBuilder).build();

                //按新的trigger重新设置job执行
                scheduler.rescheduleJob(triggerKey, trigger);
            }
        }
    }

    /**
     * 校验cron表达式合法性
     * @param cron
     * @return
     */
    private static Boolean validCron(String cron){
        if(StringUtils.isBlank(cron)){
            return false;
        }
        String regMiao = "([0-9]{1,2}|[0-9]{1,2}\\-[0-9]{1,2}|\\*|[0-9]{1,2}\\/[0-9]{1,2}|[0-9]{1,2}\\,[0-9]{1,2})";
        String regFen = "\\s([0-9]{1,2}|[0-9]{1,2}\\-[0-9]{1,2}|\\*|[0-9]{1,2}\\/[0-9]{1,2}|[0-9]{1,2}\\,[0-9]{1,2})";
        String regShi = "\\s([0-9]{1,2}|[0-9]{1,2}\\-[0-9]{1,2}|\\*|[0-9]{1,2}\\/[0-9]{1,2}|[0-9]{1,2}\\,[0-9]{1,2})";
        String regRi = "\\s([0-9]{1,2}|[0-9]{1,2}\\-[0-9]{1,2}|\\*|[0-9]{1,2}\\/[0-9]{1,2}|[0-9]{1,2}\\,[0-9]{1,2}|\\?|L|W|C)";
        String regYue = "\\s([0-9]{1,2}|[0-9]{1,2}\\-[0-9]{1,2}|\\*|[0-9]{1,2}\\/[0-9]{1,2}|[0-9]{1,2}\\,[0-9]{1,2}|JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)";
        String regZhou = "\\s([1-7]{1}|[1-7]{1}\\-[1-7]{1}|[1-7]{1}\\#[1-7]{1}|\\*|[1-7]{1}\\/[1-7]{1}|[1-7]{1}\\,[1-7]{1}|MON|TUES|WED|THUR|FRI|SAT|SUN|\\?|L|C)";
        String regNian = "(\\s([0-9]{4}|[0-9]{4}\\-[0-9]{4}|\\*|[0-9]{4}\\/[0-9]{4}|[0-9]{4}\\,[0-9]{4})){0,1}";
        String regEx = regMiao + regFen + regShi + regRi + regYue + regZhou + regNian;
        //String regEx = "(((^([0-9]|[0-5][0-9])(\\,|\\-|\\/){1}([0-9]|[0-5][0-9]) )|^([0-9]|[0-5][0-9]) |^(\\* ))((([0-9]|[0-5][0-9])(\\,|\\-|\\/){1}([0-9]|[0-5][0-9]) )|([0-9]|[0-5][0-9]) |(\\* ))((([0-9]|[01][0-9]|2[0-3])(\\,|\\-|\\/){1}([0-9]|[01][0-9]|2[0-3]) )|([0-9]|[01][0-9]|2[0-3]) |(\\* ))((([0-9]|[0-2][0-9]|3[01])(\\,|\\-|\\/){1}([0-9]|[0-2][0-9]|3[01]) )|(([0-9]|[0-2][0-9]|3[01]) )|(\\? )|(\\* )|(([1-9]|[0-2][0-9]|3[01])L )|([1-7]W )|(LW )|([1-7]\\#[1-4] ))((([1-9]|0[1-9]|1[0-2])(\\,|\\-|\\/){1}([1-9]|0[1-9]|1[0-2]) )|([1-9]|0[1-9]|1[0-2]) |(\\* ))(([1-7](\\,|\\-|\\/){1}[1-7])|([1-7])|(\\?)|(\\*)|(([1-7]L)|([1-7]\\#[1-4]))))|(((^([0-9]|[0-5][0-9])(\\,|\\-|\\/){1}([0-9]|[0-5][0-9]) )|^([0-9]|[0-5][0-9]) |^(\\* ))((([0-9]|[0-5][0-9])(\\,|\\-|\\/){1}([0-9]|[0-5][0-9]) )|([0-9]|[0-5][0-9]) |(\\* ))((([0-9]|[01][0-9]|2[0-3])(\\,|\\-|\\/){1}([0-9]|[01][0-9]|2[0-3]) )|([0-9]|[01][0-9]|2[0-3]) |(\\* ))((([0-9]|[0-2][0-9]|3[01])(\\,|\\-|\\/){1}([0-9]|[0-2][0-9]|3[01]) )|(([0-9]|[0-2][0-9]|3[01]) )|(\\? )|(\\* )|(([1-9]|[0-2][0-9]|3[01])L )|([1-7]W )|(LW )|([1-7]\\#[1-4] ))((([1-9]|0[1-9]|1[0-2])(\\,|\\-|\\/){1}([1-9]|0[1-9]|1[0-2]) )|([1-9]|0[1-9]|1[0-2]) |(\\* ))(([1-7](\\,|\\-|\\/){1}[1-7] )|([1-7] )|(\\? )|(\\* )|(([1-7]L )|([1-7]\\#[1-4]) ))((19[789][0-9]|20[0-9][0-9])\\-(19[789][0-9]|20[0-9][0-9])))";
        Pattern pattern = Pattern.compile(regEx);
        Matcher matcher = pattern.matcher(cron);
        if(matcher.matches()){
            return true;
        }else{
            return false;
        }
    }


    /**
     * 获取定时任务详细信息
     * @param request
     * @return
     * @throws Exception
     */
    public Map<String, Object> sysJobGetDetail(HttpServletRequest request) throws Exception {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        String id = request.getParameter("id");
        if (StringUtils.isBlank(id)) {
            throw new BusinessException("定时任务ID不能为空，请检查！");
        }
        if (scheduler == null) {
            throw new BusinessException("数据库模式的定时任务初始化失败，请检查定时任务配置！");
        }

        GroupMatcher<JobKey> matcher = GroupMatcher.anyJobGroup();
        Set<JobKey> jobKeys = scheduler.getJobKeys(matcher);

        // 根据jobId查询jobMdoel
        SysJobModel jobModel = new SysJobModel();
        jobModel.setId(Integer.parseInt(id));
        jobModel = sysJobMapper.selectByPrimaryKey(jobModel);
        if(jobModel == null){
            throw new BusinessException("未找到ID为["+id+"]的数据，请检查！");
        }

//        List<SysJobModel> jobList = new ArrayList<SysJobModel>();
        for (JobKey jobKey : jobKeys) {
            List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
            for (Trigger trigger : triggers) {
                if(jobModel.getName().equals(jobKey.getName()) && jobModel.getGroupNo().equals(jobKey.getGroup())){
                    //                System.out.println("定时任务名称："+jobKey.getName());
                    //                System.out.println("定时任务分组："+jobKey.getGroup());
                    //                System.out.println("触发器key（分组+名称）："+trigger.getKey());
                    //                Trigger.TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());
                    //                String status = triggerState.name();
                    //                System.out.println("定时任务状态："+status);

                    if (trigger instanceof CronTrigger) {
                        CronTrigger cronTrigger = (CronTrigger) trigger;
//                        String cronExpression = cronTrigger.getCronExpression();
                        //                    System.out.println("cron表达式："+cronExpression);
                        //                    System.out.println("本次执行时间："+DateUtils.date2Str(cronTrigger.getStartTime(), null));
                        //                    System.out.println("下次执行时间："+DateUtils.date2Str(cronTrigger.getNextFireTime(), null));
                        // 下次执行时间传递至前台页面
                        resultMap.put("data", DateFormatUtils.date2Str(cronTrigger.getNextFireTime(), null));
                    }
                }else{
                    continue;
                }
            }
        }
        return resultMap;
    }
}
