package model

import enums.PanicType
import enums.CardType

class PanicCard(
    name: String,
    description: String,
    val subType: PanicType
) : Card(name, description, CardType.PANIC) {
    override fun copy(): PanicCard = PanicCard(name, description, subType)
}