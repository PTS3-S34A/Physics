package nl.soccar.physics;

import org.jbox2d.common.Vec2;

/**
 * Constants class to define all kinds of variables which are used by physics-
 * models.
 *
 * @author PTS34A
 */
public final class PhysicsConstants {

    /**
     * Refresh rate
     */
    public static final int FPS = 60;
    public static final int MS_PER_FRAME = 1000 / FPS;

    /**
     * Car attributes
     */
    public static final int CAR_MAX_SPEED = 50;
    public static final int CAR_MAX_REVERSE_SPEED = 50;
    public static final int CAR_POWER = 10;
    public static final int CAR_NORMAL_SLIDE = 2;
    public static final int CAR_HANDBRAKE_SLIDE = 16;

    /**
     * Car wheel attributes
     */
    public static final int WHEEL_MAX_STEER_ANGLE = 25;
    public static final int WHEEL_MAX_TURN_IN_MS = 1;


    /**
     * World properties
     */
    public static final Vec2 GRAVITY_ANGLE = new Vec2(0.0F, 0.0F);

    /**
     * World step properties
     */
    public static final int VELOCITY_ITERATIONS = 6;
    public static final int POSITION_ITERATIONS = 3;

    private PhysicsConstants() {
    }

}
