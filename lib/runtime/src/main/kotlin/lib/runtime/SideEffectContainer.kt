package lib.runtime

import arrow.fx.coroutines.Resource

interface SideEffectContainer<T> {

    fun create(): Resource<T>

}
