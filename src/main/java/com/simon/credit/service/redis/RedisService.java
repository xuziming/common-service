package com.simon.credit.service.redis;

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

}