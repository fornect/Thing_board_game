package systems

import enums.PanicType
import model.PanicCard
import model.Player

class GamePanic(
    private val state: GameState,
    private val deck: GameDeck,
    private val actions: GameActions,
) {
    fun execute(
        card: PanicCard,
        player: Player,
    ): GameResult {
        return when (card.subType) {
            PanicType.OLD_ROPE -> {
                state.players.forEach { it.removeQuarantine() }
                GameResult.Panic("😱 Старые веревки!")
            }

            PanicType.BLIND_DATE -> {
                state.endTurnEarlyFlag = true
                GameResult.Panic("😱 Свидание в слепую!")
            }

            PanicType.NO_PANIC -> {
                state.skipActionPhase = false
                GameResult.Panic("😌 Без паники!")
            }

            PanicType.CHAIN_REACTION -> {
                val alive = state.getAlivePlayers()
                val cards =
                    alive.map { p ->
                        p.getHandCards()
                            .filter { it.name != "НЕЧТО" && it.name != "Заражение!" }
                            .let {
                                if (it.isNotEmpty()) {
                                    val x = it.random()
                                    p.removeCard(x)
                                    x
                                } else {
                                    null
                                }
                            }
                    }
                cards.forEachIndexed { i, c -> c?.let { alive[(i + 1) % alive.size].addCard(it) } }
                GameResult.Panic("😱 Цепная реакция!")
            }

            PanicType.THREE_FOUR -> {
                state.barricadedDoors.clear()
                GameResult.Panic("😱 Двери сброшены!")
            }

            PanicType.CALL_THAT_PARTY -> {
                state.players.forEach { it.removeQuarantine() }
                state.barricadedDoors.clear()
                val alive = state.getAlivePlayers()
                if (alive.size >= 2) {
                    val start = alive.indexOf(player)
                    var i = 0
                    while (i + 1 < alive.size) {
                        state.swapPlayers(alive[(start + i) % alive.size], alive[(start + i + 1) % alive.size])
                        i += 2
                    }
                }
                GameResult.Panic("😱 Вечеринка!")
            }

            PanicType.FORGETFULNESS -> {
                repeat(3) {
                    player.getHandCards()
                        .filter { it.name != "НЕЧТО" }
                        .let {
                            if (it.isNotEmpty()) {
                                val x = it.random()
                                player.removeCard(x)
                                deck.addToDiscard(x)
                            }
                        }
                }
                repeat(3) { actions.drawSilentSafe(player)?.let { player.addCard(it) } }
                GameResult.Panic("😱 Забывчивость!")
            }

            PanicType.OOPS -> GameResult.Panic("😱 Уупс! ${player.getHandAsString()}")

            PanicType.ONE_TWO -> {
                val alive = state.getAlivePlayers()
                val idx = alive.indexOf(player)
                if (idx != -1 && alive.size >= 4) {
                    val third = alive[(idx + 3) % alive.size]
                    if (!third.hasQuarantine) {
                        GameResult.PanicExchange(
                            "Выберите карту для обмена с ${third.name}",
                            player,
                            player.getHandCards().filter { it.name != "НЕЧТО" },
                            listOf(third),
                            PanicType.ONE_TWO,
                        )
                    } else {
                        GameResult.Panic("😱 Игрок на карантине")
                    }
                } else {
                    GameResult.Panic("😱 Нет игроков")
                }
            }

            PanicType.LET_BE_FRIENDS -> {
                val targets = state.getAlivePlayers().filter { it != player && !it.hasQuarantine }
                if (targets.isNotEmpty()) {
                    GameResult.PanicExchange(
                        "Выберите игрока и карту",
                        player,
                        player.getHandCards().filter { it.name != "НЕЧТО" },
                        targets,
                        PanicType.LET_BE_FRIENDS,
                    )
                } else {
                    GameResult.Panic("😱 Нет игроков")
                }
            }

            PanicType.GET_OUT -> {
                val targets = state.getAlivePlayers().filter { it != player && !it.hasQuarantine }
                if (targets.isNotEmpty()) {
                    GameResult.PanicExchange(
                        "Выберите с кем поменяться",
                        player,
                        emptyList(),
                        targets,
                        PanicType.GET_OUT,
                    )
                } else {
                    GameResult.Panic("😱 Нет игроков")
                }
            }

            else -> GameResult.Panic("😱 ПАНИКА!")
        }
    }
}
