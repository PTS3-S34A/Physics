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
     * Refresh rates
     */
    public static final int UI_FPS = 60;
    public static final int ENGINE_FPS = 120;
    public static final int ENGINE_REFRESH_RATE = 1000 / ENGINE_FPS;

    /**
     * Car attributes
     */
    public static final int CAR_MAX_SPEED = 50;
    public static final int CAR_MAX_REVERSE_SPEED = 50;
    public static final int CAR_MAX_BOOST_SPEED = 60;
    public static final int CAR_NORMAL_POWER = 10;
    public static final int CAR_BOOST_POWER = 30;
    public static final int CAR_NORMAL_SLIDE = 2;
    public static final int CAR_HANDBRAKE_SLIDE = 16;
    public static final float CAR_BOOST_FILL_SPEED = 0.25F;
    public static final float CAR_BOOST_DEPLETE_SPEED = 0.5F;
    public static final int CAR_BOOST_TRAIL_SIZE = 100;

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
