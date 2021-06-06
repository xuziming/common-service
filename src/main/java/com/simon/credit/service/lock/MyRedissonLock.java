package com.simon.credit.service.lock;

import io.netty.util.Timeout;
import org.redisson.RedissonBaseLock;
import org.redisson.RedissonLockEntry;
import org.redisson.api.RFuture;
import org.redisson.client.RedisException;
import org.redisson.client.codec.LongCodec;
import org.redisson.client.protocol.RedisCommands;
import org.redisson.client.protocol.RedisStrictCommand;
import org.redisson.command.CommandAsyncExecutor;
import org.redisson.misc.RPromise;
import org.redisson.misc.RedissonPromise;
import org.redisson.pubsub.LockPubSub;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class MyRedissonLock extends RedissonBaseLock {
    protected long internalLockLeaseTime;
    protected final LockPubSub pubSub;
    final CommandAsyncExecutor commandExecutor;

    public MyRedissonLock(CommandAsyncExecutor commandExecutor, String name) {
        super(commandExecutor, name);
        this.commandExecutor = commandExecutor;
        this.internalLockLeaseTime = commandExecutor.getConnectionManager().getCfg().getLockWatchdogTimeout();
        this.pubSub = commandExecutor.getConnectionManager().getSubscribeService().getLockPubSub();
    }

    String getChannelName() {
        return prefixName("redisson_lock__channel", this.getName());
    }

    public void lock() {
        try {
            this.lock(-1L, null, false);
        } catch (InterruptedException e) {
            throw new IllegalStateException();
        }
    }

    public void lock(long leaseTime, TimeUnit unit) {
        try {
            this.lock(leaseTime, unit, false);
        } catch (InterruptedException e) {
            throw new IllegalStateException();
        }
    }

    public void lockInterruptibly() throws InterruptedException {
        this.lock(-1L, null, true);
    }

    public void lockInterruptibly(long leaseTime, TimeUnit unit) throws InterruptedException {
        this.lock(leaseTime, unit, true);
    }

    private void lock(long leaseTime, TimeUnit unit, boolean interruptibly) throws InterruptedException {
        long threadId = Thread.currentThread().getId();
        Long ttl = this.tryAcquire(-1L, leaseTime, unit, threadId);
        if (ttl == null) {
            return;
        }

        RFuture<RedissonLockEntry> future = this.subscribe(threadId);
        if (interruptibly) {
            this.commandExecutor.syncSubscriptionInterrupted(future);
        } else {
            this.commandExecutor.syncSubscription(future);
        }

        try {
            while (true) {
                ttl = this.tryAcquire(-1L, leaseTime, unit, threadId);
                if (ttl == null) {
                    return;
                }

                if (ttl >= 0L) {
                    try {
                        future.getNow().getLatch().tryAcquire(ttl, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException ie) {
                        if (interruptibly) {
                            throw ie;
                        }

                        future.getNow().getLatch().tryAcquire(ttl, TimeUnit.MILLISECONDS);
                    }
                } else if (interruptibly) {
                    future.getNow().getLatch().acquire();
                } else {
                    future.getNow().getLatch().acquireUninterruptibly();
                }
            }
        } finally {
            this.unsubscribe(future, threadId);
        }
    }

    private Long tryAcquire(long waitTime, long leaseTime, TimeUnit unit, long threadId) {
        return this.get(this.tryAcquireAsync(waitTime, leaseTime, unit, threadId));
    }

    private RFuture<Boolean> tryAcquireOnceAsync(long waitTime, long leaseTime, TimeUnit unit, long threadId) {
        if (leaseTime != -1L) {
            return this.tryLockInnerAsync(waitTime, leaseTime, unit, threadId, RedisCommands.EVAL_NULL_BOOLEAN);
        }

        RFuture<Boolean> ttlRemainingFuture = this.tryLockInnerAsync(waitTime,
            internalLockLeaseTime, TimeUnit.MILLISECONDS, threadId, RedisCommands.EVAL_NULL_BOOLEAN);
        ttlRemainingFuture.onComplete((ttlRemaining, e) -> {
            if (e == null && ttlRemaining) {
                this.scheduleExpirationRenewal(threadId);
            }
        });
        return ttlRemainingFuture;
    }

    private <T> RFuture<Long> tryAcquireAsync(long waitTime, long leaseTime, TimeUnit unit, long threadId) {
        if (leaseTime != -1L) {
            return this.tryLockInnerAsync(waitTime, leaseTime, unit, threadId, RedisCommands.EVAL_LONG);
        }

        RFuture<Long> ttlRemainingFuture = this.tryLockInnerAsync(waitTime, this.internalLockLeaseTime, TimeUnit.MILLISECONDS, threadId, RedisCommands.EVAL_LONG);
        /** 过期续租 */
        ttlRemainingFuture.onComplete((ttlRemaining, e) -> {
            if (e == null && ttlRemaining == null) {
                this.scheduleExpirationRenewal(threadId);
            }
        });
        return ttlRemainingFuture;
    }

    public boolean tryLock() {
        return this.get(this.tryLockAsync());
    }

    <T> RFuture<T> tryLockInnerAsync(long waitTime, long leaseTime, TimeUnit unit, long threadId, RedisStrictCommand<T> command) {
        this.internalLockLeaseTime = unit.toMillis(leaseTime);
        String script = "if (redis.call('exists', KEYS[1]) == 0) " +
                        "then " +
                            "redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
                            "redis.call('pexpire', KEYS[1], ARGV[1]); " +
                        "return nil; " +
                        "end; " +
                        "if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) " +
                        "then " +
                            "redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
                            "redis.call('pexpire', KEYS[1], ARGV[1]); " +
                        "return nil; " +
                        "end; " +
                        "return redis.call('pttl', KEYS[1]);";
        List<Object> keys = Collections.singletonList(this.getName());
        Object[] params = {this.internalLockLeaseTime, this.getLockName(threadId)};
        return this.evalWriteAsync(this.getName(), LongCodec.INSTANCE, command, script, keys, params);
    }

    /**
     * 尝试获得锁
     * @param waitTime
     * @param leaseTime
     * @param unit
     * @return
     * @throws InterruptedException
     */
    public boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException {
        long time = unit.toMillis(waitTime);
        long current = System.currentTimeMillis();
        long threadId = Thread.currentThread().getId();
        Long timeToLive = this.tryAcquire(waitTime, leaseTime, unit, threadId);
        if (timeToLive == null) {
            return true;
        }

        time -= System.currentTimeMillis() - current;
        if (time <= 0L) {
            this.acquireFailed(waitTime, unit, threadId);
            return false;
        }

        current = System.currentTimeMillis();
        RFuture<RedissonLockEntry> subscribeFuture = this.subscribe(threadId);
        if (!subscribeFuture.await(time, TimeUnit.MILLISECONDS)) {
            if (!subscribeFuture.cancel(false)) {
                subscribeFuture.onComplete((res, e) -> {
                    if (e == null) {
                        this.unsubscribe(subscribeFuture, threadId);
                    }

                });
            }
            this.acquireFailed(waitTime, unit, threadId);
            return false;
        }

        try {
            time -= System.currentTimeMillis() - current;
            if (time <= 0L) {
                this.acquireFailed(waitTime, unit, threadId);
                return false;
            }

            do {
                long currentTime = System.currentTimeMillis();
                timeToLive = this.tryAcquire(waitTime, leaseTime, unit, threadId);
                if (timeToLive == null) {
                    return true;
                }

                time -= System.currentTimeMillis() - currentTime;
                if (time <= 0L) {
                    this.acquireFailed(waitTime, unit, threadId);
                    return false;
                }

                currentTime = System.currentTimeMillis();
                if (timeToLive >= 0L && timeToLive < time) {
                    subscribeFuture.getNow().getLatch().tryAcquire(timeToLive, TimeUnit.MILLISECONDS);
                } else {
                    subscribeFuture.getNow().getLatch().tryAcquire(time, TimeUnit.MILLISECONDS);
                }

                time -= System.currentTimeMillis() - currentTime;
            } while (time > 0L);

            this.acquireFailed(waitTime, unit, threadId);
        } finally {
            this.unsubscribe(subscribeFuture, threadId);
        }

        return false;
    }

    protected RFuture<RedissonLockEntry> subscribe(long threadId) {
        return this.pubSub.subscribe(this.getEntryName(), this.getChannelName());
    }

    protected void unsubscribe(RFuture<RedissonLockEntry> future, long threadId) {
        this.pubSub.unsubscribe(future.getNow(), this.getEntryName(), this.getChannelName());
    }

    public boolean tryLock(long waitTime, TimeUnit unit) throws InterruptedException {
        return this.tryLock(waitTime, -1L, unit);
    }

    public void unlock() {
        try {
            this.get(this.unlockAsync(Thread.currentThread().getId()));
        } catch (RedisException e) {
            if (e.getCause() instanceof IllegalMonitorStateException) {
                throw (IllegalMonitorStateException) e.getCause();
            } else {
                throw e;
            }
        }
    }

    public boolean forceUnlock() {
        return this.get(this.forceUnlockAsync());
    }

    public RFuture<Boolean> forceUnlockAsync() {
        this.cancelExpirationRenewal(null);
        String script = "if  (redis.call('del', KEYS[1]) == 1) " +
                        "then redis.call('publish', KEYS[2], ARGV[1]); " +
                        "return 1 " +
                        "else return 0 " +
                        "end";
        return this.evalWriteAsync(this.getName(), LongCodec.INSTANCE, RedisCommands.EVAL_BOOLEAN, script,
                Arrays.asList(this.getName(), this.getChannelName()), new Object[]{LockPubSub.UNLOCK_MESSAGE});
    }

    protected RFuture<Boolean> unlockInnerAsync(long threadId) {
        String script = "if (redis.call('hexists', KEYS[1], ARGV[3]) == 0) " +
                        "   then return nil; " +
                        "end; " +
                        "local counter = redis.call('hincrby', KEYS[1], ARGV[3], -1); " +
                        "if (counter > 0) " +
                        "   then redis.call('pexpire', KEYS[1], ARGV[2]); " +
                        "   return 0; " +
                        "else redis.call('del', KEYS[1]); " +
                        "   redis.call('publish', KEYS[2], ARGV[1]); " +
                        "   return 1; " +
                        "end; " +
                        "return nil;";
        List<Object> keys = Arrays.asList(this.getName(), this.getChannelName());
        Object[] params = {LockPubSub.UNLOCK_MESSAGE, this.internalLockLeaseTime, this.getLockName(threadId)};
        return this.evalWriteAsync(this.getName(), LongCodec.INSTANCE, RedisCommands.EVAL_BOOLEAN, script, keys, params);
    }

    public RFuture<Void> lockAsync() {
        return this.lockAsync(-1L, null);
    }

    public RFuture<Void> lockAsync(long leaseTime, TimeUnit unit) {
        long currentThreadId = Thread.currentThread().getId();
        return this.lockAsync(leaseTime, unit, currentThreadId);
    }

    public RFuture<Void> lockAsync(long currentThreadId) {
        return this.lockAsync(-1L, null, currentThreadId);
    }

    public RFuture<Void> lockAsync(long leaseTime, TimeUnit unit, long currentThreadId) {
        RPromise<Void> result = new RedissonPromise();
        RFuture<Long> ttlFuture = this.tryAcquireAsync(-1L, leaseTime, unit, currentThreadId);
        ttlFuture.onComplete((ttl, e) -> {
            if (e != null) {
                result.tryFailure(e);
            } else if (ttl == null) {
                if (!result.trySuccess(null)) {
                    this.unlockAsync(currentThreadId);
                }
            } else {
                RFuture<RedissonLockEntry> subscribeFuture = this.subscribe(currentThreadId);
                subscribeFuture.onComplete((res, ex) -> {
                    if (ex != null) {
                        result.tryFailure(ex);
                    } else {
                        this.lockAsync(leaseTime, unit, subscribeFuture, result, currentThreadId);
                    }
                });
            }
        });
        return result;
    }

    private void lockAsync(long leaseTime, TimeUnit unit, RFuture<RedissonLockEntry> subscribeFuture, RPromise<Void> result, long currentThreadId) {
        RFuture<Long> ttlFuture = this.tryAcquireAsync(-1L, leaseTime, unit, currentThreadId);
        ttlFuture.onComplete((ttl, e) -> {
            if (e != null) {
                this.unsubscribe(subscribeFuture, currentThreadId);
                result.tryFailure(e);
            } else if (ttl == null) {
                this.unsubscribe(subscribeFuture, currentThreadId);
                if (!result.trySuccess(null)) {
                    this.unlockAsync(currentThreadId);
                }
            } else {
                final RedissonLockEntry entry = subscribeFuture.getNow();
                if (entry.getLatch().tryAcquire()) {
                    this.lockAsync(leaseTime, unit, subscribeFuture, result, currentThreadId);
                } else {
                    AtomicReference<Timeout> futureRef = new AtomicReference();
                    final Runnable listener = () -> {
                        if (futureRef.get() != null) {
                            futureRef.get().cancel();
                        }
                        this.lockAsync(leaseTime, unit, subscribeFuture, result, currentThreadId);
                    };
                    entry.addListener(listener);
                    if (ttl >= 0L) {
                        Timeout scheduledFuture = this.commandExecutor.getConnectionManager().newTimeout(timeout -> {
                            if (entry.removeListener(listener)) {
                                MyRedissonLock.this.lockAsync(leaseTime, unit, subscribeFuture, result, currentThreadId);
                            }
                        }, ttl, TimeUnit.MILLISECONDS);
                        futureRef.set(scheduledFuture);
                    }
                }
            }
        });
    }

    public RFuture<Boolean> tryLockAsync() {
        return this.tryLockAsync(Thread.currentThread().getId());
    }

    public RFuture<Boolean> tryLockAsync(long threadId) {
        return this.tryAcquireOnceAsync(-1L, -1L, null, threadId);
    }

    public RFuture<Boolean> tryLockAsync(long waitTime, TimeUnit unit) {
        return this.tryLockAsync(waitTime, -1L, unit);
    }

    public RFuture<Boolean> tryLockAsync(long waitTime, long leaseTime, TimeUnit unit) {
        long currentThreadId = Thread.currentThread().getId();
        return this.tryLockAsync(waitTime, leaseTime, unit, currentThreadId);
    }

    public RFuture<Boolean> tryLockAsync(long waitTime, long leaseTime, TimeUnit unit, long currentThreadId) {
        RPromise<Boolean> result = new RedissonPromise();
        AtomicLong time = new AtomicLong(unit.toMillis(waitTime));
        long currentTime = System.currentTimeMillis();
        RFuture<Long> ttlFuture = this.tryAcquireAsync(waitTime, leaseTime, unit, currentThreadId);
        ttlFuture.onComplete((ttl, e) -> {
            if (e != null) {
                result.tryFailure(e);
            } else if (ttl == null) {
                if (!result.trySuccess(true)) {
                    this.unlockAsync(currentThreadId);
                }

            } else {
                long el = System.currentTimeMillis() - currentTime;
                time.addAndGet(-el);
                if (time.get() <= 0L) {
                    this.trySuccessFalse(currentThreadId, result);
                } else {
                    long current = System.currentTimeMillis();
                    AtomicReference<Timeout> futureRef = new AtomicReference();
                    final RFuture<RedissonLockEntry> subscribeFuture = this.subscribe(currentThreadId);
                    subscribeFuture.onComplete((r, ex) -> {
                        if (ex != null) {
                            result.tryFailure(ex);
                        } else {
                            if (futureRef.get() != null) {
                                futureRef.get().cancel();
                            }

                            long elapsed = System.currentTimeMillis() - current;
                            time.addAndGet(-elapsed);
                            this.tryLockAsync(time, waitTime, leaseTime, unit, subscribeFuture, result, currentThreadId);
                        }
                    });
                    if (!subscribeFuture.isDone()) {
                        Timeout scheduledFuture = this.commandExecutor.getConnectionManager().newTimeout(timeout -> {
                            if (!subscribeFuture.isDone()) {
                                subscribeFuture.cancel(false);
                                MyRedissonLock.this.trySuccessFalse(currentThreadId, result);
                            }
                        }, time.get(), TimeUnit.MILLISECONDS);
                        futureRef.set(scheduledFuture);
                    }
                }
            }
        });
        return result;
    }

    private void trySuccessFalse(long currentThreadId, RPromise<Boolean> result) {
        this.acquireFailedAsync(-1L, null, currentThreadId).onComplete((res, e) -> {
            if (e == null) {
                result.trySuccess(false);
            } else {
                result.tryFailure(e);
            }
        });
    }

    private void tryLockAsync(AtomicLong time, long waitTime, long leaseTime, TimeUnit unit,
        RFuture<RedissonLockEntry> subscribeFuture, RPromise<Boolean> result, long currentThreadId) {

        if (result.isDone()) {
            this.unsubscribe(subscribeFuture, currentThreadId);
        } else if (time.get() <= 0L) {
            this.unsubscribe(subscribeFuture, currentThreadId);
            this.trySuccessFalse(currentThreadId, result);
        } else {
            long curr = System.currentTimeMillis();
            RFuture<Long> ttlFuture = this.tryAcquireAsync(waitTime, leaseTime, unit, currentThreadId);
            ttlFuture.onComplete((ttl, e) -> {
                if (e != null) {
                    this.unsubscribe(subscribeFuture, currentThreadId);
                    result.tryFailure(e);
                } else if (ttl == null) {
                    this.unsubscribe(subscribeFuture, currentThreadId);
                    if (!result.trySuccess(true)) {
                        this.unlockAsync(currentThreadId);
                    }
                } else {
                    long elapsedTime = System.currentTimeMillis() - curr;
                    time.addAndGet(-elapsedTime);
                    if (time.get() <= 0L) {
                        this.unsubscribe(subscribeFuture, currentThreadId);
                        this.trySuccessFalse(currentThreadId, result);
                    } else {
                        final long current = System.currentTimeMillis();
                        final RedissonLockEntry entry = subscribeFuture.getNow();
                        if (entry.getLatch().tryAcquire()) {
                            this.tryLockAsync(time, waitTime, leaseTime, unit, subscribeFuture, result, currentThreadId);
                        } else {
                            AtomicBoolean executed = new AtomicBoolean();
                            AtomicReference<Timeout> futureRef = new AtomicReference();
                            final Runnable listener = () -> {
                                executed.set(true);
                                if (futureRef.get() != null) {
                                    futureRef.get().cancel();
                                }

                                long elapsed = System.currentTimeMillis() - current;
                                time.addAndGet(-elapsed);
                                this.tryLockAsync(time, waitTime, leaseTime, unit, subscribeFuture, result, currentThreadId);
                            };
                            entry.addListener(listener);
                            long t = time.get();
                            if (ttl >= 0L && ttl < time.get()) {
                                t = ttl;
                            }

                            if (!executed.get()) {
                                Timeout scheduledFuture = this.commandExecutor.getConnectionManager().newTimeout(timeout -> {
                                    if (entry.removeListener(listener)) {
                                        long elapsed = System.currentTimeMillis() - current;
                                        time.addAndGet(-elapsed);
                                        MyRedissonLock.this.tryLockAsync(time, waitTime, leaseTime, unit, subscribeFuture, result, currentThreadId);
                                    }
                                }, t, TimeUnit.MILLISECONDS);
                                futureRef.set(scheduledFuture);
                            }
                        }
                    }
                }
            });
        }
    }

}