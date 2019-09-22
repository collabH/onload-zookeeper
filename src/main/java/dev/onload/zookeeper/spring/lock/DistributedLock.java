package dev.onload.zookeeper.spring.lock;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

/**
 * @author echo huang
 * @version 1.0
 * @date 2019-09-22 20:33
 * @description 分布式锁
 */
@Component
@Slf4j
public class DistributedLock {
    @Autowired
    private CuratorFramework client;

    /**
     * 用于阻塞获取不到锁的请求
     */
    private static CountDownLatch ZK_LOCK_LATCH = new CountDownLatch(1);
    private static final String NAMESPACE_LOCK = "lock";
    private static final String LOCK_PREFIX = "/";
    private static final String ZK_LOCK_PROJECT = "onload";
    private static final String DISTRIBUTED_LOCK = "distributed_lock";

    @PostConstruct
    public void init() {
        client = client.usingNamespace(NAMESPACE_LOCK);
        try {
            //锁子节点不存在，就创建
            if (Objects.isNull(client.checkExists().forPath(LOCK_PREFIX + ZK_LOCK_PROJECT))) {
                client.create()
                        .withMode(CreateMode.PERSISTENT)
                        .forPath(LOCK_PREFIX + ZK_LOCK_PROJECT);
            }
            //针对zk的分布式锁节点，创建响应的watcher事件监听
            //用于释放锁， 删除锁节点触发事件后，获取接触CountDownLatch的await
            addWatcherToLock(LOCK_PREFIX + ZK_LOCK_PROJECT);
        } catch (Exception e) {
            log.info("客户端链接zookeeper服务器错误");
        }
    }

    private void addWatcherToLock(String nodePath) throws Exception {
        final PathChildrenCache cache = new PathChildrenCache(client, nodePath, true);
        cache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
        cache.getListenable()
                .addListener((curatorFramework, event) -> {
                            if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_REMOVED)) {
                                String path = event.getData().getPath();
                                log.info("上一个会话以及释放锁或者该会话已断开");
                                if (path.contains(DISTRIBUTED_LOCK)) {
                                    log.info("释放计数器");
                                    ZK_LOCK_LATCH.countDown();
                                }
                            }
                        }
                );
    }

    public void getLock() {
        while (true) {
            try {
                client.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.EPHEMERAL)
                        .forPath(LOCK_PREFIX + ZK_LOCK_PROJECT + LOCK_PREFIX + DISTRIBUTED_LOCK);
                log.info("获取锁成功");
                return;
            } catch (Exception e) {
                log.info("获取锁失败");
                try {
                    if (ZK_LOCK_LATCH.getCount() <= 0) {
                        ZK_LOCK_LATCH = new CountDownLatch(1);
                    }
                    //阻塞线程
                    ZK_LOCK_LATCH.await();
                } catch (Exception a) {
                    a.printStackTrace();
                }
            }
        }
    }

    public boolean releaseLock() {
        try {
            if (client.checkExists().forPath(LOCK_PREFIX + ZK_LOCK_PROJECT + LOCK_PREFIX + DISTRIBUTED_LOCK) != null) {
                client.delete().forPath(LOCK_PREFIX + ZK_LOCK_PROJECT + LOCK_PREFIX + DISTRIBUTED_LOCK);
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
