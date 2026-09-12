# Large service result representation is undefined

**Observed:** `NodeApi.md` forbids embedding very large binary resources in JSON, but leaves the way a
result references or exposes streamed data as a TODO.

**Impact:** Storage, Public File Sharing, and Page results can naturally exceed JSON limits. Two nodes
may interoperate at the peer layer while clients cannot retrieve the resulting bytes through the
standard Node API.

**Proposal:** Define a bounded result descriptor and a local authenticated streaming operation with
length, content hash, cancellation, expiry, and error semantics. Specify which party cleans up an
unconsumed stream.

**Conformance test:** Invoke a result larger than the JSON limit, validate its descriptor, stream and
hash every byte, then test cancellation, expiry, and a corrupted/truncated stream.
