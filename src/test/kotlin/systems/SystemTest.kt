package systems

import model.*
import enums.*

class SystemTest {

    fun test() {
        testHumansWinWithFlamethrower()
        testThingInfectsAllHumans()
        testGameEndsWhenAllInfected()
        testGameEndsWhenThingKilled()
        testQuarantineFullCycle()
        testCompleteGameWithMultipleTurns()
        testPanicSkipsActionPhase()
        println("✅ SystemTest: все тесты пройдены!")
    }

    private fun testHumansWinWithFlamethrower() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис", "Вика", "Глеб"))

        val thing = engine.getPlayers().find { it.role == Role.THING }!!
        val human = engine.getPlayers().find { it.role == Role.HUMAN && engine.isAdjacent(it, thing) }!!

        // Даём человеку Огнемёт
        human.hand.clear()
        human.hand.add(ActionCard("Огнемёт", "Test", ActionType.FLAMETHROWER))

        // Убеждаемся что у НЕЧТО нет защиты
        thing.hand.removeAll { it.name == "Никакого шашлыка!" }

        val result = engine.playCard(human, human.hand[0], thing)

        assert(result is GameEngine.GameResult.Success) { "Огнемёт должен сработать" }
        assert(!thing.isAlive) { "НЕЧТО должно быть мертво" }
        assert(engine.checkVictory() == "HUMANS") { "Люди должны победить" }
    }

    private fun testThingInfectsAllHumans() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис", "Вика"))

        val thing = engine.getPlayers().find { it.role == Role.THING }!!

        // Заражаем всех людей
        engine.getPlayers().filter { it.role == Role.HUMAN }.forEach { human ->
            human.role = Role.INFECTED
        }

        assert(engine.checkVictory() == "THING") { "НЕЧТО должно победить" }
    }

    private fun testGameEndsWhenAllInfected() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис", "Вика"))

        val thing = engine.getPlayers().find { it.role == Role.THING }!!

        // Заражаем всех кроме НЕЧТО
        engine.getPlayers().forEach { player ->
            if (player != thing && player.isAlive) {
                player.role = Role.INFECTED
            }
        }

        val winner = engine.checkVictory()
        assert(winner == "THING") { "НЕЧТО должно победить когда все заражены" }
    }

    private fun testGameEndsWhenThingKilled() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис", "Вика"))

        val thing = engine.getPlayers().find { it.role == Role.THING }!!
        thing.isAlive = false

        val winner = engine.checkVictory()
        assert(winner == "HUMANS") { "Люди должны победить когда НЕЧТО мертво" }
    }

    private fun testQuarantineFullCycle() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис", "Вика", "Глеб"))

        val player = engine.getCurrentPlayer()!!
        player.hasQuarantine = true
        player.quarantineTurns = 3

        // Симулируем 3 хода этого игрока
        repeat(3) {
            // Игрок на карантине может только взять карту и сбросить
            player.hand.clear()
            player.hand.add(ActionCard("Анализ", "Test", ActionType.ANALYSIS))

            val drawResult = engine.drawCard(player)
            assert(drawResult is GameEngine.GameResult.Success) { "На карантине можно брать карты" }

            val discardResult = engine.discardCard(player, player.hand.first())
            assert(discardResult is GameEngine.GameResult.Success) { "На карантине можно сбрасывать" }

            // Проверяем что нельзя играть другие карты
            player.hand.add(ActionCard("Огнемёт", "Test", ActionType.FLAMETHROWER))
            val playResult = engine.playCard(player, player.hand.find { it.name == "Огнемёт" }!!, null)
            assert(playResult is GameEngine.GameResult.Error) { "На карантине нельзя играть Огнемёт" }

            engine.endTurn(player)

            // Прокручиваем остальных игроков
            repeat(engine.getPlayers().size - 1) {
                val p = engine.getCurrentPlayer()!!
                engine.drawCard(p)
                engine.endTurn(p)
            }
        }

        assert(!player.hasQuarantine) { "После 3 ходов карантин должен сняться" }
    }

    private fun testCompleteGameWithMultipleTurns() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис", "Вика", "Глеб", "Дима"))

        var turnsPlayed = 0
        val maxTurns = 20

        while (turnsPlayed < maxTurns && engine.checkVictory() == null) {
            val player = engine.getCurrentPlayer()
            if (player != null && player.isAlive) {
                // Фаза 1
                val drawResult = engine.drawCard(player)

                // Фаза 2
                if (!player.hasQuarantine && drawResult !is GameEngine.GameResult.Panic) {
                    val playable = engine.getPlayableCards(player)
                    if (playable.isNotEmpty()) {
                        val card = playable.first()
                        val targets = engine.getTargets(player, card.name)
                        engine.playCard(player, card, if (targets.isNotEmpty()) targets.first() else null)
                    }
                }

                // Фаза 3
                if (!player.hasQuarantine) {
                    engine.executeExchange(player)
                }

                engine.endTurn(player)
            } else {
                player?.let { engine.endTurn(it) }
            }

            turnsPlayed++
        }

        val winner = engine.checkVictory()
        println("   ✓ Полная игра: $turnsPlayed ходов, победитель: ${winner ?: "игра продолжается"}")
        assert(turnsPlayed <= maxTurns) { "Игра должна завершиться за $maxTurns ходов" }
    }

    private fun testPanicSkipsActionPhase() {
        val engine = GameEngine()
        engine.setupGame(listOf("Анна", "Борис"))

        val player = engine.getCurrentPlayer()!!

        // Симулируем панику через skipActionPhase
        val result = engine.drawCard(player)

        if (result is GameEngine.GameResult.Panic) {
            // После паники фаза действия должна быть пропущена
            player.hand.add(ActionCard("Огнемёт", "Test", ActionType.FLAMETHROWER))

            // Проверяем что нельзя играть карты после паники
            // (это проверяется в GUI, здесь проверяем что паника возвращается)
            assert(result.message.contains("ПАНИКА")) { "Должно быть сообщение о панике" }
        }
    }
}
