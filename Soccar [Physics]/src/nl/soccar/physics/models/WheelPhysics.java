package nl.soccar.physics.models;

import nl.soccar.library.enumeration.HandbrakeAction;
import nl.soccar.library.enumeration.ThrottleAction;
import nl.soccar.physics.AbstractWorldObject;
import nl.soccar.physics.PhysicsConstants;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.*;
import org.jbox2d.dynamics.joints.Joint;
import org.jbox2d.dynamics.joints.PrismaticJointDef;
import org.jbox2d.dynamics.joints.RevoluteJointDef;

/**
 * WheelPhysics is a utility class that keeps track of the physics of a Wheel,
 * which is in turn connected to a Car physics-model.
 *
 * @author PTS34A
 */
public class WheelPhysics extends AbstractWorldObject {

    private static final float LINEAR_DAMPING = 1.0F;
    private static final float ANGULAR_DAMPING = 1.0F;
    private static final float DENSITY = 1.0F;
    private static final boolean IS_SENSOR = true; // Do not include wheels in collision system (for performance).

    private final CarPhysics carPhysics;

    private final Vec2 originalPos;

    private final float width;
    private final float height;

    private final boolean steerable;
    private final boolean powered;

    private final World world;
    private Body body;
    private Joint joint;

    private float desiredSpeed = 0.0F;
    private int power;


    /**
     * Initiates a new WheelPhysics Object using the given parameters.
     *
     * @param relPosX    The x-coordinate, relative to the Car.
     * @param relPosY    The y-coordinate, relative to the Car.
     * @param width      The width of this Wheel.
     * @param height     The height of this Wheel.
     * @param steerable  Determines whether this wheel is used to steer the Car.
     * @param powered    Determines whether this wheel is used to power the Car.
     * @param carPhysics The CarPhysics object.
     * @param world      The world in which this Wheel is placed in.
     */
    public WheelPhysics(float relPosX, float relPosY, float width, float height, boolean steerable, boolean powered, CarPhysics carPhysics, World world) {
        this.world = world;

        this.carPhysics = carPhysics;

        this.width = width;
        this.height = height;
        this.steerable = steerable;
        this.powered = powered;

        originalPos = carPhysics.getBody().getWorldPoint(new Vec2(relPosX, relPosY));
        doReset();
    }

    @Override
    public void doStep() {
        eliminateLateralVelocity();

        if (isSteerable()) {
            setAngle(carPhysics.getSteerAngle());
        }

        if (isPowered()) {
            setDesiredSpeed(carPhysics.getCar().getThrottleAction());
            updateDrive();
        }
    }


    @Override
    public void doSetPosition(float x, float y, float degree, float linearVelocityX, float linearVelocityY, float angularVelocity) {
        throw new UnsupportedOperationException("Can't set position of wheels.");
    }

    @Override
    protected void doReset() {
        if (joint != null) {
            world.destroyJoint(joint);
        }

        if (body != null) {
            world.destroyBody(body);
        }

        BodyDef bd = new BodyDef();
        bd.type = BodyType.DYNAMIC;
        bd.position = originalPos;
        bd.angle = carPhysics.getBody().getAngle();
        bd.linearDamping = LINEAR_DAMPING; // Simulates friction
        bd.angularDamping = ANGULAR_DAMPING;

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(width / 2, height / 2);

        FixtureDef fd = new FixtureDef();
        fd.density = DENSITY;
        fd.isSensor = IS_SENSOR;
        fd.shape = shape;

        body = world.createBody(bd);
        body.createFixture(fd);

        if (steerable) {
            RevoluteJointDef jd = new RevoluteJointDef();
            jd.initialize(carPhysics.getBody(), body, body.getWorldCenter());
            jd.enableMotor = true;
            joint = world.createJoint(jd);
        } else {
            PrismaticJointDef jd = new PrismaticJointDef();
            jd.initialize(carPhysics.getBody(), body, body.getWorldCenter(), new Vec2(1, 0));
            jd.enableLimit = true;
            jd.lowerTranslation = 0;
            jd.upperTranslation = 0;
            joint = world.createJoint(jd);
        }
    }

