# 手撸Spring

## 实现Bean的定义、注册、获取

### 设计

在Spring框架中，Spring Bean 是由 Spring IoC 容器管理的对象。IoC（Inversion of Control）是一种设计模式，它反转了应用程序控制对象的方式。在这个模式中，对象的创建和管理被委托给容器，而不是由应用程序代码直接控制。

#### BeanDefinition（bean的定义类）

```java
public class BeanDefinition {
    private Class beanClass;

    public BeanDefinition (Class beanClass){
        this.beanClass = beanClass;
    }

    public Class getBeanClass() {
        return beanClass;
    }

    public void setBeanClass(Class beanClass) {
        this.beanClass = beanClass;
    }
}
```

之所以用class作为属性，我们可以从class中获取对象即Bean的各种信息，如类名、作用域、生命周期等。可以把 Bean 的实例化操作放到容器中处理

#### BeanFactoty

`BeanFactory` 是 Spring 框架中的一个核心接口，用于定义了访问 Spring IoC 容器的基本方法。它是 IoC 容器的根接口，定义了从容器中获取 Bean 实例的一些基本方法。`BeanFactory` 接口的主要实现类是 `DefaultListableBeanFactory`。

```java
public interface BeanFactory {

    Object getBean(String BeanName) throws BeansException;

}
```

#### BeanDefinitionRegistry

**该接口的主要目的是允许程序动态地注册和卸载 Bean 定义。**

```java
public interface BeanDefinitionRegistry {
    /**
     *向注册表中注册BeanDefinition
     * @param beanName
     * @param beanDefinition
     */
    void registerBeanDefinition(String beanName, BeanDefinition beanDefinition);
}
```

#### 模板模式

* 首先我们需要定义 BeanFactory 这样一个 Bean 工厂，提供 Bean 的获取方法 `getBean(String name)`，之后这个 Bean 工厂接口由抽象类 AbstractBeanFactory 实现。这样使用**模板模式**的设计方式，可以统一收口通用核心方法的调用逻辑和标准定义，也就很好的控制了后续的实现者不用关心调用逻辑，按照统一方式执行。那么类的继承者只需要关心具体方法的逻辑实现即可。

- 那么在继承抽象类 AbstractBeanFactory 后的 AbstractAutowireCapableBeanFactory 就可以实现相应的抽象方法了，因为 AbstractAutowireCapableBeanFactory 本身也是一个抽象类，所以它只会实现属于自己的抽象方法，其他抽象方法由继承 AbstractAutowireCapableBeanFactory 的类实现。这里就体现了类实现过程中的各司其职，你只需要关心属于你的内容，不是你的内容，不要参与。
- 另外这里还有块非常重要的知识点，就是关于单例 SingletonBeanRegistry 的接口定义实现，而 DefaultSingletonBeanRegistry 对接口实现后，会被抽象类 AbstractBeanFactory 继承。现在 AbstractBeanFactory 就是一个非常完整且强大的抽象类了，也能非常好的体现出它对模板模式的抽象定义。

### 实现

#### Spring Bean容器类关系

![](D:\IDEA\small-spring\images\spring-3-02.png)

- BeanFactory 的定义由 AbstractBeanFactory 抽象类实现接口的 getBean 方法
- 而 AbstractBeanFactory 又继承了实现了 SingletonBeanRegistry 的DefaultSingletonBeanRegistry 类。这样 AbstractBeanFactory 抽象类就具备了单例 Bean 的注册功能。
- AbstractBeanFactory 中又定义了两个抽象方法：getBeanDefinition(String beanName)、createBean(String beanName, BeanDefinition beanDefinition) ，而这两个抽象方法分别由 DefaultListableBeanFactory、AbstractAutowireCapableBeanFactory 实现。
- 最终 DefaultListableBeanFactory 还会继承抽象类 AbstractAutowireCapableBeanFactory 也就可以调用抽象类中的 createBean 方法了。

#### **单例注册接口定义和实现（DefaultSingletonBeanRegistry**）

```java
public class DefaultSingletonBeanRegistry implements SingletonBeanRegistry {
    //单例池
    private Map<String, Object> singletonObjects = new HashMap<>();
    @Override
    public Object getSingleton(String beanName) {
        return singletonObjects.get(beanName);
    }

    /**
     * 注册bean
     * @param beanName
     * @param singletonObject
     */
    protected void addSingleton(String beanName, Object singletonObject) {
        singletonObjects.put(beanName, singletonObject);
    }
}
```

