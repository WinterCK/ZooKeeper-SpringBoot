package com.cjk.zookeeper.component;

import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ZkIdGenerator implements InitializingBean {
	
	@Value("${zookeeper.connect}")
	private String path;
	
	private static final int DEFAULT_SESSION_TIMEOUT = 5000;
	
	private static ZooKeeper client = null;
	
	/** id根节点 */
	private static final String ROOT_PATH = "/zkId";
	
	/**
	 * 与zk连接成功后消除围栏
	 */
	private CountDownLatch latch = new CountDownLatch(1);

	/**
	 * 通过Spring扩展点InitializingBean保证bean初始化一次ZooKeeper
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		if (client == null) {
			client = new ZooKeeper(path, DEFAULT_SESSION_TIMEOUT,
					new Watcher() {
						@Override
						public void process(WatchedEvent event) {
							if (event.getState().equals(
									KeeperState.SyncConnected)) {
								latch.countDown();
							}

						}
					});
		}
		latch.await();
		if (client.exists(ROOT_PATH, false) == null) {
			client.create(ROOT_PATH, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
		
	}
	
	/**
	 * 根据版本号生成id
	 * @return
	 */
	public long nextId() {
		try {
            Stat stat = client.setData(ROOT_PATH, new byte[0], -1);
            return stat.getVersion();
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
        return -1;
	}

}
