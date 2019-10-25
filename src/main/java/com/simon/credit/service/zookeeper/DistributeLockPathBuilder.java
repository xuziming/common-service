package com.simon.credit.service.zookeeper;

/**
 * 分布式锁路径建造器
 * @author XUZIMING 2019-10-25
 */
public class DistributeLockPathBuilder {

	/**
	 * 解析分布式锁路径
	 * @param business 上锁业务
	 * @param resource 上锁资源
	 * @return
	 */
	public static final String build(String business, String resource) {
		return "/" + business + "/" + resource;
	}

	public static void main(String[] args) {
		String distributeLockPath = DistributeLockPathBuilder.build("whitelistCheck", "checkWhitelistBatch1");
		System.out.println(distributeLockPath);
	}

}
