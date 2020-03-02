package com.cjk.zookeeper.distribute.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.springframework.util.StringUtils;

import com.cjk.common.exception.ZooException;
import com.cjk.zookeeper.distribute.inter.DistributedLock;

public class ZooKeeperExclusiveLock extends DistributedLock {

	/**
	 * zookeeper节点的默认分隔符
	 */
	private final static String SEPARATOR = "/";
	/**
	 *  排他锁在zk中的根节点
	 */
	private final static String ROOT_EXCLUSIVE_NODE = SEPARATOR + "exclusive_lock";
	
	/**
	 * 默认的EPHEMERAL节点的超时时间，单位毫秒 临时节点
	 */
	private static final int DEFAULT_SESSION_TIMEOUT = 5000;
	/**
	 * 竞争者节点，每个想要尝试获得锁的节点都会获得一个竞争者节点
	 */
	private static final String COMPETITOR_NODE = "lock_node";
	
	/**
	 * 统一的zooKeeper连接，在Init的时候初始化
	 */
	private static ZooKeeper exclusiveZk = null;
	
	/**
	 * 与zk连接成功后消除围栏
	 */
	private CountDownLatch latch = new CountDownLatch(1);
	
	/**
	 * 等待前一个节点Watcher通知的围栏
	 * 至少前一个节点处理完毕， 说明才获取到锁
	 */
	private CountDownLatch getTheLocklatch = new CountDownLatch(1);
	
	/**
	 * 加锁的业务类型名
	 */
	private String lockName = null;

	/** 根节点的路径 持久节点 */
	private String rootPath = null;

	/** 业务锁的路径，持久节点 */
	private String lockPath = null;
	
	/** 创建的竞争者的路径 */
	private String competitorPath = null;
	
	/** 竞争到的锁的具体路径 */
	private String thisCompetitorPath = null;

	/** 判断出需要等待的节点路径 */
	private String waitCompetitorPath = null;
	
	@Override
	public void releaseLock() {
		if(StringUtils.isEmpty(rootPath) || StringUtils.isEmpty(lockName)
				|| exclusiveZk == null) {
			throw new ZooException("you can not release anyLock before you dit not initial connectZookeeper");
		}
		try {
			// -1 version匹配任意znode版本
			exclusiveZk.delete(thisCompetitorPath, -1);
		} catch (InterruptedException e) {
			throw new ZooException(
					"the release lock has been Interrupted ");
		} catch (KeeperException e) {
			throw new ZooException(
					"zookeeper connect error");
		}
		
	}
	
