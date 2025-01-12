package lib.common

open class BusinessError protected constructor(val code: String, message: String) : RuntimeException(message) {

    companion object {
        fun withCodeAndMessage(code: String, message: String): BusinessError {
            return BusinessError(code, message)
        }

        fun withCode(code: String): BusinessError {
            return BusinessError(code, code)
        }

        fun withMessage(message: String): BusinessError {
            return BusinessError("UNKNOWN", message)
        }
    }
}