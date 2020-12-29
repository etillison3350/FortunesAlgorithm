package voronoi.util;

import javafx.geometry.Point2D;

/**
 * A circle with a center and radius
 */
public class Circle2D {

    private final Point2D center;
    private final double radius;

    /**
     * Creates a new circle with the given center and radius
     *
     * @param center
     * @param radius
     */
    public Circle2D(final Point2D center, final double radius) {
        this.center = center;
        this.radius = radius;
    }

    /**
     * Creates a new circle with center {@code (centerX, centerY)} and the given
     * radius
     *
     * @param centerX
     * @param centerY
     * @param radius
     */
    public Circle2D(final double centerX, final double centerY, final double radius) {
        this(new Point2D(centerX, centerY), radius);
    }

    /**
     * @return the center of the circle
     */
    public Point2D getCenter() {
        return center;
    }

    /**
     * @return the x-coordinate of the center of the circle
     */
    public double getCenterX() {
        return center.getX();
    }

    /**
     * @return the y-coordinate of the center of the circle
     */
    public double getCenterY() {
        return center.getY();
    }

    /**
     * @return the radius of the circle
     */
    public double getRadius() {
        return radius;
    }

}
