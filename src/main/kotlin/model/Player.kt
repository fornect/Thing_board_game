package model

import enums.Role
import java.util.UUID

class Player(
    val name: String,
    val id: String = UUID.randomUUID().toString(),
) {
    var role: Role = Role.HUMAN

    val hand = mutableListOf<Card>()

    var isAlive: Boolean = true

    var hasQuarantine: Boolean = false

    var quarantineTurns: Int = 0

    var hasBarricade: Boolean = false

    var canRefuseExchange: Boolean = false
    var isSabotaged: Boolean = false
    var isSkipped: Boolean = false

    fun kill() {
        isAlive = false
        hand.clear()
    }

    fun setQuarantine(turns: Int = 3) {
        hasQuarantine = true
        quarantineTurns = turns
    }

    fun removeQuarantine() {
        hasQuarantine = false
        quarantineTurns = 0
    }

    fun decreaseQuarantine() {
        if (hasQuarantine) {
            quarantineTurns--
            if (quarantineTurns <= 0) {
                removeQuarantine()
            }
        }
    }

    fun setBarricade() {
        hasBarricade = true
    }

    fun removeBarricade() {
        hasBarricade = false
    }

    fun hasCard(name: String): Boolean = hand.any { it.name == name }

    override fun toString(): String {
        val icon =
            when (role) {
                Role.HUMAN -> "👤"
                Role.THING -> "👾"
                Role.INFECTED -> "🧟"
            }
        return "$icon $name"
    }
}
