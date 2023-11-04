package uk.co.threebugs.darwinexclient

enum class Status {
    PENDING,
    ORDER_SENT,
    PLACED_IN_MT,
    FILLED,
    OUT_OF_TIME,
    CLOSED_BY_STOP,
    CLOSED_BY_LIMIT,
    CLOSED_BY_TIME,
    CLOSED_BY_MAGIC_SENT,
    CLOSED_BY_USER,
    MISSED
}
