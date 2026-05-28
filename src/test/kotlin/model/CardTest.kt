package model

import enums.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CardTest {
    @Test
    fun `action card should have correct type`() {
        val card = ActionCard("Test", "Description", ActionType.ANALYSIS)
        assertEquals(CardType.ACTION, card.type)
        assertEquals(ActionType.ANALYSIS, card.subType)
    }

    @Test
    fun `defense card should have correct type`() {
        val card = DefenseCard("Test", "Description", DefenseType.NO_THANKS)
        assertEquals(CardType.DEFENSE, card.type)
        assertEquals(DefenseType.NO_THANKS, card.subType)
    }

    @Test
    fun `obstacle card should have correct type`() {
        val card = ObstacleCard("Test", "Description", ObstacleType.QUARANTINE)
        assertEquals(CardType.OBSTACLE, card.type)
        assertEquals(ObstacleType.QUARANTINE, card.subType)
    }

    @Test
    fun `panic card should have correct type`() {
        val card = PanicCard("Test", "Description", PanicType.NO_PANIC)
        assertEquals(CardType.PANIC, card.type)
        assertEquals(PanicType.NO_PANIC, card.subType)
    }

    @Test
    fun `copy should create new instance with same values`() {
        val card = ActionCard("Fire", "Burn", ActionType.FLAMETHROWER)
        val copy = card.copy()
        assertEquals(card.name, copy.name)
        assertEquals(card.description, copy.description)
        assertEquals(card.subType, copy.subType)
        assertNotSame(card, copy)
    }
}
