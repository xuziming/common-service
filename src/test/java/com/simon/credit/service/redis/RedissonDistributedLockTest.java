package com.simon.credit.service.redis;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class RedissonDistributedLockTest {

    public static void main(String[] args) throws InterruptedException {
        // 构造redisson实现分布式锁必要的Config
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.67.129:6379").setPassword("DevFtqw1206F").setDatabase(0);
        // 构造RedissonClient
        RedissonClient redissonClient = Redisson.create(config);
        // 设置锁定资源名称
        RLock lock = redissonClient.getLock("xxxTask");

        CountDownLatch latch = new CountDownLatch(10);

        for (int i = 1; i <= 10; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    // 尝试获得分布式锁
                    boolean acquireLock = lock.tryLock(20000, 15000, TimeUnit.MILLISECONDS);
                    if (acquireLock) {
                        // 此处可以开始写需要实现的代码
                        System.out.println(index + " 获取到锁...");
                        // 模拟5秒钟执行任务
                        TimeUnit.SECONDS.sleep(5);
                    } else {
                        System.out.println(index + " 超时获取不到锁...");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                        System.out.println(index + " 释放锁...");
                        lock.unlock();
                    }
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
    }

}