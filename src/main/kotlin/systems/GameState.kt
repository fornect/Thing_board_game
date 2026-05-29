package systems

import enums.Role
import model.Player

class GameState {
    val players = mutableListOf<Player>()
    val deadPlayers = mutableListOf<Player>()
    var currentPlayerIndex = 0
    var direction = 1
    var turnNumber = 0
    var skipActionPhase = false
    var endTurnEarlyFlag = false
    val barricadedDoors = mutableListOf<Pair<Player, Player>>()

    var actionDone = false
    var cardDrawn = false
    var panicHappened = false
    var exchangeDone = false
    var quarantineJustPlaced = false

    fun resetTurnState() {
        println("DEBUG resetTurnState: actionDone was $actionDone")
        cardDrawn = false
        actionDone = false
        panicHappened = false
        exchangeDone = false
        quarantineJustPlaced = false
        println("DEBUG resetTurnState: actionDone now $actionDone")
    }

    fun getAlivePlayers() = players.filter { it.isAlive }

    fun getCurrentPlayer(): Player? {
        val alive = getAlivePlayers()
        return if (alive.isNotEmpty() && currentPlayerIndex < alive.size) alive[currentPlayerIndex] else null
    }

    fun getAllPlayers(): List<Player> = players + deadPlayers

    fun advanceTurn() {
        currentPlayerIndex += direction
        val alive = getAlivePlayers()
        if (alive.isEmpty()) return

        if (currentPlayerIndex >= alive.size) {
            currentPlayerIndex = 0
            turnNumber++
        }
        if (currentPlayerIndex < 0) {
            currentPlayerIndex = alive.size - 1
            turnNumber++
        }

        // Сбрасываем ВСЕ флаги при переходе хода
        skipActionPhase = false
        endTurnEarlyFlag = false
    }

    fun checkVictory(): String? {
        val alive = getAlivePlayers()
        val things = alive.filter { it.role == Role.THING || it.role == Role.INFECTED }
        val humans = alive.filter { it.role == Role.HUMAN }
        return when {
            things.isEmpty() -> "HUMANS"
            humans.isEmpty() -> "THING"
            else -> null
        }
    }

    fun getNeighborIndices(
        index: Int,
        total: Int,
    ): Pair<Int, Int> {
        if (total == 1) return Pair(0, 0)
        if (total == 2) return Pair((index + 1) % total, (index + 1) % total)
        return Pair((index - 1 + total) % total, (index + 1) % total)
    }

    fun isAdjacent(
        p1: Player,
        p2: Player,
    ): Boolean {
        val alive = getAlivePlayers()
        val idx1 = alive.indexOf(p1)
        val idx2 = alive.indexOf(p2)
        if (idx1 == -1 || idx2 == -1) return false
        if (barricadedDoors.any { (a, b) -> (a == p1 && b == p2) || (a == p2 && b == p1) }) return false
        if (alive.size <= 2) return true
        val (left, right) = getNeighborIndices(idx1, alive.size)
        return idx2 == left || idx2 == right
    }

    fun swapPlayers(
        p1: Player,
        p2: Player,
    ) {
        val idx1 = players.indexOf(p1)
        val idx2 = players.indexOf(p2)
        if (idx1 == -1 || idx2 == -1) return
        players[idx1] = p2
        players[idx2] = p1
        if (currentPlayerIndex == idx1) {
            currentPlayerIndex = idx2
        } else if (currentPlayerIndex == idx2) {
            currentPlayerIndex = idx1
        }
    }

    fun discardDownToFour(player: Player) {
        while (player.getHandSize() > 4) {
            val cards = player.getHandCards().filter { it.name != "НЕЧТО" }
            if (cards.isEmpty()) break
            player.removeCard(cards.random())
        }
    }
}
