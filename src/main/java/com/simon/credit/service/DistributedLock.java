package com.simon.credit.service;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁
 * @author XUZIMING 2017-11-16
 */
public interface DistributedLock {

	/** 加锁最大等待时间(默认为3秒), 超时则加锁失败 */
	int LOCK_MAX_WAIT_SECONDS = 3;

	/**
	 * 加锁
	 * @param maxWait 最大等待时间
	 * @param waitUnit 最大等待时间单位
	 * @return true:加锁成功; false:加锁失败
	 */
	boolean tryLock(long maxWait, TimeUnit waitUnit);

	/**
	 * 加锁(使用默认等待时间3秒)
	 * @return true:加锁成功; false:加锁失败
	 */
	boolean tryLock();

	/**
	 * 释放锁
	 */
	void realseLock();

}
