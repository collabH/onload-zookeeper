package dev.onload.zookeeper.spring;

import org.apache.curator.framework.CuratorFramework;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author echo huang
 * @version 1.0
 * @date 2019-09-22 19:08
 * @description
 */
@Component
public class ZkCurator {
    @Autowired
    private CuratorFramework client;

    @PostConstruct
    public void init() {
        client.usingNamespace("lock");
    }

    public boolean isZkAlive() {
        return client.isStarted();
    }
}
