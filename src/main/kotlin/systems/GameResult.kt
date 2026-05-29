package systems

import enums.PanicType
import model.Card
import model.Player

sealed class GameResult {
    abstract val message: String

    data class Success(
        override val message: String,
        val affectedPlayers: List<Player> = emptyList(),
        val cards: List<Card> = emptyList(),
    ) : GameResult()

    data class Error(override val message: String) : GameResult()

    data class Panic(override val message: String) : GameResult()

    data class DefensePlayed(
        override val message: String,
        val drawnCard: Card?,
    ) : GameResult()

    data class PassExchange(
        override val message: String,
        val nextPlayer: Player,
        val attacker: Player,
        val drawnCard: Card?,
    ) : GameResult()

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
