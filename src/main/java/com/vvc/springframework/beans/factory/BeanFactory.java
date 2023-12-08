package com.vvc.springframework.beans.factory;

import com.vvc.springframework.beans.BeansException;

public interface BeanFactory {

    Object getBean(String BeanName) throws BeansException;

    Object getBean(String name, Object... args) throws BeansException;

}
