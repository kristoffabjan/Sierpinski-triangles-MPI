package com.company;


public class Task {
    //start point
    double x;
    double y;

    double length;
    double level;
    double max_level;
    //for mpi
    int source;
    int tag;

    public Task(double x, double y, double length, double level, double max_level) {
        this.x = x;
        this.y = y;
        this.length = length;
        this.level = level;
        this.max_level = max_level;
    }

    public Task(int source, int tag){
        this.source = source;
        this.tag = tag;
    }
}

