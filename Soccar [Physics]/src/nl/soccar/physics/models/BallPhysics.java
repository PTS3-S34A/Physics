package nl.soccar.physics.models;

import nl.soccar.library.Ball;
import nl.soccar.physics.AbstractWorldObject;
import nl.soccar.physics.GameEngine;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.*;


/**
 * BallPhysics is a physics-model that keeps track of the physics of the Ball.
 *
 * @author PTS34A
 */
public class BallPhysics extends AbstractWorldObject {

    private static final float DENSITY = 0.01F;
    private static final float FRICTION = 1.0F;
    private static final float RESTITUTION = 0.8F;

    private static final float LINEAR_DAMPING = 1.0F;
    private static final float ANGULAR_DAMPING = 1.0F;

    private final Vec2 originalPos;

    private final GameEngine engine;
    private final float radius;
    private Body body;
    private Ball ball;

    /**
     * Initiates a new BallPhysics Object using the given parameter.
     *
     * @param ball   The ball model to keep track of.
     * @param engine The engine in which this model is placed.
     */
    public BallPhysics(GameEngine engine, Ball ball) {
        this.engine = engine;

        this.ball = ball;
        originalPos = new Vec2(ball.getX(), ball.getY());
        radius = ball.getRadius();

        doReset();
    }

    @Override
    protected void doStep() {
        ball.move(getX(), getY(), getDegree());
    }

    @Override
    protected void doSetPosition(float x, float y, float degree, float linearVelocityX, float linearVelocityY, float angularVelocity) {
        if (isResetting()) {
            return;
        }

        ball.move(x, y, 0);

        body.setLinearVelocity(new Vec2(linearVelocityX, linearVelocityY));
        body.setAngularVelocity(angularVelocity);
        body.setTransform(new Vec2(x, y), body.getAngle());
    }

    @Override
    protected void doReset() {
        ball.move(originalPos.x, originalPos.y, 0);

        World world = engine.getWorld();

        BodyDef bd = new BodyDef();
        bd.type = BodyType.DYNAMIC;
        bd.position.set(originalPos);
        bd.linearDamping = LINEAR_DAMPING;
        bd.angularDamping = ANGULAR_DAMPING;

        CircleShape cs = new CircleShape();
        cs.m_radius = radius;

        FixtureDef fd = new FixtureDef();
        fd.density = DENSITY;
        fd.friction = FRICTION;
        fd.restitution = RESTITUTION;
        fd.shape = cs;
        fd.userData = ball;

        body = world.createBody(bd);
        body.createFixture(fd);
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
}
