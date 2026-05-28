package gui

import data.GameDatabase
import enums.*
import model.*
import systems.GameEngine
import java.awt.*
import javax.swing.*
import javax.swing.border.*

class GameGUI : JFrame("НЕЧТО: Из Глубокой Бездны") {
    private val engine = GameEngine()
    private val database = GameDatabase()

    private val mainPanel = JPanel(BorderLayout())
    private val playerListPanel = JPanel()
    private val gameLogArea = JTextArea(15, 50)
    private val statusLabel = JLabel("Добро пожаловать в игру!")
    private val directionLabel = JLabel("↻")
    private val phaseLabel = JLabel("")

    private val startButton = JButton("Новая игра")
    private val statsButton = JButton("Статистика")
    private val registerButton = JButton("Реестр")
    private val historyButton = JButton("История игр")

    private var turnDialog: JDialog? = null
    private var actionDone = false
    private var cardDrawn = false
    private var panicHappened = false
    private var exchangeDone = false
    private var quarantineJustPlaced = false

    private var gameStartTime: Long = 0
    private var turnsPlayed = 0
    private var gameOver = false

    init {
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        setSize(1000, 750)
        setLocationRelativeTo(null)
        setupMenuBar()
        setupUI()
        isVisible = true
        updateButtonStates()
        logMessage("🎮 Добро пожаловать в игру 'НЕЧТО: Из Глубокой Бездны'!")
        logMessage("📋 Нажмите 'Новая игра' для старта")
    }

    private fun setupMenuBar() {
        val menuBar = JMenuBar()
        val gameMenu = JMenu("Игра")
        gameMenu.add(JMenuItem("Новая игра").apply { addActionListener { startNewGame() } })
        gameMenu.addSeparator()
        gameMenu.add(JMenuItem("Статистика").apply { addActionListener { showStats() } })
        gameMenu.add(JMenuItem("Реестр игроков").apply { addActionListener { showRegistry() } })
        gameMenu.add(JMenuItem("История игр").apply { addActionListener { showHistory() } })
        gameMenu.addSeparator()
        gameMenu.add(
            JMenuItem("Выход").apply {
                addActionListener {
                    database.close()
                    System.exit(0)
                }
            },
        )
        menuBar.add(gameMenu)
        jMenuBar = menuBar
    }

    private fun setupUI() {
        val topPanel = JPanel(BorderLayout())
        topPanel.border = EmptyBorder(5, 10, 5, 10)
        topPanel.background = Color(240, 240, 240)

        val infoPanel = JPanel(GridLayout(3, 1))
        statusLabel.font = Font("Arial", Font.BOLD, 16)
        directionLabel.font = Font("Arial", Font.BOLD, 24)
        phaseLabel.font = Font("Arial", Font.PLAIN, 12)
        infoPanel.add(statusLabel)
        infoPanel.add(phaseLabel)
        infoPanel.add(directionLabel)
        topPanel.add(infoPanel, BorderLayout.CENTER)

        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        startButton.addActionListener { startNewGame() }
        statsButton.addActionListener { showStats() }
        registerButton.addActionListener { showRegistry() }
        historyButton.addActionListener { showHistory() }
        buttonPanel.add(startButton)
        buttonPanel.add(statsButton)
        buttonPanel.add(registerButton)
        buttonPanel.add(historyButton)
        topPanel.add(buttonPanel, BorderLayout.EAST)

        mainPanel.add(topPanel, BorderLayout.NORTH)

        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)

        playerListPanel.layout = BoxLayout(playerListPanel, BoxLayout.Y_AXIS)
        playerListPanel.border = TitledBorder("Игроки")
        val playerScrollPane = JScrollPane(playerListPanel)
        playerScrollPane.preferredSize = Dimension(280, 0)

        gameLogArea.isEditable = false
        gameLogArea.font = Font("Monospaced", Font.PLAIN, 13)
        gameLogArea.background = Color(30, 30, 30)
        gameLogArea.foreground = Color(200, 255, 200)
        val logScrollPane = JScrollPane(gameLogArea)
        logScrollPane.border = TitledBorder("Игровой лог")

