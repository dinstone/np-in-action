package com.dinstone.np;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class CustomeExecutor {

    public <T> Future<T> submit(final Callable<T> callableTask) {
        final Promise<T> promise = new Promise<T>();
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    promise.set(callableTask.call());
                } catch (Throwable e) {
                    promise.exception(e);
                }
            }
        };
        t.start();
        return promise;
    }

}
