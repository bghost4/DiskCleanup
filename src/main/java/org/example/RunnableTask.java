package org.example;

import javafx.concurrent.Task;

public class RunnableTask extends Task<Void> {

    private final Runnable r;

    public RunnableTask(String title,Runnable r) {
        updateTitle(title);
        this.r = r;
    }

    @Override
    protected Void call() throws Exception {
        r.run();
        return null;
    }

}
