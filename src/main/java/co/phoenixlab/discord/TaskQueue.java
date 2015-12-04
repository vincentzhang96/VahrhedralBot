package co.phoenixlab.discord;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class TaskQueue {

    private final BlockingDeque<Runnable> pendingTasks;

    public TaskQueue() {
        pendingTasks = new LinkedBlockingDeque<>();
    }

    public void runOnMain(Runnable runnable) {
        pendingTasks.add(runnable);
    }

    public void executeWaiting() {
        Runnable runnable;
        while (true) {
            try {
                runnable = pendingTasks.take();
            } catch (InterruptedException e) {
                break;
            }
            runnable.run();
        }
    }

}
