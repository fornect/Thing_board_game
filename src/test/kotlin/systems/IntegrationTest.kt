package systems

import enums.*
import model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class IntegrationTest {
    @Test
    fun `full game flow works`() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис", "Вика", "Глеб"))

        var turnsPlayed = 0
        while (turnsPlayed < 4 && engine.checkVictory() == null) {
            val player = engine.getCurrentPlayer() ?: break
            if (player.isAlive) {
                engine.drawCard(player)
                val playable = engine.getPlayableCards(player)
                if (playable.isNotEmpty()) {
                    engine.playCard(player, playable.first(), null)
                }
                engine.executeExchange(player)
                engine.endTurn(player)
            } else {
                engine.endTurn(player)
            }
            turnsPlayed++
        }
        assertTrue(turnsPlayed > 0)
    }

    @Test
    fun `infection spreads through exchange`() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис", "Вика", "Глеб"))

        // Находим НЕЧТО и его соседа
        val thing = engine.getPlayers().find { it.role == Role.THING }
        assertNotNull(thing)

        val human = engine.getPlayers().find { it.role == Role.HUMAN && engine.isAdjacent(thing!!, it) }
        assertNotNull(human)

        // Даём карты
        thing!!.hand.clear()
        thing.hand.add(ActionCard("Заражение!", "Test", ActionType.INFECTION))

        human!!.hand.clear()
        human.hand.add(ActionCard("Виски", "Test", ActionType.WHISKEY))

        // Прямой обмен через performExchange
        val result =
            engine.performExchange(
                thing,
                human,
                thing.hand[0],
                human.hand[0],
            )

        assertTrue(result is GameResult.Success)
        assertEquals(Role.INFECTED, human.role)
    }

    @Test
    fun `door blocks exchange`() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис", "Вика", "Глеб"))
        val p1 = engine.getCurrentPlayer()!!
        val p2 = engine.getPlayers().find { it != p1 }!!

        p1.hand.clear()
        p1.hand.add(ActionCard("Заколоченная дверь", "Test", ActionType.AXE)) // ← ActionType.AXE!
        p1.hand.add(ActionCard("Анализ", "Test", ActionType.ANALYSIS))
        p2.hand.clear()
        p2.hand.add(ActionCard("Виски", "Test", ActionType.WHISKEY))

        engine.playCard(p1, p1.hand.find { it.name == "Заколоченная дверь" }!!, p2)

        val exResult = engine.executeExchange(p1)
        assertTrue(exResult is GameResult.Error)
        assertTrue(exResult.message.contains("Заколоченная дверь"))
    }

    @Test
    fun `axe removes quarantine`() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис", "Вика", "Глеб"))
        val player = engine.getCurrentPlayer()!!
        player.setQuarantine(3)
        player.hand.clear()
        player.hand.add(ActionCard("Топор", "Test", ActionType.AXE))

        val result = engine.playCard(player, player.hand[0], player)

        assertTrue(result is GameResult.Success)
        assertFalse(player.hasQuarantine)
    }
}
