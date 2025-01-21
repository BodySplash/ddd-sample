package bodysplash.domain

import lib.ddd.domain.Repository

interface Repositories {

    val games: Repository<GameId, GameCommand>
}