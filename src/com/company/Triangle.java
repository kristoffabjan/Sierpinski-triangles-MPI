package com.company;

public class Triangle {
    //start point
    double x;
    double y;

    double length;
    double level;
    double max_level;
    //for mpi
    //who is this task for
    int source;
    int tag;

    public Triangle(double x, double y, double length, double level, double max_level) {
        this.x = x;
        this.y = y;
        this.length = length;
        this.level = level;
        this.max_level = max_level;
    }

    public Triangle(int source, int tag){
        this.source = source;
        this.tag = tag;
    }
}


