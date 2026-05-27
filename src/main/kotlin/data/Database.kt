package data

import enums.Role
import model.Player
import java.sql.*

data class GameStats(
    val totalGames: Int = 0,
    val humanWins: Int = 0,
    val thingWins: Int = 0,
    val totalPlayers: Int = 0,
    val topPlayers: List<List<Any>> = emptyList(),
)

class GameDatabase(private val dbPath: String = "thing_game.db") {
    private val dbUrl = "jdbc:sqlite:$dbPath"

    init {
        try {
            Class.forName("org.sqlite.JDBC")
        } catch (e: ClassNotFoundException) {
            println("SQLite driver not found: ${e.message}")
        }
        createTables()
    }

    private fun connect(): Connection = DriverManager.getConnection(dbUrl)

    private fun createTables() {
        try {
            connect().use { conn ->
                conn.createStatement().apply {
                    execute(
                        """
                        CREATE TABLE IF NOT EXISTS players (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            name TEXT UNIQUE NOT NULL,
                            total_games INTEGER DEFAULT 0,
                            wins INTEGER DEFAULT 0,
                            score INTEGER DEFAULT 0,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """,
                    )

                    execute(
                        """
                        CREATE TABLE IF NOT EXISTS games (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            winner TEXT NOT NULL,
                            total_players INTEGER NOT NULL,
                            turns_played INTEGER DEFAULT 0,
                            thing_player TEXT,
                            duration_seconds INTEGER DEFAULT 0,
                            played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """,
                    )
                }
            }
        } catch (e: SQLException) {
            println("Database error: ${e.message}")
        }
    }

    fun addPlayer(name: String) {
        try {
            connect().use { conn ->
                val sql = """
                    INSERT INTO players (name, total_games) VALUES (?, 1)
                    ON CONFLICT(name) DO UPDATE SET total_games = total_games + 1
                """
                conn.prepareStatement(sql).apply {
                    setString(1, name)
                    executeUpdate()
                }
            }
        } catch (e: SQLException) {
            println("Error adding player: ${e.message}")
        }
    }

    fun saveGame(
        winner: String,
        players: List<Player>,
        thingPlayer: String,
        turnsPlayed: Int,
    ) {
        try {
            connect().use { conn ->
                val gameStmt =
                    conn.prepareStatement(
                        """
                    INSERT INTO games (winner, total_players, turns_played, thing_player)
                    VALUES (?, ?, ?, ?)
                """,
                    )

                gameStmt.apply {
                    setString(1, winner)
                    setInt(2, players.size)
                    setInt(3, turnsPlayed)
                    setString(4, thingPlayer)
                    executeUpdate()
                }

                // Обновляем статистику
                if (winner == "HUMANS") {
                    players.filter { it.role == Role.HUMAN && it.isAlive }.forEach { player ->
                        updatePlayerWin(player.name)
                    }
                } else {
                    players.filter { it.role == Role.THING }.forEach { player ->
                        updatePlayerWin(player.name)
                    }
                }
            }
        } catch (e: SQLException) {
            println("Error saving game: ${e.message}")
        }
    }

    private fun updatePlayerWin(name: String) {
        try {
            connect().use { conn ->
                val sql = """
                UPDATE players SET wins = wins + 1, score = wins + 1
                WHERE name = ?
            """
                conn.prepareStatement(sql).apply {
                    setString(1, name)
                    executeUpdate()
                }
            }
        } catch (e: SQLException) {
            println("Error updating player: ${e.message}")
        }
    }

    fun getStats(): GameStats {
        try {
            connect().use { conn ->
                val stmt = conn.createStatement()

                val totalGames = stmt.executeQuery("SELECT COUNT(*) FROM games").apply { next() }.getInt(1)
                val humanWins = stmt.executeQuery("SELECT COUNT(*) FROM games WHERE winner = 'HUMANS'").apply { next() }.getInt(1)
                val thingWins = stmt.executeQuery("SELECT COUNT(*) FROM games WHERE winner = 'THING'").apply { next() }.getInt(1)
                val totalPlayers = stmt.executeQuery("SELECT COUNT(*) FROM players").apply { next() }.getInt(1)

                val topPlayers = mutableListOf<List<Any>>()
                val rs = stmt.executeQuery("SELECT name, total_games, wins, score FROM players ORDER BY score DESC LIMIT 10")
                while (rs.next()) {
                    topPlayers.add(
                        listOf(
                            rs.getString("name"),
                            rs.getInt("total_games"),
                            rs.getInt("wins"),
                            rs.getInt("score"),
                        ),
                    )
                }

                return GameStats(
                    totalGames = totalGames,
                    humanWins = humanWins,
                    thingWins = thingWins,
                    totalPlayers = totalPlayers,
                    topPlayers = topPlayers,
                )
            }
        } catch (e: SQLException) {
            println("Error getting stats: ${e.message}")
            return GameStats()
        }
    }

    fun getAllPlayers(): List<List<Any>> {
        try {
            connect().use { conn ->
                val result = mutableListOf<List<Any>>()
                val rs =
                    conn.createStatement().executeQuery(
                        "SELECT name, total_games, wins, score FROM players ORDER BY name",
                    )
                while (rs.next()) {
                    result.add(
                        listOf(
                            rs.getString("name"),
                            rs.getInt("total_games"),
                            rs.getInt("wins"),
                            rs.getInt("score"),
                        ),
                    )
                }
                return result
            }
        } catch (e: SQLException) {
            println("Error getting players: ${e.message}")
            return emptyList()
        }
    }

    fun getGameHistory(limit: Int = 10): List<List<Any>> {
        try {
            connect().use { conn ->
                val result = mutableListOf<List<Any>>()
                val rs =
                    conn.createStatement().executeQuery(
                        "SELECT id, winner, turns_played, played_at, thing_player FROM games ORDER BY played_at DESC LIMIT $limit",
                    )
                while (rs.next()) {
                    result.add(
                        listOf(
                            rs.getInt("id"),
                            rs.getString("winner"),
                            rs.getInt("turns_played"),
                            rs.getString("played_at"),
                            rs.getString("thing_player"),
                        ),
                    )
                }
                return result
            }
        } catch (e: SQLException) {
            println("Error getting history: ${e.message}")
            return emptyList()
        }
    }

    fun getLastGameId(): Int {
        try {
            connect().use { conn ->
                val rs = conn.createStatement().executeQuery("SELECT MAX(id) FROM games")
                return if (rs.next()) rs.getInt(1) else 0
            }
        } catch (e: SQLException) {
            return 0
        }
    }

    fun close() {
        try {
            connect().close()
        } catch (e: SQLException) {
            // игнорируем
        }
    }
}
