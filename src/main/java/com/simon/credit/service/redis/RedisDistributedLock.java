package com.simon.credit.service.redis;

import java.util.concurrent.TimeUnit;

import com.simon.credit.service.DistributedLock;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ShardedJedis;

/**
 * Redis分布式锁
 * @author XUZIMING 2019-11-10
 */
public class RedisDistributedLock implements DistributedLock {

	private JedisWrapper jedisWrapper;

	private String lockKey;

	public RedisDistributedLock(Jedis jedis, String lockKey) {
		this(JedisWrapper.wrap(jedis, null), lockKey);
	}

	public RedisDistributedLock(ShardedJedis sharedJedis, String lockKey) {
		this(JedisWrapper.wrap(null, sharedJedis), lockKey);
	}

	public RedisDistributedLock(JedisWrapper jedisWrapper, String lockKey) {
		this.jedisWrapper = jedisWrapper;
		this.lockKey = lockKey;
	}

	/**
	 * 外部调用加锁的方法
	 * @param maxWait 最大等待时间
	 * @param waitUnit 最大等待时间单位
	 * @return true:加锁成功; false:加锁失败
	 */
	@Override
	public boolean tryLock(long maxWait, TimeUnit waitUnit) {
		try {
			// 获取锁截止时间
			Long tryLockDeadline = System.currentTimeMillis() + waitUnit.toMillis(maxWait);

			// 循环不断去获取锁，直至超过设置的超时时间为止
			for (;;) {
				// 当前时间超过了设定的超时时间，循环终止
				if (System.currentTimeMillis() >= tryLockDeadline) {
					break;
				}

				// 判断上一把锁是否超时,获取到锁则返回true
				if (doTryLock(lockKey)) {
					return true;
				} else {
					// 获取不到锁时休眠0.1秒，降低服务器压力
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
		// 如果当前时间已经超过超时时间，则释放锁
		if (!isLockTimeout(lockKey)) {
			jedisWrapper.del(lockKey);
		}
	}

	/**
	 * 获取锁的实现方法
	 * @param lockKey 锁键
	 * @return
	 */
	private boolean doTryLock(String lockKey) {
		// 当前时间
		long currentTime = System.currentTimeMillis();
		// 设置锁的持续时间
		String lockTimeDuration = String.valueOf(currentTime + LOCK_MAX_WAIT_MILLISECONDS);
		Long result = jedisWrapper.setnx(lockKey, lockTimeDuration);

		// 说明在调用setnx设置lockKey时, lockKey不存在
		if (result == 1) {
			return true;
		}

		// result != 1;说明加锁不成功(其它程序在占用着锁资源，这时需要检查锁是否超时)
		if (isLockTimeout(lockKey)) {
			// 之前加锁成功时设置的锁定时间段
			String preLockTimeDuration = jedisWrapper.getSet(lockKey, lockTimeDuration);
			// 当前时间大于之前的锁的限定时间, 说明锁已经超时
			if (currentTime > Long.valueOf(preLockTimeDuration)) {
				return true;
			}
		}

		// 被其它程序占用的锁没有超时，加锁失败
		return false;
	}

	/**
	 * 判断加锁是否超时
	 * @param lockKey 锁键
	 * @return
	 */
	private boolean isLockTimeout(String lockKey) {
		if (!jedisWrapper.exists(lockKey)) {
			return true;
		}
		// 如果当前时间超过锁的持续时间，则默认之前的锁已经失效，返回true
		return System.currentTimeMillis() > Long.valueOf(jedisWrapper.get(lockKey));
	}

	static final class JedisWrapper {
		private Jedis jedis;
		private ShardedJedis sharedJedis;

		public static JedisWrapper wrap(Jedis jedis, ShardedJedis sharedJedis) {
			return new JedisWrapper(jedis, sharedJedis);
		}

		public JedisWrapper(Jedis jedis, ShardedJedis sharedJedis) {
			if (jedis == null && sharedJedis == null) {
				throw new IllegalArgumentException("jedis or sharedJedis can not be all null.");
			}
			this.jedis = jedis;
			this.sharedJedis = sharedJedis;
		}

		public Boolean exists(String key) {
			return jedis != null ? jedis.exists(key) : sharedJedis.exists(key);
		}

		public String get(String key) {
			return jedis != null ? jedis.get(key) : sharedJedis.get(key);
		}

		public String getSet(String key, String value) {
			return jedis != null ? jedis.getSet(key, value) : sharedJedis.getSet(key, value);
		}

		public Long setnx(String key, String value) {
			return jedis != null ? jedis.setnx(key, value) : sharedJedis.setnx(key, value);
		}

		public Long del(String key) {
			return jedis != null ? jedis.del(key) : sharedJedis.del(key);
		}
	}

}