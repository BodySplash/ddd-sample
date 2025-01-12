package lib.ddd.persistence

import lib.common.BusinessError
import kotlin.reflect.KClass

class EntityNotFound : BusinessError {

    constructor(message: String) : super("ENTITY_NOT_FOUND", message)

    constructor() : super("ENTITY_NOT_FOUND", "ENTITY_NOT_FOUND")

    companion object {
        fun forEntity(clazz: KClass<*>, id: String): EntityNotFound {
            return EntityNotFound(String.format("Entity %s(%s) does not exist", clazz.simpleName, id))
        }
    }
}
