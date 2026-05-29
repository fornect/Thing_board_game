package systems

import enums.*
import model.*

class GameActions(
    private val state: GameState,
    private val deck: GameDeck,
) {
    fun drawCard(player: Player): GameResult {
        println("DEBUG drawCard: skipActionPhase=${state.skipActionPhase}, player=${player.name}")

        val card = deck.drawCard() ?: return GameResult.Error("Колода пуста!")
        if (card.type == CardType.PANIC && card is PanicCard) {
            state.skipActionPhase = true
            println("DEBUG drawCard: PANIC! skipActionPhase set to true")
            val panicResult = GamePanic(state, deck, this).execute(card, player)
            deck.addToDiscard(card)
            return panicResult
        }
        player.addCard(card)
        return GameResult.Success("📤 ${player.name} взял карту: ${card.name}")
    }

    fun drawSilentSafe(player: Player): Card? {
        while (true) {
            val card = deck.drawCard() ?: return null
            if (card.type == CardType.PANIC && card is PanicCard) {
                val panicResult = GamePanic(state, deck, this).execute(card, player)
                deck.addToDiscard(card)
                continue
            }
            return card
        }
    }

    fun playCard(
        player: Player,
        card: Card,
        target: Player?,
    ): GameResult {
        // Сохраняем карты до удаления, чтобы Соблазн мог их показать
        val playerCardsBefore = player.getHandCards().filter { it.name != "НЕЧТО" && it != card }
        val targetCardsBefore = target?.getHandCards()?.filter { it.name != "НЕЧТО" } ?: emptyList()

        if (!player.removeCard(card)) return GameResult.Error("Карта не найдена")
        deck.addToDiscard(card)

        if (target != null && target.hasQuarantine && card.name != "Топор") return GameResult.Error("🦠 Игрок на карантине!")
        if (player.hasQuarantine && card.name != "Топор") return GameResult.Error("🦠 Вы на карантине!")
        if (player.hasQuarantine && card.name == "Топор" && target != player) return GameResult.Error("🦠 Топор только на себя в карантине!")

        return when (card.name) {
            "Огнемёт" -> useFlamethrower(player, target)

            "Анализ" -> useAnalysis(target)

            "Топор" -> useAxe(player, target)

            "Подозрение" -> useSuspicion(target)

            "Виски" -> useWhiskey(player)

            "Упорство" -> usePerseverance(player)

            "Гляди по сторонам" -> useLookAround()

            "Меняемся местами!", "Сматывай удочки!" -> {
                if (target != null) state.swapPlayers(player, target)
                GameResult.Success("💺 Обмен")
            }

            "Соблазн" -> {
                state.endTurnEarlyFlag = true
                if (target != null) {
                    val p1 = playerCardsBefore
                    val p2 = targetCardsBefore
                    if (p2.isNotEmpty()) {
                        GameResult.ExchangeInfo(player, target, p1, p2)
                    } else {
                        GameResult.Error("Нет карт для обмена")
                    }
                } else {
                    GameResult.Error("Нет цели")
                }
            }

            "Карантин" -> {
                target?.setQuarantine(3)
                GameResult.Success("🦠 Карантин")
            }

            "Заколоченная дверь" -> {
                if (target != null) state.barricadedDoors.add(player to target)
                GameResult.Success("🚪 Дверь")
            }

            else -> GameResult.Success("✅ ${card.name}")
        }
    }

    fun discardCard(
        player: Player,
        card: Card,
    ): GameResult {
        if (card.name == "НЕЧТО") return GameResult.Error("Нельзя сбросить НЕЧТО!")
        return if (player.removeCard(card)) {
            deck.addToDiscard(card)
            GameResult.Success("🗑️ Сброшено")
        } else {
            GameResult.Error("Ошибка")
        }
    }

    fun discardCardSilent(
        player: Player,
        card: Card,
    ) {
        if (player.removeCard(card)) deck.addToDiscard(card)
    }

    fun endTurn(player: Player): GameResult {
        player.removeBarricade()
        player.decreaseQuarantine()
        state.discardDownToFour(player)
        state.advanceTurn()
        return GameResult.Success("✅ Ход завершён")
    }

    private fun useFlamethrower(
        owner: Player,
        target: Player?,
    ): GameResult {
        if (target == null || !state.isAdjacent(owner, target)) return GameResult.Error("Нельзя")
        if (target.hasQuarantine) return GameResult.Error("Карантин")
        if (target.hasCard("Никакого шашлыка!")) {
            target.removeCardByName("Никакого шашлыка!")
            val newCard = drawSilentSafe(target)
            if (newCard != null) target.addCard(newCard)
            return GameResult.Success("🛡️ ${target.name} защитился!")
        }
        target.kill()
        return GameResult.Success("💀 ${target.name} сгорел!")
    }

    private fun useAnalysis(target: Player?): GameResult {
        if (target == null) return GameResult.Error("Нет цели")
        return GameResult.Success("🔍 ${target.name}: ${target.getHandAsString()}")
    }

    private var perseveranceCards = mutableListOf<Card>()

    fun usePerseverance(player: Player): GameResult {
        perseveranceCards.clear()

        while (perseveranceCards.size < 3) {
            val card = drawSilentSafe(player) ?: break
            perseveranceCards.add(card)
        }

        return if (perseveranceCards.isEmpty()) {
            GameResult.Error("Не удалось взять карты")
        } else {
            GameResult.Success("💪 Упорство: ${perseveranceCards.size} карт")
        }
    }

    fun getPerseveranceCards(): List<Card> = perseveranceCards.toList()

    fun confirmPerseverance(
        player: Player,
        keepIdx: Int,
    ) {
        if (keepIdx in perseveranceCards.indices) {
            player.addCard(perseveranceCards[keepIdx])
            perseveranceCards.forEachIndexed { i, c ->
                if (i != keepIdx) discardCardSilent(player, c)
            }
            perseveranceCards.clear()
        }
    }

    private fun useAxe(
        owner: Player,
        target: Player?,
    ): GameResult {
        if (target != null && target.hasQuarantine) {
            target.removeQuarantine()
            return GameResult.Success("✅ Карантин снят")
        }
        if (owner.hasQuarantine) {
            owner.removeQuarantine()
            return GameResult.Success("✅ Карантин снят")
        }
        val door = state.barricadedDoors.find { (a, b) -> (a == owner && b == target) || (a == target && b == owner) }
        if (door != null) {
            state.barricadedDoors.remove(door)
            return GameResult.Success("✅ Дверь снята")
        }
        return GameResult.Error("Нечего снимать")
    }

    private fun useSuspicion(target: Player?): GameResult {
        if (target == null || target.getHandCards().isEmpty()) return GameResult.Error("Нет цели")
        return GameResult.Success("🔎 ${target.getHandCards().random().name}")
    }

    private fun useWhiskey(owner: Player): GameResult = GameResult.Success("🥃 ${owner.getHandAsString()}")

    private fun useLookAround(): GameResult {
        state.direction *= -1
        return GameResult.Success("👀 Направление изменено")
    }

    fun getPlayableCards(p: Player) =
        p.getHandCards().filter {
            it.name != "НЕЧТО" && it.name != "Заражение!" && it.type != CardType.DEFENSE
        }

    fun getDiscardableCards(p: Player) = p.getHandCards().filter { it.name != "НЕЧТО" }

    fun getTargets(
        player: Player,
        cardName: String,
    ): List<Player> =
        when (cardName) {
            "Топор" -> state.getAlivePlayers().filter { it != player && state.isAdjacent(player, it) } + listOf(player)

            "Карантин" ->
                state.getAlivePlayers().filter {
                    it != player && state.isAdjacent(player, it) && !it.hasQuarantine
                } + listOf(player)

            "Огнемёт", "Анализ", "Подозрение", "Заколоченная дверь", "Меняемся местами!" ->
                state.getAlivePlayers().filter { it != player && state.isAdjacent(player, it) && !it.hasQuarantine }

            "Соблазн", "Сматывай удочки!" -> state.getAlivePlayers().filter { it != player && !it.hasQuarantine }

            else -> state.getAlivePlayers().filter { it != player && !it.hasQuarantine }
        }
}
