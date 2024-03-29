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
public class QueryAndWatch{
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
}
    
```
### zk子节点查询和监听
```java
public class ZKGetChildrenList implements Watcher {

    private ZooKeeper zookeeper = null;

    public static final String zkServerPath = "localhost:2181";
    public static final Integer timeout = 5000;

    public ZKGetChildrenList() {
    }

    public ZKGetChildrenList(String connectString) {
        try {
            zookeeper = new ZooKeeper(connectString, timeout, new ZKGetChildrenList());
        } catch (IOException e) {
            e.printStackTrace();
            if (zookeeper != null) {
                try {
                    zookeeper.close();
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    private static CountDownLatch countDown = new CountDownLatch(1);

    public static void main(String[] args) throws Exception {

        ZKGetChildrenList zkServer = new ZKGetChildrenList(zkServerPath);

        /**
         * 参数：
         * path：父节点路径
         * watch：true或者false，注册一个watch事件
         */
//		List<String> strChildList = zkServer.getZookeeper().getChildren("/name", true);
//		for (String s : strChildList) {
//			System.out.println(s);
//		}

        // 异步调用
        String ctx = "{'callback':'ChildrenCallback'}";
//		zkServer.getZookeeper().getChildren("/name", true, new ChildrenCallBack(), ctx);
        zkServer.getZookeeper().getChildren("/name", true, (i, s, o, s1) -> {
            log.warn("{},{},{},{}", i, s, o, s1);
        }, ctx);

        countDown.await();
    }

    @Override
    public void process(WatchedEvent event) {
        try {
            //监听子节点变化
            if (event.getType() == EventType.NodeChildrenChanged) {
                System.out.println("NodeChildrenChanged");
                ZKGetChildrenList zkServer = new ZKGetChildrenList(zkServerPath);
                List<String> strChildList = zkServer.getZookeeper().getChildren(event.getPath(), false);
                for (String s : strChildList) {
                    System.out.println(s);
                }
                countDown.countDown();
            } else if (event.getType() == EventType.NodeCreated) {
                System.out.println("NodeCreated");
            } else if (event.getType() == EventType.NodeDataChanged) {
                System.out.println("NodeDataChanged");
            } else if (event.getType() == EventType.NodeDeleted) {
                System.out.println("NodeDeleted");
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public ZooKeeper getZookeeper() {
        return zookeeper;
    }

    public void setZookeeper(ZooKeeper zookeeper) {
        this.zookeeper = zookeeper;
    }

}
```

### zk权限控制 ACL
```java
public class ZKNodeAcl implements Watcher {

    private ZooKeeper zookeeper = null;

    public static final String zkServerPath = "127.0.0.1:2181";
    public static final Integer timeout = 5000;

    public ZKNodeAcl() {
    }

    public ZKNodeAcl(String connectString) {
        try {
            zookeeper = new ZooKeeper(connectString, timeout, new ZKNodeAcl());
        } catch (IOException e) {
            e.printStackTrace();
            if (zookeeper != null) {
                try {
                    zookeeper.close();
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    public void createZKNode(String path, byte[] data, List<ACL> acls) {

        String result = "";
        try {
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
            result = zookeeper.create(path, data, acls, CreateMode.PERSISTENT);
            System.out.println("创建节点：\t" + result + "\t成功...");
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {

        ZKNodeAcl zkServer = new ZKNodeAcl(zkServerPath);

        /**
         * ======================  创建node start  ======================
         */
        // acl 任何人都可以访问
        //zkServer.createZKNode("/test", "test".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE);

        // 自定义用户认证访问
//		List<ACL> acls = new ArrayList<ACL>();
//		Id name1 = new Id("digest", AclUtils.getDigestUserPwd("name:123456"));
//		Id name2 = new Id("digest", AclUtils.getDigestUserPwd("name:123456"));
//		acls.add(new ACL(ZooDefs.Perms.ALL, name1));
//		acls.add(new ACL(ZooDefs.Perms.READ, name2));
//		acls.add(new ACL(ZooDefs.Perms.DELETE | ZooDefs.Perms.CREATE, name2));
//		zkServer.createZKNode("/test/testdigest", "testdigest".getBytes(), acls);

        // 注册过的用户必须通过addAuthInfo才能操作节点，参考命令行 addauth
//        zkServer.getZookeeper().addAuthInfo("digest", "name:123456".getBytes());
//        zkServer.createZKNode("/test/testdigest/childtest", "childtest".getBytes(), ZooDefs.Ids.CREATOR_ALL_ACL);
//        Stat stat = new Stat();
//        byte[] data = zkServer.getZookeeper().getData("/test/testdigest", false, stat);
//        System.out.println(new String(data));
//        zkServer.getZookeeper().setData("/test/testdigest", "now".getBytes(), 0);

        // ip方式的acl
//        List<ACL> aclsIP = new ArrayList<ACL>();
//        Id ipId1 = new Id("ip", "127.0.0.1");
//        aclsIP.add(new ACL(ZooDefs.Perms.ALL, ipId1));
//        zkServer.createZKNode("/test/iptest6", "iptest".getBytes(), aclsIP);

        // 验证ip是否有权限
        zkServer.getZookeeper().setData("/test/iptest6", "now".getBytes(), 0);
        Stat stat = new Stat();
        byte[] data = zkServer.getZookeeper().getData("/test/iptest6", false, stat);
        System.out.println(new String(data));
        System.out.println(stat.getVersion());
    }

    public ZooKeeper getZookeeper() {
        return zookeeper;
    }

    public void setZookeeper(ZooKeeper zookeeper) {
        this.zookeeper = zookeeper;
    }

    @Override
    public void process(WatchedEvent event) {

    }
}
```
# Apache curator zk客户端
## 相比于原生客户端的优势
* 解决watcher的注册一次就失效
* 提供更多解决方案并且实现简单：比如 分布式锁
* 提供常用的Zookeeper工具类
* 支持节点的递归创建


## curator基本操作
```java
public class CuratorOperator {
    private CuratorFramework client = null;
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
        curatorOperator.client.delete()
                .guaranteed()
                .deletingChildrenIfNeeded()
                .withVersion(2)
                .forPath(path);
        Thread.sleep(3000);
        curatorOperator.close();
        System.out.println(curatorOperator.client.isStarted());
    }
}
```
## add Watcher事件
### 使用NodeCache监听节点信息
优点:**解决原生添加一次Watcher只能触发一次问题**
```java
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
```
### 使用PathChildrenCache监听父节点的增删改事件
```java
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
```
## Curator的Acl
```java
public class CuratorAcl {

    public CuratorFramework client = null;
    public static final String zkServerPath = "127.0.0.1:2181";

    public CuratorAcl() {
        RetryPolicy retryPolicy = new RetryNTimes(3, 5000);
        //add添加authInfo
        client = CuratorFrameworkFactory.builder().authorization("digest", "name2:123456".getBytes())
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

    public static void main(String[] args) throws Exception {
        // 实例化
        CuratorAcl cto = new CuratorAcl();
        boolean isZkCuratorStarted = cto.client.isStarted();
        System.out.println("当前客户的状态：" + (isZkCuratorStarted ? "连接中" : "已关闭"));

        String nodePath = "/acl/father/child/sub/h";

        List<ACL> acls = new ArrayList<ACL>();
        Id name1 = new Id("digest", AclUtils.getDigestUserPwd("name1:123456"));
        Id name2 = new Id("digest", AclUtils.getDigestUserPwd("name2:123456"));
        acls.add(new ACL(Perms.ALL, name1));
        acls.add(new ACL(Perms.READ, name2));
        acls.add(new ACL(Perms.DELETE | Perms.CREATE, name2));

        /**
         * withACL:
         *
         *applyToParents:如果applyToParents为true，则aclList应用于创建的父类。现有父节点不受影响。
         */
        // 创建节点
//        byte[] data = "spiderman".getBytes();
//        cto.client.create().creatingParentsIfNeeded()
//                .withMode(CreateMode.PERSISTENT)
//                .withACL(acls, true)
//                .forPath(nodePath, data);

        //修改权限
       // cto.client.setACL().withACL(acls).forPath("/curatorNode");

        // 更新节点数据 name2没有权限
		byte[] newData = "batman".getBytes();
		cto.client.setData().withVersion(0).forPath(nodePath, newData);

        // 删除节点
//		cto.client.delete().guaranteed().deletingChildrenIfNeeded().withVersion(0).forPath(nodePath);

        // 读取节点数据
//		Stat stat = new Stat();
//		byte[] data = cto.client.getData().storingStatIn(stat).forPath(nodePath);
//		System.out.println("节点" + nodePath + "的数据为: " + new String(data));
//		System.out.println("该节点的版本号为: " + stat.getVersion());


        cto.closeZKClient();
        boolean isZkCuratorStarted2 = cto.client.isStarted();
        System.out.println("当前客户的状态：" + (isZkCuratorStarted2 ? "连接中" : "已关闭"));
    }

}
```

## 基于Curator的分布式锁测试
```
根据jmeter测试得到
线程组设置如下:
线程数:2
Ramp-up:0
结果如下:
2019-09-22 21:13:57,405 [http-nio-9000-exec-3] [INFO] 获取锁成功
2019-09-22 21:13:57,405 [http-nio-9000-exec-11] [INFO] 获取锁失败
```

**不足之处**
* 获得锁通过CountDownLatch和死循环，每次计数器countDown后，就会new一个新的，如果突然大并发上来，就会new多个CountDownLatch对象，造成内存溢出危险
* 并且全部都会阻塞在此，导致整个服务线程被打满，很有核心也会发生jvm线程阻塞，后期请求将会卡死在此