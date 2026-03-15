package com.practice.lld;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Demo / driver class to showcase usage of the Meeting Scheduler.
 * <p>
 * This class simulates a simple workflow:
 * - registering rooms
 * - booking meetings (manual room selection and auto room selection)
 * - querying available rooms
 * - cancelling meetings
 * - viewing active meetings in rooms
 * <p>
 * NOTE:
 * This is only a demonstration layer and not part of the core scheduling system.
 * In a production system, these operations would typically be exposed through
 * REST APIs or service interfaces rather than a main method.
 */
public class MeetingSchedulerDemo {

    public static void main(String[] args) {

        RoomSelectionStrategy strategy = new BestFitRoomStrategy();
        MeetingScheduler scheduler = new MeetingScheduler(strategy);
        Room room1 = new Room("Vista", 5);
        Room room2 = new Room("Aqua", 10);
        Room room3 = new Room("Starlight", 15);
        scheduler.addRoom(room1);
        scheduler.addRoom(room2);
        scheduler.addRoom(room3);

        User user1 = new User(1, "Alice");
        User user2 = new User(2, "Bob");
        User user3 = new User(3, "Charlie");
        List<User> participants = List.of(user1, user2, user3);

        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 4, 30);
        LocalDateTime end = LocalDateTime.of(2026, 3, 15, 5, 0);
        int participantCount = 8;

        Meeting meeting = scheduler.bookMeetingAutoRoom("Meeting-1", start, end, participantCount, participants);
        System.out.println("Meeting booked: " + meeting.toString());
        System.out.println();

        participantCount = 4;
        System.out.println("Available Rooms for start: " + start + ", end: " + end + ", and participantCount:" + participantCount);
        List<Room> rooms = scheduler.getAvailableRooms(start, end, participantCount);
        for (Room room : rooms)
            System.out.println(room);
        System.out.println();

        meeting = scheduler.bookMeetingWithRoom("Meeting-2", start, end, participantCount, null, "Starlight");
        System.out.println("Meeting booked: " + meeting.toString());
        System.out.println();

        participantCount = 12;
        meeting = scheduler.bookMeetingAutoRoom("Meeting-3", start, end, participantCount, participants);
        if (meeting == null)
            System.out.println("No Room Available for Meeting-3");
        System.out.println();


        start = LocalDateTime.of(2026, 3, 15, 5, 30);
        end = LocalDateTime.of(2026, 3, 15, 6, 0);
        meeting = scheduler.bookMeetingAutoRoom("Meeting-3", start, end, participantCount, participants);
        System.out.println("Meeting booked: " + meeting.toString());
        System.out.println();

        participantCount = 5;
        start = LocalDateTime.of(2026, 3, 15, 5, 0);
        end = LocalDateTime.of(2026, 3, 15, 5, 30);
        System.out.println("Available Rooms for start: " + start + ", end: " + end + ", and participantCount:" + participantCount);
        rooms = scheduler.getAvailableRooms(start, end, participantCount);
        for (Room room : rooms)
            System.out.println(room);
        System.out.println();

        meeting = scheduler.bookMeetingAutoRoom("Meeting-4", start, end, participantCount, participants);
        System.out.println("Meeting booked: " + meeting.toString());
        System.out.println();

        scheduler.cancelMeeting(3);
        System.out.println();

        System.out.println("Checking scheduled meetings in all rooms:");
        for (Room room : scheduler.getAllRooms()) {
            System.out.println(room.toString());
            for (Meeting m : room.getActiveMeetings())
                System.out.println("\t\t" + m.toString());

        }
        System.out.println();

    }
}


/**
 * Represents lifecycle state of a meeting.
 * <p>
 * SCHEDULED  - meeting is active and occupies a room time slot
 * CANCELLED  - meeting was cancelled before completion
 * COMPLETED  - meeting finished and removed from the room's active schedule
 * <p>
 * NOTE:
 * Only SCHEDULED meetings are stored inside Room.activeMeetings.
 * Cancelled and completed meetings remain in the global registry
 * (meetingsById) for audit/history purposes.
 */
enum MeetingStatus {SCHEDULED, CANCELLED, COMPLETED}


/**
 * Represents a meeting participant.
 * <p>
 * For simplicity this model only contains id and name.
 * <p>
 * Production Note:
 * In a real system this would likely map to a user service or
 * directory (e.g. employee directory / identity service) and
 * contain additional information such as email, timezone,
 * and availability preferences.
 */
@AllArgsConstructor
@ToString
class User {
    private final int id;
    private final String name;
}


/**
 * Domain entity representing a meeting scheduled in a room.
 * <p>
 * A meeting has:
 * - time interval (start, end)
 * - declared participant count (used for room capacity planning)
 * - optional participant list
 * - lifecycle status
 * - assigned room
 * <p>
 * Ordering:
 * Meetings are ordered by start time to support efficient scheduling
 * using a TreeSet inside Room.
 * <p>
 * Production Note:
 * In a real system meeting IDs would typically be generated by the
 * persistence layer (database auto-increment or distributed ID generator).
 */
