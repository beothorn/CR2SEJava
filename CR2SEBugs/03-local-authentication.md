# Loopback Node API has no client authentication

**Observed:** The specification correctly says loopback is not a trust boundary, but leaves portable
local authentication as a TODO.

**Impact:** Any process in the same host/network namespace may connect, inspect metadata, disconnect
peers, and invoke services that spend credits. Binding to loopback prevents remote access but not a
malicious or compromised local account/process.

**Proposal:** Define bootstrap credential storage and a challenge-response/session authentication
exchange before privileged operations; include credential rotation, permission scoping, replay
protection, and stable authorization errors.

**Conformance test:** Verify unauthenticated and incorrectly authenticated calls cannot invoke or
disconnect, valid credentials work, and captured authentication messages cannot be replayed.
