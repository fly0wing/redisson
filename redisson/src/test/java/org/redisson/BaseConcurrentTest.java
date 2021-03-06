package org.redisson;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.redisson.api.RedissonClient;

public abstract class BaseConcurrentTest extends BaseTest {

    protected void testMultiInstanceConcurrency(int iterations, final RedissonRunnable runnable) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*2);

        final Map<Integer, RedissonClient> instances = new HashMap<Integer, RedissonClient>();
        for (int i = 0; i < iterations; i++) {
            instances.put(i, BaseTest.createInstance());
        }

        long watch = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            final int n = i;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    RedissonClient redisson = instances.get(n);
                    runnable.run(redisson);
                }
            });
        }

        executor.shutdown();
        Assert.assertTrue(executor.awaitTermination(RedissonRuntimeEnvironment.isTravis ? 10 : 3, TimeUnit.MINUTES));

        System.out.println("multi: " + (System.currentTimeMillis() - watch));

        executor = Executors.newCachedThreadPool();

        for (final RedissonClient redisson : instances.values()) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    redisson.shutdown();
                }
            });
        }

        executor.shutdown();
        Assert.assertTrue(executor.awaitTermination(5, TimeUnit.MINUTES));
    }

    protected void testMultiInstanceConcurrencySequentiallyLaunched(int iterations, final RedissonRunnable runnable) throws InterruptedException {
        System.out.println("Multi Instance Concurrent Job Interation: " + iterations);
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

        final Map<Integer, RedissonClient> instances = new HashMap<Integer, RedissonClient>();
        for (int i = 0; i < iterations; i++) {
            instances.put(i, BaseTest.createInstance());
        }

        long watch = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            final int n = i;
            executor.execute(() -> runnable.run(instances.get(n)));
        }

        executor.shutdown();
        Assert.assertTrue(executor.awaitTermination(5, TimeUnit.MINUTES));

        System.out.println("multi: " + (System.currentTimeMillis() - watch));

        executor = Executors.newCachedThreadPool();

        for (final RedissonClient redisson : instances.values()) {
            executor.execute(() -> redisson.shutdown());
        }

        executor.shutdown();
        Assert.assertTrue(executor.awaitTermination(5, TimeUnit.MINUTES));
    }

    protected void testSingleInstanceConcurrency(int iterations, final RedissonRunnable runnable) throws InterruptedException {
        System.out.println("Single Instance Concurrent Job Interation: " + iterations);
        final RedissonClient r = BaseTest.createInstance();
        long watch = System.currentTimeMillis();

        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

        for (int i = 0; i < iterations; i++) {
            pool.execute(() -> {
                runnable.run(r);
            });
        }

        pool.shutdown();
        Assert.assertTrue(pool.awaitTermination(RedissonRuntimeEnvironment.isTravis ? 20 : 3, TimeUnit.MINUTES));

        System.out.println(System.currentTimeMillis() - watch);

        r.shutdown();
    }

}
