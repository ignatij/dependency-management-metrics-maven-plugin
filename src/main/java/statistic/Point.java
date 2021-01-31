package statistic;

public class Point {
    private final Double x;
    private final Double y;

    public Point(Double x, Double y) {
        this.x = x;
        this.y = y;
    }

    public Double distance() {
        return Math.abs((x + y) - 1);
    }
}
