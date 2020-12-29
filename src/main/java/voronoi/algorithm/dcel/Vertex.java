package voronoi.algorithm.dcel;

import java.util.function.Consumer;

import javafx.geometry.Point2D;

import voronoi.render.Window;

/**
 * A vertex in a doubly-connected edge list
 */
public class Vertex {
    /**
     * The coordinates of this vertex
     */
    private Point2D point;

    /**
     * An edge incident to this vertex (the choice of edge is unimportant)
     */
    private Edge incidentEdge;

    Vertex() {}

    Vertex(final double x, final double y) {
        this(new Point2D(x, y));
    }

    Vertex(final Point2D point) {
        this.point = point;
    }

    /**
     * @return the coordinates of this vertex
     */
    public Point2D getPoint() {
        return point;
    }

    /**
     * Sets this vertex's location
     *
     * @param point
     */
    public void setPoint(final Point2D point) {
        this.point = point;
    }

    /**
     * @return an edge incident to this vertex
     */
    public Edge getIncidentEdge() {
        return this.incidentEdge;
    }

    /**
     * Sets this vertex's incident edge
     *
     * @param incidentEdge
     */
    void setIncidentEdge(final Edge incidentEdge) {
        this.incidentEdge = incidentEdge;
    }

    /**
     * @return the degree of this vertex, that is, the number of edges which have
     *         this vertex as their origin.
     */
    public int getDegree() {
        int deg = 0;
        Edge e = incidentEdge;
        do {
            if (deg++ > 1000)
                throw new IllegalStateException(
                        String.format("Too many edges around vertex %08x", this.hashCode()));
            e = e.getTwin().getNext();
        } while (e != incidentEdge);
        return deg;
    }

//    /**
//     * @return a list of edges incident to this vertex.
//     */
//    public List<Edge> getIncidentEdges() {
//        final List<Edge> ret = new ArrayList<>();
//        Edge e = incidentEdge;
//        do {
//            ret.add(e);
//            e = e.getTwin().getNext();
//        } while (e != incidentEdge);
//
//        return ret;
//    }

    /**
     * Performs {@code action} for each edge incident to this vertex.
     *
     * @param action - the action to perform
     */
    public void forEachIncidentEdge(final Consumer<Edge> action) {
        // Store incident edge to allow the action to reassign this vertex's incident
        // edge while iterating
        final Edge end = incidentEdge;

        Edge e = incidentEdge;
        do {
            action.accept(e);
            e = e.getTwin().getNext();
        } while (e != end);
    }

    /**
     * @return the x-coordinate of the vertex's point
     * @throws NullPointerException if this vertex's point is null.
     */
    public double getX() {
        return this.point.getX();
    }

    /**
     * @return the y-coordinate of the vertex's point
     * @throws NullPointerException if this vertex's point is null.
     */
    public double getY() {
        return this.point.getY();
    }

    public void check() {
        if (!Window.DO_CHECK)
            return;

        System.out.printf("Check V %08x (%s)\n", this.hashCode(), new RuntimeException().getStackTrace()[1]);

        Edge e = incidentEdge;
        int i = 0;
        do {
            if (i < 100)
                System.out.println("(CV) " + e);
            if (i++ == 100) {
                new IllegalStateException("Infinite loop").printStackTrace();
            }
            if (e.getOrigin() != this)
                throw new IllegalStateException(
                        String.format("Incident edge %s has illegal origin %08x (expected %08x)", e,
                                e.getOrigin().hashCode(), this.hashCode()));
            e = e.getTwin().getNext();
        } while (e != incidentEdge);
        System.out.println();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Vertex [degree=");
        builder.append(getDegree());
        builder.append(", point=");
        builder.append(point);
        builder.append(", incidentEdge=");
        builder.append(incidentEdge);
        builder.append("]");
        return builder.toString();
    }
}