#!/bin/sh

curl -v -H "Content-Type: application/json" -H "Accept: application/json" http://localhost:8080/webhook -d "$(cat src/test/resources/sample-update-request.json)" -H "X-Hub-Signature: $(cat src/test/resources/sample-update-request.txt)"