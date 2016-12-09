package nl.soccar.physics;

import nl.soccar.library.*;
import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.collision.Manifold;
import org.jbox2d.dynamics.contacts.Contact;

/**
 * This class listens for collisions between a player and the ball in the Box2D world. When a collision happens, the beginContact method is called.
 * When the collision is between a player and a car, the lastTouched field is updated in the Game object.
 */
public class BallContactListener implements ContactListener {

    @Override
    public void beginContact(Contact contact) {

        // A car might have been part of the collision.
        Car car = null;

        // The fixtures from the collision.
        Object fixtureA = contact.getFixtureA().getUserData();
        Object fixtureB = contact.getFixtureB().getUserData();

        // Fixtures have to be an object.
        if (fixtureA == null || fixtureB == null) {
            return;
        }

        // FixtureA is a car, fixtureB is a ball.
        if (fixtureA instanceof Car && fixtureB instanceof Ball) {
            car = (Car) fixtureA;
        }

        // FixturaA is a ball, fixtureB is a car.
        else if (fixtureA instanceof Ball && fixtureB instanceof Car) {
            car = (Car) fixtureB;
        }

        // If one of the above conditions are true, a player has touched the ball.
        if (car != null) {
            Player player = car.getPlayer();
            if (player == null) {
                return;
            }

            Session session = player.getCurrentSession();
            if (session == null) {
                return;
            }

            Game game = session.getGame();
            if (game == null) {
                return;
            }

            Map map = game.getMap();
            if (map == null) {
                return;
            }

            Ball ball = map.getBall();
            if (ball == null) {
                return;
            }
            
            ball.setLastTouched(player);
        }
    }

    @Override
    public void endContact(Contact contact) {
        // Not implemented
    }

    @Override
    public void preSolve(Contact contact, Manifold manifold) {
        // Not implemented
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse contactImpulse) {
        // Not implemented
    }

}
