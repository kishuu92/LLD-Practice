package com.practice.lld;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;

/**
 *  Elevator System Simulation
 *
 *  This program models a simplified multi-elevator control system.
 *  It focuses on elevator scheduling logic rather than UI or real hardware interaction.
 *
 *
 *  1. Controller
 *    - Receives external pickup requests (floor buttons).
 *    - Delegates elevator selection to a pluggable scheduling strategy.
 *
 *  2. Elevator
 *    - Maintains its own request queue and movement state.
 *    - Processes requests step-by-step in a tick-based simulation.
 *
 *  3. Scheduling Strategy (Strategy Pattern)
 *    - Allows different elevator assignment algorithms without modifying Controller.
 *    - Example strategies: nearest elevator, least-loaded elevator, zone-based scheduling.
 *
 *  4. Concurrency Model
 *    - Requests may be added concurrently by the controller while elevators process steps.
 *    - Elevator methods accessing the request structure are synchronized to ensure
 *      thread-safe compound operations.
 *
 *  5. Request Model
 *    - External requests: pickup (UP/DOWN button pressed on a floor)
 *    - Internal requests: drop (floor selected inside elevator)
 *
 *  This simulation demonstrates core elevator scheduling principles while keeping
 *  the implementation simple and extensible.
 */


public class ElevatorSystem {

    public static void main(String[] args) {

        Building building = new Building(0, 9);
        Controller controller = new Controller(building, new DirectionAwareNearestElevatorStrategy());

        controller.addPickupRequest(3, Direction.UP);
        controller.addDropRequest(1, 8);
        controller.addPickupRequest(7, Direction.DOWN);

        for (int i = 0; i < 8; i++)
            controller.step();
        controller.addPickupRequest(1, Direction.UP);
        controller.addDropRequest(2, 8);
        controller.addDropRequest(1, 5);
        for (int i = 0; i < 8; i++)
            controller.step();
        controller.addPickupRequest(7, Direction.DOWN);
        controller.addDropRequest(1, 1);
        for (int i = 0; i < 20; i++)
            controller.step();
    }

}


/**
 *  Represents static building configuration.
 *
 *  This class defines the valid floor range for the building.
 *  Elevators use this to validate incoming pickup and drop requests.
 *
 *  Keeping building configuration separate makes the system adaptable
 *  to buildings with different floor ranges.
 */
class Building {

    private final int minFloor;
    private final int maxFloor;

    Building(int minFloor, int maxFloor) {
        this.minFloor = minFloor;
        this.maxFloor = maxFloor;
    }

    boolean isValidFloor(int floor) {
        return floor >= minFloor && floor <= maxFloor;
    }

    int getMinFloor() {
        return minFloor;
    }

    int getMaxFloor() {
        return maxFloor;
    }
}


/**
 *  Represents the current movement direction of an elevator.
 *
 *  This enum is used internally by the elevator to determine
 *  movement and request servicing behavior.
 */
enum Direction {UP, DOWN, IDLE}


/**
 *  Represents the type of request stored in the elevator queue.
 *
 *  PICKUP_UP / PICKUP_DOWN
 *   - External requests generated when a user presses Up/Down button.
 *
 *  DROP
 *   - Internal request generated when a passenger inside the elevator
 *     selects a destination floor.
 *
 *  These types help the elevator decide whether it should serve a pickup
 *  request depending on its current movement direction.
 */
enum RequestType {PICKUP_UP, PICKUP_DOWN, DROP}


/**
 * Represents a stop request for an elevator.
 *
 * Requests are stored inside the elevator's request map keyed by floor.
 * Multiple requests may exist for the same floor (e.g., pickup + drop).
 */
@AllArgsConstructor
class Request {
    final int floor;
    final RequestType requestType;
}


/**
 *  Represents a single elevator car.
 *
 *  Responsibilities:
 *  - Maintain its current state (floor, direction).
 *  - Store pending requests in a sorted structure.
 *  - Move one floor at a time during each simulation tick.
 *  - Decide when to stop based on request type and movement direction.
 *
 *  Requests are stored in a TreeMap so floors remain sorted,
 *  allowing efficient lookup of the next stop above or below.
 *
 *  Concurrency:
 *  - Requests may be added while the elevator processes movement steps.
 *  - Methods interacting with the request structure are synchronized
 *    to prevent concurrent modification during iteration.
 */
@Getter
class Elevator {

    private final int id;
    private final Building building;
    private int currentFloor;
    private Direction direction;


