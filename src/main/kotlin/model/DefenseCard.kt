package model

import enums.CardType
import enums.DefenseType

class DefenseCard(
    name: String,
    description: String,
    val subType: DefenseType,
) : Card(name, description, CardType.DEFENSE) {
    override fun copy(): DefenseCard = DefenseCard(name, description, subType)
}
