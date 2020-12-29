package voronoi.util;

import javafx.geometry.Point2D;

import voronoi.algorithm.dcel.Edge;
import voronoi.algorithm.dcel.ParabolaEdge;

public class Util {

    private Util() {}

    /**
     * Returns the y-coordinate of the beach line parabola with the given focus at
     * the given x-coordinate, given the sweep line is at
     * {@code y = sweepLineHeight}. This parabola is the locus of points equidistant
     * from the focus and the line {@code y = sweepLineHeight} (the directrix), and
     * can be represented as the curve
     * {@code y = 1/2 ((x - x0) ^ 2 / (y0 - h) + y0 + h)}, where {@code x0, y0} is
     * the focus, and {@code h} is the sweep line height.
     *
     * @param x               - the x coordinate to check
     * @param focus           - the focus of the parabola
     * @param sweepLineHeight - the height of the sweep line, which defines the
     *                        directrix
     * @return the height of the beach line parabola with the given parameters
     */
    public static double beachLineHeightForPoint(final double x, final Point2D focus, final double sweepLineHeight) {
        final double px = focus.getX(), py = focus.getY();
        return 0.5 * ((x - px) * (x - px) / (py - sweepLineHeight) + py + sweepLineHeight);
    }

    /**
     * Calculates the x-coordinate of the intersection point between the two given
     * edges, given that the sweep line is at {@code y = sweepLineHeight}. There are
     * multiple cases:
     * <ul>
     * <li>Both edges are parabola edges. In this case, the intersection between
     * them is given by
     *
     * <pre>
     *     x1 (y0 - h) - x0 (y1 - h) + sqrt((y0 - h)(y1 - h)((x0 - x1) ^ 2 + (y0 - y1) ^ 2))
     * x = ---------------------------------------------------------------------------------
     *                                         (y0 - y1)
     * </pre>
     *
     * where {@code (x0, y0)} and {@code (x1, y1)} are the foci of the left and
     * right parabolas, repsectively, and {@code h} is the sweep line height.
     *
     * Note that this is the quadratic formula, and thus subtracting the radical
     * (instead of adding) is also an intersection between the given parabolas.
     * However, given that the left parabola is to the left of the intersection
     * point, and the right parabola to the right, the positive square root gives
     * the correct answer. Notably, interchanging left and right gives the solution
     * with the negative square root.</li>
     * <li>Only one edge is a parabola, and the straight-line edge is horizontal. In
     * this case, the intersection is given by
     *
     * <pre>
     * x = x0 +/- sqrt((h - y0)(h + y0 - 2 y1))
     * </pre>
     *
     * where {@code (x0, y0)} is the focus of the parabolic edge, {@code y1} is the
     * height of the horizontal edge, and {@code h} is the sweep line height.
     *
     * The sign of the square root is determined by whether the horizontal edge is
     * to the left (+) or right (-) of the parabolic edge.</li>
     * <li>Only one edge is a parabola, and the straight-line edge is not
     * horizontal. In this case, the straight edge is assumed to be vertical, and
     * its x-coordinate is returned.</li>
     * <li>Neither edge is a parabola. Returns the x-coodinate of the right edge's
     * origin.</li>
     * </ul>
     *
     * @param leftEdge        - the left edge
     * @param rightEdge       - the right edge
     * @param sweepLineHeight - the height of the sweep line, which defines the
     *                        directrix of any parabola edges given
     * @return the x-coordinate of the intersection between leftEdge and rightEdge.
     */
    public static double beachLineIntersectionX(final Edge leftEdge, final Edge rightEdge,
            final double sweepLineHeight) {
        if (leftEdge instanceof ParabolaEdge && rightEdge instanceof ParabolaEdge) {
            final Point2D leftParCenter = ((ParabolaEdge) leftEdge).focus;
            final Point2D rightParCenter = ((ParabolaEdge) rightEdge).focus;

            final double xl = leftParCenter.getX();
            final double yl = leftParCenter.getY();
            final double xr = rightParCenter.getX();
            final double yr = rightParCenter.getY();

            final double det = Math.sqrt((yl - sweepLineHeight) * (yr - sweepLineHeight)
                    * ((xl - xr) * (xl - xr) + (yl - yr) * (yl - yr)));
            final double nb = xr * (yl - sweepLineHeight) - xl * (yr - sweepLineHeight);

            final double int1 = (nb + det) / (yl - yr);
//            final double int2 = (nb - det) / (yl - yr);

            // There are two solutions; if the center of the left parabola is above
            // the center of the right parabola, it's the smaller of the two
            // In other words, always pick int1
            return int1; // yl < yr ? Math.min(int1, int2) : Math.max(int1, int2);
        } else {
            final boolean left;
            final Point2D parCenter;
            final double y;

            if (leftEdge instanceof ParabolaEdge) {
                if (!rightEdge.isHorizontal())
                    return rightEdge.getOrigin().getX();

                left = true;
                parCenter = ((ParabolaEdge) leftEdge).focus;
                y = rightEdge.getOrigin().getY();
            } else if (rightEdge instanceof ParabolaEdge) {
                if (!leftEdge.isHorizontal())
                    return leftEdge.getOrigin().getX();

                left = false;
                parCenter = ((ParabolaEdge) rightEdge).focus;
                y = leftEdge.getOrigin().getY();
            } else {
                return rightEdge.getOrigin().getX();
            }

            final double det = Math.sqrt((sweepLineHeight - parCenter.getY())
                    * (sweepLineHeight + parCenter.getY() - 2 * y));
            if (Double.isNaN(det))
                return left ? rightEdge.getOrigin().getX() : leftEdge.getOrigin().getX();
            return left ? parCenter.getX() - det : parCenter.getX() + det;
        }
    }

