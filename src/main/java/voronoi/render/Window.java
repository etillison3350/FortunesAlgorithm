package voronoi.render;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javafx.animation.Transition;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

import voronoi.algorithm.CircleEvent;
import voronoi.algorithm.PointEvent;
import voronoi.algorithm.Voronoi;
import voronoi.algorithm.dcel.Edge;
import voronoi.algorithm.dcel.Face;
import voronoi.algorithm.dcel.ParabolaEdge;
import voronoi.algorithm.dcel.Vertex;
import voronoi.util.Line2D;
import voronoi.util.Util;

public class Window extends Application {

    private static final Random rand = new Random(11610);

    private static final boolean AUTO_MODE = true;
    private static final double ANIMATION_LENGTH = 20;
    private static final boolean REPEAT_ANIMATION = false;
    private static final boolean REGENERATE_POINTS = true;
    private static final boolean SHOW_HASHCODES = false;
    private static final boolean SHOW_POINTS = true;
    private static final boolean SHOW_POINT_LABELS = true;
    private static final boolean SHOW_CIRCLES = false;
    private static final boolean SHOW_MISSING_CIRCLES = false;
    private static final boolean SHOW_INSETS = true;
    private static final double INSET_DISTANCE = 8;
    private static final boolean JITTER_EDGES = false;
    private static final boolean COLOR_MODE = true;
    public static final boolean DO_CHECK = false;

    private static final int NUM_POINTS = 64;
    private static final int SCREEN_WIDTH = 1920;
    private static final int SCREEN_HEIGHT = 1000;

    private final Rectangle2D bounds;
    private Voronoi voronoi;
    private final List<Point2D> points;

    private Pane mainPanel;

    private Transition activeTransition = null;

    public Window() {
        final double leftSide = Math.min(100, SCREEN_WIDTH * 0.5);
        final double rightSide = Math.max(SCREEN_WIDTH - 100, SCREEN_WIDTH * 0.5);
        final double topSide = SCREEN_HEIGHT * 0.2;
        final double bottomSide = SCREEN_HEIGHT * 0.8;

        this.bounds = new Rectangle2D(leftSide, topSide, rightSide - leftSide, bottomSide - topSide);

        this.points = Stream.generate(
                () -> new Point2D(rand.nextDouble() * bounds.getWidth() + bounds.getMinX(),
                        rand.nextDouble() * bounds.getHeight() + bounds.getMinY()))
                .limit(NUM_POINTS)
                .collect(Collectors.toList());

        this.voronoi = new Voronoi(this.points, this.bounds);
        this.voronoi.dump(null);
    }

    @Override
    public void start(final Stage stage) throws Exception {
        mainPanel = new Pane();

        final Scene s = new Scene(mainPanel, SCREEN_WIDTH, SCREEN_HEIGHT);
        stage.setScene(s);

        stage.setX(0);
        stage.setY(0);
        stage.show();

        final GraphicsState initialGraphicsState = recreateShapes(bounds.getMaxY());

        if (AUTO_MODE) {
            final Transition trans = new FullVoronoiTransition(this, initialGraphicsState,
                    Duration.seconds(ANIMATION_LENGTH));
            trans.setDelay(Duration.seconds(1));
            trans.play();

            if (REPEAT_ANIMATION) {
                trans.setOnFinished(new EventHandler<ActionEvent>() {

                    @Override
                    public void handle(final ActionEvent event) {

                        if (REGENERATE_POINTS) {
                            Window.this.points.clear();
                            Stream.generate(
                                    () -> new Point2D(
                                            rand.nextDouble() * Window.this.bounds.getWidth()
                                                    + Window.this.bounds.getMinX(),
                                            rand.nextDouble() * Window.this.bounds.getHeight()
                                                    + Window.this.bounds.getMinY()))
                                    .limit(NUM_POINTS)
                                    .forEach(Window.this.points::add);
                        } else {
                            Window.this.points.add(new Point2D(
                                    rand.nextDouble() * Window.this.bounds.getWidth() + Window.this.bounds.getMinX(),
                                    rand.nextDouble() * Window.this.bounds.getHeight() + Window.this.bounds.getMinY()));
                        }

                        Window.this.voronoi = new Voronoi(points, Window.this.bounds);

                        final Transition t = new FullVoronoiTransition(Window.this, initialGraphicsState,
                                Duration.seconds(ANIMATION_LENGTH));
                        t.setDelay(Duration.seconds(1));
                        t.setOnFinished(this);
                        t.play();
                    }

                });
            }
        } else {
            mainPanel.setOnMouseClicked(mouseeevent -> {
                final PointEvent event = voronoi.step();

                if (event != null) {
                    final double eventY = event.point.getY();
                    final double nextY = voronoi.hasEvents() ? voronoi.nextEvent().point.getY() : eventY;

                    voronoi.dump(eventY);

                    final GraphicsState gs = recreateShapes(eventY);

                    final double duration = Math.min(2.4, Math.abs(nextY - eventY) / 120 + 0.2);

                    if (activeTransition != null)
                        activeTransition.stop();
                    activeTransition = new VoronoiTransition(this, gs, eventY, nextY, Duration.seconds(duration));
                    activeTransition.play();
                    activeTransition.setOnFinished(finished -> Window.this.activeTransition = null);
                }
            });
        }
    }

