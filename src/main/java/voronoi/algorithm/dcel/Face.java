package voronoi.algorithm.dcel;

import java.util.function.Consumer;

import javafx.geometry.Point2D;

import voronoi.render.Window;

/**
 * A face in a doubly-connected edge list
 */
public class Face {

    /**
     * An edge on the boundary of this face (the choice of edge is unimportant).
     */
    private Edge boundaryEdge;

    /**
     * The voronoi point contained by this face.
     *
     * Note that this is not part of the standard implementation of a DCEL; rather,
     * it is used to color faces according to their corresponding point in a voronoi
     * diagram.
     */
    private Point2D containedPoint;

    Face() {}

    Face(final Edge boundaryEdge) {
        this.boundaryEdge = boundaryEdge;
    }

    /**
     * Performs the given action for each edge on the boundary of this face.
     *
     * @param action - the action to perform
     */
    public void forEachEdge(final Consumer<Edge> action) {
        this.boundaryEdge.forEachEdgeInFace(action);
    }

    /**
     * @return an edge on the boundary of this face
     */
    public Edge getBoundaryEdge() {
        return this.boundaryEdge;
    }

    /**
     * Sets this face's boundary edge
     *
     * @param boundaryEdge
     */
    void setBoundaryEdge(final Edge boundaryEdge) {
        this.boundaryEdge = boundaryEdge;
    }

    /**
     * @return the voronoi point contained by this face, or null if there is no such
     *         point
     */
    public Point2D getContainedPoint() {
        return this.containedPoint;
    }

    /**
     * Sets this face's contained point
     *
     * @param containedPoint
     */
    public void setContainedPoint(final Point2D containedPoint) {
        this.containedPoint = containedPoint;
    }

    public void check() {
        if (!Window.DO_CHECK)
            return;

        System.out.printf("Check F %08x (%s)\n", this.hashCode(), new RuntimeException().getStackTrace()[1]);

        Edge e = this.boundaryEdge;
        int i = 0;
        do {
            System.out.println("(CF) " + e);
            if (i++ == 100) {
                new IllegalStateException("Infinite loop").printStackTrace();
            }
            if (e.getInteriorFace() != this)
                throw new IllegalStateException(
                        String.format("Edge %s has incorrect interior face %08x (expected %08x)", e,
                                e.getInteriorFace().hashCode(), this.hashCode()));
            e = e.getNext();
        } while (e != this.boundaryEdge);
        System.out.println();
    }
}