    /**
     * Calculates the intersection point between the line through the points
     * {@code a} and {@code b}, and the horizontal line at the given y-coordinate.
     *
     * @param a - the first point that the line passes through
     * @param b - the second point that the line passes through
     * @param y - the y-coordinate of the horizontal line
     * @return the point at which the given lines intersect.
     */
    public static Point2D intersectLineHorizontal(final Point2D a, final Point2D b, final double y) {
        return new Point2D((b.getX() - a.getX()) / (b.getY() - a.getY()) * (y - a.getY()) + a.getX(), y);
    }

    /**
     * Calculates the x-coordinate of the circle containing points {@code a} and
     * {@code b}, given that the y-coordinate of the center is at the given value.
     *
     * @param a       - the first point that lies on the circle
     * @param b       - the second point that lies on the circle
     * @param centerY - the y-coordinate of the center of the circle
     * @return the x-coordinate of the center of the circle.
     */
    public static double circleCenterX(final Point2D a, final Point2D b, final double centerY) {
        final double x1 = a.getX();
        final double y1 = a.getY();
        final double x2 = b.getX();
        final double y2 = b.getY();

        return 0.5 * ((y1 * y1 - y2 * y2 - 2 * centerY * (y1 - y2)) / (x1 - x2) + x1 + x2);
    }

    /**
     * Calculates the y-coordinate of the circle containing points {@code a} and
     * {@code b}, given that the x-coordinate of the center is at the given value.
     *
     * @param a       - the first point that lies on the circle
     * @param b       - the second point that lies on the circle
     * @param centerX - the x-coordinate of the center of the circle
     * @return the y-coordinate of the center of the circle.
     */
    public static double circleCenterY(final Point2D a, final Point2D b, final double centerX) {
        final double x1 = a.getX();
        final double y1 = a.getY();
        final double x2 = b.getX();
        final double y2 = b.getY();

        return 0.5 * ((x1 * x1 - x2 * x2 - 2 * centerX * (x1 - x2)) / (y1 - y2) + y1 + y2);
    }

    /**
     * Calculates the center point of the circle containing the three given points.
     *
     * @param a - the first point that lies on the circle
     * @param b - the second point that lies on the circle
     * @param c - the third point that lies on the circle
     * @return the center of the circle
     */
    public static Point2D circleCenter(final Point2D a, final Point2D b, final Point2D c) {
        // See https://math.stackexchange.com/a/1460096
        final double ax = a.getX();
        final double ay = a.getY();
        final double bx = b.getX();
        final double by = b.getY();
        final double cx = c.getX();
        final double cy = c.getY();
        final double a2 = ax * ax + ay * ay;
        final double b2 = bx * bx + by * by;
        final double c2 = cx * cx + cy * cy;

        final double m11 = ax * by + ay * cx + bx * cy - ax * cy - ay * bx - by * cx;
        final double m12 = a2 * by + ay * c2 + b2 * cy - a2 * cy - ay * b2 - by * c2;
        final double m13 = a2 * bx + ax * c2 + b2 * cx - a2 * cx - ax * b2 - bx * c2;

        return new Point2D(0.5 * m12 / m11, -0.5 * m13 / m11);
    }

    /**
     * Calculates the point at which the two lines defined by offsetting the given
     * lines by the given distance intersect. The offset direction is to the left,
     * of the direction of the line, when looking down the z-axis of a right-handed
     * coordinate system.
     *
     * @param line1          - the first line
     * @param line2          - the second line
     * @param offsetDistance - the distance to offset each line by (positive values
     *                       offset left, negative values offset right)
     * @return the point at which the offset lines intersect.
     */
    public static Point2D offsetIntersection(final Line2D line1, final Line2D line2, final double offsetDistance) {
        final double x11 = line1.getX1();
        final double y11 = line1.getY1();
        final double x12 = line1.getX2();
        final double y12 = line1.getY2();
        final double x21 = line2.getX1();
        final double y21 = line2.getY1();
        final double x22 = line2.getX2();
        final double y22 = line2.getY2();
        final double len1 = offsetDistance * line1.getLength();
        final double len2 = offsetDistance * line2.getLength();

        final double denom = x21 * y11 - x22 * y11 - x21 * y12 + x22 * y12 - x11 * y21 + x12 * y21 + x11 * y22
                - x12 * y22;

        final double numX = len2 * x11 - len2 * x12 - len1 * x21 + len1 * x22 - x21 * x11 * y12 + x22 * x11 * y12
                - x22 * x11 * y21 + x21 * x11 * y22 + x12 * x21 * y11 - x12 * x22 * y11 + x12 * x22 * y21
                - x12 * x21 * y22;
        final double numY = len2 * y11 - len2 * y12 - len1 * y21 + len1 * y22 + x12 * y21 * y11 - x22 * y21 * y11
                - x12 * y22 * y11 + x21 * y22 * y11 - x11 * y12 * y21 + x22 * y12 * y21 + x11 * y12 * y22
                - x21 * y12 * y22;

        return new Point2D(numX / denom, numY / denom);
    }

