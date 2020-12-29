package voronoi.algorithm.dcel;

import java.util.function.Consumer;

import javafx.geometry.Point2D;

import voronoi.render.Window;

/**
 * An edge (more correctly, a "half-edge") in a Doubly-connected edge list.
 *
 * Each edge can be thought of as a ray with a defined start point, but no
 * defined end; the end point is defined by the start of the edge it's connected
 * to.
 */
public class Edge {
    /**
     * The start point of the edge
     */
    private Vertex origin;

    /**
     * Whether or not the edge is horizontal.
     *
     * This is not part of a standard DCEL implementation, but is used to keep track
     * of which lines comprise the bottom border and side borders in the beach line
     * for the modified Fortune's algorithm.
     */
    private boolean horizontal = false;

    /**
     * The other half of this edge
     */
    private Edge twin;

    /**
     * The edge after this edge
     */
    private Edge next;

    /**
     * The edge before this edge
     */
    private Edge previous;

    /**
     * The face bounded by this edge.
     */
    private Face interiorFace;

    Edge() {}

//    Edge(final Vertex origin, final Face interiorFace) {
//        this.origin = origin;
//        this.origin.incidentEdge = this;
//        this.interiorFace = interiorFace;
//        this.interiorFace.boundaryEdge = this;
//
//        this.twin = new Edge();
//        this.twin.twin = this;
//    }
//
//    Edge(final Vertex origin, final Edge next) {
//        this.origin = origin;
//        this.origin.incidentEdge = this;
//        this.next = next;
//
//        this.interiorFace = this.next.interiorFace;
//
//        this.next.previous = this;
//
//        this.twin = new Edge();
//        this.twin.origin = this.next.origin;
//        this.twin.origin.incidentEdge = this.twin;
//        this.twin.twin = this;
//    }

    /**
     * Subdivide this edge and its twin into two half-edges each, at the given point
     * (a new vertex is created at this location). This edge is not removed from the
     * DCEL; instead the new edge is inserted into the DCEL between this edge and
     * its successor, and the endpoint of this edge is updated. The new edge is
     * returned.
     *
     * Note that the newly-created vertex can be accessed as the origin of the
     * return value.
     *
     * @param point - the location to split this edge at
     * @return the newly-added edge (such that given
     *         {@code Edge e2 = e1.split(...);}, {@code e1.next == e2}).
     */
    public Edge subdivide(final Point2D point) {
        final Edge other = new Edge();
        other.setTwin(new Edge());

        other.twin.setOrigin(this.twin.origin);

        final Vertex vertex = new Vertex(point);
        this.twin.setOrigin(vertex);
        other.setOrigin(vertex);

        this.insertSuccessor(other);
        this.twin.insertPredecessor(other.twin);

        other.setHorizontal(this.horizontal);
        other.twin.setHorizontal(this.twin.horizontal);

        other.setInteriorFace(this.interiorFace);
        other.twin.setInteriorFace(this.twin.interiorFace);

        this.check();
        this.twin.check();
        this.previous.check();
        this.previous.twin.check();

        return other;
    }

    /**
     * Removes this edge and joins its endpoints into a single vertex. The resulting
     * vertex is the origin of this edge; the other endpoint (the origin of this
     * edge's twin) is removed.
     *
     * @return the joined vertex
     */
    public Vertex collapse() {
        final Vertex collapsedVertex = this.origin;
        this.twin.origin.forEachIncidentEdge(e -> e.setOrigin(collapsedVertex));

        this.next.interiorFace.setBoundaryEdge(this.next);
        this.twin.next.interiorFace.setBoundaryEdge(this.twin.next);

        this.remove();
        this.twin.remove();

        collapsedVertex.setIncidentEdge(this.next);

        collapsedVertex.check();

        this.next.check();
        this.twin.next.check();

        return collapsedVertex;
    }

    /**
     * Removes this edge and joins the faces that it was separating. Also dissolves
     * this edge's neighbors separating the same faces (so that no vertices with
     * degree 1 are left).
     *
     * All removed edges have their interior face set to null, and no other changes
     * are made, to aid iteration.
     *
     * @return the joined face
     */
    public Face dissolve() {
        final Face joinedFace = this.interiorFace;
        this.twin.interiorFace.setBoundaryEdge(null);

        this.twin.forEachEdgeInFace(e -> e.interiorFace = joinedFace);

        Edge e1 = this;
        Edge e2 = this.twin;
        do {
            e1.interiorFace = e2.interiorFace = null;
            e1 = e1.next;
            e2 = e2.previous;
        } while (e1 == e2.twin);
        e2.setNext(e1);

        e1.origin.setIncidentEdge(e1);

        e1 = this.twin;
        e2 = this;
        do {
            e1.interiorFace = e2.interiorFace = null;
            e1 = e1.next;
            e2 = e2.previous;
        } while (e1 == e2.twin);
        e2.setNext(e1);

        e1.origin.setIncidentEdge(e1);
        e2.interiorFace.setBoundaryEdge(e2);

        e2.interiorFace.check();
        e2.check();

        return e2.interiorFace;
    }

