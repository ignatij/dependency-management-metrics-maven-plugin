package com.github.ignatij.statistic;

public class Point {
    private final String component;
    private final Double x;
    private final Double y;

    public Point(String component, Double x, Double y) {
        this.component = component;
        this.x = x;
        this.y = y;
    }

    public Double distance() {
        return Math.abs((x + y) - 1);
    }

    public String getComponent() {
        return component;
    }

    public Double getX() {
        return x;
    }

    public Double getY() {
        return y;
    }

    public Boolean isInZoneOfPain() {
        return x <= 0.5 && y <= 0.5;
    }

    public Boolean isInZoneOfUselessness() {
        return x >= 0.5 && y >= 0.5;
    }
}
