# CR2SE Java command-line client

This repository is a **reference Java client** for the CR2SE local Node API described by the
[upstream CR2SE specification](https://github.com/beothorn/CR2SE). It controls an already-running
CR2SE node; it is not a peer/node implementation and does not implement the CR2SE peer wire
protocol. The generic `service-invoke` command reaches every standard service family (Storage,
Computation, Public File Sharing, Messaging, Discovery, and Page) and custom services alike.

The implementation intentionally separates layers:

* `cli` parses commands, prints JSON, and maps failures to exit codes;
* `application` offers a Java facade for all seven version-1 operations;
* `protocol` owns JSON envelope encoding and validation;
* `transport` owns persistent loopback TCP and JSON-lines framing.

See [`CR2SEBugs/README.md`](CR2SEBugs/README.md) for interoperability gaps discovered while writing
the client. Those reports explain why some otherwise useful behavior cannot yet be standardized.

## Requirements and build

Install JDK 17 or newer and Maven 3.9 or newer, then run:

```sh
mvn clean verify
java -jar target/cr2se.jar --help
```

The shaded `target/cr2se.jar` contains its dependencies. By default the client connects to
`127.0.0.1:39471`; global options must precede the subcommand:

```sh
java -jar target/cr2se.jar --host 127.0.0.1 --port 39471 connections-list
```

The process exits with `0` on success, `2` for a structured node error, and `1` for transport,
syntax, or local validation failures. Successful output is JSON on stdout; diagnostics and logs
are on stderr, so scripts can consume stdout safely.

## Complete command reference

```text
connection-open ADDRESS PORT [--expected-peer-id ID]
connections-list
connection-close CONNECTION_ID
connection-ping CONNECTION_ID
board-get CONNECTION_ID
service-get CONNECTION_ID OFFERING_ID
service-invoke CONNECTION_ID OFFERING_ID --service NAME --service-version N --arguments JSON [--maximum-price N]
raw OPERATION JSON_OBJECT
batch
```

Typical discovery and invocation flow:

```sh
java -jar target/cr2se.jar connection-open 192.0.2.10 8042 --expected-peer-id cr2se:EXPECTED_ID
java -jar target/cr2se.jar board-get 7f90c317
java -jar target/cr2se.jar service-get 7f90c317 weather-current
java -jar target/cr2se.jar service-invoke 7f90c317 weather-current \
  --service example.weather.current --service-version 1 \
  --arguments '{"location":"New York"}' --maximum-price 1
java -jar target/cr2se.jar connection-close 7f90c317
```

Each ordinary command creates one local Node API session. `batch` instead keeps one persistent
session and reads operation objects from stdin (caller-provided `id` values are ignored):

```sh
printf '%s\n' \
  '{"operation":"connections.list"}' \
  '{"operation":"connection.ping","connection_id":"7f90c317"}' \
  | java -jar target/cr2se.jar --compact batch
```

`raw` is the forward-compatible escape hatch for implementation extensions and future operations:

```sh
java -jar target/cr2se.jar raw vendor.status '{"verbose":true}'
```

## Logging and troubleshooting

Logging defaults to `WARN`. Select any Logback level with a JVM property; the transport has
`ERROR`, `INFO`, `DEBUG`, and `TRACE` instrumentation:

```sh
java -Dcr2se.log.level=INFO  -jar target/cr2se.jar connections-list
java -Dcr2se.log.level=DEBUG -jar target/cr2se.jar connections-list
java -Dcr2se.log.level=TRACE -jar target/cr2se.jar connections-list
```

`TRACE` includes complete protocol frames and can expose service arguments, results, or other
sensitive data. Do not enable it in untrusted logs. The client deliberately refuses non-loopback
Node API hosts because version 1 has no local-client authentication. A timeout is configured with
`--timeout MILLISECONDS`.

## Development and compatibility

Run tests with `mvn test`. Tests act as executable protocol documentation: they cover envelopes,
structured errors, duplicate-key rejection, request construction, UTF-8 framing, session reuse,
loopback enforcement, and the complete CLI registry. The project targets Java 17 bytecode.

This client follows upstream `NodeApi.md` as observed on 12 September 2026. CR2SE is evolving;
pinning a formal Node API version is currently impossible (see the bug reports). Unknown response
fields are accepted as required for extensibility.