    /**
     * Sorted structure of pending requests.
     *
     * Key   : floor number
     * Value : list of requests for that floor
     *
     * TreeMap is used so we can efficiently find:
     *  - next stop above current floor  → higherKey()
     *  - next stop below current floor  → lowerKey()
     */
    private final TreeMap<Integer, List<Request>> requests = new TreeMap<>();

    Elevator(int id, Building building) {
        this.id = id;
        this.building = building;
        currentFloor = building.getMinFloor();
        direction = Direction.IDLE;
    }

    /**
     * Simulates one time tick for the elevator.
     *
     * During each step the elevator:
     *  1. Stops at current floor if there are serviceable requests.
     *  2. Determines direction if currently idle.
     *  3. Adjusts direction if no more requests exist ahead.
     *  4. Moves one floor in the chosen direction.
     *
     * The method is synchronized to prevent concurrent modification
     * of the request structure while processing requests.
     */
    synchronized void step() {
        if (requests.isEmpty()) {
            direction = Direction.IDLE;
            return;
        }

        // stop at current floor if needed
        if (processCurrentFloor()) {
            return;
        }

        if (direction == Direction.IDLE) {
            determineDirection();
        }

        Direction previousDirection = direction;

        adjustDirectionIfNeeded();

        // Only re-check if direction actually changed
        if (previousDirection != direction) {
            if (processCurrentFloor()) {
                return;
            }
        }

        moveOneFloor();

        System.out.println("(elevator: " + id + " floor: " + currentFloor + " dir: " + direction+")");
    }

    /**
     *  Processes requests at the current floor.
     *
     *  Returns true if the elevator stopped at this floor.
     *
     *  We must use an Iterator instead of a for-each loop because
     *  requests may be removed while iterating. Using Iterator.remove()
     *  prevents ConcurrentModificationException.
     */
    private boolean processCurrentFloor() {

        if (!requests.containsKey(currentFloor)) {
            return false;
        }

        boolean shouldStop = false;

        List<Request> floorRequests = requests.get(currentFloor);
        Iterator<Request> itr = floorRequests.iterator();

        while (itr.hasNext()) {
            Request request = itr.next();

            if (shouldServe(request)) {
                itr.remove();
                shouldStop = true;
            }
        }

        if (floorRequests.isEmpty()) {
            requests.remove(currentFloor);
        }

        if (shouldStop) {
            System.out.println("\t\tElevator " + id + " stopped at floor " + currentFloor);
        }
        return shouldStop;
    }

    /**
     *  Determines whether the elevator should serve a request at the current floor.
     *
     *  Drop requests are always served because the passenger is already inside.
     *
     *  Pickup requests are only served if the elevator is moving in the same
     *  direction as the request to avoid inefficient direction reversals.
     */
    private boolean shouldServe(Request request) {

        if (request.requestType == RequestType.DROP) {
            return true;
        }

        if (direction == Direction.IDLE) {
            return true;
        }

        if (direction == Direction.UP && request.requestType == RequestType.PICKUP_UP) {
            return true;
        }

        if (direction == Direction.DOWN && request.requestType == RequestType.PICKUP_DOWN) {
            return true;
        }

        return false;
    }


    /**
     *  Determines the initial direction when the elevator is idle.
     *
     *  The elevator moves toward the nearest pending request
     *  based on floor distance.
     */
    private void determineDirection() {

        Integer nextUp = requests.higherKey(currentFloor);
        Integer nextDown = requests.lowerKey(currentFloor);

        if (nextUp == null) {
            direction = Direction.DOWN;
        } else if (nextDown == null) {
            direction = Direction.UP;
        } else {
            direction = (currentFloor - nextDown < nextUp - currentFloor)
                    ? Direction.DOWN : Direction.UP;
        }
    }

    /**
     *  Reverses direction when there are no more requests ahead
     *  in the current movement direction.
     *
     *  This ensures the elevator eventually serves requests that
     *  exist behind it.
     */
    private void adjustDirectionIfNeeded() {

        if (direction == Direction.UP && requests.higherKey(currentFloor) == null) {
            direction = Direction.DOWN;
        } else if (direction == Direction.DOWN && requests.lowerKey(currentFloor) == null) {
            direction = Direction.UP;
        }
    }

    private void moveOneFloor() {

        if (direction == Direction.UP) {
            currentFloor++;
        } else if (direction == Direction.DOWN) {
            currentFloor--;
        }
    }

    boolean addDropRequest(int dropFloor) {

        if (!building.isValidFloor(dropFloor) || dropFloor == currentFloor) {
            return false;
        }

        Request request = new Request(dropFloor, RequestType.DROP);
        return addRequest(request);
    }

