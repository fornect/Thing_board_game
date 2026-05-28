package model

import enums.ActionType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DeckTest {
    @Test
    fun `deck should be empty when created`() {
        val deck = Deck()
        assertEquals(0, deck.size())
        assertTrue(deck.isEmpty())
    }

    @Test
    fun `add card should increase size`() {
        val deck = Deck()
        val card = ActionCard("Test", "Test card", ActionType.ANALYSIS)
        deck.add(card)
        assertEquals(1, deck.size())
    }

    @Test
    fun `draw should return null from empty deck`() {
        val deck = Deck()
        assertNull(deck.draw())
    }

    @Test
    fun `draw should return card and remove it`() {
        val deck = Deck()
        val card = ActionCard("Test", "Test card", ActionType.ANALYSIS)
        deck.add(card)
        val drawn = deck.draw()
        assertNotNull(drawn)
        assertEquals("Test", drawn?.name)
        assertEquals(0, deck.size())
    }

    @Test
    fun `draw should return cards in order`() {
        val deck = Deck()
        val card1 = ActionCard("First", "First card", ActionType.FLAMETHROWER)
        val card2 = ActionCard("Second", "Second card", ActionType.ANALYSIS)
        deck.add(card1)
        deck.add(card2)
        assertEquals("First", deck.draw()?.name)
        assertEquals("Second", deck.draw()?.name)
    }

    @Test
    fun `shuffle should keep same number of cards`() {
        val deck = Deck()
        repeat(10) { deck.add(ActionCard("Card $it", "Desc", ActionType.ANALYSIS)) }
        val sizeBefore = deck.size()
        deck.shuffle()
        assertEquals(sizeBefore, deck.size())
    }

    @Test
    fun `addAll should add multiple cards`() {
        val deck = Deck()
        val cards =
            listOf(
                ActionCard("A", "a", ActionType.ANALYSIS),
                ActionCard("B", "b", ActionType.FLAMETHROWER),
            )
        deck.addAll(cards)
        assertEquals(2, deck.size())
    }
}
