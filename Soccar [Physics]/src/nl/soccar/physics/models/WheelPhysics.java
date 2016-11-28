package nl.soccar.physics.models;

import nl.soccar.physics.PhysicsConstants;
import nl.soccar.physics.WorldObject;
import nl.soccar.library.enumeration.HandbrakeAction;
import nl.soccar.library.enumeration.ThrottleAction;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.*;
import org.jbox2d.dynamics.joints.PrismaticJointDef;
import org.jbox2d.dynamics.joints.RevoluteJointDef;

/**
 * WheelPhysics is a utility class that keeps track of the physics of a Wheel,
 * which is in turn connected to a Car physics-model.
 *
 * @author PTS34A
 */
public class WheelPhysics implements WorldObject {

    private static final float LINEAR_DAMPING = 1.0F;
    private static final float ANGULAR_DAMPING = 1.0F;
    private static final float DENSITY = 1.0F;
    private static final boolean IS_SENSOR = true; // Do not include wheels in collision system (for performance).

    private final Body body;
    private final CarPhysics car;
    private final Body carBody;

    private final Vec2 originalPos;

    private final float width;
    private final float height;
    private boolean steerable;
    private boolean powered;

    private float desiredSpeed = 0.0F;


    /**
     * Initiates a new WheelPhysics Object using the given parameters.
     *
     * @param relPosX   The x-coordinate, relative to the Car.
     * @param relPosY   The y-coordinate, relative to the Car.
     * @param width     The width of this Wheel.
     * @param height    The height of this Wheel.
     * @param steerable Determines whether this wheel is used to steer the Car.
     * @param powered   Determines whether this wheel is used to power the Car.
     * @param car       The CarPhysics object.
     * @param world     The world in which this Wheel is placed in.
     */
    public WheelPhysics(float relPosX, float relPosY, float width, float height, boolean steerable, boolean powered, CarPhysics car, World world) {
        this.car = car;
        this.carBody = car.getBody();

        this.width = width;
        this.height = height;
        this.steerable = steerable;
        this.powered = powered;

        originalPos = carBody.getWorldPoint(new Vec2(relPosX, relPosY));

        BodyDef bd = new BodyDef();
        bd.type = BodyType.DYNAMIC;
        bd.position = originalPos;
        bd.angle = carBody.getAngle();
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
            jd.initialize(carBody, body, body.getWorldCenter());
            jd.enableMotor = true;
            world.createJoint(jd);
        } else {
            PrismaticJointDef jd = new PrismaticJointDef();
            jd.initialize(carBody, body, body.getWorldCenter(), new Vec2(1, 0));
            jd.enableLimit = true;
            jd.lowerTranslation = 0;
            jd.upperTranslation = 0;
            world.createJoint(jd);
        }
    }

    /**
     * Apply force on the wheel based on the power of the car and the desired speed.
     */
    public void updateDrive() {

        Vec2 currentForwardNormal = body.getWorldVector(new Vec2(0, 1));

        float currentSpeed = Vec2.dot(getForwardVelocity(), currentForwardNormal);
        float force = PhysicsConstants.CAR_POWER * 10;

        // Negative force
        if (desiredSpeed < currentSpeed) {
            force *= -1;
        }

        // Don't do anything
        if (Math.abs(desiredSpeed - currentSpeed) < 0.0001F) { // Calculate absolute of values, then check if it is below a treshold
            // Because floating points literals will (almost) never be equal.
            return;
        }

        body.applyForce(currentForwardNormal.mul(force), body.getWorldCenter());
    }

    /**
     * Eliminates sideways velocity.
     */
    public void eliminateLateralVelocity() {

        float massDiv;

        if (car.getHandbrakeAction() == HandbrakeAction.ACTIVE) {
            massDiv = PhysicsConstants.CAR_HANDBRAKE_SLIDE;
        } else {
            massDiv = PhysicsConstants.CAR_NORMAL_SLIDE;
        }

        // Lateral velocity
        Vec2 impulse = getLateralVelocity().mul(-body.getMass() / massDiv);
        body.applyLinearImpulse(impulse, body.getWorldCenter());
    }

    /**
     * Sets the speed the car should go towards. This is based on the ThrottleAction.
     * The car will move towards this speed in the UpdateDrive method.
     *
     * @param throttleAction The throttle action.
     */
    public void setDesiredSpeed(ThrottleAction throttleAction) {
        switch (throttleAction) {
            case ACCELERATE:
                desiredSpeed = PhysicsConstants.CAR_MAX_SPEED;
                break;
            case REVERSE:
                desiredSpeed = -PhysicsConstants.CAR_MAX_REVERSE_SPEED;
                break;
            default:
            case IDLE:
                desiredSpeed = 0;
                break;
        }
    }

    /**
     * Sets wheel's angle relative to the car's body (in degrees).
     *
     * @param angle the new angle (not relative to the car) of this Wheel.
     */
    public void setAngle(float angle) {
        body.m_sweep.a = carBody.getAngle() + angle;
    }

    /**
     * Sets wheel's velocity vector with sideways velocity subtracted.
     */
    private Vec2 getLateralVelocity() {
        Vec2 currentRightNormal = body.getWorldVector(new Vec2(1, 0));
        return currentRightNormal.mul(Vec2.dot(currentRightNormal, body.getLinearVelocity()));
    }

    private Vec2 getForwardVelocity() {
        Vec2 currentRightNormal = body.getWorldVector(new Vec2(0, 1));
        return currentRightNormal.mul(Vec2.dot(currentRightNormal, body.getLinearVelocity()));
    }

    @Override
    public void step() {

        eliminateLateralVelocity();

        if (isSteerable()) {
            setAngle(car.getSteerAngle());
        }

        if (isPowered()) {
            setDesiredSpeed(car.getThrottleAction());
            updateDrive();
        }

    }

    @Override
    public void reset() {
        body.setLinearVelocity(new Vec2(0.0F, 0.0F));
        body.setAngularVelocity(0.0F);
        body.setTransform(originalPos, 0.0F);
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

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public boolean isSteerable() {
        return steerable;
    }

    public boolean isPowered() {
        return powered;
    }

}
