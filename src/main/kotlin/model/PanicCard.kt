package model

import enums.CardType
import enums.PanicType

class PanicCard(
    name: String,
    description: String,
    val subType: PanicType,
) : Card(name, description, CardType.PANIC) {
    override fun copy(): PanicCard = PanicCard(name, description, subType)
}
