package dev.onload.zookeeper.spring;

import dev.onload.zookeeper.spring.lock.DistributedLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author echo huang
 * @version 1.0
 * @date 2019-09-22 20:02
 * @description
 */
@SpringBootApplication
@RestController
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Autowired
    private ZkCurator zkCurator;

    @GetMapping("/isConnection")
    public String lock() {
        return zkCurator.isZkAlive() ? "链接" : "没有链接";
    }

    @Autowired
    private DistributedLock distributedLock;

    @GetMapping("lock")
    public boolean lock1() {
        distributedLock.getLock();
        return distributedLock.releaseLock();
    }
}
