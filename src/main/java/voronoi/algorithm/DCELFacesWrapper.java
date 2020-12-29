package voronoi.algorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javafx.geometry.Point2D;

import voronoi.algorithm.dcel.Edge;
import voronoi.algorithm.dcel.Face;
import voronoi.algorithm.dcel.ParabolaEdge;
import voronoi.algorithm.dcel.Vertex;

/**
 * A wrapper for a doubly-connected edge list.
 *
 * While, technically, all operations for a DCEL are provided by a {@link Edge}
 * class, there is no method of iterating through all faces efficiently. This
 * class serves as a wrapper to the functionality provided in the {@code Edge}
 * class that also keeps a list of faces in the DCEL. This list is only
 * guaranteed to be accurate if the methods in the {@code Edge} class are not
 * used, and their equivalents in this class used instead.
 */
public class DCELFacesWrapper {

    /**
     * The list of faces in the DCEL
     */
    private final List<Face> faces;

    public DCELFacesWrapper() {
        this.faces = new ArrayList<>();

        final Edge edge = Edge.newDCEL();

        this.faces.add(edge.getInteriorFace());
        this.faces.add(edge.getTwin().getInteriorFace());
    }

    /**
     * @return the list of faces in the DCEL.
     */
    public List<Face> getFaces() {
//        final List<Face> faces = new ArrayList<>();
//        final ArrayDeque<Face> queue = new ArrayDeque<>(this.faces);
//        while (!queue.isEmpty()) {
//            final Face f = queue.removeFirst();
//            if (faces.contains(f))
//                continue;
//            faces.add(f);
//            f.forEachEdge(e -> queue.addLast(e.getTwin().getInteriorFace()));
//        }
//        return faces;

        return new ArrayList<>(faces);
    }

    /**
     * Performs action on each face.
     *
     * @see {@link #getFaces()}
     *
     * @param action - the action to perform
     */
    public void forEachFace(final Consumer<Face> action) {
        this.faces.forEach(action);
    }

    /**
     * Subdivide the given edge at the given point.
     *
     * @see {@link Edge#subdivide(Point2D)}
     *
     * @param splitEdge  - the edge to subdivide
     * @param splitPoint - the location at which to subdivide the edge
     * @return the newly-added edge
     */
    public Edge subdivide(final Edge splitEdge, final Point2D splitPoint) {
        return splitEdge.subdivide(splitPoint);
    }

    /**
     * Collapses the given edge.
     *
     * @see {@link Edge#collapse()}
     *
     * @param edge - the edge to collapse
     * @return the joined vertex
     */
    public Vertex collapse(final Edge edge) {
        return edge.collapse();
    }

    /**
     * Dissolves the edge, joining the faces that it separates. May also dissolve
     * edges connected to the given edge.
     *
     * @param edge - the edge to dissolve
     * @return the joined face.
     */
    public Face dissolve(final Edge edge) {
        final Face interior = edge.getInteriorFace();
        final Face exterior = edge.getTwin().getInteriorFace();

        final Face joinedFace = edge.dissolve();
        this.faces.remove(joinedFace == interior ? exterior : interior);

        return joinedFace;
    }

    /**
     * Splits the face bounded by edge1 and edge2 by adding a new edge between the
     * given edges' origins.
     *
     * @param edge1 - an edge bounding the face to split
     * @param edge2 - another such edge
     * @return the newly-added edge
     */
    public Edge splitFaceBetween(final Edge edge1, final Edge edge2) {
        final Edge newEdge = Edge.splitFaceBetween(edge1, edge2);
        this.faces.add(newEdge.getTwin().getInteriorFace());
        return newEdge;
    }

    /**
     * Splits the face bounded by edge1 and edge2 by adding a new parabola between
     * the given edges' origins.
     *
     * @param focus - the focus of the parabola to add
     * @param edge1 - an edge bounding the face to split
     * @param edge2 - another such edge
     * @return the newly-added parabola
     */
    public ParabolaEdge splitFaceWithParabola(final Point2D focus, final Edge edge1, final Edge edge2) {
        final ParabolaEdge newEdge = ParabolaEdge.splitFaceBetween(focus, edge1, edge2);
        this.faces.add(newEdge.getTwin().getInteriorFace());
        return newEdge;
    }

    /**
     * Rips the vertex at the origin of the given edges into two vertices, adding an
     * edge between them.
     *
     * @param point      - the location of the new vertex
     * @param fixedEdge  - an edge with its origin at the vertex to rip
     * @param movingEdge - another edge with the same origin
     * @return the newly-added edge.
     */
    public Edge ripVertex(final Point2D point, final Edge fixedEdge, final Edge movingEdge) {
        return Edge.ripVertex(point, fixedEdge, movingEdge);
    }

}
