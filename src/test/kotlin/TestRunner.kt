import data.*
import model.*
import systems.*
import gui.*

fun main() {
    println("=".repeat(60))
    println("🧪 ЗАПУСК ВСЕХ ТЕСТОВ")
    println("=".repeat(60))
    println()

    var allPassed = true

    try {
        // Unit тесты — Model
        println("─── UNIT TESTS: MODEL ───")
        DeckTest().test()
        DiscardPileTest().test()
        PlayerTest().test()
        CardTest().test()
        println()

        // Unit тесты — Systems
        println("─── UNIT TESTS: SYSTEMS ───")
        GameEngineTest().test()
        println()

        // Unit тесты — Data
        println("─── UNIT TESTS: DATA ───")
        DatabaseTest().test()
        println()

        // Интеграционные тесты
        println("─── INTEGRATION TESTS ───")
        IntegrationTest().test()
        println()

        // Системные тесты
        println("─── SYSTEM TESTS ───")
        SystemTest().test()
        println()

        println("=".repeat(60))
        println("✅ ВСЕ ТЕСТЫ ПРОЙДЕНЫ УСПЕШНО!")
        println("=".repeat(60))
    } catch (e: AssertionError) {
        println()
        println("=".repeat(60))
        println("❌ ТЕСТ ПРОВАЛЕН: ${e.message}")
        println("=".repeat(60))
        allPassed = false
    }

    if (allPassed) {
        println("\nЗапуск GUI...")
        val gui = GameGUI()
        Thread.sleep(3000)
        gui.dispose()
        println("GUI успешно запущен и закрыт.")
    }
}
