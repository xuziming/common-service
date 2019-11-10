package com.simon.credit.service.zookeeper;

import java.util.concurrent.TimeUnit;

import com.simon.credit.service.DistributeLock;
import com.simon.credit.service.zookeeper.ZooKeeperDistributeLock.ClientInitializer;

public class ZooKeeperDistributeLockTest {

	public static void main(String[] args) {
		ClientInitializer clientInitializer = new ClientInitializer("zookeeper://127.0.0.1:2181", "credit");
		clientInitializer.init();// 初始化zookeeper客户端

		for (int i = 1; i <= 10; i++) {
			final int index = i;
			new Thread(new Runnable() {
				@Override
				public void run() {
					DistributeLock distributeLock = new ZooKeeperDistributeLock("/riskman");
					try {
						// 判断是否获取了锁
						boolean getLock = distributeLock.tryLock(3, TimeUnit.SECONDS);
						if (getLock) {
							// 此处可以开始写需要实现的代码
							System.out.println(index + " 获取到锁...");
							TimeUnit.SECONDS.sleep(1);// do biz
						} else {
							System.out.println(index + " 超时获取不到锁...");
						}
					} catch (Exception e) {
						System.out.println(e);
					} finally {
						// 判断是否超时了，如果未超时，则释放锁。
						// 超时了，锁有可能被其他线程拿走了，就不做任何操作
						distributeLock.realseLock();
					}
				}
			}).start();
		}
	}

}
