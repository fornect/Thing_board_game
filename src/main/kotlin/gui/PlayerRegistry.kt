package gui

import java.io.*

data class PlayerStats(
    val name: String,
    var gamesPlayed: Int = 0,
    var wins: Int = 0,
    var score: Int = 0,
)

data class GameStats(
    val totalGames: Int = 0,
    val humanWins: Int = 0,
    val thingWins: Int = 0,
    val totalPlayers: Int = 0,
    val topPlayers: List<Pair<String, Int>> = emptyList(),
)

class PlayerRegistry {
    private val players = mutableMapOf<String, PlayerStats>()
    private var totalGames = 0
    private var humanWins = 0
    private var thingWins = 0

    private val saveFile = File("player_registry.dat")

    init {
        load()
    }

    fun addPlayers(names: List<String>) {
        names.forEach { name ->
            if (!players.containsKey(name)) {
                players[name] = PlayerStats(name)
            }
            players[name]?.gamesPlayed = (players[name]?.gamesPlayed ?: 0) + 1
        }
        totalGames++
        save()
    }

    fun recordWin(winnerType: String) {
        if (winnerType == "HUMANS") {
            humanWins++
        } else if (winnerType == "THING") {
            thingWins++
        }
        save()
    }

    fun addScore(
        name: String,
        points: Int,
    ) {
        players[name]?.score = (players[name]?.score ?: 0) + points
        players[name]?.wins = (players[name]?.wins ?: 0) + 1
        save()
    }

    fun getAllPlayers(): List<PlayerStats> {
        return players.values.sortedByDescending { it.score }
    }

    fun getStats(): GameStats {
        val topPlayers =
            players.values
                .sortedByDescending { it.score }
                .take(10)
                .map { it.name to it.score }

        return GameStats(
            totalGames = totalGames,
            humanWins = humanWins,
            thingWins = thingWins,
            totalPlayers = players.size,
            topPlayers = topPlayers,
        )
    }

    private fun save() {
        try {
            ObjectOutputStream(FileOutputStream(saveFile)).use { oos ->
                oos.writeInt(totalGames)
                oos.writeInt(humanWins)
                oos.writeInt(thingWins)
                oos.writeInt(players.size)
                players.values.forEach { player ->
                    oos.writeUTF(player.name)
                    oos.writeInt(player.gamesPlayed)
                    oos.writeInt(player.wins)
                    oos.writeInt(player.score)
                }
            }
        } catch (e: IOException) {
            // Файл не может быть сохранён
        }
    }

    private fun load() {
        if (!saveFile.exists()) return

        try {
            ObjectInputStream(FileInputStream(saveFile)).use { ois ->
                totalGames = ois.readInt()
                humanWins = ois.readInt()
                thingWins = ois.readInt()
                val count = ois.readInt()

                repeat(count) {
                    val name = ois.readUTF()
                    val gamesPlayed = ois.readInt()
                    val wins = ois.readInt()
                    val score = ois.readInt()
                    players[name] = PlayerStats(name, gamesPlayed, wins, score)
                }
            }
        } catch (e: IOException) {
            // Файл повреждён
        }
    }
}
