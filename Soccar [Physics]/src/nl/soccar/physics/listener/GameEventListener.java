package nl.soccar.physics.listener;

import nl.soccar.library.Ball;
import nl.soccar.library.Session;
import nl.soccar.library.enumeration.EventType;
import nl.soccar.physics.GameEngine;

/**
 * @author PTS34A
 */
public interface GameEventListener {

    void onBallInGoal(GameEngine engine, Session session, Ball ball, EventType type);

}
