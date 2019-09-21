package dev.onload.zookeeper.curator.watch;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

/**
 * @author echo huang
 * @version 1.0
 * @date 2019-09-21 22:58
 * @description
 */
@Slf4j
public class MyWatcher implements Watcher {
    @Override
    public void process(WatchedEvent watchedEvent) {
        log.warn("MyWatcher 监听路径:{},stat:{}", watchedEvent.getPath(), watchedEvent.getState());
    }
}
