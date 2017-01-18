package nl.soccar.physics;

import nl.soccar.library.Ball;
import nl.soccar.library.Car;
import nl.soccar.library.Player;
import nl.soccar.library.Session;
import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.collision.Manifold;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.contacts.Contact;

/**
 * This class listens for collisions between a player and the ball in the Box2D world. When a collision happens, the beginContact method is called.
 * When the collision is between a player and a car, the lastTouched field is updated in the Game object.
 */
public class BallContactListener implements ContactListener {

    @Override
    public void beginContact(Contact contact) {
        Car car = getCarFromFixtures(contact.getFixtureA(), contact.getFixtureB());
        if (car == null) {
            return;
        }

        Player player = car.getPlayer();

        Session session = player.getCurrentSession();
        if (session == null) {
            return;
        }

        session.getGame().getMap().getBall().setLastTouched(player);
    }

    /**
     * Gets a car from two fixtures, by determining which one is a Ball and which one is a Car.
     *
     * @param fixtureA The first Fixture connected to a Contact.
     * @param fixtureB The second Fixture connected to a Contact.
     * @return The Car, if found at all, of the Player that touched the Ball.
     */
    private Car getCarFromFixtures(Fixture fixtureA, Fixture fixtureB) {
        if (fixtureA == null || fixtureB == null) {
            return null;
        }

        Object userDataA = fixtureA.getUserData();
        Object userDataB = fixtureB.getUserData();

        if (userDataA == null || userDataB == null) {
            return null;
        }

        if (userDataA instanceof Car && userDataB instanceof Ball) {
            return (Car) userDataA; // FixtureA is a car, fixtureB is a ball.
        } else if (userDataA instanceof Ball && userDataB instanceof Car) {
            return (Car) userDataB; // FixtureA is a ball, fixtureB is a car.
        }

        return null;
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
