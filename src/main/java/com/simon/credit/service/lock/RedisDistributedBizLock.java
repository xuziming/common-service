package com.simon.credit.service.lock;

import com.simon.credit.service.redis.RedisDistributedLock;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

/**
 * Redis分布式锁(Redisson实现方式)
 * @author XUZIMING 2021-01-30
 */
public class RedisDistributedBizLock implements DistributedBizLock {
	private static final Logger LOGGER = LoggerFactory.getLogger(RedisDistributedLock.class);

	@Autowired
	private Redisson redisson;

	@Override
	public void lock(String lockName, LockBiz lockBiz, long maxWait, TimeUnit waitUnit) {
		// RLock lock = redisson.getLock(lockName);
		RLock lock = new MyRedissonLock(redisson.getConnectionManager().getCommandExecutor(), lockName);

		try {
			// try to acquire lock(long waitTime, long leaseTime, TimeUnit unit)
			boolean acquireLock = lock.tryLock(15, 10, TimeUnit.SECONDS);
			if (acquireLock) {
				LOGGER.info(Thread.currentThread().getName() + " hold distributed lock");
				lockBiz.execute();
			} else {
				LOGGER.error(Thread.currentThread().getName() + " hasn't acquired distributed lock");
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw new RuntimeException("分布式锁业务执行出错");
		} finally {
			if (lock.isLocked() && lock.isHeldByCurrentThread()) {
				lock.unlock();
				LOGGER.info(Thread.currentThread().getName() + " released distributed lock");
			}
		}
	}

	@Override
	public void lock(String lockName, LockBiz lockBiz) {
		lock(lockName, lockBiz, LOCK_MAX_WAIT_SECONDS, TimeUnit.SECONDS);
	}

}