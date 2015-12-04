package co.phoenixlab.discord.stats;

public class RunningAverage {

    private double running;
    private int count;

    public RunningAverage() {
        running = 0;
        count = 0;
    }

    public void add(long val) {
        running = (running * (double) count + (double) val) / (double) (count + 1);
        count++;
    }

    public void add(int val) {
        running = (running * (double) count + (double) val) / (double) (count + 1);
        count++;
    }

    public void add(float val) {
        running = (running * (double) count + (double) val) / (double) (count + 1);
        count++;
    }

    public void add(double val) {
        running = (running * (double) count + (double) val) / (double) (count + 1);
        count++;
    }

    public double getRunningAverage() {
        return running;
    }

    public int getSize() {
        return count;
    }
}
