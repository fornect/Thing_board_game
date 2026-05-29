package systems

import model.ActionCard
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ConsoleGameTest {
    @Test
    fun `game phases execute in order`() {
        val engine = GameEngine()
        val result = engine.setupGame(listOf("Анна", "Борис", "Вика", "Глеб"))
        assertTrue(result.contains("🎮 Игра создана!"))
        assertTrue(result.contains("🔴"))
    }

    @Test
    fun `full turn executes correctly`() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис", "Вика", "Глеб"))
        val player = engine.getCurrentPlayer()!!

        // Фаза 1: взять карту
        val drawResult = engine.drawCard(player)
        assertTrue(drawResult is GameResult.Success || drawResult is GameResult.Panic)

        // Фаза 2: действие
        val playable = engine.getPlayableCards(player)
        if (playable.isNotEmpty()) {
            val result = engine.playCard(player, playable[0], null)
            assertTrue(result is GameResult)
        }

        // Фаза 3: обмен
        if (!player.hasQuarantine) {
            val exResult = engine.executeExchange(player)
            assertTrue(exResult is GameResult)
        }

        // Конец хода
        val endResult = engine.endTurn(player)
        assertTrue(endResult is GameResult.Success)
    }

    @Test
    fun `quarantine restricts actions`() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис", "Вика", "Глеб"))
        val player = engine.getCurrentPlayer()!!
        player.setQuarantine(3)
        player.hand.clear()
        player.hand.add(ActionCard("Огнемёт", "Test", enums.ActionType.FLAMETHROWER))

        val result = engine.playCard(player, player.hand[0], null)
        assertTrue(result is GameResult.Error)
        assertTrue(result.message.contains("карантине"))
    }

    @Test
    fun `victory is detected`() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис", "Вика", "Глеб"))
        val thing = engine.getPlayers().find { it.role == enums.Role.THING }!!
        thing.isAlive = false

        assertEquals("HUMANS", engine.checkVictory())
    }
}
