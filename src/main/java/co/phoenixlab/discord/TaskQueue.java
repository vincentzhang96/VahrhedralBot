package co.phoenixlab.discord;

import java.util.concurrent.ConcurrentLinkedQueue;

public class TaskQueue {

    private final ConcurrentLinkedQueue<Runnable> pendingTasks;

    public TaskQueue() {
        pendingTasks = new ConcurrentLinkedQueue<>();
    }

    public void runOnMain(Runnable runnable) {
        pendingTasks.add(runnable);
    }

    public void executeWaiting() {
        Runnable runnable;
        while ((runnable = pendingTasks.poll()) != null) {
            runnable.run();
        }
    }

}
