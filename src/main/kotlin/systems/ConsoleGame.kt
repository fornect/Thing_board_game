package systems

import enums.*
import model.*

class ConsoleGame {
    private val engine = GameEngine()

    fun start() {
        printWelcome()
        val names = askPlayerNames()
        println(engine.setupGame(names))

        while (true) {
            val player = engine.getCurrentPlayer() ?: break
            if (!player.isAlive) {
                engine.endTurn(player)
                continue
            }

            var turnEndedEarly = false

            printTurnHeader(player)
            drawPhase(player)

            if (!engine.getSkipActionPhase()) {
                turnEndedEarly = actionPhase(player)
            }

            if (!turnEndedEarly) {
                exchangePhase(player)
            }

            println(engine.endTurn(player).message)

            if (checkVictory()) break
        }
    }

    private fun printWelcome() {
        println("=".repeat(60))
        println("🎮 НЕЧТО: ИЗ ГЛУБОКОЙ БЕЗДНЫ")
        println("=".repeat(60))
    }

    private fun askPlayerNames(): List<String> {
        while (true) {
            println("\nВведите имена игроков через запятую (4-12 человек):")
            val input = readlnOrNull() ?: ""
            val names = input.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            // 1. Количество
            if (names.size !in 4..12) {
                println("❌ Нужно от 4 до 12 игроков. Вы ввели: ${names.size}")
                continue
            }

            // 2. Пустые имена
            if (names.any { it.isBlank() }) {
                println("❌ Имена не могут быть пустыми")
                continue
            }

            // 3. Дубликаты
            if (names.size != names.distinct().size) {
                println("❌ Имена не должны повторяться")
                continue
            }

            // 4. Длина
            if (names.any { it.length > 20 }) {
                println("❌ Имена не могут быть длиннее 20 символов")
                continue
            }

            return names
        }
    }

    private fun printTurnHeader(player: Player) {
        println("\n${"=".repeat(40)}")
        println("🎯 Ход ${engine.getTurnNumber() + 1}: ${player.name} (${player.role})")
        println("Рука: ${engine.getHandAsString(player)}")
    }

    private fun drawPhase(player: Player) {
        println("\n📤 Фаза 1: Взятие карты (Enter)")
        readlnOrNull()
        val drawResult = engine.drawCard(player)
        println(drawResult.message)
        if (drawResult is GameResult.Panic) {
            println("⚠️ Фаза действия пропущена!")
        }
    }

    private fun actionPhase(player: Player): Boolean {
        var actionDone = false
        var turnEndedEarly = false

        while (!actionDone) {
            println("\n🎮 Фаза 2: Действие")

            if (player.hasQuarantine) {
                actionDone = quarantineAction(player)
            } else {
                println("1 - Сыграть карту")
                println("2 - Сбросить карту")
                print("Выбор: ")
                actionDone =
                    when (readlnOrNull()) {
                        "1" -> {
                            val (done, early) = playCardAction(player)
                            turnEndedEarly = early
                            done
                        }

                        "2" -> discardAction(player)

                        else -> false
                    }
            }
        }

        return turnEndedEarly
    }

    private fun quarantineAction(player: Player): Boolean {
        println("🦠 ВЫ НА КАРАНТИНЕ!")
        println("1 - Сыграть Топор на себя")
        println("2 - Сбросить карту")
        print("Выбор: ")

        return when (readlnOrNull()) {
            "1" -> {
                val axeCard = player.hand.find { it.name == "Топор" }
                if (axeCard != null) {
                    println(engine.playCard(player, axeCard, player).message)
                } else {
                    println("У вас нет Топора!")
                }
                true
            }

            "2" -> discardAction(player)

            else -> false
        }
    }

    private fun playCardAction(player: Player): Pair<Boolean, Boolean> {
        val playable = engine.getPlayableCards(player)
        if (playable.isEmpty()) {
            println("Нет карт!")
            return Pair(false, false)
        }

        playable.forEachIndexed { i, c -> println("$i - ${c.name}") }
        print("Карта: ")
        val ci = readlnOrNull()?.toIntOrNull() ?: 0
        if (ci !in playable.indices) return Pair(false, false)

        val card = playable[ci]
        val target = selectTarget(player, card)

        if (needsTarget(card) && target == null) {
            return Pair(false, false)
        }

        val result = engine.playCard(player, card, target)
        println(result.message)

        if (result is GameResult.ExchangeInfo) {
            handleExchangeResult(result)
            return Pair(true, true)
        }

        return Pair(card.name != "Упорство", false)
    }

    private fun needsTarget(card: Card): Boolean {
        return when (card) {
            is ActionCard ->
                card.subType in
                    listOf(
                        ActionType.FLAMETHROWER,
                        ActionType.ANALYSIS,
                        ActionType.AXE,
                        ActionType.SUSPICION,
                        ActionType.TEMPTATION,
                        ActionType.SWAP_SEATS_NEIGHBOR,
                        ActionType.SWAP_SEATS_ANY,
                    )

            is ObstacleCard ->
                card.subType in
                    listOf(
                        ObstacleType.QUARANTINE,
                        ObstacleType.BARRICADED_DOOR,
                    )

            else -> false
        }
    }

