package voronoi.algorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;

import voronoi.algorithm.dcel.Edge;
import voronoi.algorithm.dcel.Face;
import voronoi.algorithm.dcel.ParabolaEdge;
import voronoi.util.Util;

public class Voronoi {

    private final List<Point2D> points;

    private final Rectangle2D bounds;

    private final DCELFacesWrapper dcel;
    private Edge topBorder;

    private final PriorityQueue<PointEvent> events;

    private final Face beachLine, infiniteFace;

    private final Edge leftBorder;
    private final Edge rightBorder;

    private final Map<Face, Edge> topPoints;

    public Voronoi(final Collection<Point2D> points, final Rectangle2D bounds) {
        this.points = new ArrayList<>(points);
        this.bounds = bounds;

        this.dcel = new DCELFacesWrapper();
        this.topBorder = dcel.getFaces().get(0).getBoundaryEdge();

        topBorder.getOrigin().setPoint(new Point2D(this.bounds.getMinX(), this.bounds.getMinY()));
        this.rightBorder = dcel.subdivide(topBorder, new Point2D(this.bounds.getMaxX(), this.bounds.getMinY()));
        final Edge bottomBorder = dcel.subdivide(rightBorder,
                new Point2D(this.bounds.getMaxX(), this.bounds.getMaxY()));
        this.leftBorder = dcel.subdivide(bottomBorder, new Point2D(this.bounds.getMinX(), this.bounds.getMaxY()));

        topBorder.setHorizontal(true);
        bottomBorder.setHorizontal(true);

        this.beachLine = topBorder.getInteriorFace();
        this.infiniteFace = topBorder.getTwin().getInteriorFace();

        this.events = points.stream().map(PointEvent::new).collect(Collectors.toCollection(PriorityQueue::new));

        this.topPoints = new HashMap<>();
    }

    public synchronized PointEvent step() {
        if (!events.isEmpty()) {
            rightBorder.check();
            final PointEvent event = events.poll();
            System.out.println(event + " at " + event.point);
            if (event instanceof CircleEvent) {
                handleCircleEvent((CircleEvent) event);
            } else {
                handlePointEvent(event);
            }

            return event;
        }

        return null;
    }

    private void handlePointEvent(final PointEvent event) {
        for (Edge edge = topBorder.getNext(); edge != topBorder; edge = edge.getNext()) {
            if (Util.beachLineIntersectionX(edge, edge.getNext(), event.point.getY()) < event.point.getX()) {
                final Edge ed = edge;
                events.removeIf(pe -> pe instanceof CircleEvent && ((CircleEvent) pe).midEdge == ed);

                final Edge next = edge.subdivide(edge.getOrigin().getPoint());
                final Edge mid = edge.subdivide(edge.getOrigin().getPoint());

                final ParabolaEdge par = dcel.splitFaceWithParabola(event.point, next, mid);
                par.getTwin().getInteriorFace().setContainedPoint(event.point);

                if (mid instanceof ParabolaEdge)
                    ((ParabolaEdge) mid).convertToNonParEdge().check();

                par.check();
                par.getTwin().check();

                par.getOrigin().check();
                par.getTwin().getOrigin().check();

                if (CircleEvent.canGenerateEvent(par.getPrevious())) {
                    final CircleEvent newEvent = new CircleEvent(par.getPrevious());
                    if (isValidEvent(event, newEvent)) {
                        events.add(newEvent);
                        System.out.println("ADD PP");
                    }
                }
                if (CircleEvent.canGenerateEvent(par.getNext())) {
                    final CircleEvent newEvent = new CircleEvent(par.getNext());
                    if (isValidEvent(event, newEvent)) {
                        events.add(newEvent);
                        System.out.println("ADD PN");
                    }
                }
                System.out.println();

                break;
            }
        }
    }

