package com.cjk.zookeeper.distribute.inter;

import java.net.ConnectException;

import org.apache.zookeeper.KeeperException;

public abstract class DistributedLock {
	
	/**
	 * 释放锁
	 * @throws KeeperException 
	 * @throws InterruptedException 
	 */
	public abstract void releaseLock();
	/**
	 * 尝试获得锁，能获得就立马获得锁，如果不能获得就立马返回
	 * @throws ConnectException 
	 */
	public abstract boolean tryLock();
	/**
	 * 尝试获得锁，如果有锁就返回，如果没有锁就等待，如果等待了一段时间后还没能获取到锁，那么就返回
	 * @param timeout 单位：秒
	 * @return
	 */
	public abstract boolean tryLock(int timeout);
	
	/**
	 * 尝试获得锁，一直阻塞，直到获得锁为止
	 * @param timeout 单位：秒
	 * @throws ConnectException 
	 */
	public abstract void lock() ;

}
