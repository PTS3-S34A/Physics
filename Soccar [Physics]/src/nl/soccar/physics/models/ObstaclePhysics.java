package nl.soccar.physics.models;

import nl.soccar.library.Obstacle;
import nl.soccar.physics.AbstractWorldObject;
import nl.soccar.physics.GameEngine;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.FixtureDef;

/**
 * ObstaclePhysics is a physics-model that keeps track of the physics of an
 * Obstacle.
 *
 * @author PTS34A
 */
public class ObstaclePhysics extends AbstractWorldObject {

    private static final float FRICTION = 0.0F;

    private final GameEngine engine;
    private final Obstacle obstacle;
    private final float width;
    private final float height;
    private Body body;

    /**
     * Initiates a new ObstaclePhysics Object using the given parameters.
     *
     * @param obstacle The model to keep track of.
     * @param engine    The World in which this model is placed in.
     */
    public ObstaclePhysics(GameEngine engine, Obstacle obstacle) {
        this.engine = engine;
        this.obstacle = obstacle;

        width = obstacle.getWidth();
        height = obstacle.getHeight();

        doReset();
    }

    @Override
    protected void doSetPosition(float x, float y, float degree, float linearVelocityX, float linearVelocityY, float angularVelocity) {
        obstacle.move(x, y, degree);

        body.setLinearVelocity(new Vec2(linearVelocityX, linearVelocityY));
        body.setAngularVelocity(angularVelocity);
        body.setTransform(new Vec2(x, y), (float) Math.toRadians(degree));
    }

    @Override
    protected void doStep() {
        // The step method is not implemented because obstacles never move on the map.
    }

    @Override
    protected void doReset() {
        BodyDef bd = new BodyDef();
        bd.position.set(obstacle.getX(), obstacle.getY());
        bd.angle = (float) Math.toRadians(obstacle.getDegree());

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(width / 2, height / 2);

        FixtureDef fd = new FixtureDef();
        fd.friction = FRICTION;
        fd.shape = shape;

        body = engine.getWorld().createBody(bd);
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

    /**
     * Returns this obstacle.
     * @return This obstacle.
     */
    public Obstacle getObstacle() {
        return obstacle;
    }

    @Override
    public float getDegree() {
        return body.m_sweep.a;
    }

}
