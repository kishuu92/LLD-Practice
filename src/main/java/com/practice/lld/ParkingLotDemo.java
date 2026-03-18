package com.practice.lld;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/**
 * Entry point for demonstrating Parking Lot system.
 * <p>
 * This class simulates:
 * - Adding parking spots
 * - Vehicle entry
 * - Vehicle exit
 */
public class ParkingLotDemo {

    public static void main(String[] args) throws Exception {

        PricingStrategy pricing = new HourlyPricingStrategy(100);
        AllocationStrategy allocation = new FirstFitAllocationStrategy();

        ParkingLot lot = new ParkingLot(pricing, allocation);

        lot.addSpot(new ParkingSpot("M1", SpotType.MOTORCYCLE));
        lot.addSpot(new ParkingSpot("C1", SpotType.CAR));
        lot.addSpot(new ParkingSpot("L1", SpotType.LARGE));

        Ticket t1 = lot.enter(VehicleType.CAR);
        System.out.println("Entered: " + t1);

        Ticket t2 = lot.enter(VehicleType.CAR);
        System.out.println("Entered: " + t2);

        Thread.sleep(1200);

        long fee = lot.exit(t1.id);
        System.out.println("Exited. Fee = " + fee);

        fee = lot.exit(t2.id);
        System.out.println("Exited. Fee = " + fee);
    }
}

/**
 * Represents types of parking spots available in the system.
 */
enum SpotType {
    MOTORCYCLE, CAR, LARGE;
}

/**
 * Represents types of vehicles entering the parking lot.
 */
enum VehicleType {
    MOTORCYCLE, CAR, LARGE;
}

/**
 * Represents a single parking spot.
 * <p>
 * Thread Safety:
 * - Each spot maintains its own lock (fine-grained locking).
 * - This avoids global contention and allows parallel parking operations.
 * <p>
 * Responsibility:
 * - Maintain occupancy state
 * - Provide exclusive access via lock
 */
class ParkingSpot {

    final String id;
    final SpotType type;

    /**
     * Lock specific to this parking spot.
     * Used to ensure atomic updates to 'occupied'.
     */
    final ReentrantLock lock = new ReentrantLock();

    /**
     * Indicates whether the spot is currently occupied.
     * Guarded by 'lock'.
     */
    boolean occupied;

    ParkingSpot(String id, SpotType type) {
        this.id = id;
        this.type = type;
    }
}

/**
 * Represents a parking ticket issued at entry.
 * <p>
 * Lifecycle:
 * - Created at entry
 * - Updated with exit time
 * - Used for billing calculation
 * <p>
 * Immutability:
 * - All fields are immutable except exitTime
 */
class Ticket {

    final String id;
    final String spotId;
    final VehicleType vehicleType;
    final long entryTime;
    long exitTime;

    Ticket(String id, String spotId, VehicleType type) {
        this.id = id;
        this.spotId = spotId;
        this.vehicleType = type;
        this.entryTime = System.currentTimeMillis();
    }

    public String toString() {
        return "Ticket{id='" + id + "', spot='" + spotId + "'}";
    }
}

/**
 * Strategy interface for parking spot allocation.
 * <p>
 * Allows plugging different allocation algorithms without
 * modifying ParkingLot.
 * <p>
 * Examples:
 * - First-fit
 * - Nearest gate
 * - Priority-based (VIP)
 */
interface AllocationStrategy {

    /**
     * Allocates a parking spot for given vehicle type.
     *
     * @param spots       list of all parking spots
     * @param vehicleType type of incoming vehicle
     * @return allocated ParkingSpot or null if none available
     */
    ParkingSpot allocate(List<ParkingSpot> spots, VehicleType vehicleType);
}

/**
 * First-fit allocation strategy.
 * <p>
 * Logic:
 * - Iterates through spots
 * - Picks first compatible and free spot
 * <p>
 * Concurrency:
 * - Uses per-spot locking via tryLock()
 * - Avoids blocking entire system
 * <p>
 * Trade-off:
 * - O(n) scan
 * - Good for moderate scale
 */
