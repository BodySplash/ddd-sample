package bodysplash.support

import arrow.core.left
import arrow.core.raise.Raise
import arrow.core.raise.fold
import arrow.core.right
import lib.common.BusinessError
import lib.ddd.domain.BusinessResult

fun interface ReplyConsumer<in R> {
    fun accept(reply: R)
}

interface AggregateRef<TCommand> {
    suspend fun handle(command: TCommand)
}

sealed interface AggregateEffect<out TEvent> {
    data class Persist<out TEvent>(val events: List<TEvent>) : AggregateEffect<TEvent> {
    }

    data class Reply<TReply, out TEvent>(val reply: TReply, val consumer: ReplyConsumer<TReply>) :
        AggregateEffect<TEvent>

    data class Both<TEvent>(
        val reply: Reply<*, TEvent>,
        val persist: Persist<TEvent>
    ) : AggregateEffect<TEvent>
}

fun <TEvent, Reply> AggregateEffect.Persist<TEvent>.andReply(reply: Reply, consumer: ReplyConsumer<Reply>) =
    AggregateEffect.Both(AggregateEffect.Reply(reply, consumer), this)


interface AggregateBehaviour<ID, Command, State, Event> {

    fun initialState(): State

    fun decide(id: ID, command: Command, state: State): AggregateEffect<Event>

    fun evolve(state: State, event: Event): State

}

fun <Reply : Any, Event> buildEffect(
    replyConsumer: ReplyConsumer<BusinessResult<Reply>>,
    block: Raise<BusinessError>.(builder: EffectBuilder<Reply, Event>) -> EffectBuilderFinalStep
): AggregateEffect<Event> {
    val builder = InnerBuilder<Reply, Event>(replyConsumer)
    return fold(
        { block.invoke(this, builder) },
        { businessError -> AggregateEffect.Reply(businessError.left(), replyConsumer) },
        { effect -> builder.build() })
}

interface EffectBuilder<Reply, Event> {
    fun persist(vararg event: Event): EffectBuilderStep2<Reply, Event>
}

interface EffectBuilderStep2<Reply, Event> {
    fun andReply(reply: Reply): EffectBuilderFinalStep
}

interface EffectBuilderFinalStep

private data class InnerBuilder<Reply : Any, Event>(private val consumer: ReplyConsumer<BusinessResult<Reply>>) :
    EffectBuilder<Reply, Event>, EffectBuilderStep2<Reply, Event>,
    EffectBuilderFinalStep {

    private lateinit var events: List<Event>
    private lateinit var reply: Reply

    override fun persist(vararg event: Event): EffectBuilderStep2<Reply, Event> {
        events = event.toList()
        return this
    }

    override fun andReply(reply: Reply): EffectBuilderFinalStep {
        this.reply = reply
        return this
    }

    fun build(): AggregateEffect<Event> {
        return AggregateEffect.Persist(events).andReply(reply.right(), consumer)
    }
}