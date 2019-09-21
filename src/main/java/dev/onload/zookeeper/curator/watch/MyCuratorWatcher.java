package dev.onload.zookeeper.curator.watch;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.zookeeper.WatchedEvent;

/**
 * @author echo huang
 * @version 1.0
 * @date 2019-09-21 22:53
 * @description
 */
@Slf4j
public class MyCuratorWatcher implements CuratorWatcher {
    @Override
    public void process(WatchedEvent watchedEvent) throws Exception {
        log.warn("MyCuratorWatcher 监听路径:{},stat:{}", watchedEvent.getPath(), watchedEvent.getState());
    }
}
