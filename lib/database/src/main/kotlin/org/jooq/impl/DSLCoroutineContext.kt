package org.jooq.impl

import org.jooq.DSLContext

interface DSLCoroutineContext {
    val dsl: DSLContext
}