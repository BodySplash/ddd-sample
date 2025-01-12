package bodysplash.domain

import bodysplash.support.Repository

interface Repositories {

    val games: Repository<GameId, GameCommand, GameState, GameEvent>
}