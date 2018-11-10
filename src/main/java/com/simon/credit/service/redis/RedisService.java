package com.simon.credit.service.redis;

import java.util.Map;

/**
 * REDIS操作服务
 * @author XUZIMING 2017-12-13
 */
public interface RedisService {

	/**
	 * 执行set操作
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	String set(String key, String value);

	/**
	 * 执行hash set操作
	 * 
	 * @param key
	 * @param field hash数据结构名
	 * @param value
	 * @return
	 */
	Long hset(String key, String field, String value);

	/**
	 * 执行set操作，同时设置生存时间，单位为：秒
	 * 
	 * @param key
	 * @param value
	 * @param seconds
	 * @return
	 */
	String set(String key, String value, Integer seconds);

	/**
	 * 执行get操作
	 * 
	 * @param key
	 * @return
	 */
	String get(String key);

	/**
	 * 执行hash get操作
	 * 
	 * @param key
	 * @param field hash数据结构名
	 * @return
	 */
	String hget(String key, String field);

	/**
	 * 执行删除操作
	 * 
	 * @param key
	 * @return
	 */
	Long del(String key);

	/**
	 * 设置生存时间，单位为：秒
	 * 
	 * @param key
	 * @param seconds
	 * @return
	 */
	Long expire(String key, Integer seconds);

	/**
	 * 判断hash是否包含指定key
	 * @param key
	 * @param field hash数据结构名
	 * @return
	 */
	Boolean hexists(String key, String field);

	/**
	 * 执行hash multi set操作
	 * @param key
	 * @param hash
	 * @return
	 */
	String hmset(final String key, final Map<String, String> hash);

}