    private void handleCircleEvent(final CircleEvent cevent) {
        System.out.printf("Center: %s\n", cevent.center);

        events.removeIf(pe -> pe instanceof CircleEvent
                && (((CircleEvent) pe).midEdge.getPrevious() == cevent.midEdge
                        || ((CircleEvent) pe).midEdge.getNext() == cevent.midEdge));

        final Edge prev = cevent.midEdge.getPrevious();
        final Edge next = cevent.midEdge.getNext();
        final Point2D nextOrigin = next.getOrigin().getPoint();

        final Edge twinNext = cevent.midEdge.getTwin().getNext();
        final Edge twinPrev = cevent.midEdge.getTwin().getPrevious();

        System.out.printf("%08x, %08x\n", cevent.midEdge.getInteriorFace().hashCode(), beachLine.hashCode());

        System.out.println("RE " + cevent.midEdge.getOrigin().getDegree());
        System.out.println("RE " + cevent.midEdge.getTwin().getOrigin().getDegree());

        final Edge fixedEdge = cevent.midEdge.getPrevious().getTwin();
        Edge movingEdge = cevent.midEdge.getNext().getTwin().getNext();
        if (movingEdge == cevent.midEdge.getTwin())
            movingEdge = movingEdge.getNext();

        dcel.collapse(cevent.midEdge);
        dcel.ripVertex(cevent.center, fixedEdge, movingEdge);

        if (!(next instanceof ParabolaEdge))
            fixedEdge.getOrigin().setPoint(nextOrigin);

        if (prev != leftBorder && prev != rightBorder && CircleEvent.canGenerateEvent(prev)) {
            final CircleEvent newEvent = new CircleEvent(prev);
            if (isValidEvent(cevent, newEvent)) {
                events.add(newEvent);
                System.out.println("ADD A");
            }
        }
        if (next != leftBorder && next != rightBorder && CircleEvent.canGenerateEvent(next)) {
            final CircleEvent newEvent = new CircleEvent(next);
            if (isValidEvent(cevent, newEvent)) {
                events.add(newEvent);
                System.out.println("ADD C");
            }
        }
        System.out.println();

        final double topY = topBorder.getOrigin().getY();
        if (cevent.center.getY() < topY) {
            System.out.println("SPLIT TOP:");
            System.out.println(twinPrev);
            System.out.println(twinNext);
            System.out.println();

            splitTop(twinPrev, false);
            splitTop(twinNext, true);

            System.out.println("Join faces:");
            dump(null);
            twinNext.getInteriorFace().setContainedPoint(null);
            Edge edge = twinNext;
            do {
                if (edge.getTwin().getInteriorFace() != topBorder.getTwin().getInteriorFace()
                        && edge.getTwin().getInteriorFace().getContainedPoint() == null) {
                    dcel.dissolve(edge);
                    while (edge.getInteriorFace() == null)
                        edge = edge.getNext();
                } else {
                    edge = edge.getNext();
                }
            } while (edge != twinNext);
        }
        System.out.println("Top Points");
        topPoints.forEach((face, edge) -> System.out.printf("\t%08x: %s\n", face.hashCode(), edge));
        System.out.println();

        if (events.isEmpty()) {
            finish(cevent);
        }
    }

    private void finish(final CircleEvent lastEvent) {
        System.out.println("No events!");
        final Edge lastPar = topBorder.getNext().getNext();

        lastPar.getOrigin().setPoint(new Point2D(rightBorder.getOrigin().getX(), lastEvent.point.getY()));
        lastPar.getTwin().getOrigin().setPoint(new Point2D(leftBorder.getOrigin().getX(), lastEvent.point.getY()));

        splitTop(lastPar.getTwin().getNext(), true);
        splitTop(lastPar.getTwin().getPrevious(), false);
        lastPar.getTwin().getInteriorFace().setContainedPoint(null);

        dcel.dissolve(topBorder);
        topBorder = null;
        Face extraFace = dcel.dissolve(lastPar);
        Edge startEdge = extraFace.getBoundaryEdge();
        Edge edge = startEdge;
        while (edge != startEdge.getPrevious()) {
            if (edge.getTwin().getInteriorFace().getContainedPoint() == null) {
                extraFace = dcel.dissolve(edge);
                edge = startEdge = extraFace.getBoundaryEdge();
            } else {
                edge = edge.getNext();
            }
            edge.check();
        }

//        dump(lastEvent.point.getY());
    }

    private boolean splitTop(final Edge e, final boolean right) {
        System.out.printf("Split %s %s\n", right ? "RIGHT" : "LEFT", e);

        if (e.getOrigin().getY() > topBorder.getOrigin().getY() //
                == e.getNext().getOrigin().getY() > topBorder.getOrigin().getY())
            return false;

        final Face leftFace = (right ? e : e.getTwin()).getInteriorFace();
        final Face rightFace = (right ? e.getTwin() : e).getInteriorFace();

        final Edge e2 = e.subdivide(Util.intersectLineHorizontal(e.getOrigin().getPoint(),
                e.getNext().getOrigin().getPoint(),
                topBorder.getOrigin().getY()));

        if (topPoints.containsKey(leftFace) && leftFace != topBorder.getTwin().getInteriorFace()) {
            System.out.printf("Break %08x\n", leftFace.hashCode());
            final Edge o1 = topPoints.remove(leftFace);
            final Edge topEdge = dcel.splitFaceBetween(o1, right ? e2 : e.getTwin());

            topEdge.getTwin().getInteriorFace().setContainedPoint(topEdge.getInteriorFace().getContainedPoint());
        } else {
            topPoints.put(leftFace, right ? e2 : e.getTwin());
        }

        if (topPoints.containsKey(rightFace) && rightFace != topBorder.getTwin().getInteriorFace()) {
            System.out.printf("Break %08x\n", rightFace.hashCode());
            final Edge o1 = right ? e.getTwin() : e2;
            final Edge o2 = topPoints.remove(rightFace).getPrevious();
            final Edge topEdge = dcel.splitFaceBetween(o1, o2.getNext());

            topEdge.getTwin().getInteriorFace().setContainedPoint(topEdge.getInteriorFace().getContainedPoint());
        } else {
            topPoints.put(rightFace, right ? e.getTwin() : e2);
        }

        return true;
    }

