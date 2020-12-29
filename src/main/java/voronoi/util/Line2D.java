package voronoi.util;

import javafx.geometry.Point2D;

/**
 * A directed line segment.
 */
public class Line2D {

    private final Point2D start;
    private final Point2D end;

    /**
     * Creates a new line from start to end.
     *
     * @param start - the start of the line
     * @param end   - the end of the line
     */
    public Line2D(final Point2D start, final Point2D end) {
        this.start = start;
        this.end = end;
    }

    /**
     * Creates a new line from (x1, y1) to (x2, y2)
     *
     * @param x1 - the x-coodinate of the start of the line
     * @param y1 - the y-coodinate of the start of the line
     * @param x2 - the x-coodinate of the end of the line
     * @param y2 - the y-coodinate of the end of the line
     */
    public Line2D(final double x1, final double y1, final double x2, final double y2) {
        this.start = new Point2D(x1, y1);
        this.end = new Point2D(x2, y2);
    }

    /**
     * @return the start of the line
     */
    public Point2D getStart() {
        return this.start;
    }

    /**
     * @return the end of the line
     */
    public Point2D getEnd() {
        return this.end;
    }

    /**
     * @return the x-coordinate of the start of the line
     */
    public double getX1() {
        return this.start.getX();
    }

    /**
     * @return the y-coordinate of the start of the line
     */
    public double getY1() {
        return this.start.getY();
    }

    /**
     * @return the x-coordinate of the end of the line
     */
    public double getX2() {
        return this.end.getX();
    }

    /**
     * @return the y-coordinate of the end of the line
     */
    public double getY2() {
        return this.end.getY();
    }

    /**
     * @return the length of the line, that is, the distance between start and end.
     */
    public double getLength() {
        return this.end.distance(this.start);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Line2D [x1=");
        builder.append(getX1());
        builder.append(", y1=");
        builder.append(getY1());
        builder.append(", x2=");
        builder.append(getX2());
        builder.append(", y2=");
        builder.append(getY2());
        builder.append("]");
        return builder.toString();
    }

}
