package dev.onload.zookeeper.spring;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author echo huang
 * @version 1.0
 * @date 2019-09-22 18:57
 * @description
 */
@Configuration
public class CuratorConfig {
    /**
     * curator重试策略
     *
     * @return
     */
    @Bean
    public RetryPolicy retryPolicy() {
        return new RetryNTimes(3, 5000);
    }

    /**
     * zk客户端
     *
     * @param retryPolicy
     * @return
     */
    @Bean(initMethod = "start")
    public CuratorFramework client(@Qualifier("retryPolicy") RetryPolicy retryPolicy) {
        return CuratorFrameworkFactory.newClient("127.0.0.1", 10000, 5000, retryPolicy);
    }

}
