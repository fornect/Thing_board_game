package model

class DiscardPile {
    private val cards = mutableListOf<Card>()

    fun add(card: Card) {
        cards.add(card)
    }

    fun takeAll(): List<Card> {
        val result = cards.toList()
        cards.clear()
        return result
    }

    fun clear() {
        cards.clear()
    }

    fun size(): Int = cards.size

    fun isNotEmpty(): Boolean = cards.isNotEmpty()

    fun isEmpty(): Boolean = cards.isEmpty()

    fun cardsList(): List<Card> = cards.toList()
}
