package systems

import enums.*
import model.*
import kotlin.random.Random

class GameEngine {
    private val players = mutableListOf<Player>()
    private val deck = Deck()
    private val discardPile = DiscardPile()
    private var currentPlayerIndex = 0
    private var direction = 1
    private var turnNumber = 0
    private var skipActionPhase = false
    private var endTurnEarlyFlag = false
    private val barricadedDoors = mutableListOf<Pair<Player, Player>>()

    sealed class GameResult {
        abstract val message: String

        data class Success(override val message: String, val affectedPlayers: List<Player> = emptyList()) : GameResult()

        data class Error(override val message: String) : GameResult()

        data class Panic(override val message: String) : GameResult()

        data class DefensePlayed(override val message: String, val drawnCard: Card?) : GameResult()

        data class ExchangeInfo(
            val player: Player,
            val neighbor: Player,
            val playerCards: List<Card>,
            val neighborCards: List<Card>,
        ) : GameResult() {
            override val message: String get() = "Выберите карты для обмена"
        }

        data class PanicExchange(
            override val message: String,
            val player: Player,
            val availableCards: List<Card>,
            val availableTargets: List<Player>,
            val panicType: PanicType,
        ) : GameResult()
    }

    // ==================== НАСТРОЙКА ====================

    fun setupGame(playerNames: List<String>): String {
        val log = StringBuilder()
        players.clear()
        deck.clear()
        discardPile.clear()
        barricadedDoors.clear()

        playerNames.forEach { players.add(Player(it)) }

        // ==================== КАРТЫ ДЕЙСТВИЙ (46 штук) ====================
        val actionCards =
            listOf(
                ActionCard("Огнемёт", "Убить соседнего игрока", ActionType.FLAMETHROWER),
                ActionCard("Анализ", "Посмотреть руку соседа", ActionType.ANALYSIS),
                ActionCard("Топор", "Снять карантин или дверь", ActionType.AXE),
                ActionCard("Подозрение", "Взять карту соседа", ActionType.SUSPICION),
                ActionCard("Виски", "Показать всем свои карты", ActionType.WHISKEY),
                ActionCard("Упорство", "Взять 3 карты, оставить 1", ActionType.PERSEVERANCE),
                ActionCard("Гляди по сторонам", "Сменить направление хода", ActionType.LOOK_AROUND),
                ActionCard("Меняемся местами!", "Поменяться местами с соседом", ActionType.SWAP_SEATS_NEIGHBOR),
                ActionCard("Сматывай удочки!", "Поменяться местами с любым", ActionType.SWAP_SEATS_ANY),
                ActionCard("Соблазн", "Обмен картой, ход заканчивается", ActionType.TEMPTATION),
            )

        // Добавляем действия в нужном количестве
        repeat(6) { deck.add(actionCards[0].copy()) } // Огнемёт ×6
        repeat(4) { deck.add(actionCards[1].copy()) } // Анализ ×4
        repeat(4) { deck.add(actionCards[2].copy()) } // Топор ×4
        repeat(6) { deck.add(actionCards[3].copy()) } // Подозрение ×6
        repeat(3) { deck.add(actionCards[4].copy()) } // Виски ×3
        repeat(5) { deck.add(actionCards[5].copy()) } // Упорство ×5
        repeat(3) { deck.add(actionCards[6].copy()) } // Гляди по сторонам ×3
        repeat(5) { deck.add(actionCards[7].copy()) } // Меняемся местами ×5
        repeat(4) { deck.add(actionCards[8].copy()) } // Сматывай удочки ×4
        repeat(6) { deck.add(actionCards[9].copy()) } // Соблазн ×6
        // Всего действий: 6+4+4+6+3+5+3+5+4+6 = 46 ✓

        // ==================== КАРТЫ ЗАЩИТЫ (17 штук) ====================
        val defenseCards =
            listOf(
                DefenseCard("Мне и здесь неплохо", "Отменить смену мест", DefenseType.IM_FINE_HERE),
                DefenseCard("Нет уж, спасибо!", "Отказ от обмена", DefenseType.NO_THANKS),
                DefenseCard("Мимо!", "Обмен переходит дальше", DefenseType.PASS),
                DefenseCard("Никакого шашлыка!", "Отменить огнемёт", DefenseType.NO_BBQ),
                DefenseCard("Страх", "Отказ от обмена + просмотр карты", DefenseType.FEAR),
            )

        repeat(3) { deck.add(defenseCards[0].copy()) } // Мне и здесь неплохо ×3
        repeat(4) { deck.add(defenseCards[1].copy()) } // Нет уж, спасибо! ×4
        repeat(4) { deck.add(defenseCards[2].copy()) } // Мимо! ×4
        repeat(3) { deck.add(defenseCards[3].copy()) } // Никакого шашлыка! ×3
        repeat(3) { deck.add(defenseCards[4].copy()) } // Страх ×3
        // Всего защит: 3+4+4+3+3 = 17 ✓

        // ==================== КАРТЫ ПРЕПЯТСТВИЙ (5 штук) ====================
        val obstacleCards =
            listOf(
                ObstacleCard("Заколоченная дверь", "Блок между игроками", ObstacleType.BARRICADED_DOOR),
                ObstacleCard("Карантин", "Блокирует игрока на 3 хода", ObstacleType.QUARANTINE),
            )

        repeat(3) { deck.add(obstacleCards[0].copy()) } // Заколоченная дверь ×3
        repeat(2) { deck.add(obstacleCards[1].copy()) } // Карантин ×2
        // Всего препятствий: 3+2 = 5 ✓

        // ==================== КАРТА НЕЧТО (1 штука) ====================
        val thingCard = ActionCard("НЕЧТО", "Вы - НЕЧТО! Нельзя сбросить или передать!", ActionType.INFECTION)

        deck.shuffle()

        // Раздаём по 4 карты каждому игроку
        val totalNeeded = players.size * 4
        val cardsForDeal = mutableListOf<Card>()
        repeat(totalNeeded) { deck.draw()?.let { cardsForDeal.add(it) } }

        // Заменяем одну случайную карту на НЕЧТО
        val thingPos = Random.nextInt(cardsForDeal.size)
        cardsForDeal[thingPos] = thingCard.copy()

        // Перемешиваем карты для раздачи
        cardsForDeal.shuffle()

        // Раздаём по 4 карты каждому
        var cardIndex = 0
        players.forEach { player ->
            repeat(4) {
                if (cardIndex < cardsForDeal.size) {
                    player.hand.add(cardsForDeal[cardIndex])
                    cardIndex++
                }
            }
        }

        // Назначаем НЕЧТО
        players.forEach { it.role = Role.HUMAN }
        val thingPlayer = players.find { it.hand.any { c -> c.name == "НЕЧТО" } }
        thingPlayer?.role = Role.THING

        // ==================== ОСНОВНАЯ КОЛОДА ====================
        // Добавляем оставшиеся карты из начальной колоды (уже в deck)

        // Карты заражения (20 штук)
        repeat(20) { deck.add(ActionCard("Заражение!", "Заразить при обмене", ActionType.INFECTION).copy()) }

        // Карты паник (24 штуки = 12 × 2)
        val panicCards =
            listOf(
                PanicCard("Старые веревки", "Все карантины сбрасываются", PanicType.OLD_ROPE),
                PanicCard("Свидание в слепую", "Обмен карты с колодой", PanicType.BLIND_DATE),
                PanicCard("Раз, два...", "Обмен с третьим игроком", PanicType.ONE_TWO),
                PanicCard("Давай дружить?", "Обмен картами с любым", PanicType.LET_BE_FRIENDS),
                PanicCard("Только между нами", "Показать карты соседу", PanicType.JUST_BETWEEN_US),
                PanicCard("Без паники!", "Ничего не происходит", PanicType.NO_PANIC),
                PanicCard("Цепная реакция", "Все передают карту", PanicType.CHAIN_REACTION),
                PanicCard("...Три, четыре...", "Все двери сбрасываются", PanicType.THREE_FOUR),
                PanicCard("И вы это называете вечеринкой", "Сброс всего + обмен местами", PanicType.CALL_THAT_PARTY),
                PanicCard("Убирайся прочь!", "Обмен местами с любым", PanicType.GET_OUT),
                PanicCard("Забывчивость", "Сбросить 3, взять 3", PanicType.FORGETFULNESS),
                PanicCard("Уупс!", "Показать все свои карты", PanicType.OOPS),
            )

        repeat(2) { panicCards.forEach { deck.add(it.copy()) } }

        deck.shuffle()

        currentPlayerIndex = 0
        turnNumber = 0
        direction = 1
        skipActionPhase = false
        endTurnEarlyFlag = false

        log.append("🎮 Игра создана!\n")
        log.append("👥 Игроки: ${playerNames.joinToString(", ")}\n")
        thingPlayer?.let { log.append("🔴 ${it.name} - НЕЧТО!\n") }

        return log.toString()
    }

