package nl.soccar.physics.models;

import nl.soccar.library.Ball;
import nl.soccar.physics.WorldObject;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.*;


/**
 * BallPhysics is a physics-model that keeps track of the physics of the Ball.
 *
 * @author PTS34A
 */
public class BallPhysics implements WorldObject {

    private static final float DENSITY = 0.01F;
    private static final float FRICTION = 1.0F;
    private static final float RESTITUTION = 0.8F;

    private static final float LINEAR_DAMPING = 1.0F;
    private static final float ANGULAR_DAMPING = 1.0F;

    private final Vec2 originalPos;

    private final Body body;
    private final float radius;
    private Ball ball;

    /**
     * Initiates a new BallPhysics Object using the given parameter.
     *
     * @param ball  The ball model to keep track of.
     * @param world The world in which this model is placed.
     */
    public BallPhysics(Ball ball, World world) {
        this.ball = ball;
        originalPos = new Vec2(ball.getX(), ball.getY());
        radius = ball.getRadius();

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
    public void step() {
        ball.move(getX(), getY(), getDegree());
    }

    @Override
    public void reset() {
        setPosition(originalPos.x, originalPos.y, 0, body.getLinearVelocity().x, body.getLinearVelocity().y, body.getAngularVelocity());
    }

    public void setPosition(float x, float y, float degree, float linearVelocityX, float linearVelocityY, float angularVelocity) {
        ball.move(x, y, degree);

        body.setLinearVelocity(new Vec2(linearVelocityX, linearVelocityY));
        body.setAngularVelocity(angularVelocity);
        body.getPosition().set(x, y);
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

    public float getLinearVelocityX() {
        return body.getLinearVelocity().x;
    }

    public float getLinearVelocityY() {
        return body.getLinearVelocity().y;
    }

    public float getAngularVelocity() {
        return body.getAngularVelocity();
    }

}
