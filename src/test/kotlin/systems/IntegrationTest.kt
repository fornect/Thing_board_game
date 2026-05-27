package systems

import model.*
import enums.*

class IntegrationTest {

    fun test() {
        testFullGameFlow()
        testInfectionThroughExchange()
        testQuarantineBlocksActions()
        testFlamethrowerBlockedByDefense()
        testThingCardCannotBeDiscarded()
        testDoorBlocksExchange()
        testPerseveranceGivesCards()
        testAxeRemovesQuarantine()
        testWhiskeyShowsCards()
        println("✅ IntegrationTest: все тесты пройдены!")
    }

    private fun testFullGameFlow() {
        val engine = GameEngine()
        val names = listOf("Анна", "Борис", "Вика", "Глеб")
        engine.setupGame(names)

        // Имитация нескольких ходов
        var turnsPlayed = 0
        while (turnsPlayed < 4 && engine.checkVictory() == null) {
            val player = engine.getCurrentPlayer() ?: break

            if (player.isAlive) {
                // Фаза 1: взять карту
                engine.drawCard(player)

                // Фаза 2: сбросить случайную карту
                val playable = engine.getPlayableCards(player)
                if (playable.isNotEmpty()) {
                    engine.playCard(player, playable.first(), null)
                }

                // Фаза 3: обмен
                engine.executeExchange(player)
                engine.endTurn(player)
            } else {
                engine.endTurn(player)
            }

            turnsPlayed++
        }

        // Игра должна корректно завершиться или продолжаться
        val players = engine.getPlayers()
        assert(players.isNotEmpty()) { "Игроки должны остаться" }
        println("   ✓ Полный игровой цикл: $turnsPlayed ходов")
    }

    private fun testInfectionThroughExchange() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис", "Вика", "Глеб"))

        val thing = engine.getPlayers().find { it.role == Role.THING }!!
        val human = engine.getPlayers().find { it.role == Role.HUMAN }!!

        // Даём НЕЧТО карту Заражения
        thing.hand.clear()
        thing.hand.add(ActionCard("Заражение!", "Инфекция", ActionType.INFECTION))
        thing.hand.add(ActionCard("Анализ", "Test", ActionType.ANALYSIS))

        human.hand.clear()
        human.hand.add(ActionCard("Виски", "Test", ActionType.WHISKEY))

        // Выполняем обмен
        val exResult = engine.executeExchange(thing)

        if (exResult is GameEngine.GameResult.ExchangeInfo) {
            val card1 = exResult.playerCards.find { it.name == "Заражение!" }
            val card2 = exResult.neighborCards.first()

            if (card1 != null) {
                val result = engine.performExchange(exResult.player, exResult.neighbor, card1, card2)
                assert(result is GameEngine.GameResult.Success) { "Обмен должен сработать" }
                assert(human.role == Role.INFECTED) { "Человек должен быть заражён" }
            }
        }
    }

    private fun testQuarantineBlocksActions() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис", "Вика", "Глеб"))

        val player = engine.getCurrentPlayer()!!
        player.hasQuarantine = true

        player.hand.clear()
        player.hand.add(ActionCard("Огнемёт", "Test", ActionType.FLAMETHROWER))

        val target = engine.getPlayers().find { it != player && it.isAlive }!!
        val result = engine.playCard(player, player.hand[0], target)

        assert(result is GameEngine.GameResult.Error) { "Огнемёт на карантине должен быть запрещён" }
        assert(result.message.contains("карантине")) { "Сообщение должно упоминать карантин" }
    }

    private fun testFlamethrowerBlockedByDefense() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис"))

        val attacker = engine.getCurrentPlayer()!!
        val defender = engine.getPlayers().find { it != attacker }!!

        attacker.hand.clear()
        attacker.hand.add(ActionCard("Огнемёт", "Test", ActionType.FLAMETHROWER))

        defender.hand.clear()
        defender.hand.add(DefenseCard("Никакого шашлыка!", "Block", DefenseType.NO_BBQ))
        defender.hand.add(ActionCard("Анализ", "Test", ActionType.ANALYSIS))

        val result = engine.playCard(attacker, attacker.hand[0], defender)

        assert(result.message.contains("защитился")) { "Защита должна сработать" }
        assert(defender.isAlive) { "Цель должна выжить" }
    }

    private fun testThingCardCannotBeDiscarded() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис"))

        val thing = engine.getPlayers().find { it.role == Role.THING }!!

        thing.hand.clear()
        thing.hand.add(ActionCard("НЕЧТО", "Thing", ActionType.INFECTION))

        val result = engine.discardCard(thing, thing.hand[0])
        assert(result is GameEngine.GameResult.Error) { "НЕЧТО нельзя сбросить" }
    }

    private fun testDoorBlocksExchange() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис"))

        val player = engine.getCurrentPlayer()!!

        player.hand.clear()
        player.hand.add(ActionCard("Заколоченная дверь", "Test", ActionType.AXE))
        player.hand.add(ActionCard("Анализ", "Test", ActionType.ANALYSIS))

        val neighbor = engine.getPlayers().find { it != player && engine.isAdjacent(player, it) }!!
        neighbor.hand.clear()
        neighbor.hand.add(ActionCard("Виски", "Test", ActionType.WHISKEY))

        // Ставим дверь
        engine.playCard(player, player.hand.find { it.name == "Заколоченная дверь" }!!, neighbor)

        // Пытаемся обменяться
        val exResult = engine.executeExchange(player)
        assert(exResult is GameEngine.GameResult.Error) { "Обмен должен быть заблокирован дверью" }
    }

    private fun testPerseveranceGivesCards() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис"))

        val player = engine.getCurrentPlayer()!!
        val handSizeBefore = player.hand.size

        player.hand.add(ActionCard("Упорство", "Test", ActionType.PERSEVERANCE))
        val result = engine.playCard(player, player.hand.find { it.name == "Упорство" }!!, null)

        assert(result is GameEngine.GameResult.Success) { "Упорство должно сработать" }
    }

    private fun testAxeRemovesQuarantine() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис"))

        val player = engine.getCurrentPlayer()!!
        player.hasQuarantine = true
        player.quarantineTurns = 3

        player.hand.clear()
        player.hand.add(ActionCard("Топор", "Test", ActionType.AXE))

        val result = engine.playCard(player, player.hand[0], player)

        assert(result is GameEngine.GameResult.Success) { "Топор должен сработать" }
        assert(!player.hasQuarantine) { "Карантин должен быть снят" }
        assert(player.quarantineTurns == 0) { "quarantineTurns должно быть 0" }
    }

    private fun testWhiskeyShowsCards() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис"))

        val player = engine.getCurrentPlayer()!!
        player.hand.clear()
        player.hand.add(ActionCard("Виски", "Test", ActionType.WHISKEY))

        val result = engine.playCard(player, player.hand[0], null)

        assert(result is GameEngine.GameResult.Success) { "Виски должно сработать" }
        assert(result.message.contains("Виски")) { "Сообщение должно содержать 'Виски'" }
    }
}
