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

        val thing = engine.getPlayers().find { it.role == Role.THING }
        assertNotNull(thing, "Должен быть НЕЧТО")

        val human = engine.getPlayers().find { it.role == Role.HUMAN && engine.isAdjacent(thing!!, it) }
        assertNotNull(human, "Должен быть сосед-человек")

        thing!!.hand.clear()
        thing.hand.add(ActionCard("Заражение!", "Test", ActionType.INFECTION))
        thing.hand.add(ActionCard("Анализ", "Test", ActionType.ANALYSIS))

        human!!.hand.clear()
        human.hand.add(ActionCard("Виски", "Test", ActionType.WHISKEY))

        val exResult = engine.executeExchange(thing)
        assertTrue(
            exResult is GameEngine.GameResult.ExchangeInfo,
            "Должен быть ExchangeInfo, получили: ${exResult.message}",
        )

        val info = exResult as GameEngine.GameResult.ExchangeInfo

        assertEquals(human, info.neighbor, "Human должен быть соседом для обмена")

        val infectionCard = info.playerCards.find { it.name == "Заражение!" }
        assertNotNull(infectionCard, "У НЕЧТО должна быть карта Заражения")

        val result = engine.performExchange(thing, human, infectionCard!!, info.neighborCards.first())
        assertTrue(result is GameEngine.GameResult.Success, "Обмен должен быть успешным")

        assertEquals(Role.INFECTED, human.role, "Человек должен быть заражён после обмена")
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
        assertTrue(exResult is GameEngine.GameResult.Error)
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

        assertTrue(result is GameEngine.GameResult.Success)
        assertFalse(player.hasQuarantine)
    }
}
