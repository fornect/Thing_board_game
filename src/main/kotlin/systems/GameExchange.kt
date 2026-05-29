package systems

import enums.DefenseType
import enums.Role
import model.Card
import model.DefenseCard
import model.Player

class GameExchange(
    private val state: GameState,
    private val deck: GameDeck,
    private val action: GameActions,
) {
    fun executeExchange(player: Player): GameResult {
        val alive = state.getAlivePlayers()
        val idx = alive.indexOf(player)
        if (idx == -1) return GameResult.Error("Игрок не найден")

        val (l, r) = state.getNeighborIndices(idx, alive.size)
        val neighbor = alive[if (state.direction == 1) r else l]

        if (player.hasQuarantine) return GameResult.Error("🦠 Карантин")
        if (neighbor.hasQuarantine) return GameResult.Error("🦠 Сосед на карантине")
        if (player.hasBarricade) {
            player.removeBarricade()
            return GameResult.Error("🧱 Баррикада")
        }
        if (state.barricadedDoors.any { (a, b) -> (a == player && b == neighbor) || (a == neighbor && b == player) }) {
            return GameResult.Error("🚪 Заколоченная дверь")
        }

        val p1 = player.getHandCards().filter { it.name != "НЕЧТО" && (player.role == Role.THING || it.name != "Заражение!") }
        val p2 = neighbor.getHandCards().filter { it.name != "НЕЧТО" && (neighbor.role == Role.THING || it.name != "Заражение!") }

        if (p1.isEmpty()) return GameResult.Error("Нечего предложить")
        if (p2.isEmpty()) return GameResult.Error("Соседу нечего предложить")

        return GameResult.ExchangeInfo(player, neighbor, p1, p2)
    }

    fun performExchange(
        p1: Player,
        p2: Player,
        c1: Card,
        c2: Card,
    ): GameResult {
        if (!p1.removeCard(c1)) return GameResult.Error("Карта не найдена")
        if (!p2.removeCard(c2)) return GameResult.Error("Карта не найдена")
        p1.addCard(c2)
        p2.addCard(c1)

        val sb = StringBuilder("🔄 ${p1.name} отдал ${c1.name}, получил ${c2.name}")
        if (c1.name == "Заражение!" && p1.role == Role.THING && p2.role == Role.HUMAN) {
            p2.role = Role.INFECTED
            sb.append("\n🦠 ${p2.name} ЗАРАЖЁН!")
        }
        if (c2.name == "Заражение!" && p2.role == Role.THING && p1.role == Role.HUMAN) {
            p1.role = Role.INFECTED
            sb.append("\n🦠 ${p1.name} ЗАРАЖЁН!")
        }
        return GameResult.Success(sb.toString())
    }

    fun handleDefense(
        defender: Player,
        attacker: Player,
        offeredCard: Card,
        defenseCard: Card? = null,
    ): GameResult {
        val actualDefense = defenseCard ?: defender.getHandCards().find { it is DefenseCard }

        if (actualDefense == null || actualDefense !is DefenseCard) {
            return GameResult.Error("Нет защиты")
        }

        defender.removeCard(actualDefense)
        deck.addToDiscard(actualDefense)

        val newCard = action.drawSilentSafe(defender)
        if (newCard != null) defender.addCard(newCard)
        val dn = newCard?.name ?: "нет"

        return when (actualDefense.subType) {
            DefenseType.FEAR -> {
                val randomCard = attacker.getHandCards().random()
                GameResult.DefensePlayed("🛡️ СТРАХ! Видит: ${randomCard.name}. Взята: $dn", newCard)
            }

            DefenseType.NO_THANKS -> GameResult.DefensePlayed("🙅 Отказ! Взята: $dn", newCard)

            DefenseType.PASS -> {
                val alive = state.getAlivePlayers()
                val di = alive.indexOf(defender)
                if (di != -1 && alive.size > 1) {
                    var steps = 1
                    var nextPlayer: Player? = null
                    while (steps < alive.size) {
                        val nextIdx = (di + state.direction * steps + alive.size) % alive.size
                        val candidate = alive[nextIdx]
                        if (candidate != attacker && candidate != defender && !candidate.hasQuarantine) {
                            nextPlayer = candidate
                            break
                        }
                        steps++
                    }
                    if (nextPlayer != null) {
                        return GameResult.PassExchange(
                            "➡️ МИМО! ${defender.name} → ${nextPlayer.name}. Взята: $dn",
                            nextPlayer,
                            attacker,
                            newCard,
                        )
                    }
                }
                GameResult.DefensePlayed("➡️ МИМО! Некому передать. Взята: $dn", newCard)
            }

            else -> GameResult.DefensePlayed("🛡️ ${actualDefense.name}! Взята: $dn", newCard)
        }
    }
}
