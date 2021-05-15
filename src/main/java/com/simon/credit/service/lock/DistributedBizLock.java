package com.simon.credit.service.lock;

import java.util.concurrent.TimeUnit;

/**
 * 分布式业务锁
 * @author xuziming 2021-05-03
 */
public interface DistributedBizLock {

    /** 加锁最大等待时间(默认为3秒), 超时则加锁失败 */
    long LOCK_MAX_WAIT_SECONDS = 3L;

    /**
     * 加锁
     * @param maxWait 最大等待时间
     * @param waitUnit 最大等待时间单位
     */
    void lock(String lockName, LockBiz lockBiz, long maxWait, TimeUnit waitUnit);

    /**
     * 加锁(使用默认等待时间: 3秒)
     */
    void lock(String lockName, LockBiz lockBiz);

}