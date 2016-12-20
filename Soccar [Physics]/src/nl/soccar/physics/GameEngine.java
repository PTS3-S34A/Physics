/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.soccar.physics;

import javafx.scene.shape.Rectangle;
import nl.soccar.library.*;
import nl.soccar.library.Map;
import nl.soccar.library.enumeration.EventType;
import nl.soccar.library.enumeration.GameStatus;
import nl.soccar.physics.models.BallPhysics;
import nl.soccar.physics.models.CarPhysics;
import org.jbox2d.dynamics.World;

import java.time.LocalTime;
import java.util.*;

/**
 * @author PTS34A
 */
public final class GameEngine {

    private final World world;
    private final Game game;
    private final List<WorldObject> objects = new ArrayList<>();
    private final java.util.Map<Player, CarPhysics> cars = new HashMap<>();
    private Timer timer;
    private BallPhysics ball;

    private long lastSecondsDecreasedMs = 0;

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
    }

    public void start() {
        if (game.getStatus() != GameStatus.STOPPED) {
            return;
        }

        game.start();

        timer = new Timer();
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
        if (game.getStatus() == GameStatus.PAUSED) {
            return;
        }

        world.step(1.0F / PhysicsConstants.ENGINE_FPS, PhysicsConstants.VELOCITY_ITERATIONS, PhysicsConstants.POSITION_ITERATIONS);

        if (System.currentTimeMillis() - lastSecondsDecreasedMs >= 1000) {
            game.decreaseGameTime();
            lastSecondsDecreasedMs = System.currentTimeMillis();
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
        if (notification != null && (ballX > rightGoal.getX() + ballRadius
                || ballX < leftGoal.getX() + leftGoal.getWidth() - ballRadius)) {
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

    public void addCar(Player player, CarPhysics car) {
        cars.put(player, car);

        synchronized (objects) {
            objects.add(car);
        }
    }

    public void removeCar(Player player) {
        CarPhysics car = cars.remove(player);

        synchronized (objects) {
            objects.remove(car);
        }
    }

    public void addWorldObject(WorldObject object) {
        if (object instanceof CarPhysics) {
            throw new UnsupportedOperationException("Please use addCar(..) to add a car to the World.");
        }

        if (object instanceof BallPhysics) {
            ball = (BallPhysics) object;
        }

        synchronized (objects) {
            objects.add(object);
        }
    }

    public void removeWorldObject(WorldObject object) {
        if (object instanceof Car) {
            throw new UnsupportedOperationException("Please use removeCar(..) to remove a car from the World.");
        }

        if (object instanceof BallPhysics) {
            ball = null;
        }

        synchronized (objects) {
            objects.remove(object);
        }
    }

    public CarPhysics getCarFromPlayer(Player player) {
        return cars.get(player);
    }

    public List<WorldObject> getWorldObjects() {
        synchronized (objects) {
            return Collections.unmodifiableList(objects);
        }
    }

    public BallPhysics getBall() {
        return ball;
    }

    public World getWorld() {
        return world;
    }

    public Game getGame() {
        return game;
    }
}
