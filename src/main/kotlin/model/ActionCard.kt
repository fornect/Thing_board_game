package model

import enums.ActionType
import enums.CardType

class ActionCard(
    name: String,
    description: String,
    val subType: ActionType,
) : Card(name, description, CardType.ACTION) {
    override fun copy(): ActionCard = ActionCard(name, description, subType)
}
