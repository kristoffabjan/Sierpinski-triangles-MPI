package com.company;


public class Task {
    //start point
    private double x;
    private double y;

    private double length;
    private double level;
    private double max_level;
    //for mpi
        //who is this task for
    private int source;
    private int tag;

    public Task(double x, double y, double length, double level, double max_level, int source, int tag) {
        this.x = x;
        this.y = y;
        this.length = length;
        this.level = level;
        this.max_level = max_level;
        this.source = source;
        this.tag = tag;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public double getLevel() {
        return level;
    }

    public void setLevel(double level) {
        this.level = level;
    }

    public double getMax_level() {
        return max_level;
    }

    public void setMax_level(double max_level) {
        this.max_level = max_level;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }
}

