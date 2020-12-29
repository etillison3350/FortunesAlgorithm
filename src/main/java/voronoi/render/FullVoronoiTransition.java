package voronoi.render;

import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.util.Duration;

import voronoi.algorithm.PointEvent;

class FullVoronoiTransition extends Transition {

    private final Window window;

    private GraphicsState lastGraphicsState;

    public FullVoronoiTransition(final Window window, final GraphicsState initialGraphicsState,
            final Duration duration) {
        super(24);
        this.window = window;

        this.lastGraphicsState = initialGraphicsState;

        this.setCycleDuration(duration);
        this.setInterpolator(Interpolator.LINEAR);

    }

    @Override
    protected void interpolate(final double frac) {
        double sweepY = this.window.getBounds().getMaxY() * (1 - frac) - 2 * this.window.getBounds().getMinY() * frac;

        PointEvent e = null;
        while (this.window.getVoronoi().hasEvents()
                && sweepY < this.window.getVoronoi().nextEvent().point.getY()) {
            e = this.window.getVoronoi().step();
            if (e != null)
                this.window.getVoronoi().dump(e.point.getY());
        }

        if (frac == 1) {
            while (this.window.getVoronoi().hasEvents()) {
                e = this.window.getVoronoi().step();
                if (e != null)
                    this.window.getVoronoi().dump(e.point.getY());
            }

            if (e != null)
                sweepY = e.point.getY();
        }

        if (e != null)
            this.lastGraphicsState = this.window.recreateShapes(e.point.getY());

        this.window.drawGraphicsState(this.lastGraphicsState, sweepY);
    }

}