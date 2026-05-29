package systems

import enums.*
import model.*
import kotlin.random.Random

class GameSetup(
    private val state: GameState,
    private val deck: GameDeck,
) {
    companion object {
        val DEFENSE_CARDS = listOf("Страх", "Нет уж, спасибо!", "Мимо!")
        val DEFENSE_CARDS_SEAT_SWAP = listOf("Мне и здесь неплохо")
        val DEFENSE_CARDS_FLAMETHROWER = listOf("Никакого шашлыка!")
    }

    fun setupGame(playerNames: List<String>): String {
        if (playerNames.size !in 4..12) return "❌ Ошибка: нужно от 4 до 12 игроков"
        if (playerNames.any { it.isBlank() }) return "❌ Ошибка: имена не могут быть пустыми"
        if (playerNames.size != playerNames.distinct().size) return "❌ Ошибка: имена не должны повторяться"
        if (playerNames.any { it.length > 20 }) return "❌ Ошибка: имена не длиннее 20 символов"

        val log = StringBuilder()
        state.players.clear()
        state.deadPlayers.clear()
        deck.clear()
        state.barricadedDoors.clear()

        playerNames.forEach { state.players.add(Player(it)) }

        addActionCards()
        addDefenseCards()
        addObstacleCards()

        val thingCard = ActionCard("НЕЧТО", "Вы - НЕЧТО!", ActionType.INFECTION)
        deck.deck.shuffle()

        val totalNeeded = state.players.size * 4
        val cardsForDeal = mutableListOf<Card>()
        repeat(totalNeeded) { deck.drawCard()?.let { cardsForDeal.add(it) } }

        val thingPos = Random.nextInt(cardsForDeal.size)
        cardsForDeal[thingPos] = thingCard.copy()
        cardsForDeal.shuffle()

        var cardIndex = 0
        state.players.forEach { player ->
            repeat(4) {
                if (cardIndex < cardsForDeal.size) {
                    player.addCard(cardsForDeal[cardIndex])
                    cardIndex++
                }
            }
        }

        state.players.forEach { it.role = Role.HUMAN }
        state.players.find { it.hasCard("НЕЧТО") }?.role = Role.THING

        repeat(20) { deck.deck.add(ActionCard("Заражение!", "Заразить при обмене", ActionType.INFECTION).copy()) }

        addPanicCards()
        deck.deck.shuffle()

        state.currentPlayerIndex = 0
        state.turnNumber = 0
        state.direction = 1
        state.skipActionPhase = false
        state.endTurnEarlyFlag = false
        state.resetTurnState()

        log.append("🎮 Игра создана!\n👥 Игроки: ${playerNames.joinToString(", ")}\n")
        state.players.find { it.role == Role.THING }?.let { log.append("🔴 ${it.name} - НЕЧТО!\n") }
        return log.toString()
    }

    private fun addActionCards() {
        val cards =
            listOf(
                ActionCard("Огнемёт", "Убить соседа", ActionType.FLAMETHROWER),
                ActionCard("Анализ", "Посмотреть руку", ActionType.ANALYSIS),
                ActionCard("Топор", "Снять карантин", ActionType.AXE),
                ActionCard("Подозрение", "Посмотреть карту", ActionType.SUSPICION),
                ActionCard("Виски", "Показать карты", ActionType.WHISKEY),
                ActionCard("Упорство", "Взять 3, оставить 1", ActionType.PERSEVERANCE),
                ActionCard("Гляди по сторонам", "Сменить направление", ActionType.LOOK_AROUND),
                ActionCard("Меняемся местами!", "Смена с соседом", ActionType.SWAP_SEATS_NEIGHBOR),
                ActionCard("Сматывай удочки!", "Смена с любым", ActionType.SWAP_SEATS_ANY),
                ActionCard("Соблазн", "Обмен + конец хода", ActionType.TEMPTATION),
            )
        repeat(6) { deck.deck.add(cards[0].copy()) }
        repeat(4) { deck.deck.add(cards[1].copy()) }
        repeat(4) { deck.deck.add(cards[2].copy()) }
        repeat(6) { deck.deck.add(cards[3].copy()) }
        repeat(3) { deck.deck.add(cards[4].copy()) }
        repeat(5) { deck.deck.add(cards[5].copy()) }
        repeat(3) { deck.deck.add(cards[6].copy()) }
        repeat(5) { deck.deck.add(cards[7].copy()) }
        repeat(4) { deck.deck.add(cards[8].copy()) }
        repeat(6) { deck.deck.add(cards[9].copy()) }
    }

    private fun addDefenseCards() {
        val cards =
            listOf(
                DefenseCard("Мне и здесь неплохо", "Отмена смены", DefenseType.IM_FINE_HERE),
                DefenseCard("Нет уж, спасибо!", "Отказ от обмена", DefenseType.NO_THANKS),
                DefenseCard("Мимо!", "Обмен дальше", DefenseType.PASS),
                DefenseCard("Никакого шашлыка!", "Отмена огнемёта", DefenseType.NO_BBQ),
                DefenseCard("Страх", "Отказ + просмотр", DefenseType.FEAR),
            )
        repeat(3) { deck.deck.add(cards[0].copy()) }
        repeat(4) { deck.deck.add(cards[1].copy()) }
        repeat(4) { deck.deck.add(cards[2].copy()) }
        repeat(3) { deck.deck.add(cards[3].copy()) }
        repeat(3) { deck.deck.add(cards[4].copy()) }
    }

    private fun addObstacleCards() {
        val cards =
            listOf(
                ObstacleCard("Заколоченная дверь", "Блок", ObstacleType.BARRICADED_DOOR),
                ObstacleCard("Карантин", "Блок на 3 хода", ObstacleType.QUARANTINE),
            )
        repeat(3) { deck.deck.add(cards[0].copy()) }
        repeat(2) { deck.deck.add(cards[1].copy()) }
    }

    private fun addPanicCards() {
        val cards =
            listOf(
                PanicCard("Старые веревки", "Карантины сброшены", PanicType.OLD_ROPE),
                PanicCard("Свидание в слепую", "Обмен с колодой", PanicType.BLIND_DATE),
                PanicCard("Раз, два...", "Обмен с третьим", PanicType.ONE_TWO),
                PanicCard("Давай дружить?", "Обмен с любым", PanicType.LET_BE_FRIENDS),
                PanicCard("Только между нами", "Показать карты", PanicType.JUST_BETWEEN_US),
                PanicCard("Без паники!", "Ничего", PanicType.NO_PANIC),
                PanicCard("Цепная реакция", "Передать карту", PanicType.CHAIN_REACTION),
                PanicCard("...Три, четыре...", "Двери сброшены", PanicType.THREE_FOUR),
                PanicCard("Вечеринка", "Сброс + обмен", PanicType.CALL_THAT_PARTY),
                PanicCard("Убирайся прочь!", "Обмен местами", PanicType.GET_OUT),
                PanicCard("Забывчивость", "Сброс 3, взять 3", PanicType.FORGETFULNESS),
                PanicCard("Уупс!", "Показать карты", PanicType.OOPS),
            )
        repeat(2) { cards.forEach { deck.deck.add(it.copy()) } }
    }
}
