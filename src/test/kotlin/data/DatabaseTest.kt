package data

import enums.Role
import model.Player
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

class DatabaseTest {
    @Test
    fun `tables are created successfully`() {
        val db = GameDatabase("test_game.db")
        val stats = db.getStats()
        assertNotNull(stats)
        assertTrue(stats.totalGames >= 0)
        File("test_game.db").delete()
    }

    @Test
    fun `addPlayer adds new player`() {
        val db = GameDatabase("test_add.db")
        db.addPlayer("TestPlayer")
        val stats = db.getStats()
        assertTrue(stats.totalPlayers >= 1)
        File("test_add.db").delete()
    }

    @Test
    fun `saveGame records game`() {
        val db = GameDatabase("test_save.db")
        val players =
            listOf(
                Player("Анна").apply {
                    role = Role.HUMAN
                    isAlive = true
                },
                Player("Борис").apply {
                    role = Role.THING
                    isAlive = true
                },
            )
        db.saveGame("HUMANS", players, "Борис", 10)
        val stats = db.getStats()
        assertTrue(stats.totalGames >= 1)
        File("test_save.db").delete()
    }

    @Test
    fun `getAllPlayers returns list`() {
        val db = GameDatabase("test_list.db")
        db.addPlayer("Player1")
        val players = db.getAllPlayers()
        assertTrue(players.isNotEmpty())
        File("test_list.db").delete()
    }

    @Test
    fun `getGameHistory returns list`() {
        val db = GameDatabase("test_history.db")
        val history = db.getGameHistory(5)
        assertNotNull(history)
        File("test_history.db").delete()
    }
}
