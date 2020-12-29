package voronoi.algorithm;
import javafx.geometry.Point2D;

public class PointEvent implements Comparable<PointEvent> {
    public Point2D point;

    protected PointEvent() {}

    public PointEvent(final Point2D point) {
        this.point = point;
    }

    @Override
    public int compareTo(final PointEvent o) {
        return Double.compare(o.point.getY(), this.point.getY());
    }

    @Override
    public int hashCode() {
        return 31 + (point == null ? 0 : point.hashCode());
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final PointEvent other = (PointEvent) obj;
        if (point == null) {
            if (other.point != null)
                return false;
        } else if (!point.equals(other.point))
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
        builder.append("]");
        return builder.toString();
    }

}