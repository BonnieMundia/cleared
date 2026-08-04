package app.cleared.data.model

/**
 * The two halves of the app plus the two ways out of it.
 *
 * WORK is effort that has not become money (violet). MONEY means a payout exists somewhere in the
 * chain (green). TERMINAL is a record that will never move again.
 */
enum class Phase { PRE, WORK, MONEY, TERMINAL }

/**
 * A record's position in the chain. `order` breaks ties between two events written at the same
 * instant — see [app.cleared.data.derive.StageResolver].
 *
 * REJECTED and REVERSED are both terminal. A record never re-enters a stage it has left: that would
 * break "current stage is the last event" and double-count the record in every settle-time
 * percentile. Recovery from a reversal is a *new* record linked by `supersedesRecordId`.
 */
enum class Stage(val phase: Phase, val order: Int) {
    PROSPECT(Phase.PRE, 0),
    SUBMITTED(Phase.WORK, 1),
    IN_REVIEW(Phase.WORK, 2),
    APPROVED(Phase.WORK, 3),
    PAYOUT_ISSUED(Phase.MONEY, 4),
    RECEIVED(Phase.MONEY, 5),
    LANDED(Phase.MONEY, 6),
    REJECTED(Phase.TERMINAL, 7),
    REVERSED(Phase.TERMINAL, 8);

    /** True when the record still has somewhere to go. Terminal and landed rows do not advance. */
    val isAdvanceable: Boolean get() = phase != Phase.TERMINAL && this != LANDED

    /** The stage a tap moves this record to, or null when it does not advance. */
    fun next(): Stage? = when (this) {
        PROSPECT -> SUBMITTED
        SUBMITTED -> IN_REVIEW
        IN_REVIEW -> APPROVED
        APPROVED -> PAYOUT_ISSUED
        PAYOUT_ISSUED -> RECEIVED
        RECEIVED -> LANDED
        LANDED, REJECTED, REVERSED -> null
    }
}

/**
 * Storage scale is 2 for all three currencies — money is held as a scaled Long of minor units and
 * never as a Double. [displayScale] is a presentation concern only: KES renders to zero decimals,
 * USD and EUR to two.
 */
enum class Currency(val displayScale: Int) {
    USD(2),
    EUR(2),
    KES(0);

    companion object {
        const val STORAGE_SCALE = 2
    }
}

enum class PlatformKind { AI_TRAINING, WRITING, MARKETPLACE, OWN_COMPANY }

enum class PayoutDestination { PAYPAL, PAYONEER }

enum class WalletProvider { PAYPAL, PAYONEER }

enum class BankDestination { EQUITY_BANK, MPESA }

/** Fees are never refunded on a reversal, which is why a reversed record can net negative. */
enum class FeeKind {
    PLATFORM_COMMISSION,
    WITHDRAWAL_FEE,
    BANK_CREDIT_FEE,
    FX_SPREAD,
    RETURN_HANDLING_FEE
}

/** Where a stage event came from. A conflict resolution records who won here. */
enum class EventSource { MANUAL, EMAIL_PARSE, PLATFORM_API, SMS_PARSE }

enum class SyncOpState { WAITING, RETRYING, CONFLICT, DONE, FAILED }
