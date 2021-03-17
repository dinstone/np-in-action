package com.dinstone.np;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

final class CallableTask implements Callable<List<String>> {

    private int tag;

    public CallableTask(int t) {
        this.tag = t;
    }

    public List<String> call() throws Exception {
        int ut = 31536000 / tag;

        // load data from db
        List<String> ls = new ArrayList<String>();
        for (int i = 0; i < ut; i++) {
            ls.add("ut-" + i);
        }

        System.out.println(ls.size());

        return ls;
    }
}