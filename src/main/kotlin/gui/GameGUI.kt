package gui

import model.*
import enums.*
import systems.GameEngine
import data.GameDatabase
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
        logMessage("")
        logMessage("📖 ПРАВИЛА:")
        logMessage("   • Люди побеждают, если убьют НЕЧТО огнемётом")
        logMessage("   • НЕЧТО побеждает, если заразит всех людей")
        logMessage("   • Заражение передаётся только НЕЧТО при обмене")
        logMessage("   • Карта НЕЧТО не может быть сброшена или передана")
        logMessage("   • Карантин: только Топор на себя или сброс")
        logMessage("   • Заколоченная дверь блокирует обмен")
        logMessage("   • После защиты игрок берёт карту из колоды")
        logMessage("   • Мне и здесь неплохо — защита от смены мест")
        logMessage("   • Никакого шашлыка — защита от огнемёта")
        logMessage("   • Статистика сохраняется в SQLite базу данных")
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
        gameMenu.add(JMenuItem("Выход").apply { addActionListener { database.close(); System.exit(0) } })
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

    private fun showMessage(title: String, message: String) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE)
    }

    private fun showWarning(title: String, message: String) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.WARNING_MESSAGE)
    }

    // ==================== ИГРОВЫЕ МЕТОДЫ ====================

    private fun startNewGame() {
        gameOver = false
        val dialog = NewGameDialog(this)
        dialog.isVisible = true

        if (dialog.isConfirmed) {
            val playerNames = dialog.getPlayerNames()

            if (playerNames.size !in 4..12) {
                showWarning("Ошибка", "Нужно от 4 до 12 игроков!\nВы ввели: ${playerNames.size}")
                startNewGame()
                return
            }

            gameLogArea.text = ""

            // Регистрируем игроков в БД
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

        val phaseDesc = when {
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
            javax.swing.Timer(500) { finishTurn(player) }.apply { isRepeats = false; start() }
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
                    handlePanic(result, player)
                }
                is GameEngine.GameResult.PanicExchange -> {
                    panicHappened = true
                    handlePanicExchange(result)
                }
                is GameEngine.GameResult.Error -> {
                    logMessage("❌ ${result.message}")
                }
                else -> {
                }
            }

            updateTurnWindow()
            updateDisplay()
        }

        actionButton.addActionListener {
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

    private fun handlePanic(result: GameEngine.GameResult, player: Player) {
        when (result) {
            is GameEngine.GameResult.Panic -> {
                showMessage("Паника", result.message)
            }
            is GameEngine.GameResult.PanicExchange -> {
                handlePanicExchange(result)
            }
            is GameEngine.GameResult.Error -> {
                logMessage("❌ ${result.message}")
            }
            else -> {
            }
        }
    }

    private fun handlePanicExchange(panic: GameEngine.GameResult.PanicExchange) {
        val player = panic.player

        when (panic.panicType) {
            PanicType.GET_OUT -> {
                // Выбор с кем поменяться местами
                val targetNames = panic.availableTargets.map { it.name }.toTypedArray()
                val choice = JOptionPane.showInputDialog(
                    this, panic.message, "Выбор игрока",
                    JOptionPane.QUESTION_MESSAGE, null, targetNames, targetNames[0]
                ) as? String

                if (choice != null) {
                    val target = panic.availableTargets.find { it.name == choice }
                    if (target != null) {
                        // Меняем местами через engine
                        engine.getPlayers().let { players ->
                            val idx1 = players.indexOf(player)
                            val idx2 = players.indexOf(target)
                            // swapPlayers приватный, нужно сделать публичный метод
                        }
                        logMessage("🔄 ${player.name} меняется с ${target.name}")
                    }
                }
            }

            PanicType.ONE_TWO, PanicType.LET_BE_FRIENDS -> {
                // Сначала выбор карты
                val cardNames = panic.availableCards.map { it.name }.toTypedArray()
                val cardChoice = JOptionPane.showInputDialog(
                    this, "${panic.message}\n\nВыберите карту для передачи:",
                    "Выбор карты", JOptionPane.QUESTION_MESSAGE, null, cardNames, cardNames[0]
                ) as? String

                if (cardChoice != null) {
                    val card = panic.availableCards.find { it.name == cardChoice }
                    val target = panic.availableTargets.first()

                    if (card != null) {
                        val targetCards = target.hand.filter { it.name != "НЕЧТО" }
                        val targetNames = targetCards.map { it.name }.toTypedArray()
                        val targetChoice = JOptionPane.showInputDialog(
                            this, "${target.name}, какую карту отдать?",
                            "Выбор карты", JOptionPane.QUESTION_MESSAGE, null, targetNames, targetNames[0]
                        ) as? String

                        if (targetChoice != null) {
                            val targetCard = targetCards.find { it.name == targetChoice }
                            if (targetCard != null) {
                                engine.performExchange(player, target, card, targetCard)
                                logMessage("🔄 Паника: обмен с ${target.name}")
                            }
                        }
                    }
                }
            }

            else -> {
                showMessage("Паника", panic.message)
                updateDisplay()
            }
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
            JOptionPane.showMessageDialog(this, "Нет доступных действий на карантине.\nХод будет завершён.", "🦠 Карантин", JOptionPane.INFORMATION_MESSAGE)
            actionDone = true
            return
        }

        val choice = JOptionPane.showOptionDialog(
            this, "${player.name}, вы на карантине!\n\nДоступные действия:",
            "🦠 Карантин", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options.toTypedArray(), options[0]
        )

        when {
            hasAxe && choice == 0 -> {
                val axeCard = player.hand.find { it.name == "Топор" }
                if (axeCard != null) {
                    val result = engine.playCard(player, axeCard, player)
                    logMessage(result.message)
                    JOptionPane.showMessageDialog(this, "🪓 ТОПОР!\n\nКарантин снят!\nТеперь вы можете обмениваться.", "Топор", JOptionPane.INFORMATION_MESSAGE)
                }
            }
            choice == (if (hasAxe) 1 else 0) -> {
                val cardNames = discardable.map { it.name }.toTypedArray()
                val cardChoice = JOptionPane.showInputDialog(this, "Выберите карту для сброса:", "Сбросить карту",
                    JOptionPane.QUESTION_MESSAGE, null, cardNames, cardNames[0]) as? String
                if (cardChoice != null) {
                    val idx = cardNames.indexOf(cardChoice)
                    if (idx >= 0 && idx < discardable.size) {
                        val result = engine.discardCard(player, discardable[idx])
                        logMessage(result.message)
                    }
                }
            }
        }

        actionDone = true
    }

    private fun showActionDialog(player: Player) {
        val options = arrayOf("Сыграть карту", "Сбросить карту")
        val choice = JOptionPane.showOptionDialog(
            this, "${player.name}, выберите действие:\n\nВаша рука:\n${player.hand.joinToString("\n") { "• ${it.name} (${it.description})" }}",
            "Фаза 2: Действие", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]
        )
        when (choice) { 0 -> actionDone = onPlayCard(); 1 -> actionDone = onDiscardCard() }
    }

    private fun onPlayCard(): Boolean {
        if (gameOver) return false
        val player = engine.getCurrentPlayer() ?: return false

        if (player.hasQuarantine) {
            showWarning("Карантин", "На карантине можно играть только Топор на себя!\nИспользуйте кнопку 'Топор на себя / Сброс' в окне хода.")
            return false
        }

        val playableCards = engine.getPlayableCards(player)
        if (playableCards.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Нет карт, которые можно сыграть", "Нет карт", JOptionPane.INFORMATION_MESSAGE)
            return false
        }

        val cardNames = playableCards.map { "${it.name} (${it.description})" }.toTypedArray()
        val choice = JOptionPane.showInputDialog(this, "Выберите карту для игры:", "Сыграть карту",
            JOptionPane.QUESTION_MESSAGE, null, cardNames, cardNames[0]) as? String ?: return false

        val idx = cardNames.indexOf(choice)
        if (idx < 0 || idx >= playableCards.size) return false

        val selectedCard = playableCards[idx]

        var target: Player? = null
        val needsTarget = selectedCard.name in listOf(
            "Огнемёт", "Анализ", "Топор", "Подозрение", "Карантин",
            "Заколоченная дверь", "Меняемся местами!", "Сматывай удочки!", "Соблазн"
        )

        if (needsTarget) {
            val targets = engine.getTargets(player, selectedCard.name)
            if (targets.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Нет доступных целей для этой карты", "Нет целей", JOptionPane.WARNING_MESSAGE)
                return false
            }
            val targetNames = targets.map { "${it.name} ${if (it.hasQuarantine) "🦠" else ""}" }.toTypedArray()
            val targetChoice = JOptionPane.showInputDialog(this, "Выберите цель для '${selectedCard.name}':", "Выбор цели",
                JOptionPane.QUESTION_MESSAGE, null, targetNames, targetNames[0]) as? String ?: return false
            val targetIdx = targetNames.indexOf(targetChoice)
            if (targetIdx >= 0) target = targets[targetIdx]
        }

        // УПОРСТВО
        if (selectedCard.name == "Упорство") {
            engine.playCard(player, selectedCard, null)
            logMessage("💪 ${player.name} использует УПОРСТВО!")

            val drawnCards = listOf(
                ActionCard("Огнемёт", "Убить соседнего игрока", ActionType.FLAMETHROWER),
                ActionCard("Анализ", "Посмотреть руку соседа", ActionType.ANALYSIS),
                ActionCard("Топор", "Снять карантин или дверь", ActionType.AXE),
                ActionCard("Подозрение", "Взять карту соседа", ActionType.SUSPICION),
                DefenseCard("Нет уж, спасибо!", "Отказ от обмена", DefenseType.NO_THANKS),
                DefenseCard("Страх", "Отказ от обмена + просмотр карты", DefenseType.FEAR),
                ObstacleCard("Карантин", "Блокирует игрока на 3 хода", ObstacleType.QUARANTINE),
                ObstacleCard("Заколоченная дверь", "Блок между игроками", ObstacleType.BARRICADED_DOOR)
            ).shuffled().take(3)

            val drawnNames = drawnCards.map { "${it.name} (${it.description})" }.toTypedArray()
            val keepChoice = JOptionPane.showInputDialog(this,
                "💪 УПОРСТВО!\n\nВзято 3 карты из колоды. Выберите одну, которую хотите оставить в руке:",
                "Упорство — выбор карты", JOptionPane.QUESTION_MESSAGE, null, drawnNames, drawnNames[0]) as? String

            if (keepChoice != null) {
                val keepIdx = drawnNames.indexOf(keepChoice)
                if (keepIdx >= 0) {
                    val keptCard = drawnCards[keepIdx]
                    player.hand.add(keptCard)
                    logMessage("   ✅ Оставлена в руке: ${keptCard.name}")
                    drawnCards.forEachIndexed { i, card ->
                        if (i != keepIdx) {
                            engine.discardCardSilent(player, card)
                            logMessage("   🗑️ В сброс: ${card.name}")
                        }
                    }
                }
            }

            JOptionPane.showMessageDialog(this,
                "💪 УПОРСТВО!\n\nОдна карта оставлена в руке.\nДве другие карты отправлены в сброс.\n\nФаза 2 продолжается — вы можете сыграть или сбросить ещё одну карту.",
                "Упорство", JOptionPane.INFORMATION_MESSAGE)
            updateDisplay()
            return false
        }

        // СМЕНА МЕСТ — проверка защиты "Мне и здесь неплохо"
        if (selectedCard.name in listOf("Меняемся местами!", "Сматывай удочки!")) {
            if (target != null && target.hand.any { it.name == "Мне и здесь неплохо" }) {
                val defenseChoice = JOptionPane.showOptionDialog(this,
                    "${target.name}, ${player.name} хочет поменяться с вами местами!\n\nУ вас есть защита: Мне и здесь неплохо\n\nЧто вы хотите сделать?",
                    "🛡️ Защита от смены мест", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                    arrayOf("🛡️ Сыграть защиту (отменить смену)", "✅ Согласиться на смену"), "Согласиться")

                if (defenseChoice == 0) {
                    val defenseCard = target.hand.find { it.name == "Мне и здесь неплохо" }
                    if (defenseCard != null) {
                        target.hand.remove(defenseCard)
                        val newCard = engine.drawCardSilent()
                        if (newCard != null) target.hand.add(newCard)
                        engine.playCard(player, selectedCard, target)
                        logMessage("🏠 ${target.name} использует 'Мне и здесь неплохо' — смена мест отменена")
                        JOptionPane.showMessageDialog(this,
                            "🏠 МНЕ И ЗДЕСЬ НЕПЛОХО!\n\n${target.name} отказывается меняться местами.\nВзамен защиты взята карта: ${newCard?.name ?: "нет"}",
                            "Защита от смены мест", JOptionPane.INFORMATION_MESSAGE)
                        return true
                    }
                }
            }

            val result = engine.playCard(player, selectedCard, target)
            logMessage(result.message)
            JOptionPane.showMessageDialog(this,
                if (selectedCard.name == "Меняемся местами!") "💺 МЕНЯЕМСЯ МЕСТАМИ!\n\n${player.name} и ${target?.name} поменялись местами за столом."
                else "💨 СМАТЫВАЙ УДОЧКИ!\n\n${player.name} быстро меняется местами с ${target?.name}.",
                "Смена мест", JOptionPane.INFORMATION_MESSAGE)
            updateDisplay()
            updateButtonStates()
            return true
        }

        val result = engine.playCard(player, selectedCard, target)
        logMessage(result.message)

        // ExchangeInfo (Соблазн)
        if (result is GameEngine.GameResult.ExchangeInfo) {
            JOptionPane.showMessageDialog(this,
                "😈 СОБЛАЗН!\n\nВы инициировали обмен с ${result.neighbor.name}.\nСейчас откроется окно обмена.",
                "Соблазн", JOptionPane.INFORMATION_MESSAGE)
            showExchangeDialogFromResult(result)
            finishTurn(player)
            return true
        }

        // Отображение результата
        when (selectedCard.name) {
            "Огнемёт" -> {
                if (result.message.contains("сгорел")) {
                    JOptionPane.showMessageDialog(this,
                        "🔥 ОГНЕМЁТ!\n\n${target?.name} сгорел заживо!\nРоль: ${target?.role}\n\n${if (target?.role == Role.THING) "🎉 НЕЧТО УНИЧТОЖЕНО!" else ""}",
                        "Огнемёт", JOptionPane.INFORMATION_MESSAGE)
                } else if (result.message.contains("защитился")) {
                    JOptionPane.showMessageDialog(this,
                        "🛡️ НИКАКОГО ШАШЛЫКА!\n\n${target?.name} использовал защиту!\nОгнемёт не причинил вреда.",
                        "Защита", JOptionPane.INFORMATION_MESSAGE)
                }
            }
            "Анализ" -> {
                if (target != null) JOptionPane.showMessageDialog(this,
                    "🔍 АНАЛИЗ!\n\nВы изучили руку ${target.name}:\n${target.hand.joinToString("\n") { "• ${it.name} (${it.description})" }}",
                    "Анализ", JOptionPane.INFORMATION_MESSAGE)
            }
            "Топор" -> JOptionPane.showMessageDialog(this, "🪓 ТОПОР!\n\n${result.message}", "Топор", JOptionPane.INFORMATION_MESSAGE)
            "Подозрение" -> JOptionPane.showMessageDialog(this,
                "🔎 ПОДОЗРЕНИЕ!\n\nВы увидели одну случайную карту из руки ${target?.name}.", "Подозрение", JOptionPane.INFORMATION_MESSAGE)
            "Виски" -> JOptionPane.showMessageDialog(this,
                "🥃 ВИСКИ!\n\n${player.name} выпивает виски и показывает всем свои карты:\n${player.hand.joinToString("\n") { "• ${it.name} (${it.description})" }}",
                "Виски", JOptionPane.INFORMATION_MESSAGE)
            "Гляди по сторонам" -> {
                val newDir = if (engine.getDirection() == 1) "против часовой стрелки ↺" else "по часовой стрелке ↻"
                JOptionPane.showMessageDialog(this, "👀 ГЛЯДИ ПО СТОРОНАМ!\n\nНаправление хода изменено!\nТеперь ход идёт $newDir", "Направление", JOptionPane.INFORMATION_MESSAGE)
            }
            "Соблазн" -> JOptionPane.showMessageDialog(this, "😈 СОБЛАЗН!\n\nОбмен с ${target?.name}.\nХод завершён досрочно.", "Соблазн", JOptionPane.INFORMATION_MESSAGE)
            "Карантин" -> {
                if (target == player) {
                    quarantineJustPlaced = true
                    JOptionPane.showMessageDialog(this, "🦠 КАРАНТИН НА СЕБЯ!\n\n${player.name} добровольно уходит на карантин.\nХод завершается.", "Карантин", JOptionPane.INFORMATION_MESSAGE)
                } else if (target != null) {
                    JOptionPane.showMessageDialog(this, "🦠 КАРАНТИН!\n\n${player.name} отправляет ${target.name} на карантин на 3 хода.", "Карантин", JOptionPane.INFORMATION_MESSAGE)
                }
            }
            "Заколоченная дверь" -> {
                if (target != null) JOptionPane.showMessageDialog(this,
                    "🚪 ЗАКОЛОЧЕННАЯ ДВЕРЬ!\n\n${player.name} заколачивает дверь между собой и ${target.name}.\nОбмен с этим игроком теперь невозможен.",
                    "Дверь", JOptionPane.INFORMATION_MESSAGE)
            }
            else -> JOptionPane.showMessageDialog(this, result.message, selectedCard.name, JOptionPane.INFORMATION_MESSAGE)
        }

        if (selectedCard.name == "Карантин" && target == player) quarantineJustPlaced = true
        if (selectedCard.name == "Соблазн") { finishTurn(player); return true }

        updateDisplay()
        updateButtonStates()
        return true
    }

    private fun onDiscardCard(): Boolean {
        val player = engine.getCurrentPlayer() ?: return false

        val discardableCards = engine.getDiscardableCards(player)
        if (discardableCards.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Нет карт для сброса (нельзя сбросить только НЕЧТО)", "Нет карт", JOptionPane.INFORMATION_MESSAGE)
            return false
        }

        val cardNames = discardableCards.map { it.name }.toTypedArray()
        val choice = JOptionPane.showInputDialog(this, "Выберите карту для сброса:", "Сбросить карту",
            JOptionPane.QUESTION_MESSAGE, null, cardNames, cardNames[0]) as? String ?: return false

        val idx = cardNames.indexOf(choice)
        if (idx < 0 || idx >= discardableCards.size) return false

        val result = engine.discardCard(player, discardableCards[idx])
        logMessage(result.message)
        JOptionPane.showMessageDialog(this, result.message, "Сброс", JOptionPane.INFORMATION_MESSAGE)

        updateDisplay()
        updateButtonStates()
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

        JOptionPane.showMessageDialog(this,
            "🔄 ОБМЕН\n\n${player.name} меняется картами с соседом ${neighbor.name}.\n\nСначала ${player.name} выберет карту для предложения.\nЗатем ${neighbor.name} выберет карту в ответ.",
            "Обмен", JOptionPane.INFORMATION_MESSAGE)

        val card1 = showCardSelectionDialog("${player.name}, выберите карту для предложения ${neighbor.name}:",
            exInfo.playerCards, player.role) ?: return

        if (card1.name == "Заражение!" && player.role != Role.THING) {
            showWarning("Ошибка", "❌ Только НЕЧТО может передавать карту Заражение!"); return
        }

        // Защита от обмена (Страх, Нет уж, спасибо, Мимо)
        val defenseCards = listOf("Страх", "Нет уж, спасибо!", "Мимо!")
        val receiverDefense = neighbor.hand.find { it.name in defenseCards }

        if (receiverDefense != null) {
            JOptionPane.showMessageDialog(this,
                "${player.name} предлагает карту: ${card1.name}\n\nУ ${neighbor.name} есть карта защиты: ${receiverDefense.name}",
                "Информация", JOptionPane.INFORMATION_MESSAGE)

            val defenseChoice = JOptionPane.showOptionDialog(this,
                "${neighbor.name}, вам предлагают карту: ${card1.name}\n\nУ вас есть защита: ${receiverDefense.name}\n\nЧто вы хотите сделать?",
                "🛡️ Защита доступна", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                arrayOf("🛡️ Сыграть защиту (отменить обмен)", "✅ Принять обмен"), "Принять обмен")

            if (defenseChoice == 0) {
                neighbor.hand.remove(receiverDefense)
                val defResult = engine.handleDefense(neighbor, player, card1)
                logMessage(defResult.message)

                val drawnCardName = (defResult as? GameEngine.GameResult.DefensePlayed)?.drawnCard?.name ?: "нет карты"

                when (receiverDefense.name) {
                    "Страх" -> {
                        val randomCard = player.hand.random()
                        JOptionPane.showMessageDialog(this,
                            "😨 СТРАХ!\n\n${neighbor.name} отказывается от обмена и смотрит случайную карту ${player.name}:\n'${randomCard.name}'\n\nВзамен защиты взята карта: $drawnCardName",
                            "Страх", JOptionPane.INFORMATION_MESSAGE)
                    }
                    "Нет уж, спасибо!" -> JOptionPane.showMessageDialog(this,
                        "🙅 НЕТ УЖ, СПАСИБО!\n\n${neighbor.name} вежливо отказывается от обмена.\nВзамен защиты взята карта: $drawnCardName",
                        "Отказ", JOptionPane.INFORMATION_MESSAGE)
                    "Мимо!" -> JOptionPane.showMessageDialog(this,
                        "➡️ МИМО!\n\n${neighbor.name} уворачивается от обмена.\nВзамен защиты взята карта: $drawnCardName",
                        "Мимо", JOptionPane.INFORMATION_MESSAGE)
                }
                return
            }
        }

        JOptionPane.showMessageDialog(this,
            "${neighbor.name}, теперь ваша очередь выбрать карту для передачи ${player.name}.",
            "Выбор карты", JOptionPane.INFORMATION_MESSAGE)

        val card2 = showCardSelectionDialog("${neighbor.name}, выберите карту для передачи ${player.name}:",
            exInfo.neighborCards, neighbor.role) ?: return

        if (card2.name == "Заражение!" && neighbor.role != Role.THING) {
            showWarning("Ошибка", "❌ Только НЕЧТО может передавать карту Заражение!"); return
        }

        val exchangeResult = engine.performExchange(player, neighbor, card1, card2)
        logMessage(exchangeResult.message)
        JOptionPane.showMessageDialog(this,
            "🔄 ОБМЕН ВЫПОЛНЕН!\n\n${player.name} отдал: ${card1.name}\n${player.name} получил: ${card2.name}",
            "Обмен", JOptionPane.INFORMATION_MESSAGE)
    }

    private fun showCardSelectionDialog(title: String, cards: List<Card>, viewerRole: Role): Card? {
        val dialog = JDialog(this, "Выбор карты", true)
        dialog.setSize(400, 350)
        dialog.setLocationRelativeTo(this)

        val panel = JPanel(BorderLayout())
        panel.border = EmptyBorder(10, 10, 10, 10)
        panel.add(JLabel("<html>$title</html>").apply { font = Font("Arial", Font.BOLD, 12) }, BorderLayout.NORTH)

        val cardsPanel = JPanel()
        cardsPanel.layout = BoxLayout(cardsPanel, BoxLayout.Y_AXIS)
        val buttonGroup = ButtonGroup()
        val buttons = mutableListOf<JRadioButton>()

        for (card in cards) {
            val displayName = if (card.name == "Заражение!" && viewerRole != Role.THING) "???? (скрыто)" else card.name
            val rb = JRadioButton(displayName)
            rb.putClientProperty("card", card)
            buttonGroup.add(rb)
            buttons.add(rb)
            cardsPanel.add(rb)
        }

        if (buttons.isNotEmpty()) buttons[0].isSelected = true
        panel.add(JScrollPane(cardsPanel), BorderLayout.CENTER)

        var selectedCard: Card? = null
        val btnPanel = JPanel(FlowLayout(FlowLayout.CENTER))
        btnPanel.add(JButton("✅ Выбрать").apply {
            addActionListener { val idx = buttons.indexOfFirst { it.isSelected }; if (idx >= 0) selectedCard = cards[idx]; dialog.dispose() }
        })
        btnPanel.add(JButton("Отмена").apply { addActionListener { dialog.dispose() } })
        panel.add(btnPanel, BorderLayout.SOUTH)

        dialog.add(panel)
        dialog.isVisible = true
        return selectedCard
    }

    private fun finishTurn(player: Player) {
        if (gameOver) return
        turnsPlayed++
        val result = engine.endTurn(player)
        logMessage(result.message)
        resetTurnState()
        turnDialog?.dispose()
        updateDisplay()
        updateButtonStates()

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
            turnsPlayed = turnsPlayed
        )

        val message = if (winner == "HUMANS") "🎉 ЛЮДИ ПОБЕДИЛИ!\n\nНЕЧТО уничтожено!"
        else "👾 НЕЧТО ПОБЕДИЛО!\n\nВсе люди заражены или убиты!"

        logMessage("\n" + "=".repeat(60))
        logMessage(message)
        logMessage("=".repeat(60))
        logMessage("📊 Игра сохранена в базу данных")
        logMessage("⏱️ Длительность: ${(System.currentTimeMillis() - gameStartTime) / 1000} сек | Ходов: $turnsPlayed")

        resetTurnState()
        updateDisplay()

        JOptionPane.showMessageDialog(
            this,
            message,
            "Игра окончена!",
            JOptionPane.INFORMATION_MESSAGE
        )

        statusLabel.text = "Игра завершена"
        phaseLabel.text = ""
        logMessage("📋 Нажмите 'Новая игра' для старта")
    }

    private fun resetTurnState() {
        cardDrawn = false; actionDone = false; panicHappened = false; exchangeDone = false; quarantineJustPlaced = false
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
            val borderColor = when {
                !player.isAlive -> Color.GRAY; isCurrent -> Color.BLUE
                player.role == Role.THING -> Color.RED; player.role == Role.INFECTED -> Color.ORANGE; else -> Color.BLACK
            }

            val playerPanel = JPanel(BorderLayout())
            playerPanel.border = CompoundBorder(LineBorder(borderColor, if (isCurrent) 3 else 1), EmptyBorder(5, 5, 5, 5))
            playerPanel.maximumSize = Dimension(270, 75)
            playerPanel.background = when { isCurrent -> Color(230, 240, 255); !player.isAlive -> Color(240, 240, 240); else -> Color.WHITE }
            playerPanel.isOpaque = true

            val icon = when { !player.isAlive -> "💀"; player.role == Role.THING -> "👾"; player.role == Role.INFECTED -> "🧟"; else -> "👤" }
            val nameLabel = JLabel("$icon ${player.name}").apply { font = Font("Arial", Font.BOLD, if (isCurrent) 14 else 12) }

            val infoPanel = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS); isOpaque = false }
            infoPanel.add(JLabel(when { !player.isAlive -> "💀 Мёртв"; player.hasQuarantine -> "🦠 Карантин (${player.quarantineTurns} хода)"; else -> "✅ Жив" }))
            infoPanel.add(JLabel(when (player.role) { Role.HUMAN -> "👤 Человек"; Role.THING -> "👾 НЕЧТО"; Role.INFECTED -> "🧟 Заражённый" }))
            infoPanel.add(JLabel("🎴 ${player.hand.size} карт в руке"))

            playerPanel.add(nameLabel, BorderLayout.NORTH)
            playerPanel.add(infoPanel, BorderLayout.CENTER)
            playerListPanel.add(playerPanel)
            playerListPanel.add(Box.createVerticalStrut(5))
        }

        directionLabel.text = if (engine.getDirection() == 1) "↻ По часовой стрелке" else "↺ Против часовой стрелки"

        val cp = engine.getCurrentPlayer()
        if (cp != null) {
            statusLabel.text = "Ход ${engine.getTurnNumber() + 1}: ${cp.name} (${cp.role})"
            phaseLabel.text = when {
                cp.hasQuarantine -> "🦠 Карантин — ограниченные действия"
                !cardDrawn -> "📤 Фаза 1: Взятие карты из колоды"
                panicHappened -> "😱 Паника! Фаза действия пропущена"
                !actionDone -> "🎮 Фаза 2: Выбор действия"
                else -> "🔄 Фаза 3: Обмен с соседом"
            }
        }

        playerListPanel.revalidate()
        playerListPanel.repaint()
    }

    private fun updateButtonStates() {}

    // ==================== СТАТИСТИКА ИЗ БД ====================

    private fun showStats() {
        val stats = database.getStats()
        val sb = StringBuilder()
        sb.append("╔════════════════════════════╗\n")
        sb.append("║        СТАТИСТИКА          ║\n")
        sb.append("╠════════════════════════════╣\n")
        sb.append("║ Всего игр: ${stats.totalGames.toString().padEnd(16)}║\n")
        sb.append("║ Побед людей: ${stats.humanWins.toString().padEnd(14)}║\n")
        sb.append("║ Побед НЕЧТО: ${stats.thingWins.toString().padEnd(14)}║\n")
        sb.append("╚════════════════════════════╝\n")

        val textArea = JTextArea(sb.toString()).apply {
            isEditable = false
            font = Font("Monospaced", Font.PLAIN, 12)
        }
        JOptionPane.showMessageDialog(this, JScrollPane(textArea), "Статистика", JOptionPane.INFORMATION_MESSAGE)
    }

    private fun showRegistry() {
        val players = database.getAllPlayers()
        val sb = StringBuilder()

        if (players.isEmpty()) sb.append("Реестр пуст.\n\nСыграйте несколько игр, чтобы накопить статистику.")
        else {
            sb.append("╔═══════════════════════════════════════╗\n")
            sb.append("║         РЕЕСТР ИГРОКОВ                ║\n")
            sb.append("╠═══════════════════════════════════════╣\n")
            players.forEach { player ->
                val name = player[0] as String
                val games = player[1] as Int
                val wins = player[2] as Int
                sb.append("║ ${name}${" ".repeat(18 - name.length)} Игр:${games}${" ".repeat(4 - games.toString().length)} Побед:${wins}${" ".repeat(4 - wins.toString().length)}║\n")
            }
            sb.append("╚═══════════════════════════════════════╝\n")
        }

        val textArea = JTextArea(sb.toString()).apply { isEditable = false; font = Font("Monospaced", Font.PLAIN, 12) }
        val dialog = JDialog(this, "Реестр игроков (SQLite)", true).apply {
            setSize(450, 500); setLocationRelativeTo(this@GameGUI); add(JScrollPane(textArea))
        }
        dialog.isVisible = true
    }

    private fun showHistory() {
        val history = database.getGameHistory(20)
        val sb = StringBuilder()

        if (history.isEmpty()) sb.append("История игр пуста.")
        else {
            sb.append("История последних игр:\n\n")
            history.forEach { game ->
                val id = game[0] as Int
                val winner = game[1] as String
                val turns = game[2] as Int
                val date = game[3] as String
                val thing = game[4] as String

                sb.append("Игра #$id | ${date.take(16)} | $winner\n")
                sb.append("  Ходов: $turns | НЕЧТО: $thing\n")
                sb.append("─".repeat(40) + "\n")
            }
        }

        val textArea = JTextArea(sb.toString()).apply { isEditable = false; font = Font("Monospaced", Font.PLAIN, 12) }
        val dialog = JDialog(this, "История игр (SQLite)", true).apply {
            setSize(550, 500); setLocationRelativeTo(this@GameGUI); add(JScrollPane(textArea))
        }
        dialog.isVisible = true
    }

    fun logMessage(message: String) {
        gameLogArea.append("$message\n")
        gameLogArea.caretPosition = gameLogArea.document.length
    }
    private fun String.padEnd(length: Int, padChar: Char = ' '): String {
        return if (this.length >= length) this
        else this + padChar.toString().repeat(length - this.length)
    }
}