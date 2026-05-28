package systems

import enums.*
import model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GameEngineTest {
    private lateinit var engine: GameEngine

    @BeforeEach
    fun setUp() {
        engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис", "Вика", "Глеб"))
    }

    @Test
    fun `setupGame creates correct number of players`() {
        assertEquals(4, engine.getPlayers().size)
    }

    @Test
    fun `setupGame assigns one THING`() {
        val things = engine.getPlayers().filter { it.role == Role.THING }
        assertEquals(1, things.size)
    }

    @Test
    fun `drawCard adds card to hand`() {
        val player = engine.getCurrentPlayer()!!
        val handSizeBefore = player.hand.size
        val result = engine.drawCard(player)
        if (result is GameEngine.GameResult.Success) {
            assertEquals(handSizeBefore + 1, player.hand.size)
        }
    }

    @Test
    fun `flamethrower kills adjacent player`() {
        val player = engine.getCurrentPlayer()!!
        player.hand.clear()
        player.hand.add(ActionCard("Огнемёт", "Test", ActionType.FLAMETHROWER))
        val target = engine.getPlayers().find { it != player && engine.isAdjacent(player, it) }!!
        target.hand.clear()

        val result = engine.playCard(player, player.hand[0], target)

        assertTrue(result is GameEngine.GameResult.Success)
        assertFalse(target.isAlive)
    }

    @Test
    fun `flamethrower blocked by defense`() {
        val player = engine.getCurrentPlayer()!!
        player.hand.clear()
        player.hand.add(ActionCard("Огнемёт", "Test", ActionType.FLAMETHROWER))
        val target = engine.getPlayers().find { it != player && engine.isAdjacent(player, it) }!!
        target.hand.clear()
        target.hand.add(DefenseCard("Никакого шашлыка!", "Block", DefenseType.NO_BBQ))

        val result = engine.playCard(player, player.hand[0], target)

        assertTrue(result.message.contains("защитился"))
        assertTrue(target.isAlive)
    }

    @Test
    fun `quarantine blocks non-axe actions`() {
        val player = engine.getCurrentPlayer()!!
        player.hand.clear()
        player.hand.add(ActionCard("Огнемёт", "Test", ActionType.FLAMETHROWER))
        val target = engine.getPlayers().find { it != player }!!
        target.hasQuarantine = true

        val result = engine.playCard(player, player.hand[0], target)

        assertTrue(result is GameEngine.GameResult.Error)
        assertTrue(result.message.contains("карантине"))
    }

    @Test
    fun `axe removes quarantine`() {
        val player = engine.getCurrentPlayer()!!
        player.hasQuarantine = true
        player.quarantineTurns = 3
        player.hand.clear()
        player.hand.add(ActionCard("Топор", "Test", ActionType.AXE))

        val result = engine.playCard(player, player.hand[0], player)

        assertTrue(result is GameEngine.GameResult.Success)
        assertFalse(player.hasQuarantine)
    }

    @Test
    fun `discard removes card from hand`() {
        val player = engine.getCurrentPlayer()!!
        player.hand.clear()
        val card = ActionCard("Анализ", "Test", ActionType.ANALYSIS)
        player.hand.add(card)

        val result = engine.discardCard(player, card)

        assertTrue(result is GameEngine.GameResult.Success)
        assertEquals(0, player.hand.size)
    }

    @Test
    fun `cannot discard THING card`() {
        val player = engine.getCurrentPlayer()!!
        player.hand.clear()
        val thingCard = ActionCard("НЕЧТО", "Thing", ActionType.INFECTION)
        player.hand.add(thingCard)

        val result = engine.discardCard(player, thingCard)

        assertTrue(result is GameEngine.GameResult.Error)
        assertTrue(player.hand.contains(thingCard))
    }

    @Test
    fun `exchange moves cards between players`() {
        val player = engine.getCurrentPlayer()!!
        val neighbor = engine.getPlayers().find { it != player && engine.isAdjacent(player, it) }!!

        player.hand.clear()
        player.hand.add(ActionCard("Анализ", "Test", ActionType.ANALYSIS))
        neighbor.hand.clear()
        neighbor.hand.add(ActionCard("Виски", "Test", ActionType.WHISKEY))

        val result = engine.executeExchange(player)

        if (result is GameEngine.GameResult.ExchangeInfo) {
            val card1 = result.playerCards[0]
            val card2 = result.neighborCards[0]
            val exchangeResult = engine.performExchange(player, neighbor, card1, card2)
            assertTrue(exchangeResult is GameEngine.GameResult.Success)
            assertTrue(player.hand.any { it.name == "Виски" })
            assertTrue(neighbor.hand.any { it.name == "Анализ" })
        } else {
            fail("Expected ExchangeInfo, got: ${result.message}")
        }
    }

    @Test
    fun `handleDefense returns DefensePlayed`() {
        val defender = engine.getCurrentPlayer()!!
        defender.hand.clear()
        defender.hand.add(DefenseCard("Страх", "Test", DefenseType.FEAR))

        val attacker = engine.getPlayers().find { it != defender && it.isAlive }
        assertNotNull(attacker, "Должен быть другой игрок для атаки")

        val result = engine.handleDefense(defender, attacker!!, ActionCard("Test", "", ActionType.ANALYSIS))

        assertTrue(
            result is GameEngine.GameResult.DefensePlayed,
            "Должен быть DefensePlayed, получили: ${result.message}",
        )
        assertFalse(defender.hand.any { it.name == "Страх" }, "Карта защиты должна быть удалена")
    }

    @Test
    fun `door blocks adjacency`() {
        val p1 = engine.getPlayers()[0]
        val p2 = engine.getPlayers()[1]
        p1.hand.clear()
        p1.hand.add(ActionCard("Заколоченная дверь", "", ActionType.AXE))

        engine.playCard(p1, p1.hand[0], p2)

        assertFalse(engine.isAdjacent(p1, p2))
    }

    @Test
    fun `getTargets excludes quarantined players`() {
        val player = engine.getPlayers()[0]
        val target = engine.getPlayers()[1]
        target.hasQuarantine = true

        val targets = engine.getTargets(player, "Огнемёт")

        assertFalse(targets.contains(target))
    }

    @Test
    fun `getPlayableCards excludes infection and defense`() {
        val player = engine.getCurrentPlayer()!!
        player.hand.clear()
        player.hand.add(ActionCard("Заражение!", "", ActionType.INFECTION))
        player.hand.add(DefenseCard("Страх", "", DefenseType.FEAR))
        player.hand.add(ActionCard("Огнемёт", "", ActionType.FLAMETHROWER))

        val playable = engine.getPlayableCards(player)

        assertEquals(1, playable.size)
        assertEquals("Огнемёт", playable[0].name)
    }

    @Test
    fun `endTurn decreases quarantine`() {
        val player = engine.getCurrentPlayer()!!
        player.setQuarantine(3)

        engine.endTurn(player)

        assertEquals(2, player.quarantineTurns)
        assertTrue(player.hasQuarantine)
    }

    @Test
    fun `checkVictory returns null when game continues`() {
        assertNull(engine.checkVictory())
    }

    @Test
    fun `checkVictory returns HUMANS when thing is dead`() {
        val thing = engine.getPlayers().find { it.role == Role.THING }!!
        thing.isAlive = false
        assertEquals("HUMANS", engine.checkVictory())
    }

    @Test
    fun `checkVictory returns THING when all infected`() {
        engine.getPlayers().forEach { if (it.role == Role.HUMAN) it.role = Role.INFECTED }
        assertEquals("THING", engine.checkVictory())
    }

    @Test
    fun `setupGame validates player count`() {
        val engine = GameEngine()
        val result = engine.setupGame(listOf("Анна"))
        assertTrue(result.contains("Ошибка"))
    }

    @Test
    fun `setupGame validates empty names`() {
        val engine = GameEngine()
        val result = engine.setupGame(listOf("Анна", "  ", "Вика", "Глеб"))
        assertTrue(result.contains("Ошибка"))
    }

    @Test
    fun `setupGame validates duplicates`() {
        val engine = GameEngine()
        val result = engine.setupGame(listOf("Анна", "Анна", "Вика", "Глеб"))
        assertTrue(result.contains("Ошибка"))
    }
}
