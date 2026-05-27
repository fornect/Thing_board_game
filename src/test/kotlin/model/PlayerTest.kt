package model

import enums.ActionType
import enums.Role

class PlayerTest {

    fun test() {
        testDefaultRole()
        testDefaultAlive()
        testDefaultHand()
        testKill()
        testDefaultQuarantine()
        testDefaultBarricade()
        testHasCard()
        println("✅ PlayerTest: все тесты пройдены!")
    }

    private fun testDefaultRole() {
        val player = Player("Test")
        assert(player.role == Role.HUMAN) { "По умолчанию роль должна быть HUMAN" }
    }

    private fun testDefaultAlive() {
        val player = Player("Test")
        assert(player.isAlive) { "По умолчанию игрок должен быть жив" }
    }

    private fun testDefaultHand() {
        val player = Player("Test")
        assert(player.hand.size == 0) { "По умолчанию рука должна быть пуста" }
    }

    private fun testKill() {
        val player = Player("Test")
        player.hand.add(ActionCard("Test", "Desc", ActionType.ANALYSIS))
        player.kill()
        assert(!player.isAlive) { "После kill игрок должен быть мёртв" }
        assert(player.hand.size == 0) { "После kill рука должна быть пуста" }
    }

    private fun testDefaultQuarantine() {
        val player = Player("Test")
        assert(!player.hasQuarantine) { "По умолчанию карантина быть не должно" }
        assert(player.quarantineTurns == 0) { "По умолчанию quarantineTurns = 0" }
    }

    private fun testDefaultBarricade() {
        val player = Player("Test")
        assert(!player.hasBarricade) { "По умолчанию баррикады быть не должно" }
    }

    private fun testHasCard() {
        val player = Player("Test")
        val card = ActionCard("Огнемёт", "Test", ActionType.FLAMETHROWER)
        player.hand.add(card)
        assert(player.hand.any { it.name == "Огнемёт" }) { "Карта должна быть в руке" }
        assert(!player.hand.any { it.name == "Анализ" }) { "Этой карты не должно быть в руке" }
    }
}