@Getter
@Setter
@ToString
class Meeting implements Comparable<Meeting> {

    private int id;
    private String title;
    private LocalDateTime start;
    private LocalDateTime end;
    private int participantCount;
    private final Set<User> participants;
    private MeetingStatus status;
    private Room room;

    public Meeting(String title, LocalDateTime start, LocalDateTime end,
                   List<User> participants, Room room, int participantCount) {

        if (!start.isBefore(end))
            throw new IllegalArgumentException("Invalid meeting time range");
        this.title = title;
        this.start = start;
        this.end = end;
        this.participants = participants == null ? new HashSet<>() : new HashSet<>(participants);
        this.participantCount = participantCount;
        this.room = room;
        this.status = MeetingStatus.SCHEDULED;
    }

    Meeting(LocalDateTime start, LocalDateTime end) {

        if (!start.isBefore(end))
            throw new IllegalArgumentException("Invalid meeting time range");
        this.start = start;
        this.end = end;
        this.participants = new HashSet<>();
    }

    boolean addParticipant(User user) {
        if (participants.contains(user)) return false;
        participants.add(user);
        return true;
    }

    boolean removeParticipant(User user) {
        if (!participants.contains(user)) return false;
        participants.remove(user);
        return true;
    }


    /**
     * Meetings are ordered by start time to support efficient
     * interval conflict detection using TreeSet floor/ceiling.
     * <p>
     * The meeting ID is included only as a defensive tie-breaker to guarantee
     * strict ordering in the TreeSet and to remain safe if future changes allow
     * meetings with identical start times.
     */
    @Override
    public int compareTo(Meeting other) {
        int cmp = this.start.compareTo(other.start);
        if (cmp != 0) return cmp;
        return Integer.compare(this.id, other.id);
    }
}

/**
 * Room maintains its own meeting schedule.
 * <p>
 * Concurrency Note:
 * All schedule mutations are synchronized at room level.
 * This prevents race conditions when multiple threads attempt
 * to book meetings in the same room simultaneously.
 * <p>
 * Different rooms can still be booked concurrently.
 */
@AllArgsConstructor
class Room {

    @Getter
    private final String name;
    @Getter
    private final int capacity;
    private final TreeSet<Meeting> activeMeetings = new TreeSet<>();

    /**
     * Checks if this room is available for the given time slot.
     * <p>
     * Concurrency Note:
     * Method is synchronized to ensure consistent reads while
     * booking operations may be modifying the meeting set.
     */
    public synchronized boolean isAvailable(LocalDateTime start, LocalDateTime end) {
        Meeting probe = new Meeting(start, end);
        Meeting floor = activeMeetings.floor(probe);
        Meeting ceiling = activeMeetings.ceiling(probe);

        return (floor == null || !floor.getEnd().isAfter(start)) &&
                (ceiling == null || !ceiling.getStart().isBefore(end));
    }

    /**
     * Book this room for a meeting.
     * Returns true if successful, false if conflict.
     */
    public synchronized boolean book(Meeting meeting) {

        if (meeting.getParticipantCount() > this.capacity) return false;

        if (isAvailable(meeting.getStart(), meeting.getEnd())) {
            activeMeetings.add(meeting);
            return true;
        }
        return false;
    }

    public synchronized void removeMeeting(Meeting meeting) {
        activeMeetings.remove(meeting);
    }

    public Collection<Meeting> getActiveMeetings() {
        return Collections.unmodifiableCollection(activeMeetings);
    }

    public synchronized void removeCompletedMeetings(LocalDateTime now) {

        Meeting probe = new Meeting(now, now.plusNanos(1));

        Iterator<Meeting> it = activeMeetings.headSet(probe).iterator();
        while (it.hasNext()) {
            Meeting m = it.next();
            if (m.getEnd().isBefore(now)) {
                if (m.getStatus() == MeetingStatus.SCHEDULED)
                    m.setStatus(MeetingStatus.COMPLETED);
                it.remove();
            }
        }
    }

    @Override
    public String toString() {
        return "Room(name=" + name + ", capacity=" + capacity + ")";
    }
}


/**
 * Central coordinator for meeting scheduling.
 * <p>
 * Concurrency Notes:
 * - Room booking synchronization is handled inside Room.
 * - Room registry and meeting lookup use ConcurrentHashMap.
 * - Meeting ID generation uses AtomicInteger for thread safety.
 * <p>
 * Production Note:
 * In a real system meeting IDs would typically be generated by
 * the persistence layer (database auto-increment or distributed ID generator).
 *
 * NOTE:
 * IDs may have gaps if booking fails. This is acceptable and common in
 * production systems where ID generation is not transactional
 * (e.g., database sequences or distributed ID generators).
 */
@AllArgsConstructor
class MeetingScheduler {

    private static final AtomicInteger MEETING_ID_GENERATOR = new AtomicInteger(0);

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<Integer, Meeting> meetingsById = new ConcurrentHashMap<>();
    private final RoomSelectionStrategy roomSelectionStrategy;