    public GraphicsState recreateShapes(final double sweepLineHeight) {
        final List<Node> childList = new ArrayList<>();

        final GraphicsState gs = new GraphicsState();
        if (voronoi.getTopBorder() != null) {
            final Line topLine = new Line();
            topLine.setStroke(Color.DARKGRAY);
            gs.setTopLine(topLine);
            childList.add(topLine);
        }

        for (final Face f : voronoi.getFaces()) {
            if (voronoi.isSpecialFace(f))
                continue;

            final Polygon poly = new Polygon();
            Color fill, stroke;
            if (COLOR_MODE) {
                final double hue = getHueOfPoint(f.getContainedPoint());
                final double brightness = hue < 0 ? 0 : 0.75;
                fill = Color.hsb(hue, 0.5, brightness, hue < 0 ? 0.05 : 0.2);
                stroke = Color.hsb(hue, 0.5, brightness);
                poly.setStroke(stroke);
            } else {
                fill = null;
                stroke = Color.BLACK;
                poly.setStroke(SHOW_INSETS ? null : stroke);
            }
            poly.setFill(fill);
            childList.add(poly);

            Polygon inset = null;
            if (SHOW_INSETS) {
                inset = new Polygon();
                inset.setFill(fill);
                inset.setStroke(stroke);
                childList.add(inset);
            }

            Text label = null;
            if (SHOW_HASHCODES) {
                label = new Text(String.format("%08x", f.hashCode()));
                label.setFill(stroke);
                childList.add(label);
            }

            gs.putFace(f, poly, inset, label);
        }

        final List<Edge> beachLineEdges = new ArrayList<>();

        if (voronoi.getTopBorder() != null) {
            Edge g = voronoi.getTopBorder().getNext().getNext();
            do {
                if (g.getPrevious() != voronoi.getTopBorder() && g != voronoi.getTopBorder()
                        && g.getNext() != voronoi.getTopBorder())
                    beachLineEdges.add(g);

                if (g instanceof ParabolaEdge) {
                    final Point2D focus = ((ParabolaEdge) g).focus;

                    final Polyline parabola = new Polyline();
                    final Color c = COLOR_MODE ? Color.hsb(getHueOfPoint(focus), 1, 0.75)
                            : Color.BLACK;
                    parabola.setStroke(c);
                    parabola.setStrokeWidth(2);
                    childList.add(parabola);

                    Text text = null;
                    if (SHOW_HASHCODES) {
                        text = new Text(String.format("%08x", g.hashCode()));
                        text.setFill(c);
                        text.setTextAlignment(TextAlignment.CENTER);
                        childList.add(text);
                    }

                    gs.putBeachLine(g, parabola, text);
                } else {
                    final Line line = new Line();
                    line.setFill(Color.DARKGRAY);
                    line.setStrokeWidth(2);
                    childList.add(line);

                    Text text = null;
                    if (SHOW_HASHCODES) {
                        text = new Text(String.format("%08x", g.hashCode()));
                        text.setFill(Color.DARKGRAY);
                        text.setTextAlignment(TextAlignment.CENTER);
                        childList.add(text);
                    }

                    gs.putBeachLine(g, line, text);
                }
                g = g.getNext();
            } while (g != voronoi.getTopBorder().getNext().getNext());
        }

        if (SHOW_POINTS) {
            for (final Point2D p : voronoi.getPoints()) {
                final boolean isNext = voronoi.hasEvents() && p == voronoi.nextEvent().point;
                final Color fill = COLOR_MODE ? Color.hsb(getHueOfPoint(p), 1, 0.75) : Color.BLACK;
                final Circle point = new Circle(p.getX(), p.getY(), isNext ? 4 : 3, fill);
                childList.add(point);

                Text label = null;
                if (SHOW_POINT_LABELS) {
                    label = new Text(String.format("(%.4g, %.4g)", p.getX(), p.getY()));
                    label.setFill(fill);
                    childList.add(label);
                }

                gs.putPoint(p, point, label);
            }
        }

        if (SHOW_CIRCLES) {
            final List<PointEvent> events = voronoi.getEvents();
            for (int i = events.size() - 1; i >= 0; i--) {
                final PointEvent p = events.get(i);
                if (p instanceof CircleEvent) {
                    final CircleEvent cevent = (CircleEvent) p;
                    final boolean isNext = p == voronoi.nextEvent();

                    beachLineEdges.remove(cevent.midEdge);

                    final Circle circle = new Circle();
                    circle.setFill(null);
                    circle.setStroke(COLOR_MODE ? Color.LIGHTGREEN : Color.BLACK);
                    circle.setStrokeWidth(isNext ? 2 : 1);
                    childList.add(circle);

                    final Circle eventPt = new Circle(isNext ? 4 : 2, COLOR_MODE ? Color.LIME : Color.BLACK);
                    childList.add(eventPt);

                    final Text label = new Text(String.format("%08x", cevent.hashCode()));
                    if (SHOW_HASHCODES)
                        childList.add(label);

                    final Circle center = new Circle(isNext ? 4 : 2,
                            COLOR_MODE ? isNext ? Color.DODGERBLUE : Color.BLUE : Color.BLACK);
                    childList.add(center);

                    gs.putCircleEvent(cevent, circle, eventPt, label, center);
                }
            }
        }

        if (SHOW_MISSING_CIRCLES) {
            for (final Edge edge : beachLineEdges) {
                if (CircleEvent.canGenerateEvent(edge)) {
                    final CircleEvent cevent = new CircleEvent(edge);

                    final Circle circle = new Circle();
                    circle.setFill(null);
                    circle.setStroke(COLOR_MODE ? Color.SALMON : Color.BLACK);
                    childList.add(circle);

                    final Circle eventPt = new Circle(2, COLOR_MODE ? Color.RED : Color.BLACK);
                    childList.add(eventPt);

                    final Text label = new Text(String.format("%08x", cevent.hashCode()));

                    final Circle center = new Circle(2, COLOR_MODE ? Color.MEDIUMPURPLE : Color.BLACK);
                    childList.add(center);

                    gs.putCircleEvent(cevent, circle, eventPt, label, center);
                }
            }
        }

        final Line sweepLine = new Line();
        gs.setSweepLine(sweepLine);
        childList.add(sweepLine);

        drawGraphicsState(gs, sweepLineHeight);

        this.mainPanel.getChildren().setAll(childList);

        return gs;
    }

