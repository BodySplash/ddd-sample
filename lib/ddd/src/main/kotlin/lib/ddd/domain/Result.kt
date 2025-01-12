package lib.ddd.domain

import arrow.core.Either
import lib.common.BusinessError

typealias BusinessResult<T> = Either<BusinessError, T>
