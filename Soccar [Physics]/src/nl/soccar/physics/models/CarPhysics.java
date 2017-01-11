package nl.soccar.physics.models;

import javafx.geometry.Point2D;
import nl.soccar.library.Car;
import nl.soccar.library.enumeration.ThrottleAction;
import nl.soccar.physics.AbstractWorldObject;
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

    private final Vec2 originalPos;
    private final float originalDegree;

    private final List<WheelPhysics> wheels;

    private final World world;
    private Body body;

    private final Car car;

    private List<Point2D> trail;

    private float steerAngle;
    private boolean boostActive;

    /**
     * Initiates a new CarPhysics Object using the given parameters.
     *
     * @param car   The car model to keep track of.
     * @param world The world in which this model is placed.
     */
    public CarPhysics(Car car, World world) {
        this.world = world;

        this.car = car;
        this.trail = new ArrayList<>();
        this.boostActive = false;
        this.steerAngle = 0.0F;

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
        wheels.add(new WheelPhysics(-carWidth / WHEEL_POS_RATIO_X, carHeight / WHEEL_POS_RATIO_Y, wheelWidth, wheelHeight, true, true, this, world));
        wheels.add(new WheelPhysics(carWidth / WHEEL_POS_RATIO_X, carHeight / WHEEL_POS_RATIO_Y, wheelWidth, wheelHeight, true, true, this, world));
        wheels.add(new WheelPhysics(-carWidth / WHEEL_POS_RATIO_X, -carHeight / WHEEL_POS_RATIO_Y, wheelWidth, wheelHeight, false, false, this, world));
        wheels.add(new WheelPhysics(carWidth / WHEEL_POS_RATIO_X, -carHeight / WHEEL_POS_RATIO_Y, wheelWidth, wheelHeight, false, false, this, world));
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
        car.move(x, y, degree);

        body.setLinearVelocity(new Vec2(linearVelocityX, linearVelocityY));
        body.setAngularVelocity(angularVelocity);
        body.setTransform(new Vec2(x, y), (float) Math.toRadians(degree));
    }

    @Override
    protected void doReset() {
        car.move(originalPos.x, originalPos.y, (float) Math.toDegrees(originalDegree));
        car.setBoostAmount(Car.DEFAULT_BOOST_AMOUNT);

        if (body != null) {
            world.destroyBody(body);
        }

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
     * Handles the boost amount and the boost trail list
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

    /**
     * Returns the X value from the linear velocity vector
     *
     * @return LinearVelocityX
     */
    public float getLinearVelocityX() {
        return body.getLinearVelocity().x;
    }

    /**
     * Returns the Y value from the linear velocity vector
     *
     * @return LinearVelocityY
     */
    public float getLinearVelocityY() {
        return body.getLinearVelocity().y;
    }

    /**
     * Returns the angular velocity
     *
     * @return AngularVelocity
     */
    public float getAngularVelocity() {
        return body.getAngularVelocity();
    }

    /**
     * Returns the current steering angle of the car.
     *
     * @return The steering angle.
     */
    public float getSteerAngle() {
        return steerAngle;
    }


    /**
     * Returns the Box2D body.
     *
     * @return The Box2D body.
     */
    public Body getBody() {
        return body;
    }

    /**
     * Return the wheels that belong to this car.
     *
     * @return The wheel physics objects.
     */
    public List<WheelPhysics> getWheels() {
        return Collections.unmodifiableList(wheels);
    }

    /**
     * Returns the boost trail list.
     *
     * @return The boost trail list
     */
    public List<Point2D> getTrail() {
        return trail;
    }

    /**
     * Returns whether the boost is currently active.
     *
     * @return boolean
     */
    public boolean isBoostActive() {
        return boostActive;
    }

    /**
     * Returns the car object (library).
     *
     * @return The car object.
     */
    public Car getCar() {
        return car;
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
