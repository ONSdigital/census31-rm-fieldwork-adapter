# census-rm-fieldwork-adapter

Census Response Management (RM) Fieldwork Adapter.

## Overview

`census-rm-fieldwork-adapter` is a stateless Spring Boot microservice that transforms `CASE_UPDATE` events into fieldwork action instructions for FWMTG.

It exists to keep fieldwork mapping logic out of upstream services and to provide one controlled place for:
- validation
- business filtering
- transformation
- publication to the fieldwork messaging contract

## Architecture Summary

```text
case-processor -> case-update topic -> [subscription] -> fieldwork-adapter -> fieldwork-action-instruction topic -> FWMTG
```

The service consumes from Pub/Sub, evaluates each case update, and publishes a mapped fieldwork instruction when required.

### Why Direct Publish (and no outbox)

The adapter is intentionally stateless and does not persist case state. Because there is no local business-state write to coordinate with outbound publish, there is no dual-write problem to solve in this service.

For this reason, direct publish is the chosen architecture:
- simpler operational model (no outbox table/poller lifecycle)
- lower end-to-end latency (publish in-handler)
- clearer ownership of retries/redelivery through Pub/Sub delivery semantics
- cleaner separation from `census31-rm-case-processor`, where outbox remains the correct pattern for a stateful service

This is a scoped architectural decision for this adapter only, based on current fieldwork requirements and implementation.

## Processing Behaviour

For each inbound `CASE_UPDATE`:
1. Validate event type and required structure.
2. If `fieldActionInstruction` is missing, ignore and acknowledge.
3. If case region is NISRA (region starts with `N`), suppress publish.
4. If instruction is `CREATE`, `UPDATE`, or `CANCEL`, map to FWMT instruction payload.
5. Publish to `fieldwork-action-instruction` with message attributes (including the inbound `eventId` when available).

## Reliability Model

The adapter uses at-least-once messaging semantics.

- Publish failures raise exceptions so the inbound message is not acknowledged.
- Pub/Sub redelivery handles transient failure paths.
- Duplicate delivery is expected in distributed messaging and must be handled idempotently by consumers.
- Structured outcome logging provides operational and audit visibility for published, suppressed, and ignored decisions.

## Key Configuration

Application defaults are in `src/main/resources/application.yml`.

Important properties:
- `queueconfig.case-update-subscription`
- `queueconfig.fieldwork-action-instruction-topic`
- `queueconfig.publishtimeout`
- `spring.cloud.gcp.pubsub.subscriber.parallel-pull-count`
- `spring.cloud.gcp.pubsub.subscriber.executor-threads`
- `spring.cloud.gcp.pubsub.subscriber.max-ack-extension-period`
- `spring.cloud.gcp.pubsub.subscriber.flow-control.max-outstanding-element-count`

## Build and Test

Podman and Docker are both supported for build and local runs.

The `Makefile` chooses a default runtime based on host architecture:
- `amd64` -> `docker`
- `arm64` (for example M1/M2 Mac) -> `podman`

You can override this default at command time:

```shell
DOCKER=docker make build
```

```shell
make build
make build-no-test
make test
```

### Local Docker Java Healthcheck

Container health checks run inside the container image, so this repository also includes a tiny Java-based healthcheck helper used by local Docker Compose test flows.

If you change `src/test/resources/java_healthcheck/HealthCheck.java`, rebuild the jar and commit the generated update:

```shell
make rebuild-java-healthcheck
```

## Local Development

To run with the Pub/Sub emulator, set:

```shell
SPRING_CLOUD_GCP_PUBSUB_EMULATOR_HOST=localhost:8538
SPRING_CLOUD_GCP_PUBSUB_PROJECT_ID=our-project
```

To run against a real GCP project, set:

```shell
SPRING_CLOUD_GCP_PROJECT_ID=<GCP Project>
SPRING_CLOUD_GCP_PUBSUB_PROJECT_ID=<GCP Project>
```

