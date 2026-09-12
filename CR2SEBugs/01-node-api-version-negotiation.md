# Node API version negotiation is undefined

**Observed:** `NodeApi.md` gives the peer protocol and Node API independent versions, but leaves the
discovery/negotiation operation and incompatibility response as a TODO. Requests carry no version.

**Impact:** Two valid implementations can assign different semantics to the same operation and have
no portable way to detect the mismatch. A reference client cannot state which contract the endpoint
implements.

**Proposal:** Require a side-effect-free `api.info` operation available in every version, returning
the selected major/minor version, limits, and capabilities. Major mismatch must return a stable
`unsupported_api_version` error.

**Conformance test:** Connect, call `api.info`, negotiate a mutually supported version, then verify an
unsupported major fails without closing the session.
