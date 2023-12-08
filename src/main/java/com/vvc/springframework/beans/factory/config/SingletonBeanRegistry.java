package com.vvc.springframework.beans.factory.config;

/**
 * @author vvc
 * 义了一个获取单例对象的接口
 */
public interface SingletonBeanRegistry {
    Object getSingleton(String beanName);
}
