package model

import enums.DefenseType
import enums.CardType

class DefenseCard(
    name: String,
    description: String,
    val subType: DefenseType
) : Card(name, description, CardType.DEFENSE) {
    override fun copy(): DefenseCard = DefenseCard(name, description, subType)
}