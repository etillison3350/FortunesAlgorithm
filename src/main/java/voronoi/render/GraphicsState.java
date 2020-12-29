package voronoi.render;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javafx.geometry.Point2D;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;

import voronoi.algorithm.CircleEvent;
import voronoi.algorithm.dcel.Edge;
import voronoi.algorithm.dcel.Face;

class GraphicsState {
    private Line topLine;
    private final Map<Edge, BeachLineGraphics> beachLine = new LinkedHashMap<>();
    private final Map<Face, FaceGraphics> faces = new HashMap<>();
    private final Map<Point2D, PointGraphics> points = new HashMap<>();
    private final Map<CircleEvent, EventGraphics> circleEvents = new HashMap<>();
    private Line sweepLine;

    public Line getTopLine() {
        return topLine;
    }

    public void setTopLine(final Line topLine) {
        this.topLine = topLine;
    }

    public Line getSweepLine() {
        return sweepLine;
    }

    public void setSweepLine(final Line sweepLine) {
        this.sweepLine = sweepLine;
    }

    public Map<Edge, BeachLineGraphics> getBeachLine() {
        return beachLine;
    }

    public void putBeachLine(final Edge edge, final Shape line, final Text label) {
        beachLine.put(edge, new BeachLineGraphics(line, label));
    }

    public Shape getBeachLine(final Edge edge) {
        return beachLine.get(edge).line;
    }

    public Text getBeachLineLabel(final Edge edge) {
        return beachLine.get(edge).label;
    }

    public Map<Face, FaceGraphics> getFaces() {
        return faces;
    }

    public void putFace(final Face face, final Polygon poly, final Polygon inset, final Text label) {
        faces.put(face, new FaceGraphics(poly, inset, label));
    }

    public Polygon getFacePolygon(final Face face) {
        return faces.get(face).face;
    }

    public Polygon getFaceInset(final Face face) {
        return faces.get(face).inset;
    }

    public Text getFaceLabel(final Face face) {
        return faces.get(face).label;
    }

    public Map<Point2D, PointGraphics> getPoints() {
        return points;
    }

    public void putPoint(final Point2D point, final Circle circle, final Text label) {
        points.put(point, new PointGraphics(circle, label));
    }

    public Circle getPoint(final Point2D point) {
        return points.get(point).point;
    }

    public Text getPointLabel(final Point2D point) {
        return points.get(point).label;
    }

    public Map<CircleEvent, EventGraphics> getCircleEvents() {
        return circleEvents;
    }

    public void putCircleEvent(final CircleEvent event, final Circle circle, final Circle eventPoint,
            final Text eventLabel, final Circle eventCenter) {
        circleEvents.put(event, new EventGraphics(circle, eventPoint, eventLabel, eventCenter));
    }

    public Circle getCircleEventCircle(final CircleEvent event) {
        return circleEvents.get(event).circle;
    }

    public Circle getCircleEventPoint(final CircleEvent event) {
        return circleEvents.get(event).eventPoint;
    }

    public Text getCircleEventLabel(final CircleEvent event) {
        return circleEvents.get(event).eventLabel;
    }

    public Circle getCircleEventCenter(final CircleEvent event) {
        return circleEvents.get(event).eventCenter;
    }

    static class BeachLineGraphics {
        private final Shape line;
        private final Text label;

        BeachLineGraphics(final Shape line, final Text label) {
            this.line = line;
            this.label = label;
        }

        public Shape getLine() {
            return line;
        }

        public Text getLabel() {
            return label;
        }
    }

    static class FaceGraphics {
        private final Polygon face;
        private final Polygon inset;
        private final Text label;

        FaceGraphics(final Polygon face, final Polygon inset, final Text label) {
            this.face = face;
            this.inset = inset;
            this.label = label;
        }

        public Polygon getFace() {
            return face;
        }

        public Polygon getInset() {
            return inset;
        }

        public Text getLabel() {
            return label;
        }
    }

    static class PointGraphics {
        private final Circle point;
        private final Text label;

        PointGraphics(final Circle point, final Text label) {
            this.point = point;
            this.label = label;
        }

        public Circle getPoint() {
            return point;
        }

        public Text getLabel() {
            return label;
        }
    }

    static class EventGraphics {
        private final Circle circle;
        private final Circle eventPoint;
        private final Text eventLabel;
        private final Circle eventCenter;

        EventGraphics(final Circle circle, final Circle eventPoint, final Text eventLabel, final Circle eventCenter) {
            this.circle = circle;
            this.eventPoint = eventPoint;
            this.eventLabel = eventLabel;
            this.eventCenter = eventCenter;
        }

        public Circle getCircle() {
            return circle;
        }

        public Circle getEventPoint() {
            return eventPoint;
        }

        public Text getEventLabel() {
            return eventLabel;
        }

        public Circle getEventCenter() {
            return eventCenter;
        }
    }
}