    public void drawGraphicsState(final GraphicsState gs, final double sweepY) {
        final Map<Vertex, Point2D> actualPoints = new HashMap<>();
        final Map<ParabolaEdge, double[]> parEdges = new HashMap<>();

        if (gs.getTopLine() != null) {
            gs.getTopLine().setStartX(voronoi.getTopBorder().getOrigin().getX());
            gs.getTopLine().setStartY(voronoi.getTopBorder().getOrigin().getY());
            gs.getTopLine().setEndX(voronoi.getTopBorder().getNext().getOrigin().getX());
            gs.getTopLine().setEndY(voronoi.getTopBorder().getOrigin().getY());
        }

        if (!gs.getBeachLine().isEmpty()) {
            double minY = voronoi.getTopBorder().getOrigin().getY();
            for (final Edge edge : gs.getBeachLine().keySet()) {
                final Shape shape = gs.getBeachLine(edge);
                final Text text = gs.getBeachLineLabel(edge);

                if (edge instanceof ParabolaEdge) {
                    final Point2D focus = ((ParabolaEdge) edge).focus;

                    final double topY = Util.beachLineHeightForPoint(focus.getX(), focus, sweepY);
                    if (topY < minY)
                        minY = topY;

                    final double x0 = Util.beachLineIntersectionX(edge.getPrevious(), edge, sweepY);
                    final double x1 = Util.beachLineIntersectionX(edge, edge.getNext(), sweepY);

                    final double[] pts = new double[66];

                    for (int i = 0; i <= 32; i++) {
                        final double x = (x1 - x0) * i / 32 + x0;
                        final double y = Util.beachLineHeightForPoint(x, focus, sweepY);

                        pts[i * 2] = x;
                        pts[i * 2 + 1] = y;
                    }

                    actualPoints.put(edge.getOrigin(), new Point2D(pts[0], pts[1]));
                    parEdges.put((ParabolaEdge) edge, pts);

                    ((Polyline) shape).getPoints().clear();
                    DoubleStream.of(pts).boxed().forEach(((Polyline) shape).getPoints()::add);

                    if (text != null) {
                        text.setX(pts[32] - text.getLayoutBounds().getWidth() / 2);
                        text.setY(pts[33]);
                    }
                } else {
                    double x0, y0, x1, y1;
                    if (edge.isHorizontal()) {
                        x0 = Util.beachLineIntersectionX(edge.getPrevious(), edge, sweepY);
                        x1 = Util.beachLineIntersectionX(edge, edge.getNext(), sweepY);
                        y0 = y1 = edge == voronoi.getTopBorder() ? minY : edge.getOrigin().getY();

                        actualPoints.put(edge.getOrigin(), new Point2D(x0, y0));
                    } else {
                        x0 = x1 = edge.getOrigin().getX();
                        y0 = edge.getPrevious() == voronoi.getTopBorder() ? minY : edge.getOrigin().getY();
                        y1 = edge.getNext() == voronoi.getTopBorder() ? minY : edge.getNext().getOrigin().getY();

                        if (edge.getPrevious() instanceof ParabolaEdge) {
                            x0 = x1 = edge.getNext().getOrigin().getX();
                            y0 = Util.beachLineHeightForPoint(x0, ((ParabolaEdge) edge.getPrevious()).focus, sweepY);
                        } else if (edge.getNext() instanceof ParabolaEdge) {
                            y1 = Util.beachLineHeightForPoint(x0, ((ParabolaEdge) edge.getNext()).focus, sweepY);
                        }

                        actualPoints.put(edge.getOrigin(), new Point2D(x0, y0));
                    }

                    final Line line = (Line) shape;
                    line.setStartX(x0);
                    line.setStartY(y0);
                    line.setEndX(x1);
                    line.setEndY(y1);

                    if (text != null) {
                        text.setX(0.5 * (x0 + x1 - text.getLayoutBounds().getWidth()));
                        text.setY(0.5 * (y0 + y1));
                    }
                }
            }
        }

        for (final Face f : gs.getFaces().keySet()) {
            final Polygon poly = gs.getFacePolygon(f);

            final List<Double> pts = new ArrayList<>();

            f.forEachEdge(edge -> {
                if (parEdges.containsKey(edge)) {
                    Arrays.stream(parEdges.get(edge)).skip(2).forEach(pts::add);
                } else if (parEdges.containsKey(edge.getTwin())) {
                    final double[] p = parEdges.get(edge.getTwin());
                    for (int i = p.length - 2; i > 0; i -= 2) {
                        pts.add(p[i]);
                        pts.add(p[i + 1]);
                    }
                } else {
                    final Point2D point = actualPoints.getOrDefault(edge.getOrigin(), edge.getOrigin().getPoint());
                    pts.add(point.getX());
                    pts.add(point.getY());
                }
            });

            if (JITTER_EDGES) {
                final Random rand1 = new Random();
                final Random rand2 = new Random(f.hashCode());

                poly.getPoints().clear();
                final int n = pts.size();
                for (int i = 0; i < n; i += 2) {
                    final double x0 = pts.get((i - 2 + n) % n);
                    final double y0 = pts.get((i - 1 + n) % n);
                    final double x1 = pts.get(i);
                    final double y1 = pts.get(i + 1);
                    final double x2 = pts.get((i + 2) % n);
                    final double y2 = pts.get((i + 3) % n);

                    final double len = Math.hypot(x1 - x0, y1 - y0) + Math.hypot(x2 - x1, y2 - y1);

                    poly.getPoints()
                            .add(x1 + (rand1.nextDouble() - 0.5) * len / 100 + rand2.nextGaussian() * len / 100);
                    poly.getPoints()
                            .add(y1 + (rand1.nextDouble() - 0.5) * len / 100 + rand2.nextGaussian() * len / 100);
                }
            } else {
                poly.getPoints().setAll(pts);
            }

            final Polygon inset = gs.getFaceInset(f);
            if (inset != null) {
                final int numPts = pts.size() / 2;
                if (numPts < 2) {
                    inset.getPoints().clear();
                } else {
                    Line2DLLNode head = new Line2DLLNode();
                    head.data = new Line2D(pts.get(0), pts.get(1), pts.get(numPts * 2 - 2), pts.get(numPts * 2 - 1));
                    Line2DLLNode curr = head;
                    for (int i = 0; i < numPts - 1; i++) {
                        final Line2DLLNode next = new Line2DLLNode();
                        next.data = new Line2D(pts.get(i * 2 + 2), pts.get(i * 2 + 3), pts.get(i * 2),
                                pts.get(i * 2 + 1));
                        curr.next = next;
                        next.prev = curr;
                        curr = next;
                    }
                    curr.next = head;
                    head.prev = curr;

                    final PriorityQueue<EdgeCollapseEvent> pq = new PriorityQueue<>();
                    curr = head;
                    do {
                        pq.add(new EdgeCollapseEvent(curr));
                        curr = curr.next;
                    } while (curr != head);

                    if (f.getContainedPoint() != null && (int) f.getContainedPoint().getX() == 651) {
                        for (final EdgeCollapseEvent ece : pq) {
                            System.err.println(ece.radius);
                        }
                        System.err.println();
                    }

                    while (head.next.next != head) {
                        final EdgeCollapseEvent event = pq.poll();
                        if (event.radius > INSET_DISTANCE)
                            break;

                        if (event.mid.prev != event.prev || event.mid.next != event.next || event.prev.next != event.mid
                                || event.next.prev != event.mid)
                            continue;

                        event.prev.next = event.next;
                        event.next.prev = event.prev;
                        pq.add(new EdgeCollapseEvent(event.prev));
                        pq.add(new EdgeCollapseEvent(event.next));

                        if (head == event.mid)
                            head = event.next;
                    }

                    inset.getPoints().clear();
                    if (head.next.next != head) {
                        curr = head;
                        do {
                            final Point2D intersect = Util.offsetIntersection(curr.data,
                                    curr.next.data, INSET_DISTANCE);

                            if (f.getContainedPoint() != null && (int) f.getContainedPoint().getX() == 651) {
                                for (int i = 0; i < numPts; i++) {
                                    if (Util.distanceOffLine(new Line2D(pts.get(i * 2), pts.get(i * 2 + 1),
                                            pts.get(i == numPts - 1 ? 0 : i * 2 + 2),
                                            pts.get(i == numPts - 1 ? 1 : i * 2 + 3)), intersect) > 0) {
                                        System.err.println(pts);
                                        Line2DLLNode ll = head;
                                        do {
                                            System.err.print(ll.data);
                                            System.err.println(", ");
                                            ll = ll.next;
                                        } while (ll != head);
                                        System.err.println();
                                    }
                                }
                            }

                            inset.getPoints().addAll(intersect.getX(), intersect.getY());
                            curr = curr.next;
                        } while (curr != head);
                    }
                }
            }

            final Text label = gs.getFaceLabel(f);
            if (label != null) {
                // Convert the pts list into an array, with the first point also added at the
                // end to remove the need for the modulo operator.
                // We sacrifice a small amount of performance here for readability
                final double[] ptArr = DoubleStream
                        .concat(pts.stream().mapToDouble(d -> d), DoubleStream.of(pts.get(0), pts.get(1))).toArray();

                // Formula here: https://en.wikipedia.org/wiki/Centroid (accessed 21 Dec. 2020)
                final double area = 0.5 * IntStream.range(0, pts.size() / 2)
                        .mapToDouble(i -> ptArr[i * 2] * ptArr[i * 2 + 3] - ptArr[i * 2 + 2] * ptArr[i * 2 + 1])
                        .sum();

                final double centroidX = 1 / (6 * area) * IntStream.range(0, pts.size() / 2)
                        .mapToDouble(i -> (ptArr[i * 2] + ptArr[i * 2 + 2])
                                * (ptArr[i * 2] * ptArr[i * 2 + 3] - ptArr[i * 2 + 2] * ptArr[i * 2 + 1]))
                        .sum();
                final double centroidY = 1 / (6 * area) * IntStream.range(0, pts.size() / 2)
                        .mapToDouble(i -> (ptArr[i * 2 + 1] + ptArr[i * 2 + 3])
                                * (ptArr[i * 2] * ptArr[i * 2 + 3] - ptArr[i * 2 + 2] * ptArr[i * 2 + 1]))
                        .sum();

                label.setX(centroidX - label.getLayoutBounds().getWidth() / 2);
                label.setY(centroidY);
            }
        }

        for (final Point2D p : gs.getPoints().keySet()) {
            final Circle point = gs.getPoint(p);
            point.setCenterX(p.getX());
            point.setCenterY(p.getY());

            final Text label = gs.getPointLabel(p);
            if (label != null) {
                label.setX(p.getX() + 4);
                label.setY(p.getY() - 4);
            }
        }

        for (final CircleEvent cevent : gs.getCircleEvents().keySet()) {
            final Circle circle = gs.getCircleEventCircle(cevent);
            circle.setCenterX(cevent.center.getX());
            circle.setCenterY(cevent.center.getY());
            circle.setRadius(cevent.radius);

            final Circle point = gs.getCircleEventPoint(cevent);
            point.setCenterX(cevent.point.getX());
            point.setCenterY(cevent.point.getY());

            final Text label = gs.getCircleEventLabel(cevent);
            label.setX(cevent.point.getX() - label.getLayoutBounds().getWidth() / 2);
            label.setY(cevent.point.getY());

            final Circle center = gs.getCircleEventCenter(cevent);
            center.setCenterX(cevent.center.getX());
            center.setCenterY(cevent.center.getY());
        }

        gs.getSweepLine().setStartX(0);
        gs.getSweepLine().setStartY(sweepY);
        gs.getSweepLine().setEndX(SCREEN_WIDTH);
        gs.getSweepLine().setEndY(sweepY);
    }

    public double getHueOfPoint(final Point2D point) {
        if (!points.contains(point))
            return -1;

        return 360. * ((System.identityHashCode(point) & 0xFFFFFF) / (double) 0xFFFFFF);
    }

    public Voronoi getVoronoi() {
        return this.voronoi;
    }

    public Rectangle2D getBounds() {
        return bounds;
    }

    private static class Line2DLLNode {
        Line2DLLNode prev, next;
        Line2D data;
    }

    private static class EdgeCollapseEvent implements Comparable<EdgeCollapseEvent> {
        final double radius;

        final Line2DLLNode prev, mid, next;

        EdgeCollapseEvent(final Line2DLLNode mid) {
            this.mid = mid;
            this.prev = mid.prev;
            this.next = mid.next;

            final double radius = Util.circleTangentToLines(prev.data, mid.data, next.data).getRadius();
            this.radius = Double.isFinite(radius) ? radius : -1;
        }

        @Override
        public int compareTo(final EdgeCollapseEvent o) {
            return Double.compare(radius, o.radius);
        }
    }

}