    /**
     * Calculates the shortest distance between the given point line, that is, the
     * length of the line segment perpendicular to the given line from a point on
     * the line to the given point.
     *
     * @param line  - the line
     * @param point - the point
     * @return the distance between the line and the point.
     */
    public static double distanceOffLine(final Line2D line, final Point2D point) {
        return ((line.getX2() - line.getX1()) * (line.getY1() - point.getY())
                - (line.getY2() - line.getY1()) * (line.getX1() - point.getX())) / line.getLength();
    }

    /**
     * Calculates the circle tangent to the given lines. The circle will be left of
     * the direction of each line, when looking down the z-axis of a right-handed
     * coordinate system.
     *
     * @param line1 - the first line
     * @param line2 - the second line
     * @param line3 - the third line
     * @return a circle tangent to all three lines
     */
    public static Circle2D circleTangentToLines(final Line2D line1, final Line2D line2, final Line2D line3) {
        final double x1 = line1.getX1();
        final double y1 = line1.getY1();
        final double x2 = line1.getX2();
        final double y2 = line1.getY2();
        final double x3 = line2.getX1();
        final double y3 = line2.getY1();
        final double x4 = line2.getX2();
        final double y4 = line2.getY2();
        final double x5 = line3.getX1();
        final double y5 = line3.getY1();
        final double x6 = line3.getX2();
        final double y6 = line3.getY2();
        final double d12 = line1.getLength();
        final double d34 = line2.getLength();
        final double d56 = line3.getLength();

        final double denom = -determinant3(x1, x3, x5, y1, y3, y5, d12, d34, d56)
                + determinant3(x1, x3, x5, y2, y4, y6, d12, d34, d56)
                + determinant3(x2, x4, x6, y1, y3, y5, d12, d34, d56)
                - determinant3(x2, x4, x6, y2, y4, y6, d12, d34, d56);

        final double numX = -determinant3(x1, x3, x5, x2 * y1, x4 * y3, x6 * y5, d12, d34, d56)
                + determinant3(x1, x3, x5, x1 * y2, x3 * y4, x5 * y6, d12, d34, d56)
                + determinant3(x2, x4, x6, x2 * y1, x4 * y3, x6 * y5, d12, d34, d56)
                - determinant3(x2, x4, x6, x1 * y2, x3 * y4, x5 * y6, d12, d34, d56);

        final double numY = -determinant3(x1 * y2, x3 * y4, x5 * y6, y1, y3, y5, d12, d34, d56)
                + determinant3(x1 * y2, x3 * y4, x5 * y6, y2, y4, y6, d12, d34, d56)
                + determinant3(x2 * y1, x4 * y3, x6 * y5, y1, y3, y5, d12, d34, d56)
                - determinant3(x2 * y1, x4 * y3, x6 * y5, y2, y4, y6, d12, d34, d56);

        final double numRad = determinant3(x1 * y2, x3 * y4, x5 * y6, x1, x3, x5, y1, y3, y5)
                - determinant3(x1 * y2, x3 * y4, x5 * y6, x1, x3, x5, y2, y4, y6)
                - determinant3(x1 * y2, x3 * y4, x5 * y6, x2, x4, x6, y1, y3, y5)
                + determinant3(x1 * y2, x3 * y4, x5 * y6, x2, x4, x6, y2, y4, y6)
                - determinant3(x2 * y1, x4 * y3, x6 * y5, x1, x3, x5, y1, y3, y5)
                + determinant3(x2 * y1, x4 * y3, x6 * y5, x1, x3, x5, y2, y4, y6)
                + determinant3(x2 * y1, x4 * y3, x6 * y5, x2, x4, x6, y1, y3, y5)
                - determinant3(x2 * y1, x4 * y3, x6 * y5, x2, x4, x6, y2, y4, y6);

        return new Circle2D(numX / denom, numY / denom, Math.abs(numRad / denom));
    }

    /**
     * Calculates the determinant of the 3x3 matrix
     *
     * <pre>
     * a b c
     * d e f
     * g h i
     * </pre>
     *
     * @param a
     * @param b
     * @param c
     * @param d
     * @param e
     * @param f
     * @param g
     * @param h
     * @param i
     * @return the determinant
     */
    public static double determinant3(final double a, final double b, final double c, final double d, final double e,
            final double f, final double g, final double h, final double i) {
        return a * e * i - a * f * h - b * d * i + b * f * g + c * d * h - c * e * g;
    }

}