class FirstFitAllocationStrategy implements AllocationStrategy {

    public ParkingSpot allocate(List<ParkingSpot> spots, VehicleType vehicleType) {

        for (ParkingSpot spot : spots) {

            if (!canFit(vehicleType, spot.type)) continue;

            if (spot.lock.tryLock()) {
                try {
                    if (!spot.occupied) {
                        spot.occupied = true;
                        return spot;
                    }
                } finally {
                    spot.lock.unlock();
                }
            }
        }
        return null;
    }

    private boolean canFit(VehicleType v, SpotType s) {
        if (v == VehicleType.MOTORCYCLE) return true;
        if (v == VehicleType.CAR) return s == SpotType.CAR || s == SpotType.LARGE;
        return s == SpotType.LARGE;
    }
}

/**
 * Strategy interface for pricing calculation.
 * <p>
 * Allows flexible pricing models.
 * <p>
 * Examples:
 * - Hourly pricing
 * - Flat rate
 * - Surge pricing
 * - Subscription-based
 */
interface PricingStrategy {

    /**
     * Calculates parking fee.
     *
     * @param ticket completed ticket (with exitTime set)
     * @return fee in smallest currency unit (e.g., cents)
     */
    long calculate(Ticket ticket);
}

/**
 * Simple hourly pricing strategy.
 * <p>
 * Rules:
 * - Charges per hour
 * - Partial hours rounded up
 * - Minimum 1 hour charge
 */
class HourlyPricingStrategy implements PricingStrategy {

    private final long ratePerHour;

    HourlyPricingStrategy(long ratePerHour) {
        this.ratePerHour = ratePerHour;
    }

    public long calculate(Ticket t) {

        long duration = t.exitTime - t.entryTime;

        long hours = Math.max(1, (long) Math.ceil(duration / 3600000.0));

        return hours * ratePerHour;
    }
}

/**
 * Core Parking Lot system.
 * <p>
 * Responsibilities:
 * - Manage parking spots
 * - Handle vehicle entry/exit
 * - Maintain active tickets
 * <p>
 * Thread Safety:
 * - Uses fine-grained locking at spot level
 * - Uses ConcurrentHashMap for ticket storage
 * <p>
 * Design:
 * - Allocation and pricing are delegated via Strategy pattern
 * <p>
 * Scalability:
 * - Avoids global locks
 * - Supports concurrent operations
 */
class ParkingLot {

    private final List<ParkingSpot> spots = new ArrayList<>();
    private final Map<String, ParkingSpot> spotMap = new ConcurrentHashMap<>();
    private final Map<String, Ticket> activeTickets = new ConcurrentHashMap<>();

    private final PricingStrategy pricingStrategy;
    private final AllocationStrategy allocationStrategy;

    ParkingLot(PricingStrategy pricingStrategy,
               AllocationStrategy allocationStrategy) {

        this.pricingStrategy = pricingStrategy;
        this.allocationStrategy = allocationStrategy;
    }

    public void addSpot(ParkingSpot spot) {
        spots.add(spot);
        spotMap.put(spot.id, spot);
    }

    public Ticket enter(VehicleType vehicleType) {

        ParkingSpot spot = allocationStrategy.allocate(spots, vehicleType);

        if (spot == null) {
            throw new RuntimeException("No spot available");
        }

        String id = UUID.randomUUID().toString();
        Ticket ticket = new Ticket(id, spot.id, vehicleType);

        activeTickets.put(id, ticket);
        return ticket;
    }

    public long exit(String ticketId) {

        Ticket ticket = activeTickets.remove(ticketId);

        if (ticket == null) {
            throw new RuntimeException("Invalid ticket");
        }

        ParkingSpot spot = spotMap.get(ticket.spotId);

        spot.lock.lock();
        try {
            spot.occupied = false;
        } finally {
            spot.lock.unlock();
        }

        ticket.exitTime = System.currentTimeMillis();

        return pricingStrategy.calculate(ticket);
    }
}