    private boolean isValidEvent(final PointEvent generatingEvent, final CircleEvent newEvent) {
        final boolean prevPar = newEvent.midEdge.getPrevious() instanceof ParabolaEdge;
        final boolean midPar = newEvent.midEdge instanceof ParabolaEdge;
        final boolean nextPar = newEvent.midEdge.getNext() instanceof ParabolaEdge;
        if (!prevPar && !nextPar) {
            return false;
        } else if (midPar && (prevPar || nextPar)) {
            System.out.printf("%08x %08x %08x\n", newEvent.midEdge.getPrevious().hashCode(),
                    newEvent.midEdge.hashCode(), newEvent.midEdge.getNext().hashCode());

            final Point2D pp, pn, pm = ((ParabolaEdge) newEvent.midEdge).focus;
            if (prevPar)
                pp = ((ParabolaEdge) newEvent.midEdge.getPrevious()).focus;
            else if (newEvent.midEdge.getPrevious().isHorizontal())
                pp = newEvent.center.add(newEvent.radius, 0);
            else
                pp = newEvent.center.add(0, newEvent.radius);

            if (nextPar)
                pn = ((ParabolaEdge) newEvent.midEdge.getNext()).focus;
            else if (newEvent.midEdge.getNext().isHorizontal())
                pn = newEvent.center.subtract(newEvent.radius, 0);
            else
                pn = newEvent.center.add(0, newEvent.radius);

            return (pp.getY() - pn.getY()) * (pm.getX() - pp.getX())
                    - (pp.getX() - pn.getX()) * (pm.getY() - pp.getY()) <= 0;
        } else {
            return true;
        }
    }

    /**
     * Prints diagnostic information about this Voronoi instance to stdout, given
     * that the sweep line is currently at the given height (may be null; if it is,
     * the information that depends on it is not printed).
     *
     * @param sweepLineHeight - the height of the sweep line to use for calculating
     *                        some information, or null to leave this information
     *                        out of the output.
     */
    public void dump(final Double sweepLineHeight) {
        if (topBorder != null) {
            Edge ed = topBorder;
            do {
                System.out.print("\t");
                if (ed == topBorder)
                    System.out.print("(topBorder) ");
                else if (ed == leftBorder)
                    System.out.print("(leftBorder) ");
                else if (ed == rightBorder)
                    System.out.print("(rightBorder) ");
                System.out.println(ed);
                System.out.println("\t  @" + ed.getOrigin().getPoint());
                if (sweepLineHeight != null && ed != leftBorder)
                    System.out.println("\t  X=" + Util.beachLineIntersectionX(ed, ed.getNext(), sweepLineHeight));
                ed = ed.getNext();
            } while (ed != topBorder);
        }

        System.out.println();
        for (final PointEvent p : events) {
            if (p instanceof CircleEvent) {
                final CircleEvent c = (CircleEvent) p;

                System.out.println("\t" + p);
                System.out.println("\t\t" + c.midEdge.getPrevious());
                System.out.println("\t\t" + c.midEdge);
                System.out.println("\t\t" + c.midEdge.getNext());
                System.out.println();
            }
        }

        for (final Face f : dcel.getFaces()) {
            System.out.printf("\tFace %08x", f.hashCode());
            if (f == beachLine)
                System.out.println(" (beachLine)");
            else if (topBorder != null && f == topBorder.getTwin().getInteriorFace())
                System.out.println(" (outside)");
            else
                System.out.println();

            System.out.printf("\t\tPoint = (index %d) %s\n", getPoints().indexOf(f.getContainedPoint()),
                    f.getContainedPoint());

            final StringBuilder edges = new StringBuilder();
            f.forEachEdge(edge -> {
//                edges.append(String.format("%08x ", edge.hashCode()));
                edges.append("\t\t\t");
                edges.append(edge);
                edges.append("\n");
                edge = edge.getNext();
            });
//            System.out.printf("\t\tEdges = [ %s]\n", edges);
            System.out.printf("\t\tEdges = [\n%s\t\t]\n", edges);
        }
    }

    /**
     * @return true if there are more events that this Voronoi instance will
     *         process; false otherwise
     */
    public boolean hasEvents() {
        return !events.isEmpty();
    }

    /**
     * @return the next event that this Voronoi instance will process, or null if
     *         there are no more events to process
     */
    public PointEvent nextEvent() {
        return events.peek();
    }

    /**
     * @return the top border of this Voronoi instance (this may be null)
     */
    public Edge getTopBorder() {
        return topBorder;
    }

    /**
     * @return a list of the faces currently tracked by this Voronoi instance
     */
    public List<Face> getFaces() {
        return this.dcel.getFaces();
    }

    /**
     * Is the given face a special face, i.e. not related to any point (the special
     * faces are the beach line face, and the infinite face).
     *
     * @param f - the face to check
     * @return true if the face is special, false otherwise
     */
    public boolean isSpecialFace(final Face f) {
        return f == this.beachLine || f == this.infiniteFace;
    }

    /**
     * @return the voronoi points of this Voronoi instance
     */
    public List<Point2D> getPoints() {
        return new ArrayList<>(points);
    }

    /**
     * @return the list of events that are currently queued
     */
    public List<PointEvent> getEvents() {
        return new ArrayList<>(events);
    }

}
