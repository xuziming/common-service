package com.simon.credit.service.redis;

import java.util.concurrent.TimeUnit;

import com.simon.credit.service.DistributeLock;

import redis.clients.jedis.ShardedJedis;

public class RedisDistributeLock implements DistributeLock {
	/** 分片jedis */
	private ShardedJedis jedis;

	/** 锁的名字 */
	private String lockKey;

	public RedisDistributeLock(ShardedJedis jedis, String lockKey) {
		this.jedis = jedis;
		this.lockKey = lockKey;
	}

	/**
	 * 外部调用加锁的方法
	 * @param maxWait 最大等待时间
	 * @param waitUnit 最大等待时间单位
	 * @return true:加锁成功; false:加锁失败
	 */
	public boolean tryLock(long maxWait, TimeUnit waitUnit) {
		try {
			// 获取当前系统时间作为：开始加锁的时间
			Long tryLockStartTime = System.currentTimeMillis();

			// 设置一个死循环，不断去获取锁，直接超过设置的 超时时间 为止
			for (;;) {
				// 当前时间超过了设定的超时时间，循环终止
				if (System.currentTimeMillis() - tryLockStartTime > waitUnit.toMillis(maxWait)) {
					break;
				}

				// 判断上一把锁是否超时,获取到锁则返回true;否则休眠0.1秒，降低服务器压力
				if (innerTryLock(lockKey)) {
					return true;
				} else {
					Thread.sleep(100);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean tryLock() {
		return tryLock(LOCK_MAX_WAIT_SECONDS, TimeUnit.SECONDS);
	}

	/**
	 * 释放锁
	 */
	@Override
	public void realseLock() {
		// 如果当前时间已经超过 超时时间，则释放锁
		if (!checkIfLockTimeout(System.currentTimeMillis(), lockKey)) {
			jedis.del(lockKey);
		}
	}

	/**
	 * 内部获取锁的实现方法
	 * @param lockKey 锁的名字
	 * @return
	 */
	private boolean innerTryLock(String lockKey) {
		// 当前时间
		long currentTime = System.currentTimeMillis();
		// 设置锁的持续时间
		String lockTimeDuration = String.valueOf(currentTime + TimeUnit.SECONDS.toMillis(LOCK_MAX_WAIT_SECONDS));
		Long result = jedis.setnx(lockKey, lockTimeDuration);

		if (result == 1) {
			return true;
		}

		// result != 1;说明加锁不成功
		if (checkIfLockTimeout(currentTime, lockKey)) {// 检查加锁是否超出限定时间
			// 之前加锁成功时设置的锁定时间
			String preLockTimeDuration = jedis.getSet(lockKey, lockTimeDuration);
			// 当前时间大于之前的锁的限定时间, 说明锁已经超时
			if (currentTime > Long.valueOf(preLockTimeDuration)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * 判断加锁是否超时
	 * @param currentTime 当前时间
	 * @param lockKey     锁的名字
	 * @return
	 */
	private boolean checkIfLockTimeout(Long currentTime, String lockKey) {
		if (!jedis.exists(lockKey)) {
			return true;
		}
		// 如果当前时间超过锁的持续时间，则默认之前的锁已经失效，返回true
		return currentTime > Long.valueOf(jedis.get(lockKey));
	}

}