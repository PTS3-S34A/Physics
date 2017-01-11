package nl.soccar.physics.models;

import nl.soccar.library.Ball;
import nl.soccar.physics.AbstractWorldObject;
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

    private final World world;
    private Body body;
    private Ball ball;

    private final float radius;
    private final Vec2 originalPos;

    /**
     * Initiates a new BallPhysics Object using the given parameter.
     *
     * @param ball  The ball model to keep track of.
     * @param world The world in which this model is placed.
     */
    public BallPhysics(Ball ball, World world) {
        this.world = world;

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
        ball.move(x, y, 0);

        body.setLinearVelocity(new Vec2(linearVelocityX, linearVelocityY));
        body.setAngularVelocity(angularVelocity);
        body.setTransform(new Vec2(x, y), body.getAngle());
    }

    @Override
    protected void doReset() {
        ball.move(originalPos.x, originalPos.y, 0);

        if (body != null) {
            world.destroyBody(body);
        }

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
