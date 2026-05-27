package gui

import java.awt.*
import javax.swing.*
import javax.swing.border.*

class NewGameDialog(parent: JFrame) : JDialog(parent, "Новая игра", true) {
    private val nameFields = mutableListOf<JTextField>()
    var isConfirmed = false
        private set

    init {
        setSize(400, 500)
        setLocationRelativeTo(parent)

        val mainPanel = JPanel(BorderLayout())
        mainPanel.border = EmptyBorder(10, 10, 10, 10)

        val titleLabel = JLabel("👥 Введите имена игроков (4-12):")
        titleLabel.font = Font("Arial", Font.BOLD, 14)
        mainPanel.add(titleLabel, BorderLayout.NORTH)

        val playersPanel = JPanel()
        playersPanel.layout = BoxLayout(playersPanel, BoxLayout.Y_AXIS)

        // 6 полей по умолчанию
        repeat(6) {
            val textField = JTextField(20)
            textField.maximumSize = Dimension(300, 30)
            textField.font = Font("Arial", Font.PLAIN, 12)
            nameFields.add(textField)
            playersPanel.add(textField)
            playersPanel.add(Box.createVerticalStrut(5))
        }

        val buttonRow = JPanel(FlowLayout(FlowLayout.LEFT))
        val addButton = JButton("+ Добавить")
        val removeButton = JButton("- Убрать")

        addButton.addActionListener {
            if (nameFields.size < 12) {
                val textField = JTextField(20)
                textField.maximumSize = Dimension(300, 30)
                textField.font = Font("Arial", Font.PLAIN, 12)
                nameFields.add(textField)
                playersPanel.add(textField, playersPanel.componentCount - 1)
                playersPanel.revalidate()
                playersPanel.repaint()
            }
        }

        removeButton.addActionListener {
            if (nameFields.size > 4) {
                val lastField = nameFields.removeAt(nameFields.lastIndex)
                playersPanel.remove(lastField)
                playersPanel.revalidate()
                playersPanel.repaint()
            }
        }

        buttonRow.add(addButton)
        buttonRow.add(removeButton)
        playersPanel.add(buttonRow)

        val scrollPane = JScrollPane(playersPanel)
        scrollPane.border = TitledBorder("Имена игроков")
        mainPanel.add(scrollPane, BorderLayout.CENTER)

        val okButton = JButton("✅ Начать игру")
        val cancelButton = JButton("❌ Отмена")

        okButton.font = Font("Arial", Font.BOLD, 12)
        cancelButton.font = Font("Arial", Font.PLAIN, 12)

        okButton.addActionListener {
            val names = getPlayerNames()
            if (names.size in 4..12) {
                isConfirmed = true
                isVisible = false
            } else {
                JOptionPane.showMessageDialog(
                    this,
                    "Введите от 4 до 12 имён игроков!\nСейчас введено: ${names.size}",
                    "Ошибка",
                    JOptionPane.WARNING_MESSAGE,
                )
            }
        }

        cancelButton.addActionListener {
            isConfirmed = false
            isVisible = false
        }

        val bottomPanel = JPanel(FlowLayout(FlowLayout.CENTER))
        bottomPanel.add(okButton)
        bottomPanel.add(cancelButton)
        mainPanel.add(bottomPanel, BorderLayout.SOUTH)

        add(mainPanel)
    }

    fun getPlayerNames(): List<String> {
        return nameFields
            .map { it.text.trim() }
            .filter { it.isNotEmpty() }
    }
}
