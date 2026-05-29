package enums

enum class DefenseType(val category: DefenseCategory) {
    IM_FINE_HERE(DefenseCategory.SEAT_SWAP),
    NO_THANKS(DefenseCategory.EXCHANGE),
    PASS(DefenseCategory.EXCHANGE),
    NO_BBQ(DefenseCategory.FLAMETHROWER),
    FEAR(DefenseCategory.EXCHANGE),
}

enum class DefenseCategory {
    EXCHANGE,
    SEAT_SWAP,
    FLAMETHROWER,
}
