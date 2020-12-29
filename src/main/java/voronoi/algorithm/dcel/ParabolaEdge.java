package voronoi.algorithm.dcel;

import javafx.geometry.Point2D;

/**
 * A parabolic edge.
 */
public class ParabolaEdge extends Edge {
    /**
     * The focus of the parabola
     */
    public Point2D focus;

    ParabolaEdge(final Point2D focus) {
        this.focus = focus;
    }

//    @Override
//    public ParabolaEdge split(final Vertex point) {
//        final ParabolaEdge other = new ParabolaEdge(this.focus);
//
//        other.origin = this.origin;
//        other.origin.incidentEdge = other;
//        this.origin = other.twin.origin = point;
//        point.incidentEdge = this;
//
//        other.previous = this.previous;
//        this.previous.next = other;
//        other.twin.next = this.twin.next;
//        this.twin.next.previous = other.twin;
//        other.next = this;
//        this.previous = other;
//        other.twin.previous = this.twin;
//        this.twin.next = other.twin;
//
//        other.interiorFace = this.interiorFace;
//        other.twin.interiorFace = this.twin.interiorFace;
//
//        this.check();
//        this.twin.check();
//        this.previous.check();
//        this.previous.twin.check();
//
//        return other;
//    }

    @Override
    public ParabolaEdge subdivide(final Point2D point) {
        final ParabolaEdge other = new ParabolaEdge(this.focus);
        other.setTwin(new ParabolaEdge(this.focus));

        other.getTwin().setOrigin(this.getTwin().getOrigin());

        final Vertex origin = new Vertex(point);
        this.getTwin().setOrigin(origin);
        other.setOrigin(origin);

        this.insertSuccessor(other);
        this.getTwin().insertPredecessor(other.getTwin());

        other.setInteriorFace(this.getInteriorFace());
        other.getTwin().setInteriorFace(this.getTwin().getInteriorFace());

        this.check();
        this.getTwin().check();
        this.getPrevious().check();
        this.getPrevious().getTwin().check();

        return other;
    }

    public static ParabolaEdge splitFaceBetween(final Point2D focus, final Edge edge1, final Edge edge2) {
        if (edge1.getInteriorFace() != edge2.getInteriorFace())
            throw new IllegalArgumentException("Cannot split face between edges bounding different faces");

        final ParabolaEdge splitEdge = new ParabolaEdge(focus);
        splitEdge.setTwin(new ParabolaEdge(focus));

        splitEdge.getTwin().setOrigin(edge1.getOrigin());
        splitEdge.setOrigin(edge2.getOrigin());

        edge1.getPrevious().setNext(splitEdge.getTwin());
        edge2.getPrevious().setNext(splitEdge);
        edge1.setPrevious(splitEdge);
        edge2.setPrevious(splitEdge.getTwin());

        splitEdge.setInteriorFace(edge1.getInteriorFace());

        final Face newFace = new Face();
        splitEdge.getTwin().forEachEdgeInFace(e -> e.setInteriorFace(newFace));

        return splitEdge;
    }

    @Override
    public ParabolaEdge getTwin() {
        return (ParabolaEdge) super.getTwin();
    }

    void setTwin(final ParabolaEdge twin) {
        super.setTwin(twin);
    }

    /**
     * Converts this parabola into a straight-line edge. This edge is removed from
     * the DCEL, and a new edge is inserted in its place.
     *
     * @return the new edge
     */
    public Edge convertToNonParEdge() {
        final Edge newEdge = new Edge();
        newEdge.setTwin(new Edge());

        newEdge.setNext(this.getNext());
        newEdge.setPrevious(this.getPrevious());
        newEdge.getTwin().setNext(this.getTwin().getNext());
        newEdge.getTwin().setPrevious(this.getTwin().getPrevious());

        newEdge.setOrigin(this.getOrigin());
        newEdge.getTwin().setOrigin(this.getTwin().getOrigin());

        newEdge.setInteriorFace(this.getInteriorFace());
        newEdge.getTwin().setInteriorFace(this.getTwin().getInteriorFace());

        newEdge.getOrigin().check();
        newEdge.getTwin().getOrigin().check();
        newEdge.check();
        newEdge.getTwin().check();
        newEdge.getInteriorFace().check();
        newEdge.getTwin().getInteriorFace().check();

        return newEdge;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(this.getClass().getSimpleName());
        builder.append("@");
        builder.append(String.format("%08x", this.hashCode()));
        if (this.isHorizontal())
            builder.append(" H");
        builder.append(" [focus=");
        builder.append(focus);
        builder.append(", origin=");
        builder.append(getOrigin() == null ? "- null -" : String.format("%08x", getOrigin().hashCode()));
        builder.append(", twin=");
        builder.append(getTwin() == null ? "- null -" : String.format("%08x", getTwin().hashCode()));
        builder.append(", next=");
        builder.append(getNext() == null ? "- null -" : String.format("%08x", getNext().hashCode()));
        builder.append(", previous=");
        builder.append(getPrevious() == null ? "- null -" : String.format("%08x", getPrevious().hashCode()));
        builder.append(", interiorFace=");
        builder.append(getInteriorFace() == null ? "- null -" : String.format("%08x", getInteriorFace().hashCode()));
        builder.append("]");
        return builder.toString();
    }
}