    /**
     * Splits this edge's interior face by introducing a new edge between edge1's
     * origin and the edge2's origin. Returns the newly-created edge (the half of
     * the edge bounding the same face as this edge). The newly created face can be
     * accessed as the interior face of the returned edge's twin.
     *
     * @param edge1 - an edge bounding the face to split
     * @param edge2 - another edge bounding the face to split
     * @return the newly added edge
     */
    public static Edge splitFaceBetween(final Edge edge1, final Edge edge2) {
        if (edge1.interiorFace != edge2.interiorFace)
            throw new IllegalArgumentException("Cannot split face between edges bounding different faces");

        final Edge splitEdge = new Edge();
        splitEdge.setTwin(new Edge());

        splitEdge.twin.setOrigin(edge1.origin);
        splitEdge.setOrigin(edge2.origin);

        edge1.previous.setNext(splitEdge.twin);
        edge2.previous.setNext(splitEdge);
        edge1.setPrevious(splitEdge);
        edge2.setPrevious(splitEdge.twin);

        splitEdge.setInteriorFace(edge1.interiorFace);

        final Face newFace = new Face();
        splitEdge.twin.forEachEdgeInFace(e -> e.setInteriorFace(newFace));

        edge1.interiorFace.check();
        edge2.interiorFace.check();

        splitEdge.check();
        splitEdge.twin.check();

        splitEdge.origin.check();
        splitEdge.twin.origin.check();

        if (splitEdge.twin.interiorFace != newFace)
            throw new IllegalStateException("Incorrect interior face of twin after face split");
        if (splitEdge.interiorFace == newFace)
            throw new IllegalStateException("Incorrect interior face after face split");

        return splitEdge;
    }

    /**
     * Rips the vertex at the origin of the given edges into two vertices, and
     * connects the two vertices with an edge. The fixed edge keeps its origin in
     * the same location; the moving edge's origin is updated to the given point.
     * The added edge is added as the predecessor of the fixed edge, and its twin is
     * added as the predecessor of the moving edge.
     *
     * The newly added edge is returned. The newly created vertex can be accessed as
     * the origin of the return value's twin.
     *
     * @param point      - the new location of the origin of the moving edge
     * @param fixedEdge  - an edge whose origin defines the vertex to rip. This
     *                   edge's origin will not be updated.
     * @param movingEdge - another edge with the same origin. This edge's origin
     *                   will be updated to a new vertex at {@code point}
     * @return the newly-added edge.
     */
    public static Edge ripVertex(final Point2D point, final Edge fixedEdge, final Edge movingEdge) {
        if (fixedEdge.origin != movingEdge.origin)
            throw new IllegalArgumentException("Cannot rip vertex between edges of different origins");

        final Edge newEdge = new Edge();
        newEdge.setTwin(new Edge());

        final Vertex newVertex = new Vertex(point);

        movingEdge.setOrigin(newVertex);
        newEdge.twin.setOrigin(fixedEdge.origin);

        fixedEdge.insertPredecessor(newEdge);
        movingEdge.insertPredecessor(newEdge.twin);

        newEdge.setInteriorFace(fixedEdge.interiorFace);
        newEdge.twin.setInteriorFace(movingEdge.interiorFace);

        newVertex.forEachIncidentEdge(e -> e.setOrigin(newVertex));

        movingEdge.check();
        fixedEdge.check();

        movingEdge.origin.check();
        fixedEdge.origin.check();

        movingEdge.interiorFace.check();
        fixedEdge.interiorFace.check();

        return newEdge;
    }

    /**
     * Inserts the given edge after this edge (between this and this.next).
     *
     * @param edge - the edge to insert
     */
    protected final void insertSuccessor(final Edge edge) {
        edge.setNext(this.next);
        this.setNext(edge);
    }

    /**
     * Inserts the given edge before this edge (between this.previous and this)
     *
     * @param edge - the edge to insert
     */
    protected final void insertPredecessor(final Edge edge) {
        edge.setPrevious(this.previous);
        this.setPrevious(edge);
    }

