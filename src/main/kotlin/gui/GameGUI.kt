package gui

import data.GameDatabase
import enums.*
import model.*
import systems.GameEngine
import systems.GameResult
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

        while (true) {
            val dialog = NewGameDialog(this)
            dialog.isVisible = true

            if (!dialog.isConfirmed) return

            val playerNames = dialog.getPlayerNames()

            if (playerNames.size !in 4..12) {
                showWarning("Ошибка", "Нужно от 4 до 12 игроков!\nВы ввели: ${playerNames.size}")
                continue
            }

            gameLogArea.text = ""
            playerNames.forEach { database.addPlayer(it) }

            val result = engine.setupGame(playerNames)
            logMessage(result)

            gameStartTime = System.currentTimeMillis()
            turnsPlayed = 0

            engine.resetTurnState()
            updateDisplay()
            updateButtonStates()

            logMessage("=".repeat(60))
            logMessage("🎮 ИГРА НАЧАЛАСЬ!")
            logMessage("=".repeat(60))

            showTurnWindow()
            break
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

        val cardsText = player.getHandCards().joinToString("<br>• ") { "${it.name} (${it.description})" }

        val phaseDesc =
            when {
                player.hasQuarantine && engine.isQuarantineJustPlaced() -> "🦠 Вы поставили карантин на себя! Ход завершается"
                player.hasQuarantine -> "🦠 КАРАНТИН — можно только Топор на себя или сброс"
                !engine.isCardDrawn() -> "📤 Фаза 1: Взятие карты"
                engine.isPanicHappened() -> "😱 ПАНИКА! Действие пропущено"
                !engine.isActionDone() -> "🎮 Фаза 2: Действие (сыграть или сбросить)"
                else -> "🔄 Фаза 3: Обмен с соседом"
            }
        val phaseDescLabel = JLabel(phaseDesc)
        phaseDescLabel.font = Font("Arial", Font.BOLD, 12)
        phaseDescLabel.foreground = if (engine.isPanicHappened()) Color.RED else Color.BLUE

        infoPanel.add(turnLabel)
        infoPanel.add(Box.createVerticalStrut(5))
        infoPanel.add(phaseDescLabel)
        infoPanel.add(Box.createVerticalStrut(10))
        infoPanel.add(JLabel("Роль: ${player.role}"))
        infoPanel.add(JLabel("Карт в руке: ${player.getHandSize()}"))
        infoPanel.add(JLabel("<html>Карты:<br>• $cardsText</html>"))

        panel.add(infoPanel, BorderLayout.NORTH)

        if (engine.isQuarantineJustPlaced()) {
            engine.setQuarantineJustPlaced(false)
            engine.setActionDone(true)
            engine.setExchangeDone(true)
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
            drawButton.isEnabled = !engine.isCardDrawn()
            actionButton.isEnabled = engine.isCardDrawn()
            exchangeTurnButton.isEnabled = false
        } else {
            drawButton.isEnabled = !engine.isCardDrawn()
            val cardDrawn = engine.isCardDrawn()
            val actionDone = engine.isActionDone()
            val panic = engine.isPanicHappened()
            val skipPhase = engine.state.skipActionPhase

            println("=== BUTTON STATE ===")
            println("cardDrawn=$cardDrawn")
            println("actionDone=$actionDone")
            println("panic=$panic")
            println("skipActionPhase=$skipPhase")
            println("actionButton enabled = ${cardDrawn && !actionDone && !panic}")
            println("====================")

            actionButton.isEnabled = engine.isCardDrawn() && !engine.isActionDone() && !engine.isPanicHappened()
            exchangeTurnButton.isEnabled = engine.isCardDrawn() && (engine.isActionDone() || engine.isPanicHappened()) && !engine.isExchangeDone()
        }

        drawButton.addActionListener {
            val result = engine.drawCard(player)
            logMessage(result.message)
            engine.setCardDrawn(true)

            when (result) {
                is GameResult.Panic -> {
                    engine.setPanicHappened(true)
                    showMessage("😱 ПАНИКА!", result.message)
                }

                is GameResult.PanicExchange -> {
                    engine.setPanicHappened(true)
                    handlePanicExchange(result)
                }

                is GameResult.Error -> logMessage("❌ ${result.message}")

                else -> {}
            }

            updateTurnWindow()
            updateDisplay()
        }

        actionButton.addActionListener {
            if (gameOver) return@addActionListener

            if (player.hasQuarantine) {
                showQuarantineActions(player)
                if (engine.isActionDone()) {
                    finishTurn(player)
                    return@addActionListener // ← выходим, не обновляем окно
                }
            } else {
                showActionDialog(player)
                if (engine.isExchangeDone() || engine.isQuarantineJustPlaced()) {
                    finishTurn(player)
                    return@addActionListener // ← выходим, не обновляем окно
                }
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
        }

        buttonPanel.add(drawButton)
        buttonPanel.add(actionButton)
        buttonPanel.add(exchangeTurnButton)

        panel.add(buttonPanel, BorderLayout.CENTER)

        turnDialog?.add(panel)
        turnDialog?.isVisible = true
    }

    private fun handlePanicExchange(panic: GameResult.PanicExchange) {
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
                        val targetCards = target.getHandCards().filter { it.name != "НЕЧТО" }
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
        val hasAxe = player.hasCard("Топор")
        val discardable = player.getHandCards().filter { it.name != "НЕЧТО" }

        val options = mutableListOf<String>()
        if (hasAxe) options.add("🪓 Сыграть Топор на себя")
        if (discardable.isNotEmpty()) options.add("🗑️ Сбросить карту")

        if (options.isEmpty()) {
            showMessage("Карантин", "Нет доступных действий.\nХод будет завершён.")
            engine.setActionDone(true)
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
                val axeCard = player.getHandCards().find { it.name == "Топор" }
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
        engine.setActionDone(true)
    }

    private fun showActionDialog(player: Player) {
        val options = arrayOf("Сыграть карту", "Сбросить карту")
        val choice =
            JOptionPane.showOptionDialog(
                this,
                "${player.name}, выберите действие:",
                "Фаза 2: Действие",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0],
            )
        when (choice) {
            0 -> onPlayCard()

            1 -> {
                onDiscardCard()
                engine.setActionDone(true)
            }
        }
    }

    // ==================== ON PLAY CARD (разбит на методы) ====================

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

        val selectedCard = selectCardToPlay(playableCards) ?: return false
        val target = selectTargetIfNeeded(player, selectedCard)

        if (needsTarget(selectedCard) && target == null) return false

        return executeCardAction(player, selectedCard, target)
    }

    private fun needsTarget(card: Card): Boolean {
        return card.name in
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
    }

    private fun selectCardToPlay(playableCards: List<Card>): Card? {
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
            ) as? String ?: return null

        val idx = cardNames.indexOf(choice)
        return if (idx in playableCards.indices) playableCards[idx] else null
    }

    private fun selectTargetIfNeeded(
        player: Player,
        card: Card,
    ): Player? {
        if (!needsTarget(card)) return null

        val targets = engine.getTargets(player, card.name)
        if (targets.isEmpty()) {
            showMessage("Нет целей", "Нет доступных целей")
            return null
        }

        val targetNames = targets.map { "${it.name} ${if (it.hasQuarantine) "🦠" else ""}" }.toTypedArray()
        val targetChoice =
            JOptionPane.showInputDialog(
                this,
                "Выберите цель для '${card.name}':",
                "Выбор цели",
                JOptionPane.QUESTION_MESSAGE,
                null,
                targetNames,
                targetNames[0],
            ) as? String ?: return null

        val targetIdx = targetNames.indexOf(targetChoice)
        return if (targetIdx >= 0) targets[targetIdx] else null
    }

    private fun executeCardAction(
        player: Player,
        card: Card,
        target: Player?,
    ): Boolean {
        return when (card.name) {
            "Упорство" -> handlePerseverance(player, card)

            "Соблазн" -> handleTemptationCard(player, card, target)

            "Карантин" -> handleQuarantineCard(player, target)

            else -> {
                engine.setActionDone(true)
                handleGenericCard(player, card, target)
            }
        }
    }

    private fun handlePerseverance(
        player: Player,
        card: Card,
    ): Boolean {
        val result = engine.playCard(player, card, null)
        logMessage("💪 ${player.name} использует УПОРСТВО!")

        val drawnCards = engine.getPerseveranceCards()
        if (drawnCards.isEmpty()) {
            showMessage("Упорство", "Не удалось взять карты")
            updateDisplay()
            return false
        }

        val drawnNames = drawnCards.map { "${it.name} (${it.description})" }.toTypedArray()
        val keepChoice =
            JOptionPane.showInputDialog(
                this,
                "💪 УПОРСТВО!\n\nВыберите карту для сохранения:",
                "Упорство",
                JOptionPane.QUESTION_MESSAGE,
                null,
                drawnNames,
                drawnNames[0],
            ) as? String

        if (keepChoice != null) {
            val keepIdx = drawnNames.indexOf(keepChoice)
            if (keepIdx >= 0) {
                engine.confirmPerseverance(player, keepIdx)
                logMessage("   ✅ Карта сохранена")
            }
        }

        showMessage("💪 Упорство!", "Одна карта оставлена, остальные в сброс.\nФаза 2 продолжается.")
        updateDisplay()
        return false // НЕ устанавливаем actionDone — можно играть ещё
    }

    private fun handleSeatSwap(
        player: Player,
        card: Card,
        target: Player?,
    ): Boolean {
        if (target != null && target.hasCard("Мне и здесь неплохо")) {
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
                target.removeCardByName("Мне и здесь неплохо")
                player.removeCardByName(card.name)
                val newCard = engine.drawSilentSafe(target)
                if (newCard != null) target.addCard(newCard)
                logMessage("🏠 ${target.name} использует 'Мне и здесь неплохо' — смена отменена")
                showMessage("🏠 Мне и здесь неплохо", "Смена отменена!\nВзята карта: ${newCard?.name ?: "нет"}")
                return true
            }
        }

        val result = engine.playCard(player, card, target)
        logMessage(result.message)
        showMessage(if (card.name == "Меняемся местами!") "💺 Смена мест" else "💨 Смена мест", result.message)
        updateDisplay()
        return true
    }

    private fun handleTemptationCard(
        player: Player,
        card: Card,
        target: Player?,
    ): Boolean {
        val result = engine.playCard(player, card, target)
        logMessage(result.message)

        if (result is GameResult.ExchangeInfo) {
            showExchangeDialogFromResult(result)
        }
        engine.setExchangeDone(true)
        engine.setActionDone(true)
        finishTurn(player)
        return true
    }

    private fun handleQuarantineCard(
        player: Player,
        target: Player?,
    ): Boolean {
        val result = engine.playCard(player, ActionCard("Карантин", "", ActionType.AXE), target)
        logMessage(result.message)

        if (target == player) {
            engine.setQuarantineJustPlaced(true)
            showMessage("🦠 Карантин на себя", "Ход завершается")
        } else {
            showMessage("🦠 Карантин", result.message)
        }
        updateDisplay()
        return true
    }

    private fun handleGenericCard(
        player: Player,
        card: Card,
        target: Player?,
    ): Boolean {
        val result = engine.playCard(player, card, target)
        logMessage(result.message)

        if (result is GameResult.ExchangeInfo) {
            showExchangeDialogFromResult(result)
            finishTurn(player)
            return true
        }

        when (card.name) {
            "Огнемёт" -> showMessage("🔥 Огнемёт", result.message)
            "Анализ" -> showMessage("🔍 Анализ", result.message)
            "Топор" -> showMessage("🪓 Топор", result.message)
            "Подозрение" -> showMessage("🔎 Подозрение", result.message)
            "Виски" -> showMessage("🥃 Виски", result.message)
            "Гляди по сторонам" -> showMessage("👀 Направление", result.message)
            "Заколоченная дверь" -> showMessage("🚪 Дверь", result.message)
            else -> showMessage(card.name, result.message)
        }
        updateDisplay()
        return true
    }
    // ==================== ОСТАЛЬНЫЕ МЕТОДЫ ====================

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

        if (result is GameResult.ExchangeInfo) {
            showExchangeDialogFromResult(result)
        } else {
            logMessage(result.message)
            showWarning("Обмен невозможен", result.message)
        }
        engine.setExchangeDone(true)
        finishTurn(player)
    }

    private fun showExchangeDialogFromResult(exInfo: GameResult.ExchangeInfo) {
        if (gameOver) return
        val player = exInfo.player
        val neighbor = exInfo.neighbor

        val card1 =
            showCardSelectionDialog(
                "${player.name}, выберите карту для предложения ${neighbor.name}:",
                exInfo.playerCards,
                player.role,
            ) ?: return

        if (card1.name == "Заражение!" && player.role != Role.THING) {
            showWarning("Ошибка", "❌ Только НЕЧТО может передавать Заражение!")
            return
        }

        // Проверяем ВСЕ защиты от обмена у соседа
        val defenseCards =
            neighbor.getHandCards().filter {
                it is DefenseCard && it.subType.category == DefenseCategory.EXCHANGE
            }

        if (defenseCards.isNotEmpty()) {
            val defenseNames = defenseCards.map { it.name }
            val options = mutableListOf<String>()
            options.addAll(defenseNames)
            options.add("✅ Принять обмен")

            val defenseChoice =
                JOptionPane.showOptionDialog(
                    this,
                    "${neighbor.name}, вам предлагают: ${card1.name}\n\nУ вас есть защита: ${defenseNames.joinToString(", ")}\n\nЧто хотите сделать?",
                    "🛡️ Защита доступна",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options.toTypedArray(),
                    options[0],
                )

            if (defenseChoice in defenseNames.indices) {
                // Игрок выбрал конкретную защиту
                val selectedDefense = defenseCards[defenseChoice]
                val defResult = engine.handleDefense(neighbor, player, card1, selectedDefense)
                logMessage(defResult.message)

                if (defResult is GameResult.PassExchange) {
                    showMessage("➡️ Мимо!", defResult.message)
                    // Обмен передаётся: attacker (тот же) меняется с nextPlayer
                    val attacker = defResult.attacker
                    val nextPlayer = defResult.nextPlayer

                    // Даём карты для обмена между attacker и nextPlayer
                    val p1Cards = attacker.getHandCards().filter { it.name != "НЕЧТО" && (attacker.role == Role.THING || it.name != "Заражение!") }
                    val p2Cards = nextPlayer.getHandCards().filter { it.name != "НЕЧТО" && (nextPlayer.role == Role.THING || it.name != "Заражение!") }

                    if (p1Cards.isEmpty() || p2Cards.isEmpty()) {
                        showWarning("Обмен", "Недостаточно карт для обмена")
                        return
                    }

                    val fakeExchangeInfo = GameResult.ExchangeInfo(attacker, nextPlayer, p1Cards, p2Cards)
                    showExchangeDialogFromResult(fakeExchangeInfo)
                    return
                }

                showMessage("🛡️ ${selectedDefense.name}", defResult.message)
                return
            }
            // Если choice == "✅ Принять обмен" — продолжаем обмен
        }

        val card2 =
            showCardSelectionDialog(
                "${neighbor.name}, выберите карту для передачи ${player.name}:",
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
        engine.resetTurnState()
        logMessage(result.message)
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

        engine.resetTurnState()
        updateDisplay()

        JOptionPane.showMessageDialog(this, message, "Игра окончена!", JOptionPane.INFORMATION_MESSAGE)

        statusLabel.text = "Игра завершена"
        phaseLabel.text = ""
        logMessage("📋 Нажмите 'Новая игра' для старта")
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
            val playerPanel = buildPlayerPanel(player, player == currentPlayer)
            playerListPanel.add(playerPanel)
            playerListPanel.add(Box.createVerticalStrut(5))
        }

        directionLabel.text = if (engine.getDirection() == 1) "↻ По часовой" else "↺ Против"
        updateStatusLabels(currentPlayer)

        playerListPanel.revalidate()
        playerListPanel.repaint()
    }

    private fun buildPlayerPanel(
        player: Player,
        isCurrent: Boolean,
    ): JPanel {
        val borderColor = getBorderColor(player, isCurrent)
        val icon = getPlayerIcon(player)
        val bgColor = getBackgroundColor(player, isCurrent)

        val playerPanel =
            JPanel(BorderLayout()).apply {
                border = CompoundBorder(LineBorder(borderColor, if (isCurrent) 3 else 1), EmptyBorder(5, 5, 5, 5))
                maximumSize = Dimension(270, 75)
                background = bgColor
                isOpaque = true
            }

        val nameLabel =
            JLabel("$icon ${player.name}").apply {
                font = Font("Arial", Font.BOLD, if (isCurrent) 14 else 12)
            }

        val infoPanel = buildPlayerInfoPanel(player)

        playerPanel.add(nameLabel, BorderLayout.NORTH)
        playerPanel.add(infoPanel, BorderLayout.CENTER)

        return playerPanel
    }

    private fun getPlayerStatus(player: Player): String {
        return when {
            !player.isAlive -> "💀 Мёртв"
            player.hasQuarantine -> "🦠 Карантин (${player.quarantineTurns})"
            else -> "✅ Жив"
        }
    }

    private fun getPlayerRole(player: Player): String {
        return when (player.role) {
            Role.HUMAN -> "👤 Человек"
            Role.THING -> "👾 НЕЧТО"
            Role.INFECTED -> "🧟 Заражённый"
        }
    }

    private fun getPlayerIcon(player: Player): String {
        return when {
            !player.isAlive -> "💀"
            else -> "👤"
        }
    }

    private fun getBorderColor(
        player: Player,
        isCurrent: Boolean,
    ): Color {
        return when {
            !player.isAlive -> Color.GRAY
            isCurrent -> Color.BLUE
            else -> Color.BLACK
        }
    }

    private fun buildPlayerInfoPanel(player: Player): JPanel {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            add(JLabel(getPlayerStatus(player)))
            add(JLabel("🎴 ${player.getHandSize()} карт"))
        }
    }

    private fun updateStatusLabels(currentPlayer: Player?) {
        if (currentPlayer != null) {
            val roleText =
                when (currentPlayer.role) {
                    Role.HUMAN -> "👤 Человек"
                    Role.THING -> "👾 НЕЧТО"
                    Role.INFECTED -> "🧟 Заражённый"
                }
            statusLabel.text = "Ход ${engine.getTurnNumber() + 1}: ${currentPlayer.name} ($roleText)"
            phaseLabel.text =
                when {
                    currentPlayer.hasQuarantine -> "🦠 Карантин"
                    !engine.isCardDrawn() -> "📤 Фаза 1"
                    engine.isPanicHappened() -> "😱 Паника"
                    !engine.isActionDone() -> "🎮 Фаза 2"
                    else -> "🔄 Фаза 3"
                }
        }
    }

    private fun getBackgroundColor(
        player: Player,
        isCurrent: Boolean,
    ): Color {
        return when {
            isCurrent -> Color(230, 240, 255)
            !player.isAlive -> Color(240, 240, 240)
            else -> Color.WHITE
        }
    }

    private fun updateButtonStates() {
        val gameActive = engine.getCurrentPlayer() != null && !gameOver
        startButton.isEnabled = !gameActive
        statsButton.isEnabled = true
        registerButton.isEnabled = true
        historyButton.isEnabled = true
    }

    private fun showStats() {
        val stats = database.getStats()
        val sb = StringBuilder()
        sb.append("Игр: ${stats.totalGames} | Люди: ${stats.humanWins} | НЕЧТО: ${stats.thingWins}\n")
        showInfoDialog("Статистика", sb.toString())
    }

    private fun showRegistry() {
        val players = database.getAllPlayers()
        val sb = StringBuilder()
        if (players.isEmpty()) {
            sb.append("Реестр пуст.")
        } else {
            players.forEach { sb.append("${it[0]}: игр ${it[1]}, побед ${it[2]}\n") }
        }
        showInfoDialog("Реестр", sb.toString())
    }

    private fun showHistory() {
        val history = database.getGameHistory(20)
        val sb = StringBuilder()
        if (history.isEmpty()) {
            sb.append("История пуста.")
        } else {
            history.forEach { sb.append("Игра #${it[0]} | ${(it[3] as String).take(16)} | ${it[1]}\n") }
        }
        showInfoDialog("История", sb.toString())
    }

    private fun showInfoDialog(
        title: String,
        message: String,
    ) {
        val textArea =
            JTextArea(message).apply {
                isEditable = false
                font = Font("Monospaced", Font.PLAIN, 12)
            }
        JOptionPane.showMessageDialog(this, JScrollPane(textArea), title, JOptionPane.INFORMATION_MESSAGE)
    }

    fun logMessage(message: String) {
        gameLogArea.append("$message\n")
        gameLogArea.caretPosition = gameLogArea.document.length
    }
}
