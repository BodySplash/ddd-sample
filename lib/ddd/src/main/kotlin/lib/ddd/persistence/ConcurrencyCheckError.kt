package lib.ddd.persistence

class ConcurrencyCheckError(targetId: String, type: String) :
    RuntimeException("Check failed on $targetId@$type")
