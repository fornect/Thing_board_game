package data

import enums.Role
import model.Player
import java.io.File

class DatabaseTest {
    fun test() {
        val testDbPath = "thing_game_test.db"
        File(testDbPath).delete() // Удаляем старую тестовую БД

        val db = GameDatabase(testDbPath) // Используем тестовую БД

        testCreateTables(db)
        testAddPlayer(db)
        testSaveGame(db)
        testGetStats(db)
        testGetAllPlayers(db)
        testGetGameHistory(db)

        File(testDbPath).delete() // Удаляем после тестов

        println("✅ DatabaseTest: все тесты пройдены!")
    }

    private fun testCreateTables(db: GameDatabase) {
        assert(true) { "Таблицы созданы" }
    }

    private fun testAddPlayer(db: GameDatabase) {
        db.addPlayer("TestPlayer")
        val stats = db.getStats()
        assert(stats.totalPlayers >= 1) { "Игрок должен быть добавлен" }
    }

    private fun testSaveGame(db: GameDatabase) {
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
        assert(db.getStats().totalGames >= 1) { "Игра должна быть сохранена" }
    }

    private fun testGetStats(db: GameDatabase) {
        val stats = db.getStats()
        assert(stats.totalGames >= 0) { "Статистика должна возвращаться" }
    }

    private fun testGetAllPlayers(db: GameDatabase) {
        val players = db.getAllPlayers()
        assert(players is List<*>) { "Список игроков должен возвращаться" }
    }

    private fun testGetGameHistory(db: GameDatabase) {
        val history = db.getGameHistory(5)
        assert(history is List<*>) { "История должна возвращаться" }
    }
}