    // ==================== ДЕЙСТВИЯ ====================

    fun drawCard(player: Player): GameResult {
        if (deck.isEmpty()) {
            if (discardPile.isNotEmpty()) {
                deck.addAll(discardPile.takeAll())
                deck.shuffle()
            } else {
                return GameResult.Error("Колода пуста!")
            }
        }

        val card = deck.draw() ?: return GameResult.Error("Не удалось взять карту")

        if (card.type == CardType.PANIC && card is PanicCard) {
            skipActionPhase = true

            // Выполняем действие паники
            val panicResult = executePanic(card, player)
            discardPile.add(card)
            return panicResult
        }
        player.hand.add(card)

        return GameResult.Success("📤 ${player.name} взял карту: ${card.name}")
    }

    // Добавить новый метод:
    private fun executePanic(
        card: PanicCard,
        player: Player,
    ): GameResult {
        return when (card.subType) {
            PanicType.OLD_ROPE -> {
                players.forEach {
                    it.hasQuarantine = false
                    it.quarantineTurns = 0
                }
                GameResult.Panic("😱 ПАНИКА! Старые веревки — все карантины сброшены!")
            }

            PanicType.BLIND_DATE -> {
                endTurnEarlyFlag = true
                GameResult.Panic("😱 ПАНИКА! Свидание в слепую — случайная карта заменена!")
            }

            PanicType.ONE_TWO -> {
                val alive = getAlivePlayers()
                val idx = alive.indexOf(player)
                if (idx != -1 && alive.size >= 4) {
                    val third = alive[(idx + 3) % alive.size]
                    if (!third.hasQuarantine) {
                        return GameResult.PanicExchange(
                            "😱 ПАНИКА! Раз, два... — выберите карту для обмена с ${third.name}",
                            player,
                            player.hand.filter { it.name != "НЕЧТО" },
                            listOf(third),
                            PanicType.ONE_TWO,
                        )
                    }
                }
                GameResult.Panic("😱 ПАНИКА! Раз, два... — нет доступных игроков")
            }

            PanicType.LET_BE_FRIENDS -> {
                val targets = getAlivePlayers().filter { it != player && !it.hasQuarantine }
                if (targets.isNotEmpty()) {
                    return GameResult.PanicExchange(
                        "😱 ПАНИКА! Давай дружить? — выберите игрока и карту для обмена",
                        player,
                        player.hand.filter { it.name != "НЕЧТО" },
                        targets,
                        PanicType.LET_BE_FRIENDS,
                    )
                }
                GameResult.Panic("😱 ПАНИКА! Давай дружить? — нет доступных игроков")
            }

            PanicType.JUST_BETWEEN_US -> {
                GameResult.Panic("😱 ПАНИКА! Только между нами — карты показаны соседу!")
            }

            PanicType.NO_PANIC -> {
                skipActionPhase = false
                GameResult.Panic("😌 Без паники! — ничего не происходит")
            }

            PanicType.CHAIN_REACTION -> {
                val alive = getAlivePlayers()
                val cards =
                    alive.map { p ->
                        val av = p.hand.filter { it.name != "НЕЧТО" && it.name != "Заражение!" }
                        if (av.isNotEmpty()) {
                            val c = av.random()
                            p.hand.remove(c)
                            c
                        } else {
                            null
                        }
                    }
                for (i in alive.indices) {
                    cards[i]?.let { alive[(i + 1) % alive.size].hand.add(it) }
                }
                GameResult.Panic("😱 ПАНИКА! Цепная реакция — все передали карту!")
            }

            PanicType.THREE_FOUR -> {
                barricadedDoors.clear()
                GameResult.Panic("😱 ПАНИКА! ...Три, четыре... — все двери сброшены!")
            }

            PanicType.CALL_THAT_PARTY -> {
                players.forEach {
                    it.hasQuarantine = false
                    it.quarantineTurns = 0
                }
                barricadedDoors.clear()

                // Меняем игроков попарно, начиная с того кто вытянул панику
                val alive = getAlivePlayers()
                if (alive.size >= 2) {
                    val startIdx = alive.indexOf(player)
                    // Собираем пары: (0,1), (2,3), (4,5)...
                    var i = 0
                    while (i + 1 < alive.size) {
                        val p1 = alive[(startIdx + i) % alive.size]
                        val p2 = alive[(startIdx + i + 1) % alive.size]
                        swapPlayers(p1, p2)
                        i += 2
                    }
                    // Если нечётное количество — последний остаётся на месте
                }

                GameResult.Panic("😱 ПАНИКА! И вы это называете вечеринкой? — всё сброшено, игроки поменялись попарно!")
            }

            PanicType.GET_OUT -> {
                val targets = getAlivePlayers().filter { it != player && !it.hasQuarantine }
                if (targets.isNotEmpty()) {
                    return GameResult.PanicExchange(
                        "😱 ПАНИКА! Убирайся прочь! — выберите с кем поменяться местами",
                        player,
                        emptyList(),
                        targets,
                        PanicType.GET_OUT,
                    )
                }
                GameResult.Panic("😱 ПАНИКА! Убирайся прочь! — нет доступных игроков")
            }

            PanicType.FORGETFULNESS -> {
                repeat(3) {
                    val av = player.hand.filter { it.name != "НЕЧТО" }
                    if (av.isNotEmpty()) {
                        val c = av.random()
                        player.hand.remove(c)
                        discardPile.add(c)
                    }
                }
                repeat(3) {
                    drawCardSilent()?.let { player.hand.add(it) }
                }
                GameResult.Panic("😱 ПАНИКА! Забывчивость — 3 карты сброшены, 3 взяты!")
            }

            PanicType.OOPS -> {
                GameResult.Panic("😱 ПАНИКА! Уупс! — ${player.name} показывает: ${player.hand.joinToString { it.name }}")
            }
        }
    }

