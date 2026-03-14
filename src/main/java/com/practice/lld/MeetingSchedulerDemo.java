package com.practice.lld;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MeetingSchedulerDemo {

    public static void main(String[] args) {

    }
}

enum MeetingStatus {SCHEDULED, CANCELLED}


@AllArgsConstructor
@ToString
class User {
    private final int id;
    private final String name;
}


@Getter
@Setter
class Meeting implements Comparable<Meeting> {

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private final int id;
    private String title;
    private LocalDateTime start;
    private LocalDateTime end;
    private final Set<User> participants;
    private MeetingStatus status;
    private Room room;

    public Meeting(String title, LocalDateTime start, LocalDateTime end, List<User> participants, Room room) {

        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Invalid meeting time range");
        }
        this.id = COUNTER.incrementAndGet();
        this.title = title;
        this.start = start;
        this.end = end;
        this.participants = participants == null ? new HashSet<>() : new HashSet<>(participants);
        this.room = room;
        this.status = MeetingStatus.SCHEDULED;
    }

    Meeting(LocalDateTime start, LocalDateTime end, Room room) {

        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Invalid meeting time range");
        }
        this.id = -1;
        this.start = start;
        this.end = end;
        this.participants = new HashSet<>();
        this.room = room;
        this.status = MeetingStatus.SCHEDULED;
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

    @Override
    public int compareTo(Meeting other) {
        int cmp = this.start.compareTo(other.start);
        if (cmp != 0) return cmp;
        return Integer.compare(this.id, other.id);
    }
}


@AllArgsConstructor
@ToString
class Room {

    @Getter
    private final String name;
    @Getter
    private final int capacity;
    private final TreeSet<Meeting> activeMeetings = new TreeSet<>();

    /**
     * Checks if this room is available for the given time slot.
     */
    public synchronized boolean isAvailable(LocalDateTime start, LocalDateTime end) {
        Meeting probe = new Meeting(start, end, this);
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
        if (isAvailable(meeting.getStart(), meeting.getEnd())) {
            activeMeetings.add(meeting);
            return true;
        }
        return false;
    }

    public synchronized void removeMeeting(Meeting meeting) {
        activeMeetings.remove(meeting);
    }
}

@AllArgsConstructor
class MeetingScheduler {

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<Integer, Meeting> meetingsById = new ConcurrentHashMap<>();
    private final RoomSelectionStrategy roomSelectionStrategy;

    public Meeting getMeeting(int id) {
        return meetingsById.get(id);
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

        Meeting meeting = new Meeting(title, start, end, participants, room);

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

        Room room = roomSelectionStrategy.selectRoom(rooms.values(), start, end, participantCount);

        if (room == null) {
            System.out.println("No suitable room available");
            return null;
        }

        Meeting meeting = new Meeting(title, start, end, participants, room);

        if (room.book(meeting)) {
            meetingsById.put(meeting.getId(), meeting);
            System.out.println("Meeting booked in room: " + room.getName());
            return meeting;
        } else {
            System.out.println("No suitable room available");
            return null;
        }
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

        return true;
    }

}

interface RoomSelectionStrategy {

    Room selectRoom(
            Collection<Room> rooms,
            LocalDateTime start,
            LocalDateTime end,
            int participantCount
    );
}


class BestFitRoomStrategy implements RoomSelectionStrategy {

    @Override
    public Room selectRoom(
            Collection<Room> rooms,
            LocalDateTime start,
            LocalDateTime end,
            int participantCount) {

        Room bestRoom = null;

        for (Room room : rooms) {

            if (room.getCapacity() < participantCount)
                continue;

            if (!room.isAvailable(start, end))
                continue;

            if (bestRoom == null || room.getCapacity() < bestRoom.getCapacity()) {
                bestRoom = room;
            }
        }

        return bestRoom;
    }
}