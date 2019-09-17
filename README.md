# Zookeeper的Java原生客户端使用

## 获取连接
```java
public class ZookeeperConnection implements Watcher {
    final static Logger log = LoggerFactory.getLogger(ZookeeperConnection.class);

    public static final String zkServerPath = "127.0.0.1:2181";
    public static final Integer timeout = 5000;

    public static void main(String[] args) throws Exception {
        /**
         * 客户端和zk服务端链接是一个异步的过程
         * 当连接成功后后，客户端会收的一个watch通知
         *
         * 参数：
         * connectString：连接服务器的ip字符串，
         * 		比如: "192.168.1.1:2181,192.168.1.2:2181,192.168.1.3:2181"
         * 		可以是一个ip，也可以是多个ip，一个ip代表单机，多个ip代表集群
         * 		也可以在ip后加路径
         * sessionTimeout：超时时间，心跳收不到了，那就超时
         * watcher：通知事件，如果有对应的事件触发，则会收到一个通知；如果不需要，那就设置为null
         * canBeReadOnly：可读，当这个物理机节点断开后，还是可以读到数据的，只是不能写，
         * 					       此时数据被读取到的可能是旧数据，此处建议设置为false，不推荐使用
         * sessionId：会话的id
         * sessionPasswd：会话密码	当会话丢失后，可以依据 sessionId 和 sessionPasswd 重新获取会话
         */
        ZooKeeper zk = new ZooKeeper(zkServerPath, timeout, new ZookeeperConnection());

        log.warn("客户端开始连接zookeeper服务器...");
        log.warn("连接状态：{}", zk.getState());

        Thread.sleep(2000);

        log.warn("连接状态：{}", zk.getState());
    }

    public void process(WatchedEvent watchedEvent) {
        log.warn("接受到watch通知：{}", watchedEvent);
    }
}

```

## Zk会话重连机制
```java
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

```

## Zk基本节点增删改查操作
### Zk封装的操作类
```java
public class ZkNodeOperator implements Watcher {
    private ZooKeeper zk = null;
    private static final Integer timeout = 5000;
    private static final String zkServerPath = "127.0.0.1:2181";

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

    @Override
    public void process(WatchedEvent watchedEvent) {
        log.warn("event:{}", watchedEvent);
    }

    public static void main(String[] args) {
        ZkNodeOperator zkNodeOperator = new ZkNodeOperator(zkServerPath);
        //zkNodeOperator.createNode("/test1", "zhangsan".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE);
        //zkNodeOperator.setNode("/test1", "luoquanwg".getBytes(), 1);
        zkNodeOperator.deleteNode("/test1", 1);
    }

}
```
### zk节点查询和监听
```java

 private static final CountDownLatch LATCH = new CountDownLatch(1);
 private static final Stat stat = new Stat();

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
    
```