    fun playCard(
        player: Player,
        card: Card,
        target: Player?,
    ): GameResult {
        val index = player.hand.indexOfFirst { it.name == card.name && it.type == card.type }
        if (index == -1) return GameResult.Error("Карта не найдена")

        if (target != null && target.hasQuarantine && card.name != "Топор") {
            return GameResult.Error("🦠 ${target.name} на карантине! Нельзя использовать ${card.name}")
        }

        if (player.hasQuarantine && card.name != "Топор") {
            return GameResult.Error("🦠 Вы на карантине! Можно только Топор на себя")
        }

        if (player.hasQuarantine && card.name == "Топор" && target != player) {
            return GameResult.Error("🦠 На карантине Топор только на себя!")
        }

        player.hand.removeAt(index)
        discardPile.add(card)

        return when (card.name) {
            "Огнемёт" -> useFlamethrower(player, target)

            "Анализ" -> useAnalysis(player, target)

            "Топор" -> useAxe(player, target)

            "Подозрение" -> useSuspicion(player, target)

            "Виски" -> useWhiskey(player)

            "Упорство" -> GameResult.Success("💪 Упорство сыграно")

            "Гляди по сторонам" -> useLookAround(player)

            "Меняемся местами!" -> {
                if (target != null) swapPlayers(player, target)
                GameResult.Success("💺 Обмен местами с ${target?.name}")
            }

            "Сматывай удочки!" -> {
                if (target != null) swapPlayers(player, target)
                GameResult.Success("💨 Обмен местами с ${target?.name}")
            }

            "Соблазн" -> {
                endTurnEarlyFlag = true
                if (target != null) {
                    val p1c = player.hand.filter { it.name != "НЕЧТО" }
                    val p2c = target.hand.filter { it.name != "НЕЧТО" }
                    if (p1c.isNotEmpty() && p2c.isNotEmpty()) {
                        GameResult.ExchangeInfo(player, target, p1c, p2c)
                    } else {
                        GameResult.Error("Нет карт для обмена")
                    }
                } else {
                    GameResult.Error("Нет цели")
                }
            }

            "Карантин" -> {
                if (target != null) {
                    target.hasQuarantine = true
                    target.quarantineTurns = 3
                }
                GameResult.Success("🦠 Карантин на ${target?.name}")
            }

            "Заколоченная дверь" -> {
                if (target != null) barricadedDoors.add(player to target)
                GameResult.Success("🚪 Дверь установлена")
            }

            else -> GameResult.Success("✅ ${card.name} сыграна")
        }
    }

