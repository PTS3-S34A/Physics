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
        if (doReset.get()) {
            doReset();
            doReset.set(false);
        }

        if (doPositionUpdate) {
            doSetPosition(newX, newY, newDegree, newLinearVelocityX, newLinearVelocityY, newAngularVelocity);
            doPositionUpdate = false;
        }

        doStep();
    }

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

    protected abstract void doSetPosition(float x, float y, float degree, float linearVelocityX, float linearVelocityY, float angularVelocity);

    @Override
    public final void reset() {
        doReset.set(true);
    }

    protected abstract void doReset();

    public final boolean isResetting() {
        return doReset.get();
    }

}
