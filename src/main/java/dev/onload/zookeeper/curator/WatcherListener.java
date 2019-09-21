package dev.onload.zookeeper.curator;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.recipes.cache.NodeCache;


/**
 * @author echo huang
 * @version 1.0
 * @date 2019-09-21 22:51
 * @description 给node添加watcher
 */
@Slf4j
public class WatcherListener {
    public static void main(String[] args) throws Exception {
        CuratorOperator operator = new CuratorOperator();
        String path = "/super/name";
        //watcher事件 当使用usingWatcher的时候，监听只会触发一次，监听完毕后就销毁
      /*  byte[] curatorWatch = operator.client
                .getData()
                .usingWatcher(new MyCuratorWatcher())
                .forPath(path);
        byte[] watch = operator.client
                .getData()
                .usingWatcher(new MyCuratorWatcher())
                .forPath(path);
        log.warn("curatorWatch{}", new String(curatorWatch, StandardCharsets.UTF_8));
        log.warn("watch{}", new String(watch, StandardCharsets.UTF_8));*/
        /**
         * 为节点添加watcher
         * NodeCache:监听数据节点的变更，会触发事件
         * 注册一次，触发多次
         * dataIsCompressed:是否压缩数据，true为是，false为否
         */
        NodeCache nodeCache = new NodeCache(operator.client, path);
        //buildInitial：初始化的时候获取node的值并且缓存到本地
        nodeCache.start(true);
        if (nodeCache.getCurrentData() != null) {
            log.warn("节点初始化数据为:{}", new String(nodeCache.getCurrentData().getData()));
        } else {
            log.warn("节点初始化数据为空");
        }
        nodeCache.getListenable()
                .addListener(() -> log.warn("节点初始化数据为:{}", new String(nodeCache.getCurrentData().getData())));
        Thread.sleep(100000);
    }

}
