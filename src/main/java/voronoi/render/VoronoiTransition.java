package voronoi.render;

import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.util.Duration;

class VoronoiTransition extends Transition {

    private final Window window;

    private final GraphicsState gs;
    private final double sweepLineStart;
    private final double sweepLineEnd;

    VoronoiTransition(final Window window, final GraphicsState gs, final double sweepLineStart,
            final double sweepLineEnd, final Duration duration) {
        super(24);
        this.window = window;

        this.gs = gs;
        this.sweepLineStart = sweepLineStart;
        this.sweepLineEnd = sweepLineEnd;

        this.setCycleDuration(duration);
        this.setInterpolator(Interpolator.LINEAR);
    }

    @Override
    protected void interpolate(final double frac) {
        final double sweepY = (sweepLineEnd - sweepLineStart) * frac + sweepLineStart;

        this.window.drawGraphicsState(gs, sweepY);
    }
}