package com.simon.credit.service.redis;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.simon.credit.service.DistributedLock;

import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedisPool;

public class RedisDistributedLockTest {

	private static final String LOCK_KEY = "lock_credit_risk";

	public static void main(String[] args) throws InterruptedException {
		// 获取redis分片连接池
		ShardedJedisPool shardedJedisPool = getShardedJedisPool();

		CountDownLatch latch = new CountDownLatch(10);

		for (int i = 1; i <= 10; i++) {
			final int index = i;
			new Thread(new Runnable() {
				@Override
				public void run() {
					DistributedLock lock = new RedisDistributedLock(shardedJedisPool.getResource(), LOCK_KEY);
					try {
						// 判断是否获取了锁
						boolean getLock = lock.tryLock(20, TimeUnit.SECONDS);
						if (getLock) {
							// 此处可以开始写需要实现的代码
							System.out.println(index + " 获取到锁...");
							TimeUnit.SECONDS.sleep(10);
						} else {
							System.out.println(index + " 超时获取不到锁...");
						}
					} catch (Exception e) {
						// System.out.println(e);
					} finally {
						// 判断是否超时了，如果未超时，则释放锁。
						// 超时了，锁有可能被其他线程拿走了，就不做任何操作
						lock.realseLock();
						latch.countDown();
						System.out.println(index + " 释放锁...");
					}
				}
			}).start();
		}

		latch.await();
		shardedJedisPool.close();
	}

	private static ShardedJedisPool getShardedJedisPool() {
		JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
		jedisPoolConfig.setMaxTotal(1024);

		JedisShardInfo jedisShardInfo = new JedisShardInfo("192.168.67.129", 6379);
		jedisShardInfo.setPassword("DevFtqw1206F");

		return new ShardedJedisPool(jedisPoolConfig, Arrays.asList(jedisShardInfo));
	}

}