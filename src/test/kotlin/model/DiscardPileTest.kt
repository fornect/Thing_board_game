package model

import enums.ActionType

class DiscardPileTest {
    fun test() {
        testEmpty()
        testAddCard()
        testTakeAll()
        testClear()
        testIsNotEmpty()
        println("✅ DiscardPileTest: все тесты пройдены!")
    }

    private fun testEmpty() {
        val pile = DiscardPile()
        assert(pile.size() == 0) { "Размер должен быть 0" }
        assert(pile.isEmpty()) { "Сброс должен быть пуст" }
    }

    private fun testAddCard() {
        val pile = DiscardPile()
        val card = ActionCard("Test", "Desc", ActionType.ANALYSIS)
        pile.add(card)
        assert(pile.size() == 1) { "Размер должен быть 1" }
        assert(pile.isNotEmpty()) { "Сброс не должен быть пуст" }
    }

    private fun testTakeAll() {
        val pile = DiscardPile()
        pile.add(ActionCard("A", "a", ActionType.ANALYSIS))
        pile.add(ActionCard("B", "b", ActionType.FLAMETHROWER))

        val taken = pile.takeAll()
        assert(taken.size == 2) { "Должно вернуть 2 карты" }
        assert(pile.size() == 0) { "Сброс должен быть пуст" }
        assert(pile.isEmpty()) { "Сброс должен быть пуст" }
    }

    private fun testClear() {
        val pile = DiscardPile()
        pile.add(ActionCard("Test", "Desc", ActionType.ANALYSIS))
        pile.add(ActionCard("Test2", "Desc2", ActionType.FLAMETHROWER))
        pile.clear()
        assert(pile.size() == 0) { "После clear размер должен быть 0" }
    }

    private fun testIsNotEmpty() {
        val pile = DiscardPile()
        assert(!pile.isNotEmpty()) { "Пустой сброс — isNotEmpty = false" }
        pile.add(ActionCard("Test", "Desc", ActionType.ANALYSIS))
        assert(pile.isNotEmpty()) { "С картой isNotEmpty = true" }
    }
}