    /**
     *  Adds a new request to the elevator.
     *
     *  Synchronized to ensure thread-safe modification of the request map,
     *  since requests may be added while the elevator is processing steps.
     */
    synchronized boolean addRequest(Request request) {
        return requests.computeIfAbsent(request.floor, k -> new ArrayList<>()).add(request);
    }
}


/**
 *  Central dispatcher responsible for assigning pickup requests
 *  to elevators.
 *
 *  The controller does not implement scheduling logic directly.
 *  Instead it delegates elevator selection to a scheduling strategy,
 *  allowing different algorithms to be plugged in easily.
 */
class Controller {

    private final Building building;
    private final List<Elevator> elevators;
    private final ElevatorSchedulingStrategy schedulingStrategy;

    Controller(Building building, ElevatorSchedulingStrategy schedulingStrategy) {

        this.building = building;

        elevators = List.of(
                new Elevator(1, building),
                new Elevator(2, building),
                new Elevator(3, building)
        );

        this.schedulingStrategy = schedulingStrategy;
    }

    void step () {
        for (Elevator elevator : elevators)
            elevator.step();
    }

    boolean addDropRequest(int elevatorId, int floor) {

        if (!building.isValidFloor(floor)) {
            return false;
        }

        for (Elevator elevator : elevators) {
            if (elevator.getId() == elevatorId) {
                return elevator.addDropRequest(floor);
            }
        }

        return false;
    }

    Integer addPickupRequest(int floor, Direction direction) {

        if (!building.isValidFloor(floor) || direction == Direction.IDLE) {
            return null;
        }

        Elevator bestElevator = schedulingStrategy.selectElevator(elevators, floor, direction);
        if (bestElevator == null) {
            return null;
        }

        RequestType requestType = direction == Direction.UP ? RequestType.PICKUP_UP : RequestType.PICKUP_DOWN;

        Request request = new Request(floor, requestType);

        bestElevator.addRequest(request);
        return bestElevator.getId();
    }
}

/**
 *  Strategy interface for selecting an elevator to serve a pickup request.
 *
 *  This abstraction allows different scheduling algorithms to be used
 *  without modifying the Controller.
 *
 *  Example implementations:
 *  - Nearest elevator strategy
 *  - Direction-aware strategy
 *  - Least loaded elevator
 *  - Zone-based scheduling
 */
interface ElevatorSchedulingStrategy {

    Elevator selectElevator(List<Elevator> elevators, int floor, Direction direction);
}


/**
 *  Scheduling strategy that prefers elevators already moving toward the request.
 *
 *  Selection priority:
 *      1. Elevator already moving in the requested direction and heading toward the floor
 *      2. Nearest idle elevator
 *      3. Nearest elevator overall
 *
 *  This reduces unnecessary direction changes and improves efficiency.
 */
class DirectionAwareNearestElevatorStrategy implements ElevatorSchedulingStrategy {

    @Override
    public Elevator selectElevator(List<Elevator> elevators, int floor, Direction direction) {

        Elevator best = findCommittedToFloor(elevators, floor, direction);
        if (best != null) return best;

        best = findNearestIdle(elevators, floor);
        if (best != null) return best;

        return findNearest(elevators, floor);
    }

    private Elevator findCommittedToFloor(List<Elevator> elevators, int floor, Direction direction) {

        Elevator best = null;
        int bestDistance = Integer.MAX_VALUE;

        for (Elevator elevator : elevators) {
            if (elevator.getDirection() == direction) {
                boolean valid =
                        direction == Direction.UP
                                ? elevator.getCurrentFloor() <= floor
                                : elevator.getCurrentFloor() >= floor;
                if (valid) {
                    int distance = Math.abs(elevator.getCurrentFloor() - floor);
                    if (distance < bestDistance) {
                        best = elevator;
                        bestDistance = distance;
                    }
                }
            }
        }
        return best;
    }

    private Elevator findNearestIdle(List<Elevator> elevators, int floor) {

        Elevator best = null;
        int bestDistance = Integer.MAX_VALUE;

        for (Elevator elevator : elevators) {
            if (elevator.getDirection() == Direction.IDLE) {
                int distance = Math.abs(elevator.getCurrentFloor() - floor);
                if (distance < bestDistance) {
                    best = elevator;
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    private Elevator findNearest(List<Elevator> elevators, int floor) {

        Elevator best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Elevator elevator : elevators) {
            int distance = Math.abs(elevator.getCurrentFloor() - floor);
            if (distance < bestDistance) {
                best = elevator;
                bestDistance = distance;
            }
        }
        return best;
    }
}