    /**
     * Removes this edge
     */
    protected final void remove() {
        this.previous.setNext(this.next);
    }

    /**
     * @return the origin of this edge
     */
    public Vertex getOrigin() {
        return origin;
    }

    /**
     * Sets this edge's origin
     *
     * @param origin
     */
    void setOrigin(final Vertex origin) {
        this.origin = origin;
        origin.setIncidentEdge(this);
    }

    /**
     * @return whether or not this edge is horizontal
     */
    public boolean isHorizontal() {
        return horizontal;
    }

    /**
     * Sets whether or not this edge is horizontal
     *
     * @param horizontal
     */
    public void setHorizontal(final boolean horizontal) {
        this.horizontal = horizontal;
        this.twin.horizontal = horizontal;
    }

    /**
     * @return the half-edge opposite this half-edge
     */
    public Edge getTwin() {
        return twin;
    }

    /**
     * Sets this edge's twin
     *
     * @param twin
     */
    void setTwin(final Edge twin) {
        this.twin = twin;
        twin.twin = this;
    }

    /**
     * @return the next edge from this edge
     */
    public Edge getNext() {
        return next;
    }

    /**
     * Sets this edge's next edge
     *
     * @param next
     */
    void setNext(final Edge next) {
        this.next = next;
        next.previous = this;
    }

    /**
     * @return the previous edge from this edge
     */
    public Edge getPrevious() {
        return previous;
    }

    /**
     * Sets this edge's previous edge
     *
     * @param previous
     */
    void setPrevious(final Edge previous) {
        this.previous = previous;
        previous.next = this;
    }

    /**
     * @return the face bounded by this edge
     */
    public Face getInteriorFace() {
        return interiorFace;
    }

    /**
     * Sets this edge's interior face
     *
     * @param interiorFace
     */
    void setInteriorFace(final Face interiorFace) {
        this.interiorFace = interiorFace;
        interiorFace.setBoundaryEdge(this);
    }

    /**
     * Performs the given action for each edge on the boundary this edge's interior
     * face.
     *
     * @param action - the action to perform
     */
    public void forEachEdgeInFace(final Consumer<Edge> action) {
        Edge iterEdge = this;
        do {
            action.accept(iterEdge);
            iterEdge = iterEdge.next;
        } while (iterEdge != this);
    }

    public void check() {
        if (!Window.DO_CHECK)
            return;

        System.out.printf("Check E %08x (%s)\n", this.hashCode(), new RuntimeException().getStackTrace()[1]);

        Edge e = this;
        int i = 0;
        do {
            try {
                System.out.print("(CD) " + e.origin.getDegree());
            } catch (final IllegalStateException ex) {
                System.out.print("(CD) ?");
//                System.out.println(" (C) " + e);
//                e.origin.check();
//                throw ex;
            }
            System.out.println(" (C) " + e);
            if (i++ == 100) {
                new IllegalStateException("Infinite loop").printStackTrace();
            }
            if (e.next.previous != e)
                throw new IllegalStateException(
                        String.format("Edge %s has incorrect previous %08x (expected %08x)", e.next,
                                e.next.previous.hashCode(), e.hashCode()));
            e = e.next;
        } while (e != this);
        System.out.println();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(this.getClass().getSimpleName());
        builder.append("@");
        builder.append(String.format("%08x", this.hashCode()));
        if (this.horizontal)
            builder.append(" H");
        builder.append(" [origin=");
        builder.append(origin == null ? "- null -" : String.format("%08x", origin.hashCode()));
        builder.append(", twin=");
        builder.append(twin == null ? "- null -" : String.format("%08x", twin.hashCode()));
        builder.append(", next=");
        builder.append(next == null ? "- null -" : String.format("%08x", next.hashCode()));
        builder.append(", previous=");
        builder.append(previous == null ? "- null -" : String.format("%08x", previous.hashCode()));
        builder.append(", interiorFace=");
        builder.append(interiorFace == null ? "- null -" : String.format("%08x", interiorFace.hashCode()));
        builder.append("]");
        return builder.toString();
    }

    public static Edge newDCEL() {
        final Edge edge = new Edge();
        edge.setTwin(new Edge());

        edge.setNext(edge);
        edge.twin.setNext(edge.twin);

        final Vertex initialVertex = new Vertex();
        edge.setOrigin(initialVertex);
        edge.twin.setOrigin(initialVertex);

        final Face initialInterior = new Face();
        edge.setInteriorFace(initialInterior);

        final Face initialExterior = new Face();
        edge.twin.setInteriorFace(initialExterior);

        return edge;
    }

}