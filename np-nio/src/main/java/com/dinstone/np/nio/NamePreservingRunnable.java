/*
 * Copyright (C) 2011~2012 dinstone <dinstone@163.com>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.dinstone.np.nio;

public class NamePreservingRunnable implements Runnable {

    private Runnable runnable;

    private String newName;

    public NamePreservingRunnable(Runnable runnable, String newName) {
        this.runnable = runnable;
        this.newName = newName;
    }

    public void run() {
        Thread currentThread = Thread.currentThread();
        String oldName = currentThread.getName();

        if (newName != null) {
            currentThread.setName(newName);
        }
        try {
            runnable.run();
        } finally {
            currentThread.setName(oldName);
        }
    }

}
