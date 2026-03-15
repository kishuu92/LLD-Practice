# Low Level Design Implementations

Collection of Low Level Design (LLD) implementations for real-world backend systems.

---

## Depth of Coverage

Each design focuses on practical implementation and includes:

- Clean object-oriented architecture with well-defined classes and interfaces
- API-style service layer
- Thoughtful data structure selection
- Concurrency considerations where applicable
- Notes on extensibility and possible production improvements
- Runnable demo/driver program to validate behavior

---

## Implemented Systems

| System | Notes |
|------|------|
| Meeting Scheduler | Interval scheduling using TreeSet, room-level concurrency control |
| Rate Limiter | Per-client rate limiting across endpoints with sliding window and token bucket algorithms |
| Elevator System | Multi-elevator scheduling with pluggable strategy for request assignment |
| Producer–Consumer | Multiple producers/consumers on shared buffer; implementations using BlockingQueue and wait/notify |
| Thread Coordinated Tasks | Two threads coordinating work using wait/notify (odd-even execution model) |
| (More coming) | |
