/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.soccar.physics;

import javafx.scene.shape.Rectangle;
import nl.soccar.library.*;
import nl.soccar.library.enumeration.EventType;
import nl.soccar.library.enumeration.GameStatus;
import org.jbox2d.dynamics.World;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author PTS34A
 */
public final class GameEngine {

    private final World world;
    private final Timer timer;

    private final Game game;
    private final List<WorldObject> objects = new ArrayList<>();

    /**
     * Initiates a new GamePhysics Object. It creates a world using settings
     * defined in constants.
     *
     * @param game
     */
    public GameEngine(Game game) {
        this.game = game;

        // doSleep (second parameter) is true for better performance
        world = new World(PhysicsConstants.GRAVITY_ANGLE, true);
        world.setContactListener(new BallContactListener());

        timer = new Timer();
    }

    public void start() {
        if (game.getStatus() != GameStatus.STOPPED) {
            return;
        }

        game.start();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                step();
            }

        }, 0, PhysicsConstants.ENGINE_REFRESH_RATE);
    }

    public void stop() {
        if (game.getStatus() == GameStatus.STOPPED) {
            return;
        }

        game.stop();
        timer.cancel();
    }

    /**
     * Steps the underlying world and applies all kinds of factors to update all
     * physics models.
     */
    private void step() {
        world.step(1.0F / PhysicsConstants.ENGINE_FPS, PhysicsConstants.VELOCITY_ITERATIONS, PhysicsConstants.POSITION_ITERATIONS);

        // Stop the game when the time is over.
        if (game.getSecondsLeft() <= 0) {
            stop();
            game.stop();
        }

        // Update every object in the world
        synchronized (objects) {

            switch (game.getStatus()) {
                case RUNNING:
                    objects.forEach(WorldObject::step);
                    checkScored();
                    break;
                case SCORED:
                    objects.forEach(WorldObject::reset);
                    game.setGoalScored(false);
                    break;
                default:
                    break;
            }
        }
    }

    private void checkScored() {
        Map map = game.getMap();
        Rectangle leftGoal = map.getGoalBlue();
        Rectangle rightGoal = map.getGoalRed();
        Ball ball = map.getBall();
        Notification notification = game.getNotification();

        float ballX = ball.getX();
        float ballRadius = ball.getRadius();

        // Handle notification
        if (ballX > rightGoal.getX() + ballRadius
                || ballX < leftGoal.getX() + leftGoal.getWidth() - ballRadius) {
            notification.setDisplayTime(LocalTime.now());
            notification.setPlayer(ball.getLastTouched());
        }

        // Handle score event
        if (ballX > rightGoal.getX() + ballRadius) {
            game.addEvent(new Event(EventType.GOAL_BLUE, LocalTime.now(), ball.getLastTouched()));
            game.setGoalScored(true);
        } else if (ballX < leftGoal.getX() + leftGoal.getWidth() - ballRadius) {
            game.addEvent(new Event(EventType.GOAL_RED, LocalTime.now(), ball.getLastTouched()));
            game.setGoalScored(true);
        }
    }

    public void addWorldObject(WorldObject object) {
        synchronized (objects) {
            objects.add(object);
        }
    }

    public void removeWorldObject(WorldObject object) {
        synchronized (objects) {
            objects.remove(object);
        }
    }

    public World getWorld() {
        return world;
    }

    public Game getGame() {
        return game;
    }
}