    fun discardCard(
        player: Player,
        card: Card,
    ): GameResult {
        if (card.name == "НЕЧТО") return GameResult.Error("Нельзя сбросить НЕЧТО!")
        return if (player.hand.remove(card)) {
            discardPile.add(card)
            GameResult.Success("🗑️ ${player.name} сбросил ${card.name}")
        } else {
            GameResult.Error("Ошибка сброса")
        }
    }

    // Метод для тихого сброса (Упорство)
    fun discardCardSilent(
        player: Player,
        card: Card,
    ) {
        if (player.hand.remove(card)) {
            discardPile.add(card)
        }
    }

    fun executeExchange(player: Player): GameResult {
        val alive = getAlivePlayers()
        val idx = alive.indexOf(player)
        if (idx == -1) return GameResult.Error("Игрок не найден")

        val (leftIdx, rightIdx) = getNeighborIndices(idx, alive.size)
        val neighbor = alive[if (direction == 1) rightIdx else leftIdx]

        if (player.hasQuarantine) return GameResult.Error("🦠 Вы на карантине!")
        if (neighbor.hasQuarantine) return GameResult.Error("🦠 Сосед на карантине!")
        if (player.hasBarricade) {
            player.hasBarricade = false
            return GameResult.Error("🧱 Баррикада!")
        }
        if (barricadedDoors.any { (a, b) -> (a == player && b == neighbor) || (a == neighbor && b == player) }) {
            return GameResult.Error("🚪 Заколоченная дверь!")
        }

        val p1Cards = player.hand.filter { it.name != "НЕЧТО" && (player.role == Role.THING || it.name != "Заражение!") }
        val p2Cards = neighbor.hand.filter { it.name != "НЕЧТО" && (neighbor.role == Role.THING || it.name != "Заражение!") }

        if (p1Cards.isEmpty()) return GameResult.Error("Нечего предложить")
        if (p2Cards.isEmpty()) return GameResult.Error("Соседу нечего предложить")

        return GameResult.ExchangeInfo(player, neighbor, p1Cards, p2Cards)
    }

