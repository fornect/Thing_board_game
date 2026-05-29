package systems

import model.Card
import model.Deck
import model.DiscardPile

class GameDeck {
    val deck = Deck()
    val discardPile = DiscardPile()

    fun drawCard(): Card? {
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

    fun addToDiscard(card: Card) {
        discardPile.add(card)
    }

    fun drawSilent(): Card? = drawCard()

    fun clear() {
        discardPile.clear()
        while (deck.isNotEmpty()) deck.draw()
    }
}
