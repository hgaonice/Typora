package com.zccs.assets_management.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * 普通类调用Spring注解方式的Service层bean
 */
@Component
public class SpringBeanFactoryUtils implements ApplicationContextAware {

	private static ApplicationContext context;

	/**
	 * 可以把ApplicationContext对象inject到当前类中作为一个静态成员变量
	 * @param applicationContext ApplicationContext 对象.
	 * @throws BeansException
	 */
	@Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		context = applicationContext;
	}

	/**
	 * 获取ApplicationContext
	 * @return
	 */
	public static ApplicationContext getApplicationContext() {
		return context;
	}

	/**
	 * 一个便利的方法，能够快速得到一个bean
	 * @param beanName
	 * @return
	 */
	public static Object getBeanByName(String beanName) {
		return context.getBean(beanName);
	}

	/**
	 * 一个便利的方法，根据能够快速得到一个bean
	 * @param t
	 * @param <T>
	 * @return
	 */
    public static <T> Object getBeanByClass(Class<T> t) {
        return context.getBean(t);
    }
    
}