        splitPane.leftComponent = playerScrollPane
        splitPane.rightComponent = logScrollPane
        splitPane.dividerLocation = 300

        mainPanel.add(splitPane, BorderLayout.CENTER)
        add(mainPanel)
    }

    private fun showMessage(
        title: String,
        message: String,
    ) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE)
    }

    private fun showWarning(
        title: String,
        message: String,
    ) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.WARNING_MESSAGE)
    }

    // ==================== ИГРОВЫЕ МЕТОДЫ ====================

    private fun startNewGame() {
        gameOver = false
        val dialog = NewGameDialog(this)
        dialog.isVisible = true

        if (dialog.isConfirmed) {
            val playerNames = dialog.getPlayerNames()

            // 1. Количество
            if (playerNames.size !in 4..12) {
                showWarning("Ошибка", "Нужно от 4 до 12 игроков!\nВы ввели: ${playerNames.size}")
                startNewGame()
                return
            }

            // 2. Пустые имена
            if (playerNames.any { it.isBlank() }) {
                showWarning("Ошибка", "Имена не могут быть пустыми!")
                startNewGame()
                return
            }

            // 3. Дубликаты
            if (playerNames.size != playerNames.distinct().size) {
                showWarning("Ошибка", "Имена не должны повторяться!")
                startNewGame()
                return
            }

            // 4. Длина
            if (playerNames.any { it.length > 20 }) {
                showWarning("Ошибка", "Имена не могут быть длиннее 20 символов!")
                startNewGame()
                return
            }

            gameLogArea.text = ""
            playerNames.forEach { database.addPlayer(it) }

            val result = engine.setupGame(playerNames)
            logMessage(result)

            gameStartTime = System.currentTimeMillis()
            turnsPlayed = 0

            resetTurnState()
            updateDisplay()
            updateButtonStates()

            logMessage("=".repeat(60))
            logMessage("🎮 ИГРА НАЧАЛАСЬ!")
            logMessage("=".repeat(60))

            showTurnWindow()
        }
    }

    private fun showTurnWindow() {
        if (gameOver) return
        val player = engine.getCurrentPlayer() ?: return

        turnDialog?.dispose()
        turnDialog = JDialog(this, "Ход: ${player.name}", false)
        turnDialog?.setSize(550, 500)
        turnDialog?.setLocationRelativeTo(this)

        val panel = JPanel(BorderLayout())
        panel.border = EmptyBorder(10, 10, 10, 10)

        val infoPanel = JPanel()
        infoPanel.layout = BoxLayout(infoPanel, BoxLayout.Y_AXIS)

        val turnLabel = JLabel("🎯 Ход ${engine.getTurnNumber() + 1}: ${player.name}")
        turnLabel.font = Font("Arial", Font.BOLD, 14)

        val roleLabel = JLabel("Роль: ${player.role}")
        val handLabel = JLabel("Карт в руке: ${player.hand.size}")
        val cardsText = player.hand.joinToString("<br>• ") { "${it.name} (${it.description})" }
        val cardsLabel = JLabel("<html>Карты:<br>• $cardsText</html>")

        val phaseDesc =
            when {
                player.hasQuarantine && quarantineJustPlaced -> "🦠 Вы поставили карантин на себя! Ход завершается"
                player.hasQuarantine -> "🦠 КАРАНТИН — можно только Топор на себя или сброс"
                !cardDrawn -> "📤 Фаза 1: Взятие карты"
                panicHappened -> "😱 ПАНИКА! Действие пропущено"
                !actionDone -> "🎮 Фаза 2: Действие (сыграть или сбросить)"
                else -> "🔄 Фаза 3: Обмен с соседом"
            }
        val phaseDescLabel = JLabel(phaseDesc)
        phaseDescLabel.font = Font("Arial", Font.BOLD, 12)
        phaseDescLabel.foreground = if (panicHappened) Color.RED else Color.BLUE

        infoPanel.add(turnLabel)
        infoPanel.add(Box.createVerticalStrut(5))
        infoPanel.add(phaseDescLabel)
        infoPanel.add(Box.createVerticalStrut(10))
        infoPanel.add(roleLabel)
        infoPanel.add(handLabel)
        infoPanel.add(cardsLabel)

        panel.add(infoPanel, BorderLayout.NORTH)

        if (quarantineJustPlaced) {
            quarantineJustPlaced = false
            actionDone = true
            exchangeDone = true
            javax.swing.Timer(500) { finishTurn(player) }.apply {
                isRepeats = false
                start()
            }
            turnDialog?.add(panel)
            turnDialog?.isVisible = true
            return
        }

        val buttonPanel = JPanel(GridLayout(0, 1, 5, 5))
        buttonPanel.border = TitledBorder("Фазы хода")

        val drawButton = JButton("📤 Фаза 1: Взять карту")
        val actionButton = JButton("🎮 Фаза 2: Действие")
        val exchangeTurnButton = JButton("🔄 Фаза 3: Обмен")

        if (player.hasQuarantine) {
            drawButton.isEnabled = !cardDrawn
            actionButton.isEnabled = cardDrawn
            exchangeTurnButton.isEnabled = false
            drawButton.text = "📤 Взять карту (карантин)"
            actionButton.text = "🎮 Топор на себя / Сброс"
            exchangeTurnButton.text = "🔄 Обмен недоступен (карантин)"
        } else {
            drawButton.isEnabled = !cardDrawn
            actionButton.isEnabled = cardDrawn && !actionDone && !panicHappened
            exchangeTurnButton.isEnabled = cardDrawn && (actionDone || panicHappened) && !exchangeDone
            drawButton.text = "📤 Фаза 1: Взять карту из колоды"
            actionButton.text = "🎮 Фаза 2: Действие (сыграть/сбросить)"
            exchangeTurnButton.text = "🔄 Фаза 3: Обмен с соседом (завершает ход)"
        }

        drawButton.addActionListener {
            val result = engine.drawCard(player)
            logMessage(result.message)
            cardDrawn = true

            when (result) {
                is GameEngine.GameResult.Panic -> {
                    panicHappened = true
                    showMessage("😱 ПАНИКА!", result.message)
                }

                is GameEngine.GameResult.PanicExchange -> {
                    panicHappened = true
                    handlePanicExchange(result)
                }

                is GameEngine.GameResult.Error -> logMessage("❌ ${result.message}")

                else -> {}
            }

            updateTurnWindow()
            updateDisplay()
        }

        actionButton.addActionListener {
            if (gameOver) return@addActionListener
            if (player.hasQuarantine) {
                showQuarantineActions(player)
                if (actionDone) finishTurn(player)
            } else {
                showActionDialog(player)
                if (quarantineJustPlaced) finishTurn(player)
            }
            updateTurnWindow()
            updateDisplay()
        }

        exchangeTurnButton.addActionListener {
            if (gameOver) return@addActionListener
            if (player.hasQuarantine) {
                showWarning("Карантин", "Обмен запрещён во время карантина!")
            } else {
                onExchange()
            }
            updateTurnWindow()
            updateDisplay()
        }

        buttonPanel.add(drawButton)
        buttonPanel.add(actionButton)
        buttonPanel.add(exchangeTurnButton)

        panel.add(buttonPanel, BorderLayout.CENTER)

        turnDialog?.add(panel)
        turnDialog?.isVisible = true
    }

    private fun handlePanicExchange(panic: GameEngine.GameResult.PanicExchange) {
        if (gameOver) return
        val player = panic.player

        when (panic.panicType) {
            PanicType.CALL_THAT_PARTY, PanicType.GET_OUT -> {
                val targetNames = panic.availableTargets.map { it.name }.toTypedArray()
                val choice =
                    JOptionPane.showInputDialog(
                        this,
                        panic.message,
                        "Выбор игрока",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        targetNames,
                        targetNames[0],
                    ) as? String

                if (choice != null) {
                    val target = panic.availableTargets.find { it.name == choice }
                    if (target != null) {
                        engine.swapPlayers(player, target)
                        logMessage("🔄 ПАНИКА: ${player.name} меняется с ${target.name}")
                        updateDisplay()
                    }
                }
            }

            PanicType.ONE_TWO, PanicType.LET_BE_FRIENDS -> {
                val cardNames = panic.availableCards.map { it.name }.toTypedArray()
                val cardChoice =
                    JOptionPane.showInputDialog(
                        this,
                        "${panic.message}\n\nВыберите карту для передачи:",
                        "Выбор карты",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        cardNames,
                        cardNames[0],
                    ) as? String

                if (cardChoice != null) {
                    val card = panic.availableCards.find { it.name == cardChoice }
                    val target = panic.availableTargets.first()

                    if (card != null) {
                        val targetCards = target.hand.filter { it.name != "НЕЧТО" }
                        if (targetCards.isNotEmpty()) {
                            val targetCard = targetCards.random()
                            engine.performExchange(player, target, card, targetCard)
                            logMessage("🔄 ПАНИКА: обмен с ${target.name}")
                            updateDisplay()
                        }
                    }
                }
            }

            else -> showMessage("Паника", panic.message)
        }
    }

    private fun showQuarantineActions(player: Player) {
        if (gameOver) return
        val hasAxe = player.hand.any { it.name == "Топор" }
        val discardable = player.hand.filter { it.name != "НЕЧТО" }

        val options = mutableListOf<String>()
        if (hasAxe) options.add("🪓 Сыграть Топор на себя (снимет карантин)")
        if (discardable.isNotEmpty()) options.add("🗑️ Сбросить любую карту")

        if (options.isEmpty()) {
            showMessage("Карантин", "Нет доступных действий.\nХод будет завершён.")
            actionDone = true
            return
        }

        val choice =
            JOptionPane.showOptionDialog(
                this,
                "${player.name}, вы на карантине!\n\nДоступные действия:",
                "🦠 Карантин",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options.toTypedArray(),
                options[0],
            )

        when {
            hasAxe && choice == 0 -> {
                val axeCard = player.hand.find { it.name == "Топор" }
                if (axeCard != null) {
                    val result = engine.playCard(player, axeCard, player)
                    logMessage(result.message)
                    showMessage("🪓 Топор", "Карантин снят!")
                }
            }

            choice == (if (hasAxe) 1 else 0) -> {
                val cardNames = discardable.map { it.name }.toTypedArray()
                val cardChoice =
                    JOptionPane.showInputDialog(
                        this,
                        "Выберите карту для сброса:",
                        "Сбросить карту",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        cardNames,
                        cardNames[0],
                    ) as? String
                if (cardChoice != null) {
                    val idx = cardNames.indexOf(cardChoice)
                    if (idx >= 0 && idx < discardable.size) {
                        logMessage(engine.discardCard(player, discardable[idx]).message)
                    }
                }
            }
        }
        actionDone = true
    }

    private fun showActionDialog(player: Player) {
        val options = arrayOf("Сыграть карту", "Сбросить карту")
        val choice =
            JOptionPane.showOptionDialog(
                this,
                "${player.name}, выберите действие:\n\nВаша рука:\n${player.hand.joinToString("\n") { "• ${it.name} (${it.description})" }}",
                "Фаза 2: Действие",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0],
            )
        when (choice) {
            0 -> actionDone = onPlayCard()
            1 -> actionDone = onDiscardCard()
        }
    }

    private fun onPlayCard(): Boolean {
        if (gameOver) return false
        val player = engine.getCurrentPlayer() ?: return false

        if (player.hasQuarantine) {
            showWarning("Карантин", "На карантине можно играть только Топор на себя!")
            return false
        }

        val playableCards = engine.getPlayableCards(player)
        if (playableCards.isEmpty()) {
            showMessage("Нет карт", "Нет карт, которые можно сыграть")
            return false
        }

        val cardNames = playableCards.map { "${it.name} (${it.description})" }.toTypedArray()
        val choice =
            JOptionPane.showInputDialog(
                this,
                "Выберите карту для игры:",
                "Сыграть карту",
                JOptionPane.QUESTION_MESSAGE,
                null,
                cardNames,
                cardNames[0],
            ) as? String ?: return false

        val idx = cardNames.indexOf(choice)
        if (idx < 0 || idx >= playableCards.size) return false

        val selectedCard = playableCards[idx]

        var target: Player? = null
        val needsTarget =
            selectedCard.name in
                listOf(
                    "Огнемёт",
                    "Анализ",
                    "Топор",
                    "Подозрение",
                    "Карантин",
                    "Заколоченная дверь",
                    "Меняемся местами!",
                    "Сматывай удочки!",
                    "Соблазн",
                )

        if (needsTarget) {
            val targets = engine.getTargets(player, selectedCard.name)
            if (targets.isEmpty()) {
                showMessage("Нет целей", "Нет доступных целей")
                return false
            }
            val targetNames = targets.map { "${it.name} ${if (it.hasQuarantine) "🦠" else ""}" }.toTypedArray()
            val targetChoice =
                JOptionPane.showInputDialog(
                    this,
                    "Выберите цель для '${selectedCard.name}':",
                    "Выбор цели",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    targetNames,
                    targetNames[0],
                ) as? String ?: return false
            val targetIdx = targetNames.indexOf(targetChoice)
            if (targetIdx >= 0) target = targets[targetIdx]
        }

        // УПОРСТВО
        if (selectedCard.name == "Упорство") {
            engine.playCard(player, selectedCard, null)
            logMessage("💪 ${player.name} использует УПОРСТВО!")

            val drawnCards =
                listOf(
                    ActionCard("Огнемёт", "Убить соседа", ActionType.FLAMETHROWER),
                    ActionCard("Анализ", "Посмотреть руку", ActionType.ANALYSIS),
                    ActionCard("Топор", "Снять карантин", ActionType.AXE),
                    ActionCard("Подозрение", "Посмотреть карту", ActionType.SUSPICION),
                    DefenseCard("Нет уж, спасибо!", "Отказ от обмена", DefenseType.NO_THANKS),
                    DefenseCard("Страх", "Отказ + просмотр", DefenseType.FEAR),
                    ObstacleCard("Карантин", "Блок на 3 хода", ObstacleType.QUARANTINE),
                    ObstacleCard("Заколоченная дверь", "Блок", ObstacleType.BARRICADED_DOOR),
                ).shuffled().take(3)

            val drawnNames = drawnCards.map { "${it.name} (${it.description})" }.toTypedArray()
            val keepChoice =
                JOptionPane.showInputDialog(
                    this,
                    "💪 УПОРСТВО!\n\nВзято 3 карты. Выберите одну для сохранения:",
                    "Упорство",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    drawnNames,
                    drawnNames[0],
                ) as? String

            if (keepChoice != null) {
                val keepIdx = drawnNames.indexOf(keepChoice)
                if (keepIdx >= 0) {
                    val keptCard = drawnCards[keepIdx]
                    player.hand.add(keptCard)
                    logMessage("   ✅ Оставлена: ${keptCard.name}")
                    drawnCards.forEachIndexed { i, card ->
                        if (i != keepIdx) {
                            engine.discardCardSilent(player, card)
                            logMessage("   🗑️ В сброс: ${card.name}")
                        }
                    }
                }
            }

            showMessage("💪 Упорство!", "Одна карта оставлена, две в сброс.\nФаза 2 продолжается.")
            updateDisplay()
            return false
        }

        // СМЕНА МЕСТ — защита "Мне и здесь неплохо"
        if (selectedCard.name in listOf("Меняемся местами!", "Сматывай удочки!")) {
            if (target != null && target.hand.any { it.name == "Мне и здесь неплохо" }) {
                val defenseChoice =
                    JOptionPane.showOptionDialog(
                        this,
                        "${target.name}, ${player.name} хочет поменяться с вами местами!\n\nУ вас есть защита: Мне и здесь неплохо",
                        "🛡️ Защита от смены мест",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        arrayOf("🛡️ Сыграть защиту", "✅ Согласиться"),
                        "Согласиться",
                    )

                if (defenseChoice == 0) {
                    val defenseCard = target.hand.find { it.name == "Мне и здесь неплохо" }
                    if (defenseCard != null) {
                        target.hand.remove(defenseCard)
                        val newCard = engine.drawCardSilent()
                        if (newCard != null) target.hand.add(newCard)
                        engine.playCard(player, selectedCard, target)
                        logMessage("🏠 ${target.name} использует 'Мне и здесь неплохо' — смена отменена")
                        showMessage("🏠 Мне и здесь неплохо", "Смена отменена!\nВзята карта: ${newCard?.name ?: "нет"}")
                        return true
                    }
                }
            }

            val result = engine.playCard(player, selectedCard, target)
            logMessage(result.message)
            showMessage(if (selectedCard.name == "Меняемся местами!") "💺 Смена мест" else "💨 Смена мест", result.message)
            updateDisplay()
            return true
        }

        val result = engine.playCard(player, selectedCard, target)
        logMessage(result.message)

        if (result is GameEngine.GameResult.ExchangeInfo) {
            showExchangeDialogFromResult(result)
            finishTurn(player)
            return true
        }

        when (selectedCard.name) {
            "Огнемёт" -> showMessage("🔥 Огнемёт", result.message)

            "Анализ" -> showMessage("🔍 Анализ", result.message)

            "Топор" -> showMessage("🪓 Топор", result.message)

            "Подозрение" -> showMessage("🔎 Подозрение", result.message)

            "Виски" -> showMessage("🥃 Виски", result.message)

            "Гляди по сторонам" -> showMessage("👀 Направление", result.message)

            "Карантин" -> {
                if (target == player) {
                    quarantineJustPlaced = true
                    showMessage("🦠 Карантин на себя", "Ход завершается")
                } else {
                    showMessage("🦠 Карантин", result.message)
                }
            }

            "Заколоченная дверь" -> showMessage("🚪 Дверь", result.message)

            else -> showMessage(selectedCard.name, result.message)
        }

        if (selectedCard.name == "Карантин" && target == player) quarantineJustPlaced = true
        if (selectedCard.name == "Соблазн") {
            finishTurn(player)
            return true
        }

        updateDisplay()
        return true
    }

    private fun onDiscardCard(): Boolean {
        val player = engine.getCurrentPlayer() ?: return false
        val disc = engine.getDiscardableCards(player)
        if (disc.isEmpty()) {
            showMessage("Нет карт", "Нет карт для сброса")
            return false
        }

        val names = disc.map { it.name }.toTypedArray()
        val choice =
            JOptionPane.showInputDialog(
                this,
                "Выберите карту для сброса:",
                "Сбросить карту",
                JOptionPane.QUESTION_MESSAGE,
                null,
                names,
                names[0],
            ) as? String ?: return false
        val idx = names.indexOf(choice)
        if (idx !in disc.indices) return false

        logMessage(engine.discardCard(player, disc[idx]).message)
        updateDisplay()
        return true
    }

    private fun onExchange() {
        if (gameOver) return
        val player = engine.getCurrentPlayer() ?: return
        val result = engine.executeExchange(player)

        if (result is GameEngine.GameResult.ExchangeInfo) {
            showExchangeDialogFromResult(result)
        } else {
            logMessage(result.message)
            showWarning("Обмен невозможен", result.message)
        }
        exchangeDone = true
        finishTurn(player)
    }

    private fun showExchangeDialogFromResult(exInfo: GameEngine.GameResult.ExchangeInfo) {
        if (gameOver) return
        val player = exInfo.player
        val neighbor = exInfo.neighbor

        val card1 =
            showCardSelectionDialog(
                "${player.name}, выберите карту для ${neighbor.name}:",
                exInfo.playerCards,
                player.role,
            ) ?: return

        if (card1.name == "Заражение!" && player.role != Role.THING) {
            showWarning("Ошибка", "❌ Только НЕЧТО может передавать Заражение!")
            return
        }

        val receiverDefense = neighbor.hand.find { it.name in GameEngine.DEFENSE_CARDS }

        if (receiverDefense != null) {
            val defenseChoice =
                JOptionPane.showOptionDialog(
                    this,
                    "${neighbor.name}, предлагают: ${card1.name}\nУ вас есть: ${receiverDefense.name}",
                    "🛡️ Защита",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    arrayOf("Сыграть защиту", "Принять обмен"),
                    "Принять обмен",
                )

            if (defenseChoice == 0) {
                neighbor.hand.remove(receiverDefense)
                val defResult = engine.handleDefense(neighbor, player, card1)
                logMessage(defResult.message)

                if (defResult is GameEngine.GameResult.PassExchange) {
                    showMessage("➡️ Мимо!", defResult.message)
                    val nextResult = engine.executeExchange(defResult.nextPlayer)
                    if (nextResult is GameEngine.GameResult.ExchangeInfo) {
                        showExchangeDialogFromResult(nextResult)
                    }
                    return
                }

                showMessage("🛡️ Защита", defResult.message)
                return
            }
        }

        val card2 =
            showCardSelectionDialog(
                "${neighbor.name}, выберите карту для ${player.name}:",
                exInfo.neighborCards,
                neighbor.role,
            ) ?: return

        if (card2.name == "Заражение!" && neighbor.role != Role.THING) {
            showWarning("Ошибка", "❌ Только НЕЧТО может передавать Заражение!")
            return
        }

        val r = engine.performExchange(player, neighbor, card1, card2)
        logMessage(r.message)
        showMessage("🔄 Обмен", "${player.name} отдал: ${card1.name}\nПолучил: ${card2.name}")
    }

    private fun showCardSelectionDialog(
        title: String,
        cards: List<Card>,
        viewerRole: Role,
    ): Card? {
        val dialog =
            JDialog(this, "Выбор карты", true).apply {
                setSize(400, 350)
                setLocationRelativeTo(this@GameGUI)
            }
        val panel = JPanel(BorderLayout()).apply { border = EmptyBorder(10, 10, 10, 10) }
        panel.add(JLabel("<html>$title</html>").apply { font = Font("Arial", Font.BOLD, 12) }, BorderLayout.NORTH)

        val cp = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }
        val bg = ButtonGroup()
        val btns = mutableListOf<JRadioButton>()
        cards.forEach { c ->
            val dn = if (c.name == "Заражение!" && viewerRole != Role.THING) "????" else c.name
            bg.add(
                JRadioButton(dn).apply {
                    putClientProperty("card", c)
                    btns.add(this)
                    cp.add(this)
                },
            )
        }
        if (btns.isNotEmpty()) btns[0].isSelected = true
        panel.add(JScrollPane(cp), BorderLayout.CENTER)

        var sel: Card? = null
        val bp = JPanel(FlowLayout(FlowLayout.CENTER))
        bp.add(
            JButton("✅ Выбрать").apply {
                addActionListener {
                    sel = cards[btns.indexOfFirst { it.isSelected }]
                    dialog.dispose()
                }
            },
        )
        bp.add(JButton("Отмена").apply { addActionListener { dialog.dispose() } })
        panel.add(bp, BorderLayout.SOUTH)
        dialog.add(panel)
        dialog.isVisible = true
        return sel
    }

    private fun finishTurn(player: Player) {
        if (gameOver) return
        turnsPlayed++
        val result = engine.endTurn(player)
        logMessage(result.message)
        resetTurnState()
        turnDialog?.dispose()
        updateDisplay()

        val winner = engine.checkVictory()
        if (winner != null) endGame(winner) else showTurnWindow()
    }

    private fun endGame(winner: String) {
        gameOver = true
        turnDialog?.dispose()
        turnDialog = null

        for (window in this.ownedWindows) {
            window.dispose()
        }

        val players = engine.getPlayers()
        val thingPlayer = players.find { it.role == Role.THING }

        database.saveGame(
            winner = winner,
            players = players,
            thingPlayer = thingPlayer?.name ?: "неизвестно",
            turnsPlayed = turnsPlayed,
        )

        val message =
            if (winner == "HUMANS") {
                "🎉 ЛЮДИ ПОБЕДИЛИ!\n\nНЕЧТО уничтожено!"
            } else {
                "👾 НЕЧТО ПОБЕДИЛО!\n\nВсе люди заражены или убиты!"
            }

        logMessage("\n" + "=".repeat(60))
        logMessage(message)
        logMessage("=".repeat(60))

        resetTurnState()
        updateDisplay()

        JOptionPane.showMessageDialog(this, message, "Игра окончена!", JOptionPane.INFORMATION_MESSAGE)

        statusLabel.text = "Игра завершена"
        phaseLabel.text = ""
        logMessage("📋 Нажмите 'Новая игра' для старта")
    }

    private fun resetTurnState() {
        cardDrawn = false
        actionDone = false
        panicHappened = false
        exchangeDone = false
        quarantineJustPlaced = false
    }

    private fun updateTurnWindow() {
        turnDialog?.dispose()
        engine.getCurrentPlayer()?.let { showTurnWindow() }
    }

    private fun updateDisplay() {
        val players = engine.getPlayers()
        val currentPlayer = engine.getCurrentPlayer()

        playerListPanel.removeAll()

        for (player in players) {
            val isCurrent = player == currentPlayer
            val borderColor =
                when {
                    !player.isAlive -> Color.GRAY
                    isCurrent -> Color.BLUE
                    player.role == Role.THING -> Color.RED
                    player.role == Role.INFECTED -> Color.ORANGE
                    else -> Color.BLACK
                }

            val pp =
                JPanel(BorderLayout()).apply {
                    border = CompoundBorder(LineBorder(borderColor, if (isCurrent) 3 else 1), EmptyBorder(5, 5, 5, 5))
                    maximumSize = Dimension(270, 75)
                    background =
                        when {
                            isCurrent -> Color(230, 240, 255)
                            !player.isAlive -> Color(240, 240, 240)
                            else -> Color.WHITE
                        }
                    isOpaque = true
                }

            val icon =
                when {
                    !player.isAlive -> "💀"
                    player.role == Role.THING -> "👾"
                    player.role == Role.INFECTED -> "🧟"
                    else -> "👤"
                }
            pp.add(JLabel("$icon ${player.name}").apply { font = Font("Arial", Font.BOLD, if (isCurrent) 14 else 12) }, BorderLayout.NORTH)

            val ip =
                JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                    isOpaque = false
                }
            ip.add(
                JLabel(
                    when {
                        !player.isAlive -> "💀 Мёртв"
                        player.hasQuarantine -> "🦠 Карантин (${player.quarantineTurns})"
                        else -> "✅ Жив"
                    },
                ),
            )
            ip.add(
                JLabel(
                    when (player.role) {
                        Role.HUMAN -> "👤 Человек"
                        Role.THING -> "👾 НЕЧТО"
                        Role.INFECTED -> "🧟 Заражённый"
                    },
                ),
            )
            ip.add(JLabel("🎴 ${player.hand.size} карт"))

            pp.add(ip, BorderLayout.CENTER)
            playerListPanel.add(pp)
            playerListPanel.add(Box.createVerticalStrut(5))
        }

        directionLabel.text = if (engine.getDirection() == 1) "↻ По часовой" else "↺ Против"
        val cp = engine.getCurrentPlayer()
        if (cp != null) {
            statusLabel.text = "Ход ${engine.getTurnNumber() + 1}: ${cp.name} (${cp.role})"
            phaseLabel.text =
                when {
                    cp.hasQuarantine -> "🦠 Карантин"
                    !cardDrawn -> "📤 Фаза 1"
                    panicHappened -> "😱 Паника"
                    !actionDone -> "🎮 Фаза 2"
                    else -> "🔄 Фаза 3"
                }
        }

        playerListPanel.revalidate()
        playerListPanel.repaint()
    }

    private fun updateButtonStates() {}

    private fun showStats() {
        val stats = database.getStats()
        val sb = StringBuilder()
        sb.append("Игр: ${stats.totalGames} | Люди: ${stats.humanWins} | НЕЧТО: ${stats.thingWins}\n")
        showMessage("Статистика", sb.toString())
    }

    private fun showRegistry() {
        val players = database.getAllPlayers()
        val sb = StringBuilder()
        players.forEach { player ->
            sb.append("${player[0]}: игр ${player[1]}, побед ${player[2]}\n")
        }
        showMessage("Реестр", sb.toString().ifEmpty { "Пусто" })
    }

    private fun showHistory() {
        val history = database.getGameHistory(20)
        val sb = StringBuilder()
        history.forEach { game ->
            sb.append("Игра #${game[0]} | ${(game[3] as String).take(16)} | ${game[1]}\n")
        }
        showMessage("История", sb.toString().ifEmpty { "Пусто" })
    }

    fun logMessage(message: String) {
        gameLogArea.append("$message\n")
        gameLogArea.caretPosition = gameLogArea.document.length
    }
}
