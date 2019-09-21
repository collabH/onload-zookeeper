package dev.onload.zookeeper.curator;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;

import java.nio.charset.StandardCharsets;
import java.util.List;


/**
 * @author echo huang
 * @version 1.0
 * @date 2019-09-21 22:51
 * @description 监听节点是否增删改操作，监听父节点的子节点
 */
@Slf4j
public class PathChildrenCacheWatcherListener {
    public static void main(String[] args) throws Exception {
        CuratorOperator operator = new CuratorOperator();
        String path = "/super";
        /**
         * cacheData:设置缓存节点的数据状态
         * StartMode：初始化方式
         * 异步方式不会有数据响应，所以数据无法得到
         * POST_INITIALIZED_EVENT:异步初始化，初始化之后会触发事件
         * NORMAL:异步初始化
         * BUILD_INITIAL_CACHE: 同步初始化
         */
        PathChildrenCache cache = new PathChildrenCache(operator.client, path, true);
        cache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
        List<ChildData> childDataList = cache.getCurrentData();
        childDataList.forEach(
                childData -> {
                    String data = new String(childData.getData(), StandardCharsets.UTF_8);
                    log.warn("data:{}", data);
                }
        );
        cache.getListenable()
                .addListener((curatorFramework, event) -> {
                    if (event.getType().equals(PathChildrenCacheEvent.Type.INITIALIZED)) {
                        System.out.println("子节点初始化ok...");
                    } else if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_ADDED)) {
                        String path1 = event.getData().getPath();
                        if (path1.equals("/super/aa")) {
                            System.out.println("添加子节点:" + event.getData().getPath());
                            System.out.println("子节点数据:" + new String(event.getData().getData()));
                        } else if (path1.equals("/super/namez")) {
                            System.out.println("添加不正确...");
                        }

                    } else if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_REMOVED)) {
                        System.out.println("删除子节点:" + event.getData().getPath());
                    } else if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_UPDATED)) {
                        System.out.println("修改子节点路径:" + event.getData().getPath());
                        System.out.println("修改子节点数据:" + new String(event.getData().getData()));
                    }
                });
        Thread.sleep(100000);
    }
}
