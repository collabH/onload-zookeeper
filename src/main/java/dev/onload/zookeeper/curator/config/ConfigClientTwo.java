package dev.onload.zookeeper.curator.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.retry.RetryNTimes;

import java.util.concurrent.CountDownLatch;

/**
 * @author echo huang
 * @version 1.0
 * @date 2019-09-21 23:57
 * @description
 */
@Slf4j
public class ConfigClientTwo {
    public CuratorFramework client = null;
    public static final String zkServerPath = "127.0.0.1:2181";

    public ConfigClientTwo() {
        RetryPolicy retryPolicy = new RetryNTimes(3, 5000);
        client = CuratorFrameworkFactory.builder()
                .connectString(zkServerPath)
                .sessionTimeoutMs(10000).retryPolicy(retryPolicy)
                .namespace("namespace").build();
        client.start();
    }

    public void closeZKClient() {
        if (client != null) {
            this.client.close();
        }
    }

    public static CountDownLatch countDown = new CountDownLatch(1);
    public final static String CONFIG_NODE_PATH = "/super/redis";
    public final static String SUB_PATH = "/redis-config";
    public static void main(String[] args) throws Exception {
        ConfigClientTwo cto = new ConfigClientTwo();
        System.out.println("ConfigClientOne 启动成功...");

        final PathChildrenCache childrenCache = new PathChildrenCache(cto.client, CONFIG_NODE_PATH, true);
        //同步启动
        childrenCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);

        // 添加监听事件
        childrenCache.getListenable().addListener((client, event) -> {
            // 监听节点变化
            if(event.getType().equals(PathChildrenCacheEvent.Type.CHILD_UPDATED)){
                String configNodePath = event.getData().getPath();
                if (configNodePath.equals(CONFIG_NODE_PATH + SUB_PATH)) {
                    System.out.println("监听到配置发生变化，节点路径为:" + configNodePath);

                    // 读取节点数据
                    String jsonConfig = new String(event.getData().getData());
                    System.out.println("节点" + CONFIG_NODE_PATH + "的数据为: " + jsonConfig);
                    //todo 将配置转换，作出对应操作
                    log.warn("将jsonConfig配置抓换为pojo");

                    //todo 修改各个机器上的配置文件，然后重启服务
                }
            }
        });

        countDown.await();
        cto.closeZKClient();
    }
}
