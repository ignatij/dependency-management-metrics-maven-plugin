package com.github.ignatij.statistic;

import java.util.List;

public final class StatisticUtil {

    public static Double mean(List<Point> points) {
        return points.stream().mapToDouble(Point::distance).sum() / points.size();
    }

    public static Double variance(List<Point> points) {
        double mean = mean(points);
        double variance = 0;
        for(Point p : points) {
            variance += Math.pow(p.distance() - mean, 2);
        }
        return variance;
    }

    public static Double standardDeviation(List<Point> points) {
        return Math.sqrt(variance(points));
    }

}
