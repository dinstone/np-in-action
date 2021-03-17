package com.dinstone.np;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class CustomeExecutorTest {

    public static void main(String[] args) {
        CustomeExecutor es = new CustomeExecutor();

        extracted(es, 60);

        extracted(es, 3600);

        extracted(es, 86400);

        extracted(es, 0);

    }

    private static void extracted(CustomeExecutor es, int t) {
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
