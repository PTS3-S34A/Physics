package nl.soccar.physics.listener;

import nl.soccar.library.Ball;
import nl.soccar.library.Session;
import nl.soccar.library.enumeration.EventType;
import nl.soccar.physics.GameEngine;

/**
 * @author PTS34A
 */
public interface GameEventListener {

    /**
     * This method is called when a ball enters the goal.
     *
     * @param engine The game engine.
     * @param session The session.
     * @param ball The ball.
     * @param type The event type.
     */
    void onBallInGoal(GameEngine engine, Session session, Ball ball, EventType type);

}
