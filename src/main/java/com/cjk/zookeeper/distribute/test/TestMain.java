package com.cjk.zookeeper.distribute.test;

import com.cjk.zookeeper.distribute.impl.ZooKeeperExclusiveLock;

public class TestMain {

	public static void main(String[] args) {
		for (int i = 0; i < 10; i++) {
            new Thread() {
                public void run() {
                    try {
                    	ZooKeeperExclusiveLock exclusiveLock = new ZooKeeperExclusiveLock();
                    	exclusiveLock.connectZooKeeper("ip:2181", "chenjk");
                    	exclusiveLock.lock();
                    	System.out.println(Thread.currentThread().getName()+"在做事, 做完就释放锁");
                    	Thread.sleep(1000);
                    	System.out.println(Thread.currentThread().getName()+"做完事情, 开始释放锁");
                    	exclusiveLock.releaseLock();
                    	
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
	}
}
