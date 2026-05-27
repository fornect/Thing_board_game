package model

import enums.ActionType

class DeckTest {

    fun test() {
        testDeckEmpty()
        testAddCard()
        testDrawFromEmpty()
        testDrawCard()
        testDrawOrder()
        testShuffle()
        testAddAll()
        println("✅ DeckTest: все тесты пройдены!")
    }

    private fun testDeckEmpty() {
        val deck = Deck()
        assert(deck.size() == 0) { "Размер должен быть 0" }
        assert(deck.isEmpty()) { "Колода должна быть пуста" }
    }

    private fun testAddCard() {
        val deck = Deck()
        val card = ActionCard("Test", "Test card", ActionType.ANALYSIS)
        deck.add(card)
        assert(deck.size() == 1) { "Размер должен быть 1" }
    }

    private fun testDrawFromEmpty() {
        val deck = Deck()
        assert(deck.draw() == null) { "Из пустой колоды должен возвращаться null" }
    }

    private fun testDrawCard() {
        val deck = Deck()
        val card = ActionCard("Test", "Test card", ActionType.ANALYSIS)
        deck.add(card)
        val drawn = deck.draw()
        assert(drawn != null) { "Карта должна быть вытянута" }
        assert(drawn?.name == "Test") { "Имя карты должно быть 'Test'" }
        assert(deck.size() == 0) { "Колода должна быть пуста после draw" }
    }

    private fun testDrawOrder() {
        val deck = Deck()
        val card1 = ActionCard("First", "First card", ActionType.FLAMETHROWER)
        val card2 = ActionCard("Second", "Second card", ActionType.ANALYSIS)
        deck.add(card1)
        deck.add(card2)
        assert(deck.draw()?.name == "First") { "Первая карта должна быть First" }
        assert(deck.draw()?.name == "Second") { "Вторая карта должна быть Second" }
    }

    private fun testShuffle() {
        val deck = Deck()
        repeat(10) { deck.add(ActionCard("Card $it", "Desc", ActionType.ANALYSIS)) }
        val sizeBefore = deck.size()
        deck.shuffle()
        assert(sizeBefore == deck.size()) { "Размер после shuffle должен быть тем же" }
    }

    private fun testAddAll() {
        val deck = Deck()
        val cards = listOf(
            ActionCard("A", "a", ActionType.ANALYSIS),
                           ActionCard("B", "b", ActionType.FLAMETHROWER)
        )
        deck.addAll(cards)
        assert(deck.size() == 2) { "Размер должен быть 2 после addAll" }
    }
}
