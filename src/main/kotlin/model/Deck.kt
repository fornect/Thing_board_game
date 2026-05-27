package model

class Deck {
    private val cards = mutableListOf<Card>()

    fun shuffle() {
        cards.shuffle()
    }

    fun draw(): Card? = if (cards.isEmpty()) null else cards.removeAt(0)

    fun add(card: Card) {
        cards.add(card)
    }

    fun addAll(cards: List<Card>) {
        this.cards.addAll(cards)
    }

    fun size(): Int = cards.size

    fun isEmpty(): Boolean = cards.isEmpty()

    fun isNotEmpty(): Boolean = cards.isNotEmpty()

    fun peek(): Card? = cards.firstOrNull()

    fun cardsList(): List<Card> = cards.toList()
}
