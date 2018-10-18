package com.simon.credit.service.redis;

import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

/**
 * REDIS操作服务
 * @author XUZIMING 2017-12-13
 */
public class RedisServiceImpl implements RedisService {

	private ShardedJedisPool shardedJedisPool;

	public void setShardedJedisPool(ShardedJedisPool shardedJedisPool) {
		this.shardedJedisPool = shardedJedisPool;
	}

	/**
	 * redis通用执行方法(回调模式)<br>
	 * 二次封装redis: 将redis的资源获取、关闭等操作进行封装，从而使调用代码更简洁
	 * @param redisCallback REDIS回调
	 * @return
	 */
	private <T> T execute(RedisCallback<T, ShardedJedis> redisCallback) {
		ShardedJedis shardedJedis = null;
		try {
			// 从连接池中获取到jedis分片对象
			shardedJedis = this.shardedJedisPool.getResource();
			return redisCallback.callback(shardedJedis);
		} finally {
			if (shardedJedis != null) {
				// 关闭资源，检测连接是否有效，有效则放回到连接池中，无效则重置状态
				shardedJedis.close();
			}
		}
	}

	/**
	 * 执行set操作
	 * @param key
	 * @param value
	 * @return
	 */
	public String set(final String key, final String value) {
		return execute(new RedisCallback<String, ShardedJedis>() {
			public String callback(ShardedJedis e) {
				return e.set(key, value);
			}
		});
	}

	/**
	 * 执行set操作，同时设置生存时间，单位为：秒
	 * @param key
	 * @param value
	 * @param seconds
	 * @return
	 */
	public String set(final String key, final String value, final Integer seconds) {
		return execute(new RedisCallback<String, ShardedJedis>() {
			public String callback(ShardedJedis e) {
				String str = e.set(key, value);
				e.expire(key, seconds.intValue());
				return str;
			}
		});
	}

	/**
	 * 执行get操作
	 * @param key
	 * @return
	 */
	public String get(final String key) {
		return execute(new RedisCallback<String, ShardedJedis>() {
			public String callback(ShardedJedis e) {
				return e.get(key);
			}
		});
	}

	/**
	 * 执行删除操作
	 * @param key
	 * @return
	 */
	public Long del(final String key) {
		return execute(new RedisCallback<Long, ShardedJedis>() {
			public Long callback(ShardedJedis e) {
				return e.del(key);
			}
		});
	}

	/**
	 * 设置生存时间，单位为：秒
	 * @param key
	 * @param seconds
	 * @return
	 */
	public Long expire(final String key, final Integer seconds) {
		return execute(new RedisCallback<Long, ShardedJedis>() {
			public Long callback(ShardedJedis e) {
				return e.expire(key, seconds.intValue());
			}
		});
	}

}