package nl.soccar.physics;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author PTS34A
 */
public abstract class AbstractWorldObject implements WorldObject {

    private boolean doPositionUpdate = false;
    private AtomicBoolean doReset = new AtomicBoolean(false);

    private float newX;
    private float newY;
    private float newDegree;
    private float newLinearVelocityX;
    private float newLinearVelocityY;
    private float newAngularVelocity;

    @Override
    public final void step() {
        if (doPositionUpdate) {
            doSetPosition(newX, newY, newDegree, newLinearVelocityX, newLinearVelocityY, newAngularVelocity);
            doPositionUpdate = false;
        }

        if (doReset.get()) {
            doReset();
            doReset.set(false);
        }

        doStep();
    }

    /**
     * Go to the next frame.
     */
    protected abstract void doStep();

    @Override
    public final void setPosition(float x, float y, float degree, float linearVelocityX, float linearVelocityY, float angularVelocity) {
        newX = x;
        newY = y;
        newDegree = degree;
        newLinearVelocityX = linearVelocityX;
        newLinearVelocityY = linearVelocityY;
        newAngularVelocity = angularVelocity;

        doPositionUpdate = true;
    }

    /**
     * Sets the position of the ball.
     *
     * @param x The X value.
     * @param y The Y value.
     * @param degree The degree value.
     * @param linearVelocityX The linear velocity X value.
     * @param linearVelocityY The linear velocity Y value.
     * @param angularVelocity The angular velocity value.
     */
    protected abstract void doSetPosition(float x, float y, float degree, float linearVelocityX, float linearVelocityY, float angularVelocity);

    @Override
    public final void reset() {
        doReset.set(true);
    }

    /**
     * Reset an objects attributes in the world.
     */
    protected abstract void doReset();

    /**
     * Returns whether this object is currently being reset.
     *
     * @return Whether this object is currently being reset.
     */
    public final boolean isResetting() {
        return doReset.get();
    }

}