    fun handleDefense(
        defender: Player,
        attacker: Player,
        offeredCard: Card,
    ): GameResult {
        val defenseCards = listOf("Страх", "Нет уж, спасибо!", "Мимо!", "Мне и здесь неплохо", "Никакого шашлыка!")
        val defenseCard = defender.hand.find { it.name in defenseCards }

        if (defenseCard != null) {
            defender.hand.remove(defenseCard)
            discardPile.add(defenseCard)

            val newCard = drawCardSilent()
            if (newCard != null) defender.hand.add(newCard)

            return GameResult.DefensePlayed(
                "🛡️ ${defender.name} сыграл ${defenseCard.name}" +
                    if (newCard != null) " и взял карту: ${newCard.name}" else "",
                newCard,
            )
        }
        return GameResult.Error("Нет защиты")
    }

    fun performExchange(
        player1: Player,
        player2: Player,
        card1: Card,
        card2: Card,
    ): GameResult {
        if (!player1.hand.remove(card1)) return GameResult.Error("Карта не найдена")
        if (!player2.hand.remove(card2)) return GameResult.Error("Карта не найдена")

        player1.hand.add(card2)
        player2.hand.add(card1)

        val sb = StringBuilder()
        sb.append("🔄 ${player1.name} отдал ${card1.name}, получил ${card2.name}")

        if (card1.name == "Заражение!" && player1.role == Role.THING && player2.role == Role.HUMAN) {
            player2.role = Role.INFECTED
            sb.append("\n🦠 ${player2.name} ЗАРАЖЁН!")
        }
        if (card2.name == "Заражение!" && player2.role == Role.THING && player1.role == Role.HUMAN) {
            player1.role = Role.INFECTED
            sb.append("\n🦠 ${player1.name} ЗАРАЖЁН!")
        }

        return GameResult.Success(sb.toString())
    }

