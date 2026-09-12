# Request-ID type contradicts Node API pseudocode

**Observed:** Normative `NodeApi.md` says request IDs are JSON strings. `pseudoCode/NodeApi.md` models
the request ID and outstanding set as `string-or-integer`.

**Impact:** An implementation following the pseudocode may emit or accept numeric IDs, while a strict
implementation following the normative document rejects them. Large JSON numbers additionally risk
loss in implementations that use IEEE-754 values.

**Proposal:** Change pseudocode to `string` everywhere and explicitly reject all other JSON types with
`invalid_request`. Keep IDs opaque; never parse them numerically.

**Conformance test:** String IDs such as `"001"` round-trip unchanged; numeric, null, boolean, object,
and array IDs receive `invalid_request` without closing the session.
