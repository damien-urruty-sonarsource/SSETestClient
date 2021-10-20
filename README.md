# SSETestClient

This client is written using Apache HttpClient 5.0.3, which is the version currently in use in SonarLint for IntelliJ.

The client connects to `http://localhost:8080/eventStream`, and prints any received message in the console.

## Start the client:

> ./gradlew run

The client should remain active forever, reconnecting as soon as the request finishes.

A server implementation can be found at [SSETestServer](https://github.com/damien-urruty-sonarsource/SSETestServer).
