package nl.soccar.physics.models;

import javafx.geometry.Point2D;
import nl.soccar.library.Car;
import nl.soccar.library.enumeration.ThrottleAction;
import nl.soccar.physics.AbstractWorldObject;
import nl.soccar.physics.GameEngine;
import nl.soccar.physics.PhysicsConstants;
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
public class CarPhysics extends AbstractWorldObject {

    private static final boolean BULLET = true;

    private static final float DENSITY = 0.2F;
    private static final float RESTITUTION = 0.2F;

    private static final float WHEEL_POS_RATIO_X = 2.3F;
    private static final float WHEEL_POS_RATIO_Y = 4.0F;

    private final GameEngine engine;
    private final Vec2 originalPos;
    private final float originalDegree;
    private final List<WheelPhysics> wheels;
    private final Car car;
    private Body body;
    private float steerAngle = 0.0F;
    private List<Point2D> trail; // Holds the boost trail location
    private boolean boostActive;

    /**
     * Initiates a new CarPhysics Object using the given parameters.
     *
     * @param car   The car model to keep track of.
     * @param engine The engine in which this model is placed.
     */
    public CarPhysics(GameEngine engine, Car car) {
        this.engine = engine;
        this.car = car;
        this.trail = new ArrayList<>();
        this.boostActive = false;

        float carWidth = car.getWidth();
        float carHeight = car.getHeight();

        originalPos = new Vec2(car.getX(), car.getY());
        originalDegree = (float) Math.toRadians(car.getDegree());

        wheels = new ArrayList<>();
        doReset();

        // TODO: Calculate wheel positions from PhysicsUtilities, WHEEL_POS_RATIO should be defined in DisplayConstants.
        float wheelWidth = car.getWheelWidth();
        float wheelHeight = car.getWheelHeight();

        // Create wheels
        wheels.add(new WheelPhysics(-carWidth / WHEEL_POS_RATIO_X, carHeight / WHEEL_POS_RATIO_Y, wheelWidth, wheelHeight, true, true, this, engine));
        wheels.add(new WheelPhysics(carWidth / WHEEL_POS_RATIO_X, carHeight / WHEEL_POS_RATIO_Y, wheelWidth, wheelHeight, true, true, this, engine));
        wheels.add(new WheelPhysics(-carWidth / WHEEL_POS_RATIO_X, -carHeight / WHEEL_POS_RATIO_Y, wheelWidth, wheelHeight, false, false, this, engine));
        wheels.add(new WheelPhysics(carWidth / WHEEL_POS_RATIO_X, -carHeight / WHEEL_POS_RATIO_Y, wheelWidth, wheelHeight, false, false, this, engine));
    }

    @Override
    protected void doStep() {
        // Update the steering angle
        updateSteerAngle();

        // Update each wheel
        wheels.forEach(WheelPhysics::step);

        // Update trail
        updateBoost();

        // Move the car
        car.move(getX(), getY(), getDegree());
    }

    @Override
    protected void doSetPosition(float x, float y, float degree, float linearVelocityX, float linearVelocityY, float angularVelocity) {
        if (isResetting()) {
            return;
        }

        car.move(x, y, degree);

        body.setLinearVelocity(new Vec2(linearVelocityX, linearVelocityY));
        body.setAngularVelocity(angularVelocity);
        body.setTransform(new Vec2(x, y), (float) Math.toRadians(degree));
    }

    @Override
    protected void doReset() {
        trail.clear();

        car.move(originalPos.x, originalPos.y, (float) Math.toDegrees(originalDegree));
        car.setBoostAmount(Car.DEFAULT_BOOST_AMOUNT);

        World world = engine.getWorld();

        BodyDef bd = new BodyDef();
        bd.type = BodyType.DYNAMIC;
        bd.position.set(originalPos);
        bd.angle = originalDegree;
        bd.bullet = BULLET; // Prevents tunneling

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(car.getWidth() / 2, car.getHeight() / 2);

        FixtureDef fd = new FixtureDef();
        fd.density = DENSITY;
        fd.restitution = RESTITUTION;
        fd.shape = shape;
        fd.userData = car;

        body = world.createBody(bd);
        body.createFixture(fd);

        wheels.forEach(WheelPhysics::reset);
    }

    /**
     * Updates the steer angle of the front wheels based on SteerAction
     */
    private void updateSteerAngle() {
        float wheelMaxSteerAngle = (float) Math.toRadians(PhysicsConstants.WHEEL_MAX_STEER_ANGLE);
        float angleDiff = (wheelMaxSteerAngle / PhysicsConstants.WHEEL_MAX_TURN_IN_MS) * PhysicsConstants.ENGINE_REFRESH_RATE;

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

    /**
     * Updates the boost trail.
     */
    private void updateBoost() {
        // Only allow boosting when the trail is gone.
        if (car.getThrottleAction() == ThrottleAction.BOOST && trail.isEmpty()) {
            boostActive = true;
        }

        // Disable boost when the throttle action is not boost or boost amount is 0
        if (car.getThrottleAction() != ThrottleAction.BOOST || car.getBoostAmount() == 0) {
            boostActive = false;
        }

        // The trail has to remove itself when it's longer than max length or boost is not active.
        if (trail.size() > PhysicsConstants.CAR_BOOST_TRAIL_SIZE || !trail.isEmpty() && !boostActive) {
            trail.remove(0);
        }

        // Make boost trail if active, refill when inactive.
        if (boostActive) {
            car.setBoostAmount(car.getBoostAmount() - PhysicsConstants.CAR_BOOST_DEPLETE_SPEED);
            Vec2 exhaustPos = body.getWorldPoint(new Vec2(0, -car.getHeight() / 2));
            trail.add(new Point2D(exhaustPos.x, exhaustPos.y));
        } else {
            car.setBoostAmount(car.getBoostAmount() + PhysicsConstants.CAR_BOOST_FILL_SPEED);
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

    /**
     * Returns the linear velocity X value.
     *
     * @return The linear velocity X value.
     */
    public float getLinearVelocityX() {
        return body.getLinearVelocity().x;
    }

    /**
     * Returns the linear velocity Y value.
     *
     * @return The linear velocity Y value.
     */
    public float getLinearVelocityY() {
        return body.getLinearVelocity().y;
    }

    /**
     * Returns the angular velocity value.
     *
     * @return The angular velocity value.
     */
    public float getAngularVelocity() {
        return body.getAngularVelocity();
    }

    /**
     * Returns the steer angle.
     * @return The steer angle.
     */
    public float getSteerAngle() {
        return steerAngle;
    }

    /**
     * Returns the Box2D car body.
     * @return The Box2D car body.
     */
    public Body getBody() {
        return body;
    }

    /**
     * Returns a list of all wheels of the car.
     * @return List of all wheels of the car.
     */
    public List<WheelPhysics> getWheels() {
        return Collections.unmodifiableList(wheels);
    }

    /**
     * Returns a list of all trail positions.
     * @return A list of all trail positions.
     */
    public List<Point2D> getTrail() {
        return trail;
    }

    /**
     * Returns whether the boost is active.
     * @return Whether the boost is active.
     */
    public boolean isBoostActive() {
        return boostActive;
    }

    /**
     * Returns the car object.
     * @return The car object.
     */
    public Car getCar() {
        return car;
    }
}