    fun endTurn(player: Player): GameResult {
        player.hasBarricade = false

        // Уменьшаем карантин ТОЛЬКО у текущего игрока
        if (player.hasQuarantine) {
            player.quarantineTurns--
            if (player.quarantineTurns <= 0) {
                player.hasQuarantine = false
            }
        }

        discardDownToFour(player)
        advanceTurn()
        skipActionPhase = false
        endTurnEarlyFlag = false
        return GameResult.Success("✅ Ход завершён")
    }

    fun checkVictory(): String? {
        val alive = players.filter { it.isAlive }
        val things = alive.filter { it.role == Role.THING || it.role == Role.INFECTED }
        val humans = alive.filter { it.role == Role.HUMAN }

        return when {
            things.isEmpty() -> "HUMANS"
            humans.isEmpty() -> "THING"
            else -> null
        }
    }

    // ==================== ГЕТТЕРЫ ====================

    fun getPlayers(): List<Player> = players.toList()

    fun getCurrentPlayer(): Player? {
        val alive = getAlivePlayers()
        return if (alive.isNotEmpty() && currentPlayerIndex < alive.size) alive[currentPlayerIndex] else null
    }

    fun getTurnNumber(): Int = turnNumber

    fun getDirection(): Int = direction

    fun getSkipActionPhase(): Boolean = skipActionPhase

    fun getPlayableCards(player: Player): List<Card> =
        player.hand.filter { it.name != "НЕЧТО" && it.name != "Заражение!" && it.type != CardType.DEFENSE }

    fun getDiscardableCards(player: Player): List<Card> = player.hand.filter { it.name != "НЕЧТО" }

    fun getTargets(
        player: Player,
        cardName: String,
    ): List<Player> {
        return when (cardName) {
            "Топор" -> getAlivePlayers().filter { it != player && isAdjacent(player, it) } + listOf(player)

            "Карантин" -> getAlivePlayers().filter { it != player && isAdjacent(player, it) && !it.hasQuarantine } + listOf(player)

            "Огнемёт", "Анализ", "Подозрение", "Заколоченная дверь", "Меняемся местами!", "Соблазн" ->
                getAlivePlayers().filter { it != player && isAdjacent(player, it) && !it.hasQuarantine }

            "Сматывай удочки!" -> getAlivePlayers().filter { it != player && !it.hasQuarantine }

            else -> getAlivePlayers().filter { it != player && !it.hasQuarantine }
        }
    }

    fun isAdjacent(
        p1: Player,
        p2: Player,
    ): Boolean {
        val alive = getAlivePlayers()
        val idx1 = alive.indexOf(p1)
        val idx2 = alive.indexOf(p2)
        if (idx1 == -1 || idx2 == -1) return false
        if (alive.size <= 2) return true
        val (left, right) = getNeighborIndices(idx1, alive.size)
        return idx2 == left || idx2 == right
    }

    // ==================== ПРИВАТНЫЕ ====================

    private fun getAlivePlayers(): List<Player> = players.filter { it.isAlive }

    private fun getNeighborIndices(
        index: Int,
        total: Int,
    ): Pair<Int, Int> {
        if (total == 1) return Pair(0, 0)
        if (total == 2) return Pair((index + 1) % total, (index + 1) % total)
        return Pair((index - 1 + total) % total, (index + 1) % total)
    }

