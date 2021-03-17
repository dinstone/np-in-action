package com.dinstone.np;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ExecutorServiceTest {

    public static void main(String[] args) throws InterruptedException {
        ExecutorService es = Executors.newCachedThreadPool();

        extracted(es, 60);

        extracted(es, 3600);

        extracted(es, 86400);

        extracted(es, 0);

        es.shutdown();
        es.awaitTermination(Long.MAX_VALUE, TimeUnit.MICROSECONDS);
    }

    private static void extracted(ExecutorService es, int t) {
        Future<List<String>> f = es.submit(new CallableTask(t));
        try {
            List<String> cs = f.get();
            System.out.println(cs.size());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

}
