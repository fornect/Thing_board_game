import gui.GameGUI
import systems.ConsoleGame
import javax.swing.SwingUtilities
import javax.swing.UIManager

fun main(args: Array<String>) {
    if (args.contains("--console") || args.contains("-c")) {
        // Консольная версия
        ConsoleGame().start()
    } else {
        // GUI версия (по умолчанию)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (e: Exception) {}

        SwingUtilities.invokeLater {
            GameGUI()
        }
    }
}