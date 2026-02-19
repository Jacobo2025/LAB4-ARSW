package edu.eci.arsw.blueprints.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class Point{
    private  int x;
    private int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
    public Point(){}
}