	/**
	 * 创建竞争者节点(分布式创建，目前是顺序临时节点)
	 * 可以改成临时节点 只有一个能创建成功
	 * 
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	private void createComPertitorNode() throws KeeperException,
			InterruptedException {
		competitorPath = lockPath + SEPARATOR + COMPETITOR_NODE;
		thisCompetitorPath = exclusiveZk.create(competitorPath, null,
				Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
	}

	@Override
	public boolean tryLock() {
		if(StringUtils.isEmpty(rootPath) || StringUtils.isEmpty(lockName)
				|| exclusiveZk == null) {
			throw new ZooException("you can not release anyLock before you dit not initial connectZookeeper");
		}
		List<String> allCompetitorList = null;
		try {
		createComPertitorNode();
		allCompetitorList = exclusiveZk.getChildren(lockPath, false);
		} catch (KeeperException e) {
			throw new ZooException("zookeeper connect error");
		} catch (InterruptedException e) {
			throw new ZooException(
					"the try lock has been Interrupted " ,e);
		}
		Collections.sort(allCompetitorList);
		// 判断出在List中的索引
		int index = allCompetitorList.indexOf(thisCompetitorPath
				.substring((lockPath + SEPARATOR).length()));
		if (index == -1) {
			throw new ZooException("competitorPath not exit after create");
		} else if (index == 0) {// 如果发现自己就是最小节点,那么说明本人获得了锁
			return true;
		} else {// 说明自己不是最小节点
			return false;
		}
	}

	@Override
	public boolean tryLock(int timeout) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void lock() {
		if(StringUtils.isEmpty(rootPath) || StringUtils.isEmpty(lockPath)
				|| exclusiveZk == null) {
			throw new ZooException("you can not release anyLock before you dit not initial connectZookeeper");
		}
		List<String> allCompetitorList = null;
		try {
			createComPertitorNode();
			allCompetitorList = exclusiveZk.getChildren(lockPath, false);
		} catch (KeeperException e) {
			throw new ZooException("zookeeper connect error");
		} catch (InterruptedException e) {
			throw new ZooException("the lock has  been Interrupted");
		}
		// 默认升序排列
		Collections.sort(allCompetitorList);
		int index = allCompetitorList.indexOf(thisCompetitorPath
				.substring((lockPath + SEPARATOR).length()));
		if (index == -1) {
			throw new ZooException("competitorPath not exit after create");
		} else if (index == 0) {// 如果发现自己就是最小节点,那么说明本人获得了锁
			return;
		} else {
			// 说明自己不是最小节点
			// 获取前一个节点，监听Watcher，前一个处理完自己再处理
			waitCompetitorPath = lockPath + SEPARATOR + allCompetitorList.get(index - 1);
            // 在waitPath上注册监听器, 当waitPath被删除时, zookeeper会回调监听器的process方法
           Stat waitNodeStat;
			try {
				waitNodeStat = exclusiveZk.exists(waitCompetitorPath, new Watcher() {
					@Override
					public void process(WatchedEvent event) {
						if (event.getType().equals(EventType.NodeDeleted)&&event.getPath().equals(waitCompetitorPath)) {
							// 当等待节点被删除后， 取消阻塞
							getTheLocklatch.countDown();
						}
					}
				});
				 if (waitNodeStat==null) {
					 	//如果运行到此处发现前面一个节点已经不存在了。说明前面的进程已经释放了锁
		            	return;
					}else {
						// 阻塞直到Watcher通知
						getTheLocklatch.await();
						return;
					}
			} catch (KeeperException e) {
				throw new ZooException("zookeeper connect error");
			} catch (InterruptedException e) {
				throw new ZooException("the lock has been Interrupted");
			}
		}
		
	}
	
	
	public void connectZooKeeper(String zkhosts, String lockName)
			throws KeeperException, InterruptedException,
			IOException {
		Stat rootStat = null;
		Stat lockStat = null;
		if (StringUtils.isEmpty(zkhosts)) {
			throw new ZooException("zookeeper hosts can not be blank");
		}
		if (StringUtils.isEmpty(lockName)) {
			throw new ZooException("lockName can not be blank");
		}
		if (exclusiveZk == null) {
			exclusiveZk = new ZooKeeper(zkhosts, DEFAULT_SESSION_TIMEOUT,
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
		// 至少连接成功后 才停止阻塞
		latch.await();
		rootStat = exclusiveZk.exists(ROOT_EXCLUSIVE_NODE, false);
		
		// 可能多线程第一次多个判断为null 并创建节点
		// try catch 捕获同时创建导致节点已存在异常
		if (rootStat == null) {
			try {
				rootPath = exclusiveZk.create(ROOT_EXCLUSIVE_NODE, null,
						Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			} catch (NodeExistsException e) {
				rootPath = ROOT_EXCLUSIVE_NODE;
			}
		} else {
			rootPath = ROOT_EXCLUSIVE_NODE;
		}
		String lockNodePathString = ROOT_EXCLUSIVE_NODE + SEPARATOR + lockName;
		
		lockStat = exclusiveZk.exists(lockNodePathString, false);
		if (lockStat != null) {// 说明此锁已经存在
			lockPath = lockNodePathString;
		} else {
			// 多线程可能lockStat同时判断为null
			try {
				// 创建相对应的锁节点
				lockPath = exclusiveZk.create(lockNodePathString, null,
						Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			} catch (NodeExistsException e) {
				System.out.println(lockNodePathString + "节点重复, message: " + e.getMessage());
				lockPath = lockNodePathString;
			}
			
		}
		this.lockName = lockName;
	}

}
