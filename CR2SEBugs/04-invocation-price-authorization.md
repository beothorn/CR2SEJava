# Charged service invocation lacks a normative maximum-price field

**Observed:** `NodeApi.md` says every charged invocation agrees its price, but its normative
`service.invoke` request contains no agreed price or maximum. The Node API pseudocode conditionally
checks `maximum_price`, even though the normative operation never defines that extension's type or
semantics.

**Impact:** A client cannot place an interoperable hard cap on spending between reading a Board and
invocation. Price changes and dynamic pricing create a time-of-check/time-of-use risk. Implementations
may variously ignore, reject, or interpret the Java client's optional `maximum_price` field.

**Proposal:** Normatively require an exact unsigned `maximum_price` (JSON integer or canonical decimal
string), define the issuer/unit, and return `price_exceeds_maximum` before side effects. Return the
final agreed price in the result.

**Conformance test:** Change an offering price after selection; invocation above the cap must perform
no service or ledger mutation, while an exact-cap invocation settles exactly once.
