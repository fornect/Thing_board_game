package systems

import model.*
import enums.*

class ConsoleGame {
    private val engine = GameEngine()

    fun start() {
        println("=".repeat(60))
        println("🎮 НЕЧТО: ИЗ ГЛУБОКОЙ БЕЗДНЫ")
        println("=".repeat(60))

        var names: List<String>
        while (true) {
            println("\nВведите имена игроков через запятую (4-12 человек):")
            val input = readlnOrNull() ?: ""
            names = input.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (names.size in 4..12) break
            println("❌ Ошибка! Нужно от 4 до 12 игроков. Вы ввели: ${names.size}")
        }

        println(engine.setupGame(names))

        while (true) {
            val player = engine.getCurrentPlayer() ?: break
            if (!player.isAlive) { engine.endTurn(player); continue }

            println("\n${"=".repeat(40)}")
            println("🎯 Ход ${engine.getTurnNumber() + 1}: ${player.name} (${player.role})")
            println("Рука: ${player.hand.joinToString { it.name }}")

            println("\n📤 Фаза 1: Взятие карты (Enter)")
            readlnOrNull()
            val drawResult = engine.drawCard(player)
            println(drawResult.message)
            if (drawResult is GameEngine.GameResult.Panic) println("⚠️ Фаза действия пропущена!")

            if (!engine.getSkipActionPhase()) {
                var actionDone = false
                while (!actionDone) {
                    println("\n🎮 Фаза 2: Действие")

                    if (player.hasQuarantine) {
                        println("🦠 ВЫ НА КАРАНТИНЕ!")
                        println("1 - Сыграть Топор на себя")
                        println("2 - Сбросить карту")
                        print("Выбор: ")
                        when (readlnOrNull()) {
                            "1" -> {
                                val axeCard = player.hand.find { it.name == "Топор" }
                                if (axeCard != null) println(engine.playCard(player, axeCard, player).message)
                                else println("У вас нет Топора!")
                                actionDone = true
                            }
                            "2" -> {
                                val disc = engine.getDiscardableCards(player)
                                if (disc.isEmpty()) { println("Нет карт!"); continue }
                                disc.forEachIndexed { i, c -> println("$i - ${c.name}") }
                                print("Карта: ")
                                val ci = readlnOrNull()?.toIntOrNull() ?: 0
                                if (ci in disc.indices) println(engine.discardCard(player, disc[ci]).message)
                                actionDone = true
                            }
                        }
                    } else {
                        println("1 - Сыграть карту")
                        println("2 - Сбросить карту")
                        print("Выбор: ")
                        when (readlnOrNull()) {
                            "1" -> {
                                val playable = engine.getPlayableCards(player)
                                if (playable.isEmpty()) { println("Нет карт!"); continue }
                                playable.forEachIndexed { i, c -> println("$i - ${c.name}") }
                                print("Карта: ")
                                val ci = readlnOrNull()?.toIntOrNull() ?: 0
                                if (ci !in playable.indices) continue
                                val card = playable[ci]

                                var target: Player? = null
                                if (card.name in listOf("Огнемёт", "Анализ", "Топор", "Подозрение", "Карантин", "Заколоченная дверь", "Меняемся местами!", "Сматывай удочки!", "Соблазн")) {
                                    val targets = engine.getTargets(player, card.name)
                                    if (targets.isEmpty()) { println("Нет целей!"); continue }
                                    targets.forEachIndexed { i, t -> println("$i - ${t.name}") }
                                    print("Цель: ")
                                    val ti = readlnOrNull()?.toIntOrNull() ?: 0
                                    if (ti !in targets.indices) continue
                                    target = targets[ti]
                                }

                                val result = engine.playCard(player, card, target)
                                println(result.message)

                                if (result is GameEngine.GameResult.ExchangeInfo) {
                                    println("\n😈 СОБЛАЗН! Обмен с ${result.neighbor.name}")
                                    println("Ваши карты:")
                                    result.playerCards.forEachIndexed { i, c -> println("$i - ${c.name}") }
                                    print("Выберите: ")
                                    val c1 = readlnOrNull()?.toIntOrNull() ?: 0
                                    println("\nКарты ${result.neighbor.name}:")
                                    result.neighborCards.forEachIndexed { i, c -> println("$i - ${c.name}") }
                                    print("Выберите: ")
                                    val c2 = readlnOrNull()?.toIntOrNull() ?: 0
                                    if (c1 in result.playerCards.indices && c2 in result.neighborCards.indices) {
                                        println(engine.performExchange(result.player, result.neighbor, result.playerCards[c1], result.neighborCards[c2]).message)
                                    }
                                }

                                if (card.name != "Упорство") actionDone = true
                            }
                            "2" -> {
                                val disc = engine.getDiscardableCards(player)
                                if (disc.isEmpty()) { println("Нет карт!"); continue }
                                disc.forEachIndexed { i, c -> println("$i - ${c.name}") }
                                print("Карта: ")
                                val ci = readlnOrNull()?.toIntOrNull() ?: 0
                                if (ci in disc.indices) println(engine.discardCard(player, disc[ci]).message)
                                actionDone = true
                            }
                        }
                    }
                }
            }

            if (!player.hasQuarantine) {
                println("\n🔄 Фаза 3: Обмен (Enter)")
                readlnOrNull()
                val exResult = engine.executeExchange(player)
                when (exResult) {
                    is GameEngine.GameResult.ExchangeInfo -> {
                        println("Ваши карты:")
                        exResult.playerCards.forEachIndexed { i, c -> println("$i - ${c.name}") }
                        print("Выберите: ")
                        val c1 = readlnOrNull()?.toIntOrNull() ?: 0

                        // Проверка защиты
                        val defenseCards = listOf("Страх", "Нет уж, спасибо!", "Мимо!", "Мне и здесь неплохо")
                        val receiverDefense = exResult.neighbor.hand.find { it.name in defenseCards }

                        if (receiverDefense != null) {
                            println("\n🛡️ У ${exResult.neighbor.name} есть защита: ${receiverDefense.name}")
                            println("1 - Принять обмен")
                            println("2 - Сыграть защиту")
                            print("Выбор за ${exResult.neighbor.name}: ")
                            if (readlnOrNull() == "2") {
                                val defResult = engine.handleDefense(exResult.neighbor, exResult.player, exResult.playerCards[c1])
                                println(defResult.message)
                                println(engine.endTurn(player).message)
                                continue
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
            } else {
                println("\n🦠 Обмен пропущен (карантин)")
            }

            println(engine.endTurn(player).message)

            val winner = engine.checkVictory()
            if (winner != null) {
                println("\n${"=".repeat(40)}")
                println(if (winner == "HUMANS") "🎉 ЛЮДИ ПОБЕДИЛИ!" else "👾 НЕЧТО ПОБЕДИЛО!")
                break
            }
        }
    }
}

fun main() { ConsoleGame().start() }