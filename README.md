# census-rm-fieldwork-adapter

Census Response Management (RM) Fieldwork Adapter.

## Introduction

The fieldwork adapter is a Spring Boot service that bridges between RM case events and the 2021-format
fieldwork action instruction messages expected by FWMTG for the 2027 Census test.

It listens to `CASE_UPDATE` events from Pub/Sub, checks the optional `fieldActionInstruction` value in the event
header, and decides whether that case update should be forwarded to fieldwork:

- if `fieldActionInstruction` is `null`, the message is acknowledged and ignored
- if `fieldActionInstruction` is `CREATE`, `UPDATE`, or `CANCEL`, the case update is converted into the equivalent
  fieldwork action instruction message and published to the fieldwork action instruction topic

This keeps the forwarding logic out of the services that produce case updates and centralises the conversion into the
message format required by FWMTG.

The adapter also applies a business rule for the 2027 test: cases in the NISRA region must not be sent to fieldwork.
If a `CASE_UPDATE` relates to a Northern Ireland case (region beginning with `N`), the message is acknowledged but no
action instruction is published.

In practice, this means that when new non-NISRA cases are created and RM marks their `CASE_UPDATE` event with
`fieldActionInstruction=CREATE`, the adapter publishes corresponding `CREATE` action instructions for FWMTG. Support for
other fieldwork instructions, such as `UPDATE` and `CANCEL`, is handled through the same metadata-driven approach.

## Building
Podman and Docker are both supported for building and running the application.
By default the Makefile will use `docker` unless you are on an `arm64` architecture (e.g. M1/M2 Mac) in which case it will use `podman`.
You can override this by setting the `DOCKER` environment variable to either `docker` or `podman`.
For example, to force using `docker` on an M1/M2 Mac:
```shell
DOCKER=docker make <command>
```

To run all the tests and build the image

```shell
   make build
```

Just build the image

```shell
    make build-no-test
```

### Local Docker Java Healthcheck

Since docker compose health checks are run inside the container, we need a method of checking service health that can
run in our minimal alpine Java JRE images. To accomplish this, we have a small Java health check class which simply
calls a http endpoint and succeeds if it gets a success status. This is compiled into a JAR, which is then mounted into
the containers, so it can be executed by the JRE at container runtime.

#### Building Changes

If you make changes to the [HealthCheck.java](src/test/resources/java_healthcheck/HealthCheck.java), you must then
run `make rebuild-java-healthcheck` to compile and package the updated class into the jar, and commit the resulting
built changes.

## Debugging With PubSub Emulator

Make sure you have the following environment variables set if you want to run in the debugger in your IDE:

```shell
SPRING_CLOUD_GCP_PUBSUB_EMULATOR_HOST=localhost:8538
QUEUECONFIG_PUBSUB-PROJECT=our-project
```

## Debugging With GCP PubSub Project

If you want to use real GCP PubSub topics and subscriptions, make sure you have the following environment variables set
if you want to run in the debugger in your IDE:

```shell
SPRING_CLOUD_GCP_PUBSUB_PROJECT-ID=<GCP Project>
QUEUECONFIG_PUBSUB-PROJECT=<GCP Project>
```
