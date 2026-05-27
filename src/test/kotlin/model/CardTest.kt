package model

import enums.*

class CardTest {

    fun test() {
        testActionCardType()
        testDefenseCardType()
        testObstacleCardType()
        testPanicCardType()
        testCopy()
        println("✅ CardTest: все тесты пройдены!")
    }

    private fun testActionCardType() {
        val card = ActionCard("Test", "Description", ActionType.ANALYSIS)
        assert(card.type == CardType.ACTION) { "Тип должен быть ACTION" }
        assert(card.subType == ActionType.ANALYSIS) { "Подтип должен быть ANALYSIS" }
    }

    private fun testDefenseCardType() {
        val card = DefenseCard("Test", "Description", DefenseType.NO_THANKS)
        assert(card.type == CardType.DEFENSE) { "Тип должен быть DEFENSE" }
        assert(card.subType == DefenseType.NO_THANKS) { "Подтип должен быть NO_THANKS" }
    }

    private fun testObstacleCardType() {
        val card = ObstacleCard("Test", "Description", ObstacleType.QUARANTINE)
        assert(card.type == CardType.OBSTACLE) { "Тип должен быть OBSTACLE" }
        assert(card.subType == ObstacleType.QUARANTINE) { "Подтип должен быть QUARANTINE" }
    }

    private fun testPanicCardType() {
        val card = PanicCard("Test", "Description", PanicType.NO_PANIC)
        assert(card.type == CardType.PANIC) { "Тип должен быть PANIC" }
        assert(card.subType == PanicType.NO_PANIC) { "Подтип должен быть NO_PANIC" }
    }

    private fun testCopy() {
        val card = ActionCard("Fire", "Burn", ActionType.FLAMETHROWER)
        val copy = card.copy()
        assert(card.name == copy.name) { "Имена должны совпадать" }
        assert(card.description == copy.description) { "Описания должны совпадать" }
        assert(card.subType == copy.subType) { "Подтипы должны совпадать" }
        assert(card !== copy) { "Это должны быть разные объекты" }
    }
}
