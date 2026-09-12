# CR2SE specification issues found by the Java reference client

These are actionable interoperability reports against
[`beothorn/CR2SE`](https://github.com/beothorn/CR2SE), found while implementing the client described
in the [project README](../README.md). No CR2SE node was available, so these are specification-review
findings rather than failures reproduced against a server. Each file contains a suggested resolution
and a conformance test that implementations can share.

| Report | Impact |
| --- | --- |
| [01 — no Node API version negotiation](01-node-api-version-negotiation.md) | incompatible clients and nodes cannot fail safely |
| [02 — no message-size contract](02-message-size-limit.md) | portability and denial-of-service behavior differ |
| [03 — no local authentication](03-local-authentication.md) | any local process may control/spend through a node |
| [04 — charged invocation lacks normative price authorization](04-invocation-price-authorization.md) | a client cannot safely cap spending interoperably |
| [05 — request-ID type contradiction](05-request-id-type.md) | pseudocode can produce messages rejected by the normative text |
| [06 — large service results are undefined](06-large-results.md) | standard binary-heavy services lack a Node API result mechanism |

Please verify each report against the upstream revision being targeted before filing it: some are
explicit upstream TODOs and may since have been resolved. This folder records them separately so a
test harness can distinguish an implementation defect from an underspecified protocol behavior.
