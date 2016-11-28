package nl.soccar.physics.models;

import nl.soccar.library.Car;
import nl.soccar.physics.PhysicsConstants;
import nl.soccar.physics.WorldObject;
import nl.soccar.library.enumeration.HandbrakeAction;
import nl.soccar.library.enumeration.SteerAction;
import nl.soccar.library.enumeration.ThrottleAction;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CarPhysics is a physics-model that keeps track of the physics of the Car.
 *
 * @author PTS34A
 */
public class CarPhysics implements WorldObject {

    private static final boolean BULLET = true;

    private static final float DENSITY = 0.2F;
    private static final float RESTITUTION = 0.2F;

    private static final float WHEEL_POS_RATIO_X = 2.3F;
    private static final float WHEEL_POS_RATIO_Y = 4.0F;

    private final Vec2 originalPos;
    private final float originalDegree;

    private final Body body;
    private final List<WheelPhysics> wheels;
    private float steerAngle = 0.0F;
    private final Car car;

    /**
     * Initiates a new CarPhysics Object using the given parameters.
     *
     * @param car   The car model to keep track of.
     * @param world The world in which this model is placed.
     */
    public CarPhysics(Car car, World world) {
        this.car = car;

        float carWidth = car.getWidth();
        float carHeight = car.getHeight();

        originalPos = new Vec2(car.getX(), car.getY());
        originalDegree = (float) Math.toRadians(car.getDegree());

        BodyDef bd = new BodyDef();
        bd.type = BodyType.DYNAMIC;
        bd.position.set(originalPos);
        bd.angle = originalDegree;
        bd.bullet = BULLET; // Prevents tunneling

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(carWidth / 2, carHeight / 2);

        FixtureDef fd = new FixtureDef();
        fd.density = DENSITY;
        fd.restitution = RESTITUTION;
        fd.shape = shape;
        fd.userData = car;

        body = world.createBody(bd);
        body.createFixture(fd);

        wheels = new ArrayList<>();

        // TODO: Calculate wheel positions from PhysicsUtilities, WHEEL_POS_RATIO should be defined in DisplayConstants.
        float wheelWidth = car.getWheelWidth();
        float wheelHeight = car.getWheelHeight();

        // Create wheels
        wheels.add(new WheelPhysics(-carWidth / WHEEL_POS_RATIO_X, carHeight / WHEEL_POS_RATIO_Y, wheelWidth, wheelHeight, true, true, this, world));
        wheels.add(new WheelPhysics(carWidth / WHEEL_POS_RATIO_X, carHeight / WHEEL_POS_RATIO_Y, wheelWidth, wheelHeight, true, true, this, world));
        wheels.add(new WheelPhysics(-carWidth / WHEEL_POS_RATIO_X, -carHeight / WHEEL_POS_RATIO_Y, wheelWidth, wheelHeight, false, false, this, world));
        wheels.add(new WheelPhysics(carWidth / WHEEL_POS_RATIO_X, -carHeight / WHEEL_POS_RATIO_Y, wheelWidth, wheelHeight, false, false, this, world));
    }

    @Override
    public void step() {
        // Update the steering angle
        updateSteerAngle();

        // Update each wheel
        wheels.forEach(WheelPhysics::step);

        car.move(getX(), getY(), getDegree());
    }

    @Override
    public void reset() {
        body.setLinearVelocity(new Vec2(0.0F, 0.0F));
        body.setAngularVelocity(0.0F);
        body.setTransform(originalPos, originalDegree);

        wheels.forEach(WheelPhysics::reset);
    }

    /**
     * Updates the steer angle of the front wheels based on SteerAction
     *
     * @param steerAction
     */
    private void updateSteerAngle() {
        float wheelMaxSteerAngle = (float) Math.toRadians(PhysicsConstants.WHEEL_MAX_STEER_ANGLE);
        float angleDiff = (wheelMaxSteerAngle / PhysicsConstants.WHEEL_MAX_TURN_IN_MS) * PhysicsConstants.MS_PER_FRAME;

        switch (car.getSteerAction()) {
            case STEER_LEFT:
                steerAngle = Math.min(Math.max(steerAngle, 0) + angleDiff, wheelMaxSteerAngle);
                break;
            case STEER_RIGHT:
                steerAngle = Math.max(Math.min(steerAngle, 0) - angleDiff, -wheelMaxSteerAngle);
                break;
            default:
                steerAngle = 0;
                break;
        }
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

    public float getSteerAngle() {
        return steerAngle;
    }

    public Body getBody() {
        return body;
    }

    public ThrottleAction getThrottleAction() {
        return car.getThrottleAction();
    }

    public SteerAction getSteerAction() {
        return car.getSteerAction();
    }

    public HandbrakeAction getHandbrakeAction() {
        return car.getHandbrakeAction();
    }

    public List<WheelPhysics> getWheels() {
        return Collections.unmodifiableList(wheels);
    }

}
