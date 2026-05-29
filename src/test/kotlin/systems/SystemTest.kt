package systems

import enums.*
import model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SystemTest {
    @Test
    fun `humans win when thing is killed`() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис", "Вика", "Глеб"))

        val thing = engine.getPlayers().find { it.role == Role.THING }!!
        val human = engine.getPlayers().find { it.role == Role.HUMAN && engine.isAdjacent(it, thing) }!!

        human.hand.clear()
        human.hand.add(ActionCard("Огнемёт", "Test", ActionType.FLAMETHROWER))
        thing.hand.removeAll { it.name == "Никакого шашлыка!" }

        engine.playCard(human, human.hand[0], thing)

        assertFalse(thing.isAlive)
        assertEquals("HUMANS", engine.checkVictory())
    }

    @Test
    fun `thing wins when all infected`() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис", "Вика", "Глеб"))

        // Заражаем всех людей
        engine.getPlayers().forEach { player ->
            if (player.role == Role.HUMAN && player.isAlive) {
                player.role = Role.INFECTED
            }
        }

        assertEquals("THING", engine.checkVictory())
    }

    @Test
    fun `complete game produces valid state`() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис", "Вика", "Глеб", "Дима"))

        var turnsPlayed = 0
        val maxTurns = 20

        while (turnsPlayed < maxTurns && engine.checkVictory() == null) {
            val player = engine.getCurrentPlayer()
            assertNotNull(player)

            if (player!!.isAlive) {
                engine.drawCard(player)
                val playable = engine.getPlayableCards(player)
                if (playable.isNotEmpty()) {
                    val targets = engine.getTargets(player, playable[0].name)
                    engine.playCard(player, playable[0], if (targets.isNotEmpty()) targets[0] else null)
                }
                if (!player.hasQuarantine) {
                    engine.executeExchange(player)
                }
            }
            engine.endTurn(player)
            turnsPlayed++
        }

        val alive = engine.getPlayers().filter { it.isAlive }
        assertTrue(alive.isNotEmpty(), "Должен быть хотя бы один живой игрок")
        assertTrue(turnsPlayed > 0, "Должны были быть ходы")
    }

    @Test
    fun `quarantine lasts 3 player turns`() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис", "Вика", "Глеб"))
        val player = engine.getCurrentPlayer()!!
        player.setQuarantine(3)

        // Прокручиваем 3 хода игрока
        repeat(3) {
            engine.endTurn(player)
            repeat(engine.getPlayers().size - 1) {
                val p = engine.getCurrentPlayer()!!
                engine.drawCard(p)
                engine.endTurn(p)
            }
        }

        assertFalse(player.hasQuarantine)
    }

    @Test
    fun `full game with temptation and perseverance`() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис", "Вика", "Глеб"))

        var turnsPlayed = 0
        while (turnsPlayed < 10 && engine.checkVictory() == null) {
            val player = engine.getCurrentPlayer() ?: break
            if (player.isAlive) {
                engine.drawCard(player)
                val playable = engine.getPlayableCards(player)
                if (playable.isNotEmpty()) {
                    val card = playable.first()
                    val targets = engine.getTargets(player, card.name)
                    engine.playCard(player, card, if (targets.isNotEmpty()) targets.first() else null)
                }
                if (!player.hasQuarantine) {
                    engine.executeExchange(player)
                }
            }
            engine.endTurn(player)
            turnsPlayed++
        }

        val alive = engine.getPlayers().filter { it.isAlive }
        assertTrue(alive.isNotEmpty())
    }
}
