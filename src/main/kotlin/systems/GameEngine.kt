package systems

import model.Card
import model.Player

class GameEngine {
    val state = GameState()
    val deck = GameDeck()
    val setup = GameSetup(state, deck)
    val actions = GameActions(state, deck)
    val exchange = GameExchange(state, deck, actions)
    val panic = GamePanic(state, deck, actions)

    fun setupGame(names: List<String>) = setup.setupGame(names)

    fun drawCard(player: Player) = actions.drawCard(player)

    fun playCard(
        player: Player,
        card: Card,
        target: Player?,
    ) = actions.playCard(player, card, target)

    fun discardCard(
        player: Player,
        card: Card,
    ) = actions.discardCard(player, card)

    fun discardCardSilent(
        player: Player,
        card: Card,
    ) = actions.discardCardSilent(player, card)

    fun executeExchange(player: Player) = exchange.executeExchange(player)

    fun performExchange(
        p1: Player,
        p2: Player,
        c1: Card,
        c2: Card,
    ) = exchange.performExchange(p1, p2, c1, c2)

    fun handleDefense(
        defender: Player,
        attacker: Player,
        offeredCard: Card,
        defenseCard: Card? = null,
    ) = exchange.handleDefense(defender, attacker, offeredCard, defenseCard)

    fun endTurn(player: Player) = actions.endTurn(player)

    fun checkVictory() = state.checkVictory()

    fun getPlayers() = state.getAllPlayers()

    fun getCurrentPlayer() = state.getCurrentPlayer()

    fun getTurnNumber() = state.turnNumber

    fun getDirection() = state.direction

    fun getSkipActionPhase() = state.skipActionPhase

    fun isActionDone() = state.actionDone

    fun setActionDone(v: Boolean) {
        state.actionDone = v
    }

    fun isCardDrawn() = state.cardDrawn

    fun setCardDrawn(v: Boolean) {
        state.cardDrawn = v
    }

    fun isPanicHappened() = state.panicHappened

    fun setPanicHappened(v: Boolean) {
        state.panicHappened = v
    }

    fun isExchangeDone() = state.exchangeDone

    fun setExchangeDone(v: Boolean) {
        state.exchangeDone = v
    }

    fun isQuarantineJustPlaced() = state.quarantineJustPlaced

    fun setQuarantineJustPlaced(v: Boolean) {
        state.quarantineJustPlaced = v
    }

    fun resetTurnState() = state.resetTurnState()

    fun getPlayableCards(p: Player) = actions.getPlayableCards(p)

    fun getDiscardableCards(p: Player) = actions.getDiscardableCards(p)

    fun getTargets(
        player: Player,
        cardName: String,
    ) = actions.getTargets(player, cardName)

    fun isAdjacent(
        p1: Player,
        p2: Player,
    ) = state.isAdjacent(p1, p2)

    fun swapPlayers(
        p1: Player,
        p2: Player,
    ) = state.swapPlayers(p1, p2)

    fun drawSilentSafe(player: Player): Card? = actions.drawSilentSafe(player)

    fun getHandAsString(player: Player): String = player.getHandAsString()

    fun getPerseveranceCards() = actions.getPerseveranceCards()

    fun confirmPerseverance(
        player: Player,
        keepIdx: Int,
    ) = actions.confirmPerseverance(player, keepIdx)
}
