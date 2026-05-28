package model

import enums.ActionType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DiscardPileTest {
    @Test
    fun `discard pile should be empty when created`() {
        val pile = DiscardPile()
        assertEquals(0, pile.size())
        assertTrue(pile.isEmpty())
    }

    @Test
    fun `add card should increase size`() {
        val pile = DiscardPile()
        val card = ActionCard("Test", "Desc", ActionType.ANALYSIS)
        pile.add(card)
        assertEquals(1, pile.size())
        assertTrue(pile.isNotEmpty())
    }

    @Test
    fun `takeAll should return all cards and clear pile`() {
        val pile = DiscardPile()
        pile.add(ActionCard("A", "a", ActionType.ANALYSIS))
        pile.add(ActionCard("B", "b", ActionType.FLAMETHROWER))
        val taken = pile.takeAll()
        assertEquals(2, taken.size)
        assertEquals(0, pile.size())
        assertTrue(pile.isEmpty())
    }

    @Test
    fun `clear should remove all cards`() {
        val pile = DiscardPile()
        pile.add(ActionCard("Test", "Desc", ActionType.ANALYSIS))
        pile.add(ActionCard("Test2", "Desc2", ActionType.FLAMETHROWER))
        pile.clear()
        assertEquals(0, pile.size())
    }
}
