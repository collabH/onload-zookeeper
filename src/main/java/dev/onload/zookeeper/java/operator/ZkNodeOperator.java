package dev.onload.zookeeper.java.operator;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author echo huang
 * @version 1.0
 * @date 2019-09-17 00:17
 * @description 操作类封装
 */
@Slf4j
public class ZkNodeOperator implements Watcher {
    private ZooKeeper zk = null;
    private static final Integer timeout = 5000;
    private static final String zkServerPath = "127.0.0.1:2181";

    private static final CountDownLatch LATCH = new CountDownLatch(1);
    private static final Stat stat = new Stat();

    public ZkNodeOperator() {

    }

    public ZkNodeOperator(String connectString) {
        try {
            zk = new ZooKeeper(connectString, timeout, this);
        } catch (IOException e) {
            log.warn("e", e);
            if (zk != null) {
                try {
                    zk.close();
                } catch (InterruptedException ex) {
                    log.warn("e", e);
                }
            }
        }
    }


    public String createNode(String path, byte[] data, List<ACL> aclList) {
        String result = "";
        /**
         * 同步或者异步创建节点，都不支持子节点的递归创建，异步有一个callback函数
         * 参数：
         * path：创建的路径
         * data：存储的数据的byte[]
         * acl：控制权限策略
         * 			Ids.OPEN_ACL_UNSAFE --> world:anyone:cdrwa
         * 			CREATOR_ALL_ACL --> auth:user:password:cdrwa
         * createMode：节点类型, 是一个枚举
         * 			PERSISTENT：持久节点
         * 			PERSISTENT_SEQUENTIAL：持久顺序节点
         * 			EPHEMERAL：临时节点
         * 			EPHEMERAL_SEQUENTIAL：临时顺序节点
         */
        //同步创建临时节点
//            result = zk.create(path, data, aclList, CreateMode.EPHEMERAL);
        try {
            String ctx = "{'create':'success'}";
            zk.create(path, data, aclList, CreateMode.PERSISTENT, (rc, p, c, name) -> {
                log.warn("创建节点:{}", path);
                log.warn("rc:{},ctx:{},name:{}", rc, c, name);
            }, ctx);
            log.warn("create result:{}", result);
            //fixme 睡眠为了防止节点还没创建成功，zk客户端就断开连接
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;

    }

    /**
     * 修改节点数据
     *
     * @param path
     * @param data
     * @param version
     */
    public void setNode(String path, byte[] data, int version) {/*
        try {

            //同步修改
            Stat stat = zk.setData(path, data, version);

            log.warn("stat:{}", stat);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }*/
        //异步修改
        String ctx = "{'create':'success'}";
        zk.setData(path, data, version, (rc, p, c, name) -> {
            log.warn("创建节点:{}", p);
            log.warn("rc:{},ctx:{},name:{}", rc, c, name);
        }, ctx);
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void deleteNode(String path, int version) {
        try {
            //同步删除
            zk.delete(path, version);
            //异步删除
            //zk.delete(path, version, (i, s, o) -> log.warn("创建节点:{},{}.{}", i,s,o), "zhangsan");
        } catch (InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }

    /**
     * 查询
     *
     * @param path
     * @param stat
     * @return
     */
    public byte[] queryNode(String path, boolean watch, Stat stat) {
        byte[] data = new byte[0];
        try {
            /**
             * 参数
             * path:节点路径
             * watch:true或false 注册一个watch事件
             * stat:状态
             */
            data = zk.getData(path, watch, stat);
            log.warn(new String(data, Charset.defaultCharset()));
            log.warn("version :{}", stat.getVersion());
            LATCH.await();

        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return data;
    }

    /**
     * 查询子节点
     *
     * @param path
     * @param watch
     * @return
     */
    public List<String> queryChirldNode(String path, boolean watch) {
        List<String> children = null;
        try {
            children = zk.getChildren(path, watch);
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return children;
    }

    public void isExists(String path, boolean watch) {
        Stat stat = null;
        try {
            stat = zk.exists(path, watch);
            log.warn(String.valueOf(stat.getVersion()));
            Thread.sleep(3000);
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        log.warn("event:{}", watchedEvent);
        try {
            switch (watchedEvent.getType()) {
                case NodeDataChanged:
                    byte[] bytes = zk.getData("/name", false, stat);
                    log.warn(new String(bytes, Charset.defaultCharset()));
                    log.warn("version变化:{}", stat.getVersion());
                    LATCH.countDown();
                    break;
                case NodeCreated:
                    break;
                case NodeDeleted:
                    break;
                case NodeChildrenChanged:
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ZkNodeOperator zkNodeOperator = new ZkNodeOperator(zkServerPath);
        //zkNodeOperator.createNode("/test1", "zhangsan".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE);
        //zkNodeOperator.setNode("/test1", "luoquanwg".getBytes(), 1);
        //zkNodeOperator.deleteNode("/test1", 1);
      //  zkNodeOperator.queryNode("/name", true, stat);
        zkNodeOperator.isExists("/name",false);
    }

}
