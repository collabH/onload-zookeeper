package dev.onload.zookeeper.java;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author echo huang
 * @version 1.0
 * @date 2019-09-16 23:58
 * @description zk 恢复之前的会话连接
 */
public class ZkConnectionSessionWatcher implements Watcher {
    final static Logger log = LoggerFactory.getLogger(ZkConnectionSessionWatcher.class);

    public static final String zkServerPath = "127.0.0.1:2181";
    public static final Integer timeout = 5000;

    public static void main(String[] args) throws Exception {

        ZooKeeper zk = new ZooKeeper(zkServerPath, timeout, new ZkConnectionSessionWatcher());
        long sessionId = zk.getSessionId();
        byte[] sessionPasswd = zk.getSessionPasswd();
        log.warn("客户端开始连接zookeeper服务器...");
        log.warn("连接状态：{}session id:{},pwd:{}", zk.getState(),sessionId,sessionPasswd);
        Thread.sleep(2000);
        log.warn("连接状态：{}session id:{},pwd:{}", zk.getState(),sessionId,sessionPasswd);
        zk = new ZooKeeper(zkServerPath, timeout, new ZkConnectionSessionWatcher(), sessionId, sessionPasswd);
        log.warn("重新连接状态：{}", zk.getState());
        Thread.sleep(2000);
        log.warn("重新连接状态：{}", zk.getState());
    }

    public void process(WatchedEvent watchedEvent) {
        log.warn("接受到watch通知：{}", watchedEvent);
    }
}
