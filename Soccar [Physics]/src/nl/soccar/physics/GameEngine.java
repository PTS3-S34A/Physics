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

import java.time.LocalTime;
import java.util.*;

/**
 * @author PTS34A
 */
public final class GameEngine {

    private final World world;

    private final Session session;
    private final Room room;
    private final Game game;

    private final List<WorldObject> objects = new ArrayList<>();
    private final java.util.Map<Player, CarPhysics> cars = new HashMap<>();

    private final List<GameEventListener> listeners = new ArrayList<>();

    private Timer timer;
    private BallPhysics ball;

    private long lastSecondsDecreasedMs = 0;

    /**
     * Initiates a new GamePhysics Object. It creates a world using settings
     * defined in constants.
     *
     * @param session the Session in which this GameEngine is operating.
     */
    public GameEngine(Session session) {
        this.session = session;
        game = session.getGame();
        room = session.getRoom();

        // doSleep (second parameter) is true for better performance
        world = new World(PhysicsConstants.GRAVITY_ANGLE, true);
        world.setContactListener(new BallContactListener());
    }

    /**
     * Creates a new timer and starts the game with the specified frame rate.
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
        if (game.getStatus() == GameStatus.STOPPED) {
            return;
        }

        game.stop();

        timer.cancel();
    }

    /**
     * Adds a listener to this GameEngine.
     *
     * @param listener The listener to add.
     */
    public void addListener(GameEventListener listener) {
        Objects.requireNonNull(listener);

        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a listener from this GameEngine.
     *
     * @param listener The listener to remove.
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
                default:
                    break;
            }
        }
    }

    /**
     * Checks on every frame whether the ball crossed the goal line and updates the score accordingly.
     */
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
            listeners.forEach(l -> l.onBallInGoal(this, session, ball, EventType.GOAL_BLUE));
        } else if (ballX < leftGoal.getX() + leftGoal.getWidth() - ballRadius) {
            listeners.forEach(l -> l.onBallInGoal(this, session, ball, EventType.GOAL_RED));
        }
    }

    /**
     * Adds a car to the World.
     *
     * @param player The Player that owns the Car.
     * @param car The Car to add to the World.
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
     * @param player The Player that owns the Car.
     */
    public void removeCar(Player player) {
        CarPhysics car = cars.remove(player);

        synchronized (objects) {
            objects.remove(car);
        }
    }

    /**
     * Adds an object to the World.
     *
     * @param object The object to add to the World.
     */
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

    /**
     * Removes an object from the World.
     *
     * @param object The Object to remove from the World.
     */
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

    /**
     * Resets all objects currently in the World.
     */
    public void resetWorldObjects() {
        synchronized (objects) {
            objects.forEach(WorldObject::reset);
        }

        world.clearForces();
    }

    /**
     * Get the car object that belongs to a player.
     *
     * @param player The player to get the car object from.
     * @return The car object.
     */
    public CarPhysics getCarFromPlayer(Player player) {
        return cars.get(player);
    }

    /**
     * Returns a list of all world objects.
     *
     * @return The world objects
     */
    public List<WorldObject> getWorldObjects() {
        synchronized (objects) {
            return Collections.unmodifiableList(objects);
        }
    }

    /**
     * Returns the ball physics object.
     *
     * @return The ball physics object.
     */
    public BallPhysics getBall() {
        return ball;
    }

    /**
     * Returns the Box2D world object.
     *
     * @return Box2D world.
     */
    public World getWorld() {
        return world;
    }

    /**
     * Returns the game object from the library.
     *
     * @return The game object.
     */
    public Game getGame() {
        return game;
    }

}