    /**
     * Apply force on the wheel based on the power of the carPhysics and the desired speed.
     */
    private void updateDrive() {
        Vec2 currentForwardNormal = body.getWorldVector(new Vec2(0, 1));

        float currentSpeed = Vec2.dot(getForwardVelocity(), currentForwardNormal);
        float force = (float) power * 10;

        // Negative force
        if (desiredSpeed < currentSpeed) {
            force *= -1;
        }

        // Don't do anything
        if (Math.abs(desiredSpeed - currentSpeed) < 0.0001F) { // Calculate absolute of values, then check if it is below a treshold
            // Because floating points literals will (almost) never be equal.
            return;
        }

        // Apply force according to desiredSpeed
        body.applyForce(currentForwardNormal.mul(force), body.getWorldCenter());
    }

    /**
     * Eliminates sideways velocity.
     */
    private void eliminateLateralVelocity() {
        float massDiv;
        if (carPhysics.getCar().getHandbrakeAction() == HandbrakeAction.ACTIVE) {
            massDiv = PhysicsConstants.CAR_HANDBRAKE_SLIDE;
        } else {
            massDiv = PhysicsConstants.CAR_NORMAL_SLIDE;
        }

        // Lateral velocity
        Vec2 impulse = getLateralVelocity().mul(-body.getMass() / massDiv);
        body.applyLinearImpulse(impulse, body.getWorldCenter());
    }

    /**
     * Sets the speed the carPhysics should go towards. This is based on the ThrottleAction.
     * The carPhysics will move towards this speed in the UpdateDrive method.
     *
     * @param throttleAction The throttle action.
     */
    private void setDesiredSpeed(ThrottleAction throttleAction) {
        switch (throttleAction) {
            case BOOST:
            case ACCELERATE:
                desiredSpeed = PhysicsConstants.CAR_MAX_SPEED;
                power = PhysicsConstants.CAR_NORMAL_POWER;
                break;
            case REVERSE:
                desiredSpeed = -PhysicsConstants.CAR_MAX_REVERSE_SPEED;
                power = PhysicsConstants.CAR_NORMAL_POWER;
                break;
            default:
            case IDLE:
                desiredSpeed = 0;
                break;
        }

        if (carPhysics.isBoostActive()) {
            desiredSpeed = PhysicsConstants.CAR_MAX_BOOST_SPEED;
            power = PhysicsConstants.CAR_BOOST_POWER;
        }
    }

    /**
     * Sets wheel's angle relative to the carPhysics's body (in degrees).
     *
     * @param angle the new angle (not relative to the carPhysics) of this Wheel.
     */
    private void setAngle(float angle) {
        body.m_sweep.a = carPhysics.getBody().getAngle() + angle;

    /**
     * Gets the lateral velocity vector.
     *
     * @return Lateral velocity
     */
    private Vec2 getLateralVelocity() {
        Vec2 currentRightNormal = body.getWorldVector(new Vec2(1, 0));
        return currentRightNormal.mul(Vec2.dot(currentRightNormal, body.getLinearVelocity()));
    }

    /**
     * Gets the forward velocity.
     *
     * @return Forward velocity
     */
    private Vec2 getForwardVelocity() {
        Vec2 currentRightNormal = body.getWorldVector(new Vec2(0, 1));
        return currentRightNormal.mul(Vec2.dot(currentRightNormal, body.getLinearVelocity()));
    }

    /**
     * Returns the wheel width.
     *
     * @return The wheel width.
     */
    public float getWidth() {
        return width;
    }

    /**
     * Returns the wheel height.
     *
     * @return The wheel height.
     */
    public float getHeight() {
        return height;
    }

    /**
     * Returns whether this wheel is steerable.
     *
     * @return boolean
     */
    public boolean isSteerable() {
        return steerable;
    }

    /**
     * Returns whether this wheel is powered.
     *
     * @return boolean
     */
    public boolean isPowered() {
        return powered;
    }

    @Override
    public float getX() {
        return body.getPosition().x;
    }

    @Override
    public float getY() {
        return body.getPosition().y;
    }

    @Override
    public float getDegree() {
        return (float) Math.toDegrees(body.getAngle());
    }

}
