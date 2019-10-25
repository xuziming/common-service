package com.simon.credit.service.zookeeper;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 分布式锁(基于APACHE CURATOR分布式锁进行二次封装)
 * @author XUZIMING 2017-11-16
 */
public class CuratorDistributeLock implements DistributeLock {
	private static final Logger LOGGER = LoggerFactory.getLogger(CuratorDistributeLock.class);

	/** 加锁最大等待时间, 超时则加锁失败 */
	private static final int LOCK_MAX_WAIT = 15000;

	/** ZK客户端 */
	private static CuratorFramework client;

	/** 分布式可重入锁 */
	private InterProcessMutex lock;

	/** 分布式锁路径 */
	private String path;

	public CuratorDistributeLock(String path) {
		this.path = path;
		LOGGER.debug("=== lock state: {}", client == null ? "" : client.getState());
		if (client != null && client.getState() == CuratorFrameworkState.STARTED) {
			// Distributed Lock
			lock = new InterProcessMutex(client, path);
		}
	}

	@Override
	public boolean tryLock(long maxWait, TimeUnit waitUnit) {
		if (lock == null) {
			return false;
		}
		try {
			// 加锁
			return lock.acquire(maxWait, waitUnit);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
		return false;
	}

	@Override
	public boolean tryLock() {
		return tryLock(LOCK_MAX_WAIT, TimeUnit.MILLISECONDS);
	}

	@Override
	public void unlock() {
		if (lock == null) {
			return;
		}
		try {
			// 解锁
			lock.release();

			// 删除节点
			client.delete().forPath(path);
		} catch (Exception e) {
			// LOGGER.error(e.getMessage(), e);
		}
	}

	/**
	 * 分布式锁客户端初始化
	 * @author XUZIMING 2019-10-25
	 */
	static final class ClientInitializer {
		/** 会话超时时间, 默认60秒 */
		private static final int SESSION_TIMEOUT = 60000;

		/** 连接超时时间, 默认15秒 */
		private static final int CONNECTION_TIMEOUT = 15000;

		private String distributeLockZooKeeper;

		private String distributeLockNamespace;

		public ClientInitializer(String distributeLockZooKeeper, String distributeLockNamespace) {
			this.distributeLockZooKeeper = distributeLockZooKeeper;
			this.distributeLockNamespace = distributeLockNamespace;
		}

		/**
		 * 客户端初始化方法
		 */
		public void init() {
			String connectString = parseConnectString(distributeLockZooKeeper);

			// 重试策略：最多重试3次, 每次间隔1秒
			RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);

			// 通过工厂创建连接, 以下是官网的使用建议:
			// Curator connection instances (CuratorFramework) are allocated from the CuratorFrameworkFactory.
			// You only need one CuratorFramework object for each ZooKeeper cluster you are connecting to.
			client = CuratorFrameworkFactory.builder().connectString(connectString).sessionTimeoutMs(SESSION_TIMEOUT)
					.connectionTimeoutMs(CONNECTION_TIMEOUT).retryPolicy(retryPolicy).namespace(distributeLockNamespace).build();

			// 开启连接
			client.start();
		}

		/**
		 * 解析ZooKeeper服务器的连接地址(增加处理ZK多集群的解析情况)
		 * @param zookeeperAddress ZooKeeper或ZooKeeper集群地址
		 * @return
		 */
		private static String parseConnectString(String zookeeperAddress) {
			String zkAddress = StringUtils.deleteWhitespace(zookeeperAddress);

			// 多个ZK集群的情况下, 截取配置的最后一个
			if (StringUtils.contains(zkAddress, "|")) {
				String[] multiZk = StringUtils.split(zkAddress, "|");
				zkAddress = multiZk[multiZk.length - 1];
			}

			return zkAddress.replace("zookeeper://", "").replace("?backup=", ",");
		}

	}

}
