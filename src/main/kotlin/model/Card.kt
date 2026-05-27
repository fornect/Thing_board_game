package model

import enums.CardType

open class Card(
    val name: String,
    val description: String,
    val type: CardType
) {
    open fun copy(): Card = Card(name, description, type)
}