- 在 DefaultSingletonBeanRegistry 中主要实现 getSingleton 方法，维护了单例池，同时实现了一个受保护的 addSingleton 方法即注册bean单例到池中，这个方法可以被继承此类的其他类调用。包括：AbstractBeanFactory 以及继承的 DefaultListableBeanFactory 调用。这样子类也就具备了单例注册的功能。

#### 抽象类定义模板方法(AbstractBeanFactory)

```java
public abstract class AbstractBeanFactory extends DefaultSingletonBeanRegistry implements BeanFactory {
    @Override
    public Object getBean(String BeanName) throws BeansException {
        Object bean = getSingleton(BeanName);
        //如果单例池中有bean则返回
        if (bean != null) {
            return bean;
        }
        //单例池中无bean则创建一个bean加入单例池
        BeanDefinition beanDefinition = getBeanDefinition(BeanName);
        return crateBean(BeanName, beanDefinition);
    }

    protected abstract Object crateBean(String name, BeanDefinition beanDefinition) throws BeansException;

    protected abstract BeanDefinition getBeanDefinition(String beanName) throws BeansException;
    
}
```

- AbstractBeanFactory 首先继承了 DefaultSingletonBeanRegistry，也就具备了使用单例注册类方法。
- 接下来很重要的一点是关于接口 BeanFactory 的实现，在方法 getBean 的实现过程中可以看到，主要是对单例 Bean 对象的获取以及在获取不到时需要拿到 Bean 的定义做相应 Bean 实例化操作。那么 getBean 并没有自身的去实现这些方法，而是只定义了调用过程以及提供了抽象方法，由实现此抽象类的其他类做相应实现。
- 后续继承抽象类 AbstractBeanFactory 的类有两个，包括：AbstractAutowireCapableBeanFactory、DefaultListableBeanFactory，这两个类分别做了相应的实现处理。

### 实例化Bean类(AbstractAutowireCapableBeanFactory)

```java
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory {
    @Override
    protected Object crateBean(String name, BeanDefinition beanDefinition) throws BeansException {
        Object bean = null;
        try {
            bean = beanDefinition.getBeanClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new BeansException("实例化失败",e);
        }
        //注册
        addSingleton(name,bean);
        return bean;
    }
}
```

- 在 AbstractAutowireCapableBeanFactory 类中实现了 Bean 的实例化操作 `newInstance`，其实这块会埋下一个坑，有构造函数入参的对象怎么处理？
- 在处理完 Bean 对象的实例化后，直接调用 `addSingleton` 方法存放到单例对象的缓存中去。
- 这个类继承了模板类，也就拥有了单例注册的能力，同时又实现了实例化的方法，模板类的单例获取方法被实现了，因此这个类还拥有单例获取的能力，即如果获取不到就创建一个bean放入单例池中的能力。

### 核心类实现(DefaultListableBeanFactory)

```java
public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory implements BeanDefinitionRegistry {
    //BeanDefinition注册表
    private Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();

    @Override
    public BeanDefinition getBeanDefinition(String beanName) throws BeansException {
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        if(beanDefinition==null) throw new BeansException("注册表中不存在"+beanName);
        return beanDefinition;
    }

    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) {
        beanDefinitionMap.put(beanName,beanDefinition);
    }
}
```

- DefaultListableBeanFactory 在 Spring 源码中也是一个非常核心的类，在目前的实现中也是逐步贴近于源码，与源码类名保持一致。
- DefaultListableBeanFactory 继承了 AbstractAutowireCapableBeanFactory 类，也就具备了接口 BeanFactory 和 AbstractBeanFactory 等一连串的功能实现。*所以有时候你会看到一些类的强转，调用某些方法，也是因为你强转的类实现接口或继承了某些类。*
- 除此之外这个类还实现了接口 BeanDefinitionRegistry 中的 registerBeanDefinition(String beanName, BeanDefinition beanDefinition) 方法，当然你还会看到一个 getBeanDefinition 的实现，这个方法我们文中提到过它是抽象类 AbstractBeanFactory 中定义的抽象方法。*现在注册Bean定义与获取Bean定义就可以同时使用了，是不感觉这个套路还蛮深的。接口定义了注册，抽象类定义了获取，都集中在 DefaultListableBeanFactory 中的 beanDefinitionMap 里*

这个类拥有了 AbstractAutowireCapableBeanFactory 单例bean的能力之外，实现了beandefinition的定义注册和获取。

## 基于Cglib实现含构造函数的类实例化策略