package com.practice.lld;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class ProducerConsumerDemo {
}


enum WorkerType {
    PRODUCER,
    CONSUMER
}

@AllArgsConstructor
@Getter
enum SystemConfig {

    BUFFER_CAPACITY(5),
    PRODUCER_COUNT(2),
    CONSUMER_COUNT(2);

    private final int value;
}

@AllArgsConstructor
@Getter
class Message {

    private final String id;
    private final String payload;
}




