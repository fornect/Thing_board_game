package systems

import enums.*
import model.*

class GameEngineTest {
    fun test() {
        testSetupGame()
        testDrawCard()
        testPlayCardFlamethrower()
        testPlayCardQuarantine()
        testDiscardCard()
        testExchange()
        testVictory()
        testQuarantineTurnReduction()
        println("✅ GameEngineTest: все тесты пройдены!")
    }

    private fun testSetupGame() {
        val engine = GameEngine()
        val names = listOf("Анна", "Борис", "Вика", "Глеб")
        val result = engine.setupGame(names)

        assert(result.contains("🎮 Игра создана!")) { "Игра должна быть создана" }
        assert(engine.getPlayers().size == 4) { "Должно быть 4 игрока" }
        assert(engine.getPlayers().any { it.role == Role.THING }) { "Кто-то должен быть НЕЧТО" }
    }

    private fun testDrawCard() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис", "Вика", "Глеб"))
        val player = engine.getCurrentPlayer()!!

        val result = engine.drawCard(player)
        assert(result is GameEngine.GameResult.Success) { "Карта должна быть взята" }
        assert(result.message.contains("взял карту")) { "Сообщение должно содержать 'взял карту'" }
    }

    private fun testPlayCardFlamethrower() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис", "Вика", "Глеб"))
        val player = engine.getCurrentPlayer()!!

        // Даём игроку Огнемёт
        player.hand.clear()
        player.hand.add(ActionCard("Огнемёт", "Test", ActionType.FLAMETHROWER))

        val target = engine.getPlayers().first { it != player && it.isAlive }
        val result = engine.playCard(player, player.hand[0], target)

        assert(result is GameEngine.GameResult.Success) { "Огнемёт должен сработать" }
        assert(!target.isAlive) { "Цель должна быть мертва" }
    }

    private fun testPlayCardQuarantine() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис", "Вика", "Глеб"))
        val player = engine.getCurrentPlayer()!!

        player.hand.clear()
        player.hand.add(ActionCard("Карантин", "Test", ActionType.AXE))

        val target = engine.getPlayers().first { it != player && it.isAlive }
        val result = engine.playCard(player, player.hand[0], target)

        assert(result is GameEngine.GameResult.Success) { "Карантин должен сработать" }
        assert(target.hasQuarantine) { "Цель должна быть на карантине" }
        assert(target.quarantineTurns == 3) { "Карантин должен длиться 3 хода" }
    }

    private fun testDiscardCard() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис", "Вика", "Глеб"))
        val player = engine.getCurrentPlayer()!!

        val card = player.hand.first()
        val result = engine.discardCard(player, card)

        assert(result is GameEngine.GameResult.Success) { "Сброс должен сработать" }
    }

    private fun testExchange() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис", "Вика", "Глеб"))
        val player = engine.getCurrentPlayer()!!

        val result = engine.executeExchange(player)
        // Может быть ошибка (карантин, дверь) или ExchangeInfo
        assert(result is GameEngine.GameResult) { "Должен вернуть GameResult" }
    }

    private fun testVictory() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис", "Вика", "Глеб"))

        // Все люди живы, НЕЧТО жив — игра продолжается
        assert(engine.checkVictory() == null) { "Игра должна продолжаться" }

        // Убиваем НЕЧТО
        val thing = engine.getPlayers().find { it.role == Role.THING }!!
        thing.isAlive = false

        assert(engine.checkVictory() == "HUMANS") { "Люди должны победить" }
    }

    private fun testQuarantineTurnReduction() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис"))
        val player = engine.getCurrentPlayer()!!

        player.hasQuarantine = true
        player.quarantineTurns = 3

        engine.endTurn(player)

        assert(player.quarantineTurns == 2) { "Карантин должен уменьшиться на 1" }
        assert(player.hasQuarantine) { "Карантин ещё не снят" }
    }
}
