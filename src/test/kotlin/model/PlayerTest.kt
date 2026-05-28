package model

import enums.ActionType
import enums.Role
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PlayerTest {
    @Test
    fun `player should be human by default`() {
        val player = Player("Test")
        assertEquals(Role.HUMAN, player.role)
    }

    @Test
    fun `player should be alive by default`() {
        val player = Player("Test")
        assertTrue(player.isAlive)
    }

    @Test
    fun `player should have empty hand by default`() {
        val player = Player("Test")
        assertEquals(0, player.hand.size)
    }

    @Test
    fun `kill should set isAlive to false and clear hand`() {
        val player = Player("Test")
        player.hand.add(ActionCard("Test", "Desc", ActionType.ANALYSIS))
        player.kill()
        assertFalse(player.isAlive)
        assertEquals(0, player.hand.size)
    }

    @Test
    fun `quarantine should be false by default`() {
        val player = Player("Test")
        assertFalse(player.hasQuarantine)
        assertEquals(0, player.quarantineTurns)
    }

    @Test
    fun `setQuarantine should set quarantine with turns`() {
        val player = Player("Test")
        player.setQuarantine(3)
        assertTrue(player.hasQuarantine)
        assertEquals(3, player.quarantineTurns)
    }

    @Test
    fun `removeQuarantine should clear quarantine`() {
        val player = Player("Test")
        player.setQuarantine(3)
        player.removeQuarantine()
        assertFalse(player.hasQuarantine)
        assertEquals(0, player.quarantineTurns)
    }

    @Test
    fun `decreaseQuarantine should reduce turns`() {
        val player = Player("Test")
        player.setQuarantine(3)
        player.decreaseQuarantine()
        assertEquals(2, player.quarantineTurns)
        assertTrue(player.hasQuarantine)
    }

    @Test
    fun `decreaseQuarantine should remove when zero`() {
        val player = Player("Test")
        player.setQuarantine(1)
        player.decreaseQuarantine()
        assertFalse(player.hasQuarantine)
        assertEquals(0, player.quarantineTurns)
    }
}
