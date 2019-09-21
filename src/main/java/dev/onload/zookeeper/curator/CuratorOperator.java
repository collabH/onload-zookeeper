package dev.onload.zookeeper.curator;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.data.Stat;

/**
 * @author echo huang
 * @version 1.0
 * @date 2019-09-19 22:51
 * @description curator操作类
 */
public class CuratorOperator {
    public CuratorFramework client = null;
    private static final String zkConnection = "127.0.0.1:2181";

    public CuratorOperator() {
        /**
         * 同步创建zk示例，原生api是异步的
         * 重试策略，在重试之间增加睡眠时间，重试一组次数
         * curator链接zookeeper的策略:ExponentialBackoffRetry
         * baseSleepTimeMs：初始sleep的时间
         * maxRetries：最大重试次数
         * maxSleepMs：最大重试时间
         */
        //RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 5);

        /**
         * curator链接zookeeper的策略:RetryNTimes 重试n次
         * n：重试的次数
         * sleepMsBetweenRetries：每次重试间隔的时间
         */
        RetryPolicy retryPolicy = new RetryNTimes(3, 5000);

        /**
         * curator链接zookeeper的策略:RetryOneTime 重试一次
         * sleepMsBetweenRetry:每次重试间隔的时间
         */
//		RetryPolicy retryPolicy2 = new RetryOneTime(3000);

        /**
         * 永远重试，不推荐使用
         */
//		RetryPolicy retryPolicy3 = new RetryForever(retryIntervalMs)

        /**
         * 重试策略，重试直到指定的时间过期为止
         * curator链接zookeeper的策略:RetryUntilElapsed
         * maxElapsedTimeMs:最大重试时间
         * sleepMsBetweenRetries:每次重试间隔
         * 重试时间超过maxElapsedTimeMs后，就不再重试
         */
//		RetryPolicy retryPolicy4 = new RetryUntilElapsed(2000, 3000);

        //重试策略，在重试之间以增加的睡眠时间(最多达到最大限度)重试一组次数
        //RetryPolicy retryPolicy=new BoundedExponentialBackoffRetry();
        client = CuratorFrameworkFactory.builder()
                .connectString(zkConnection)
                .sessionTimeoutMs(10000).retryPolicy(retryPolicy)
                //根节点为namespace
                .namespace("namespace").build();
        client.start();
    }

    private void close() {
        client.close();
    }

    public static void main(String[] args) throws Exception {
        /**
         * creatingParentsIfNeeded递归创建节点
         * /test/a/b/c
         */
        CuratorOperator curatorOperator = new CuratorOperator();
        String path = "/super/name";
        //add node
//        curatorOperator.client.create()
//                .creatingParentsIfNeeded()
//                .withMode(CreateMode.PERSISTENT)
//                .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
//                .forPath(path);
        //update node
//        curatorOperator.client.setData()
//                .withVersion(0)
//                .forPath(path, "zhangsan".getBytes());
        //delete node
        /**
         * guaranteed:防止发生网络抖动时，成功的请求可能没有返回客户端，guaranteed可以保证节点被删除
         * deletingChildrenIfNeeded:如果有子节点，就删除
         */
//        curatorOperator.client.delete()
//                .guaranteed()
//                .deletingChildrenIfNeeded()
//                .withVersion(2)
//                .forPath(path);
        //read node
        /**
         * storingStatIn:拉取stat值
         */
//        Stat stat = new Stat();
//        byte[] bytes = curatorOperator.client
//                .getData()
//                .storingStatIn(stat)
//                .forPath(path);
//        System.out.println(new String(bytes, StandardCharsets.UTF_8));
//        System.out.println(stat.getVersion());
        //query childrenNode
//        Stat stat = new Stat();
//        List<String> childrenNodes = curatorOperator.client
//                .getChildren()
//                .storingStatIn(stat)
//                .forPath("/super");
//        childrenNodes.forEach(System.out::println);
        //判断节点是否存在
        Stat stat = curatorOperator.client
                .checkExists()
                .forPath(path);
        System.out.println(stat == null ? "不存在" : stat.getVersion());
        Thread.sleep(3000);
        curatorOperator.close();
        System.out.println(curatorOperator.client.isStarted());
    }
}
