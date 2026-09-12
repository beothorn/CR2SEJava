# Node API maximum message size has no interoperable value

**Observed:** Nodes must impose a configurable maximum, while the required minimum, recommended
default, and over-limit error behavior remain TODOs.

**Impact:** A request accepted by one conforming node can be rejected or disconnect another. Clients
cannot preflight boards, service definitions, arguments, or results and cannot reliably distinguish
an intentional rejection from transport failure.

**Proposal:** Define a required minimum, advertise receive limits via `api.info`, and standardize a
`message_too_large` response when the request ID can be recovered. Define whether the connection is
then reusable.

**Conformance test:** Exercise exactly-limit and limit-plus-one UTF-8 byte frames, including a split
TCP write, and assert the standardized response/lifecycle.
