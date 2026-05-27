package model

import enums.ObstacleType
import enums.CardType

class ObstacleCard(
    name: String,
    description: String,
    val subType: ObstacleType
) : Card(name, description, CardType.OBSTACLE) {
    override fun copy(): ObstacleCard = ObstacleCard(name, description, subType)
}