    public Meeting getMeeting(int id) {
        return meetingsById.get(id);
    }

    public Collection<Room> getAllRooms() {
        return Collections.unmodifiableCollection(rooms.values());
    }

    /**
     * Registers a room in the system
     */
    public void addRoom(Room room) {
        if (rooms.containsKey(room.getName())) {
            throw new IllegalArgumentException("Room already exists");
        }
        rooms.put(room.getName(), room);
    }

    public List<Room> getAvailableRooms(LocalDateTime start, LocalDateTime end, int participantCount) {

        List<Room> availableRooms = new ArrayList<>();

        for (Room room : rooms.values()) {
            if (room.getCapacity() < participantCount)
                continue;
            if (room.isAvailable(start, end)) {
                availableRooms.add(room);
            }
        }

        availableRooms.sort(Comparator.comparingInt(Room::getCapacity));
        return availableRooms;
    }

    /**
     * Booking when user explicitly selects a room
     */
    public Meeting bookMeetingWithRoom(String title, LocalDateTime start, LocalDateTime end,
                                       int participantCount, List<User> participants, String roomName) {

        Room room = rooms.get(roomName);

        if (room == null) {
            System.out.println("Room not found: " + roomName);
            return null;
        }

        if (participantCount > room.getCapacity()) {
            System.out.println("Room capacity exceeded");
            return null;
        }

        Meeting meeting = new Meeting(title, start, end, participants, room, participantCount);
        meeting.setId(MEETING_ID_GENERATOR.incrementAndGet());

        if (room.book(meeting)) {
            meetingsById.put(meeting.getId(), meeting);
            System.out.println("Meeting booked in room: " + roomName);
            return meeting;
        } else {
            System.out.println("Room not available for given time");
            return null;
        }
    }

    /**
     * Booking where scheduler selects best room using strategy
     */
    public Meeting bookMeetingAutoRoom(String title, LocalDateTime start, LocalDateTime end,
                                       int participantCount, List<User> participants) {

        List<Room> candidates = roomSelectionStrategy.selectRooms(rooms.values(), start, end, participantCount);

        if (candidates.isEmpty()) {
            System.out.println("No suitable room available");
            return null;
        }

        int meetingId = MEETING_ID_GENERATOR.incrementAndGet();
        for (Room room : candidates) {
            Meeting meeting = new Meeting(title, start, end, participants, room, participantCount);
            meeting.setId(meetingId);
            if (room.book(meeting)) {
                meetingsById.put(meeting.getId(), meeting);
                System.out.println("Meeting booked in room: " + room.getName());
                return meeting;
            }
        }

        System.out.println("No suitable room available");
        return null;
    }

    /**
     * Cancel meeting
     */
    public boolean cancelMeeting(int meetingId) {

        Meeting meeting = meetingsById.get(meetingId);

        if (meeting == null)
            return false;

        if (meeting.getStatus() != MeetingStatus.SCHEDULED)
            return false;

        meeting.setStatus(MeetingStatus.CANCELLED);
        meeting.getRoom().removeMeeting(meeting);
        System.out.println("Meeting cancelled: " + meeting.toString());

        return true;
    }

    /**
     * Marks past meetings as completed and removes them from active room schedules.
     * <p>
     * Production Note:
     * In a real system this would likely run as a scheduled background job
     * and state would be persisted in a database instead of in-memory.
     */
    public void markCompletedMeetings() {

        LocalDateTime now = LocalDateTime.now();
        for (Room room : rooms.values())
            room.removeCompletedMeetings(now);
    }
}

/**
 * Strategy interface for selecting candidate rooms.
 * <p>
 * Production Note:
 * Different strategies can be implemented such as:
 * - Best fit (current)
 * - Least recently used room
 * - Location/floor preference
 * - Equipment availability
 * <p>
 * Concurrency Note:
 * The strategy only filters and ranks candidate rooms.
 * Actual booking happens later and may still fail if another thread books
 * the same room concurrently. Returning multiple candidates allows the
 * scheduler to attempt booking alternative rooms without recomputing
 * the selection.
 */
interface RoomSelectionStrategy {

    List<Room> selectRooms(
            Collection<Room> rooms,
            LocalDateTime start,
            LocalDateTime end,
            int participantCount
    );
}


/**
 * Selects candidate rooms using a "best fit" strategy.
 * <p>
 * Rooms that:
 * - satisfy capacity requirements
 * - are available for the requested time slot
 * <p>
 * are returned sorted by capacity in ascending order.
 * <p>
 * The scheduler attempts booking in this order to minimize wasted capacity.
 */
class BestFitRoomStrategy implements RoomSelectionStrategy {

    @Override
    public List<Room> selectRooms(
            Collection<Room> rooms,
            LocalDateTime start,
            LocalDateTime end,
            int participantCount) {

        return rooms.stream()
                .filter(r -> r.getCapacity() >= participantCount)
                .filter(r -> r.isAvailable(start, end))
                .sorted(Comparator.comparingInt(Room::getCapacity))
                .collect(Collectors.toList());
    }
}