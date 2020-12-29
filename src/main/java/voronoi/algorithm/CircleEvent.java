package voronoi.algorithm;

import javafx.geometry.Point2D;

import voronoi.algorithm.dcel.Edge;
import voronoi.algorithm.dcel.ParabolaEdge;
import voronoi.util.Util;

public class CircleEvent extends PointEvent {

    public final Point2D center;
    public final double radius;

    public final Edge midEdge;

    public CircleEvent(final Edge middleEdge) {
        int numPar = 0;
        final Point2D[] foci = new Point2D[3];
        Edge nonParEdge = null;
        if (middleEdge.getPrevious() instanceof ParabolaEdge)
            foci[numPar++] = ((ParabolaEdge) middleEdge.getPrevious()).focus;
        else
            nonParEdge = middleEdge.getPrevious();

        if (middleEdge instanceof ParabolaEdge)
            foci[numPar++] = ((ParabolaEdge) middleEdge).focus;
        else
            nonParEdge = middleEdge;

        if (middleEdge.getNext() instanceof ParabolaEdge)
            foci[numPar++] = ((ParabolaEdge) middleEdge.getNext()).focus;
        else
            nonParEdge = middleEdge.getNext();

        if (numPar == 1) {
            this.center = nonParEdge.getOrigin().getPoint();
        } else if (numPar == 2 && nonParEdge.isHorizontal()) {
            this.center = new Point2D(Util.circleCenterX(foci[0], foci[1], nonParEdge.getOrigin().getY()),
                    nonParEdge.getOrigin().getY());
        } else if (numPar == 2) {
            this.center = new Point2D(nonParEdge.getOrigin().getX(),
                    Util.circleCenterY(foci[0], foci[1], nonParEdge.getOrigin().getX()));
        } else {
            this.center = Util.circleCenter(foci[0], foci[1], foci[2]);
        }
        this.radius = this.center.distance(foci[0]);
        this.point = this.center.subtract(0, radius);

        this.midEdge = middleEdge;
    }

    public static boolean canGenerateEvent(final Edge midEdge) {
        final boolean prev = midEdge.getPrevious() instanceof ParabolaEdge;
        final boolean next = midEdge.getNext() instanceof ParabolaEdge;

        if (!prev && !next)
            return false;
        if (prev && next && ((ParabolaEdge) midEdge.getPrevious()).focus == ((ParabolaEdge) midEdge.getNext()).focus)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (midEdge == null ? 0 : midEdge.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        final CircleEvent other = (CircleEvent) obj;
        if (midEdge == null) {
            if (other.midEdge != null)
                return false;
        } else if (!midEdge.equals(other.midEdge))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(this.getClass().getSimpleName());
        builder.append("@");
        builder.append(String.format("%08x", this.hashCode()));
        builder.append(" [point=");
        builder.append(point);
        builder.append(", edges=(");
        builder.append(String.format("%08x", midEdge.getPrevious().hashCode()));
        builder.append(", ");
        builder.append(String.format("%08x", midEdge.hashCode()));
        builder.append(", ");
        builder.append(String.format("%08x", midEdge.getNext().hashCode()));
        builder.append(")]");
        return builder.toString();
    }

}