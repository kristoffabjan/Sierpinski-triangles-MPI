package com.company;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Triangles_panel extends JPanel {
    BlockingQueue<double[]> triangles;

    public Triangles_panel(BlockingQueue<double[]> triangles) {
        this.triangles = triangles;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLACK);
        this.setBackground(Color.PINK);
        for (double[] triangle: triangles) {
            int[] x = new int[]{(int)triangle[0] + (int)(triangle[2]/2),
                    (int)triangle[0] + (int)triangle[2],
                    (int)triangle[0]};
            int[] y = new int[]{(int)triangle[1] - (int)((Math.sin((double)Math.PI/3)) * (double)(triangle[2])),
                    (int)triangle[1],
                    (int)triangle[1] };
            g.fillPolygon(new Polygon(x,y, 3));
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(800, 600);
    }
}