    private fun selectTarget(
        player: Player,
        card: Card,
    ): Player? {
        if (!needsTarget(card)) return null

        val targets = engine.getTargets(player, card.name)
        if (targets.isEmpty()) {
            println("Нет целей!")
            return null
        }

        targets.forEachIndexed { i, t -> println("$i - ${t.name}") }
        print("Цель: ")
        val ti = readlnOrNull()?.toIntOrNull() ?: 0
        return if (ti in targets.indices) targets[ti] else null
    }

    private fun discardAction(player: Player): Boolean {
        val disc = engine.getDiscardableCards(player)
        if (disc.isEmpty()) {
            println("Нет карт!")
            return false
        }

        disc.forEachIndexed { i, c -> println("$i - ${c.name}") }
        print("Карта: ")
        val ci = readlnOrNull()?.toIntOrNull() ?: 0
        if (ci in disc.indices) {
            println(engine.discardCard(player, disc[ci]).message)
        }
        return true
    }

    private fun handleExchangeResult(exResult: GameResult.ExchangeInfo) {
        println("\n😈 СОБЛАЗН! Обмен с ${exResult.neighbor.name}")
        println("Ваши карты:")
        exResult.playerCards.forEachIndexed { i, c -> println("$i - ${c.name}") }
        print("Выберите: ")
        val c1 = readlnOrNull()?.toIntOrNull() ?: 0
        if (c1 !in exResult.playerCards.indices) return

        val receiverDefense =
            exResult.neighbor.getHandCards().find { card ->
                card is DefenseCard && card.subType.category == DefenseCategory.EXCHANGE
            }
        if (receiverDefense != null) {
            println("\n🛡️ У ${exResult.neighbor.name} есть защита: ${receiverDefense.name}")
            println("1 - Принять обмен")
            println("2 - Сыграть защиту (отказаться)")
            print("Выбор за ${exResult.neighbor.name}: ")
            if (readlnOrNull() == "2") {
                val defResult = engine.handleDefense(exResult.neighbor, exResult.player, exResult.playerCards[c1])
                println(defResult.message)
                return
            }
        }

        println("\nКарты ${exResult.neighbor.name}:")
        exResult.neighborCards.forEachIndexed { i, c -> println("$i - ${c.name}") }
        print("Выберите: ")
        val c2 = readlnOrNull()?.toIntOrNull() ?: 0

        if (c1 in exResult.playerCards.indices && c2 in exResult.neighborCards.indices) {
            println(engine.performExchange(exResult.player, exResult.neighbor, exResult.playerCards[c1], exResult.neighborCards[c2]).message)
        }
    }

    private fun exchangePhase(player: Player) {
        if (player.hasQuarantine) {
            println("\n🦠 Обмен пропущен (карантин)")
            return
        }

        println("\n🔄 Фаза 3: Обмен (Enter)")
        readlnOrNull()
        val exResult = engine.executeExchange(player)

        when (exResult) {
            is GameResult.ExchangeInfo -> {
                println("Ваши карты:")
                exResult.playerCards.forEachIndexed { i, c -> println("$i - ${c.name}") }
                print("Выберите: ")
                val c1 = readlnOrNull()?.toIntOrNull() ?: 0

                val receiverDefense =
                    exResult.neighbor.getHandCards().find { card ->
                        card is DefenseCard && card.subType.category == DefenseCategory.EXCHANGE
                    }
                if (receiverDefense != null) {
                    println("\n🛡️ У ${exResult.neighbor.name} есть защита: ${receiverDefense.name}")
                    println("1 - Принять обмен")
                    println("2 - Сыграть защиту")
                    print("Выбор за ${exResult.neighbor.name}: ")
                    if (readlnOrNull() == "2") {
                        val defResult = engine.handleDefense(exResult.neighbor, exResult.player, exResult.playerCards[c1])
                        println(defResult.message)
                        return
                    }
                }

                println("\nКарты ${exResult.neighbor.name}:")
                exResult.neighborCards.forEachIndexed { i, c -> println("$i - ${c.name}") }
                print("Выберите: ")
                val c2 = readlnOrNull()?.toIntOrNull() ?: 0
                if (c1 in exResult.playerCards.indices && c2 in exResult.neighborCards.indices) {
                    println(engine.performExchange(exResult.player, exResult.neighbor, exResult.playerCards[c1], exResult.neighborCards[c2]).message)
                }
            }

            else -> println(exResult.message)
        }
    }

    private fun checkVictory(): Boolean {
        val winner = engine.checkVictory()
        if (winner != null) {
            println("\n${"=".repeat(40)}")
            println(if (winner == "HUMANS") "🎉 ЛЮДИ ПОБЕДИЛИ!" else "👾 НЕЧТО ПОБЕДИЛО!")
            return true
        }
        return false
    }
}
