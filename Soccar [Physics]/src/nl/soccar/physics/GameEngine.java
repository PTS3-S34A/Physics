package nl.soccar.physics;

import javafx.scene.shape.Rectangle;
import nl.soccar.library.*;
import nl.soccar.library.Map;
import nl.soccar.library.enumeration.EventType;
import nl.soccar.library.enumeration.GameStatus;
import nl.soccar.physics.listener.GameEventListener;
import nl.soccar.physics.models.BallPhysics;
import nl.soccar.physics.models.CarPhysics;
import org.jbox2d.dynamics.World;

import java.util.*;

/**
 * @author PTS34A
 */
public final class GameEngine {

    private final Object lock = new Object();
    private final Session session;
    private final Game game;
    private final List<WorldObject> objects = new ArrayList<>();
    private final java.util.Map<Player, CarPhysics> cars = new HashMap<>();
    private final List<GameEventListener> listeners = new ArrayList<>();
    private World world;
    private BallPhysics ballPhysics;
    private Timer timer;
    private long lastSecondsDecreasedMs = 0;

    /**
     * l
     * Creates a new game engine object.
     *
     * @param session The session object.
     */
    public GameEngine(Session session) {
        this.session = session;
        game = session.getGame();

        // doSleep (second parameter) is true for better performance
        world = new World(PhysicsConstants.GRAVITY_ANGLE, true);
        world.setContactListener(new BallContactListener());
    }

    /**
     * Starts the game.
     */
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

    /**
     * Stops the game.
     */
    public void stop() {
        game.stop();

        timer.cancel();
        timer.purge();
        timer = null;
    }

    /**
     * Adds a game event listener.
     *
     * @param listener The game event listener.
     */
    public void addListener(GameEventListener listener) {
        Objects.requireNonNull(listener);

        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a game event listener.
     *
     * @param listener The game event listener.
     */
    public void removeListener(GameEventListener listener) {
        Objects.requireNonNull(listener);

        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * Steps the underlying world and applies all kinds of factors to update all
     * physics models.
     */
    private void step() {
        if (game.getStatus() == GameStatus.PAUSED) {
            return;
        }

        synchronized (lock) {
            if (world != null) {
                world.step(1.0F / PhysicsConstants.ENGINE_FPS, PhysicsConstants.VELOCITY_ITERATIONS, PhysicsConstants.POSITION_ITERATIONS);
            }
        }

        if (System.currentTimeMillis() - lastSecondsDecreasedMs >= 1000) {
            game.decreaseGameTime();
            lastSecondsDecreasedMs = System.currentTimeMillis();
        }

        if (game.getStatus() == GameStatus.RUNNING) {
            synchronized (objects) {
                objects.forEach(WorldObject::step);
                checkScored();
            }
        }
    }

    /**
     * Verifies if a goal has been scored and updates the score accordingly.
     */
    private void checkScored() {
        Map map = game.getMap();
        Rectangle leftGoal = map.getGoalBlue();
        Rectangle rightGoal = map.getGoalRed();
        Ball ball = map.getBall();

        float ballX = ball.getX();
        float ballRadius = ball.getRadius();

        // Handle score event
        if (ballX > rightGoal.getX() + ballRadius) {
            listeners.forEach(l -> l.onBallInGoal(this, session, ball, EventType.GOAL_BLUE));
        } else if (ballX < leftGoal.getX() + leftGoal.getWidth() - ballRadius) {
            listeners.forEach(l -> l.onBallInGoal(this, session, ball, EventType.GOAL_RED));
        }
    }

    /**
     * Adds a car to the world.
     *
     * @param player The player the car belongs to.
     * @param car    The car that will be added.
     */
    public void addCar(Player player, CarPhysics car) {
        cars.put(player, car);

        synchronized (objects) {
            objects.add(car);
        }
    }

    /**
     * Removes a car from the world.
     *
     * @param player The car to be removed.
     */
    public void removeCar(Player player) {
        CarPhysics car = cars.remove(player);

        synchronized (objects) {
            objects.remove(car);
        }
    }

    /**
     * Adds a world object to the world.
     *
     * @param object The world object to be added.
     */
    public void addWorldObject(WorldObject object) {
        if (object instanceof CarPhysics) {
            throw new UnsupportedOperationException("Please use addCar(..) to add a car to the World.");
        }

        if (object instanceof BallPhysics) {
            ballPhysics = (BallPhysics) object;
        }

        synchronized (objects) {
            objects.add(object);
        }
    }

    /**
     * Removes a world object from the world.
     *
     * @param object The world object to be removed.
     */
    public void removeWorldObject(WorldObject object) {
        if (object instanceof Car) {
            throw new UnsupportedOperationException("Please use removeCar(..) to remove a car from the World.");
        }

        if (object instanceof BallPhysics) {
            ballPhysics = null;
        }

        synchronized (objects) {
            objects.remove(object);
        }
    }

    /**
     * Resets the position of all world objects.
     */
    public void resetWorldObjects() {
        synchronized (lock) {
            // doSleep (second parameter) is true for better performance
            world = null;
            world = new World(PhysicsConstants.GRAVITY_ANGLE, true);
            world.setContactListener(new BallContactListener());
        }

        synchronized (objects) {
            objects.forEach(WorldObject::reset);
        }
    }

    /**
     * Gets the car object from a player.
     *
     * @param player The player to get the car from.
     * @return The car object that belongs to the player.
     */
    public CarPhysics getCarFromPlayer(Player player) {
        return cars.get(player);
    }

    public List<WorldObject> getWorldObjects() {
        synchronized (objects) {
            return Collections.unmodifiableList(objects);
        }
    }

    /**
     * Gets the ball object.
     *
     * @return The ball object.
     */
    public BallPhysics getBall() {
        return ballPhysics;
    }

    /**
     * Gets the world object.
     *
     * @return The world object.
     */
    public World getWorld() {
        synchronized (lock) {
            return world;
        }
    }

    /**
     * Gets the game object
     *
     * @return The game object.
     */
    public Game getGame() {
        return game;
    }

}
