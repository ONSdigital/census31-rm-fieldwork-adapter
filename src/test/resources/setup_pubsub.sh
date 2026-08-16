#!/bin/sh

# Wait for pubsub-emulator to come up
bash -c 'while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' '$PUBSUB_SETUP_HOST')" != "200" ]]; do sleep 1; done'

curl -X PUT http://$PUBSUB_SETUP_HOST/v1/projects/our-project/topics/event_case-update
curl -X PUT http://$PUBSUB_SETUP_HOST/v1/projects/our-project/subscriptions/event_case-update_rm-fieldwork-adapter -H 'Content-Type: application/json' -d '{"topic": "projects/our-project/topics/event_case-update"}'

curl -X PUT http://$PUBSUB_SETUP_HOST/v1/projects/our-project/topics/event_fieldwork_action-instruction
curl -X PUT http://$PUBSUB_SETUP_HOST/v1/projects/our-project/subscriptions/event_fieldwork_action-instruction_rh_at -H 'Content-Type: application/json' -d '{"topic": "projects/our-project/topics/event_fieldwork_action-instruction"}'
