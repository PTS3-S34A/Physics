package nl.soccar.physics;

/**
 * A WorldObject represents a physics-model which are eventually tracked by
 * Drawables. It provides methods to update this model, and provides getters for
 * location purposes.
 *
 * @author PTS34A
 */
public interface WorldObject {

    /**
     * The step method will be executed every tick. It should be used to
     * update states of physics-objects, such as: applying velocity to a wheel.
     */
    void step();

    /**
     * Sets the position and velocity values for a world object.
     *
     * @param x               The X position value.
     * @param y               The Y position value.
     * @param degree          The current angle.
     * @param linearVelocityX The X velocity value.
     * @param linearVelocityY The Y velocity value.
     * @param angularVelocity The angular velocity value.
     */
    void setPosition(float x, float y, float degree, float linearVelocityX, float linearVelocityY, float angularVelocity);

    /**
     * Resets the WorldObject to its original position.
     */
    void reset();

    /**
     * Gets the x-coordinate of this physics-model, relative to the map it is
     * placed in.
     *
     * @return The x-coordinate of this WorldObject.
     */
    float getX();

    /**
     * Gets the y-coordinate of this physics-model, relative to the map it is
     * placed in.
     *
     * @return The y-coordinate of this WorldObject.
     */
    float getY();

    /**
     * Gets the angle of this physics-model, relative to the map it is placed
     * in.
     *
     * @return The angle of this WorldObject.
     */
    float getDegree();

    /**
     * Returns whether this object is currently being reset.
     *
     * @return Whether this object is currently being reset.
     */
    boolean isResetting();

}
