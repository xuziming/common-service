package com.simon.credit.service.zookeeper;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁
 * @author XUZIMING 2017-11-16
 */
public interface DistributeLock {

	/**
	 * 加锁
	 * @param maxWait 最大等待时间
	 * @param waitUnit 最大等待时间单位
	 * @return true:加锁成功; false:加锁失败
	 */
	boolean tryLock(long maxWait, TimeUnit waitUnit);

	/**
	 * 加锁(使用默认等待时间30秒)
	 * @return true:加锁成功; false:加锁失败
	 */
	boolean tryLock();

	/**
	 * 解锁
	 */
	void unlock();

}