    private fun advanceTurn() {
        // Сначала уменьшаем карантин у всех игроков

        currentPlayerIndex += direction
        val alive = getAlivePlayers()
        if (alive.isEmpty()) return
        if (currentPlayerIndex >= alive.size) {
            currentPlayerIndex = 0
            turnNumber++
        }
        if (currentPlayerIndex < 0) {
            currentPlayerIndex = alive.size - 1
            turnNumber++
        }
    }

    fun swapPlayers(
        p1: Player,
        p2: Player,
    ) {
        val alive = getAlivePlayers()
        val idx1 = alive.indexOf(p1)
        val idx2 = alive.indexOf(p2)
        if (idx1 == -1 || idx2 == -1) return
        val newAlive = alive.toMutableList()
        newAlive[idx1] = p2
        newAlive[idx2] = p1
        players.clear()
        players.addAll(newAlive)
        if (currentPlayerIndex == idx1) {
            currentPlayerIndex = idx2
        } else if (currentPlayerIndex == idx2) {
            currentPlayerIndex = idx1
        }
    }

    private fun discardDownToFour(player: Player) {
        while (player.hand.size > 4) {
            val cards = player.hand.filter { it.name != "НЕЧТО" }
            if (cards.isEmpty()) break
            player.hand.remove(cards.random())
        }
    }

    fun drawCardSilent(): Card? {
        if (deck.isEmpty()) {
            if (discardPile.isNotEmpty()) {
                deck.addAll(discardPile.takeAll())
                deck.shuffle()
            } else {
                return null
            }
        }
        return deck.draw()
    }

    // ==================== КАРТЫ ====================

    private fun useFlamethrower(
        owner: Player,
        target: Player?,
    ): GameResult {
        if (target == null) return GameResult.Error("Нет цели")
        if (!isAdjacent(owner, target)) return GameResult.Error("Не сосед")
        if (target.hasQuarantine) return GameResult.Error("Цель на карантине")
        if (target.hand.any { it.name == "Никакого шашлыка!" }) {
            target.hand.removeAll { it.name == "Никакого шашлыка!" }
            return GameResult.Success("🛡️ ${target.name} защитился!")
        }
        target.isAlive = false
        return GameResult.Success("💀 ${target.name} сгорел! (${target.role})", listOf(target))
    }

    private fun useAnalysis(
        owner: Player,
        target: Player?,
    ): GameResult {
        if (target == null) return GameResult.Error("Нет цели")
        return GameResult.Success("🔍 Рука ${target.name}: ${target.hand.joinToString { it.name }}")
    }

    private fun useAxe(
        owner: Player,
        target: Player?,
    ): GameResult {
        if (target != null && target.hasQuarantine) {
            target.hasQuarantine = false
            target.quarantineTurns = 0
            return GameResult.Success("✅ Карантин снят с ${target.name}")
        }
        if (owner.hasQuarantine) {
            owner.hasQuarantine = false
            owner.quarantineTurns = 0
            return GameResult.Success("✅ Карантин снят")
        }
        val door = barricadedDoors.find { (a, b) -> (a == owner && b == target) || (a == target && b == owner) }
        if (door != null) {
            barricadedDoors.remove(door)
            return GameResult.Success("✅ Дверь снята")
        }
        return GameResult.Error("Нечего снимать")
    }

    private fun useSuspicion(
        owner: Player,
        target: Player?,
    ): GameResult {
        if (target == null || target.hand.isEmpty()) return GameResult.Error("Нет цели")
        return GameResult.Success("🔎 У ${target.name}: ${target.hand.random().name}")
    }

    private fun useWhiskey(owner: Player): GameResult = GameResult.Success("🥃 ${owner.name}: ${owner.hand.joinToString { it.name }}")

    private fun useLookAround(owner: Player): GameResult {
        direction *= -1
        return GameResult.Success("👀 Направление: ${if (direction == 1) "по часовой" else "против"}")
    }
}

fun Deck.clear() {
    while (isNotEmpty()) draw()
}
