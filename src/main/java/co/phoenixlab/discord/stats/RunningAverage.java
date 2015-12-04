package co.phoenixlab.discord.stats;

import static java.lang.Math.*;

public class RunningAverage {

    private double running;
    private int count;
    private double min;
    private double max;

    public RunningAverage() {
        running = 0;
        count = 0;
        min = Double.MAX_VALUE;
        max = 0D;
    }

    public void add(long val) {
        running = (running * (double) count + (double) val) / (double) (count + 1);
        count++;
        min = min(min, val);
        max = max(max, val);
    }

    public void add(int val) {
        running = (running * (double) count + (double) val) / (double) (count + 1);
        count++;
        min = min(min, val);
        max = max(max, val);
    }

    public void add(float val) {
        running = (running * (double) count + (double) val) / (double) (count + 1);
        count++;
        min = min(min, val);
        max = max(max, val);
    }

    public void add(double val) {
        running = (running * (double) count + (double) val) / (double) (count + 1);
        count++;
        min = min(min, val);
        max = max(max, val);
    }

    public double getRunningAverage() {
        return running;
    }

    public int getSize() {
        return count;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public String summary() {
        return String.format("%.2f/%.2f/%.2f", min, running, max);
    }
}
