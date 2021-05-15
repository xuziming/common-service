package com.simon.credit.service.redis;

import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

import java.util.Map;

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
	 * @param redisCallback redis回调
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
				// shardedJedis.close();
				shardedJedisPool.returnResourceObject(shardedJedis);
			}
		}
	}

	/**
	 * 执行set操作
	 * @param key
	 * @param value
	 * @return
	 */
	@Override
	public String set(final String key, final String value) {
		return execute(e -> e.set(key, value));
	}

	/**
	 * 执行hash set操作
	 * @param key
	 * @param hash Hash数据结构名
	 * @param value
	 * @return
	 */
	@Override
	public Long hset(final String key, final String hash, final String value) {
		return execute(e -> e.hset(key, hash, value));
	}

	/**
	 * 执行set操作，同时设置生存时间，单位为：秒
	 * @param key
	 * @param value
	 * @param seconds
	 * @return
	 */
	@Override
	public String set(final String key, final String value, final Integer seconds) {
		return execute(e -> {
			String str = e.set(key, value);
			e.expire(key, seconds.intValue());
			return str;
		});
	}

	/**
	 * 执行get操作
	 * @param key
	 * @return
	 */
	@Override
	public String get(final String key) {
		return execute(e -> e.get(key));
	}

	/**
	 * 执行hash get操作
	 * @param key 
	 * @param hash Hash数据结构名
	 * @return
	 */
	@Override
	public String hget(final String key, final String hash) {
		return execute(e -> e.hget(key, hash));
	}

	/**
	 * 执行删除操作
	 * @param key
	 * @return
	 */
	@Override
	public Long del(final String key) {
		return execute(e -> e.del(key));
	}

	/**
	 * 设置生存时间，单位为：秒
	 * @param key
	 * @param seconds 超时时间(单位：秒)
	 * @return
	 */
	@Override
	public Long expire(final String key, final Integer seconds) {
		return execute(e -> e.expire(key, seconds.intValue()));
	}

	/**
	 * 判断hash是否包含指定key
	 * @param key
	 * @param hash Hash数据结构名
	 * @return
	 */
	@Override
	public Boolean hexists(final String key, final String hash) {
		return execute(e -> e.hexists(key, hash));
	}

	/**
	 * 执行hash multi set操作
	 * @param key
	 * @param hash Hash数据结构名
	 * @return
	 */
	@Override
	public String hmset(final String key, final Map<String, String> hash) {
		return execute(e -> e.hmset(key, hash));
	}

}