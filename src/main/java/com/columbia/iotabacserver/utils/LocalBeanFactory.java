package com.columbia.iotabacserver.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class LocalBeanFactory implements ApplicationContextAware {
    private static ApplicationContext factory;


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (LocalBeanFactory.factory == null) {
            LocalBeanFactory.factory = applicationContext;
        }
    }

    public static ApplicationContext getFactory() {
        return factory;
    }

    public static Object getBean(String name) {
        return getFactory().getBean(name);
    }

    public static <T> T getBean(Class<T> clazz) {
        return getFactory().getBean(clazz);
    }

    public static <T> T getBean(String name, Class<T> clazz) {
        return getFactory().getBean(name, clazz);
    }
}
