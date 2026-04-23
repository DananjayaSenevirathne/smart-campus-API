# Smart Campus API

A RESTful API for managing campus rooms, sensors, and sensor readings.
Built using **JAX-RS (Jersey 3.1.6)** with **Grizzly2** as the embedded HTTP server.

---

## Technology Stack

- Java 17
- JAX-RS / Jersey 3.1.6 (Jakarta EE 9+)
- Grizzly2 HTTP Server (embedded)
- Jackson (JSON serialization)
- Maven (build tool)
- In-memory storage with `ConcurrentHashMap`

---

## API Design Overview

The API follows RESTful principles with a versioned base path `/api/v1`.
Three primary resources are exposed:

- **Rooms** (`/api/v1/rooms`) — manage campus rooms
- **Sensors** (`/api/v1/sensors`) — manage sensors assigned to rooms
- **Sensor Readings** (`/api/v1/sensors/{id}/readings`) — historical reading data per sensor (sub-resource)

The design uses resource nesting to reflect the physical hierarchy of the campus.
Sensors belong to rooms, and readings belong to sensors.

---

## How to Build

```bash
git clone https://github.com/YOUR_USERNAME/smart-campus-API.git
cd smart-campus-API
mvn clean install
```

---

## How to Run

```bash
mvn exec:java
```

The server starts at: `http://localhost:8080/api/v1`

---

## How to Run Tests

```bash
mvn test
```

---

## Sample curl Commands

### 1. Get API discovery info
```bash
curl -X GET http://localhost:8080/api/v1/ -H "Accept: application/json"
```

### 2. Create a room
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"name": "Library Quiet Study", "capacity": 50}'
```

### 3. Create a sensor (replace ROOM_ID with actual id)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"type": "CO2", "status": "ACTIVE", "currentValue": 0.0, "roomId": "ROOM_ID"}'
```

### 4. Get sensors filtered by type
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2" -H "Accept: application/json"
```

### 5. Post a sensor reading (replace SENSOR_ID with actual id)
```bash
curl -X POST http://localhost:8080/api/v1/sensors/SENSOR_ID/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 850.5}'
```

### 6. Delete a room (returns 409 if sensors are still assigned)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/ROOM_ID
```

---

## Report: Answers to Coursework Questions

### Part 1.1 — JAX-RS Resource Lifecycle

The default lifecycle of a JAX-RS resource class is **request-scoped**: a new instance
is created for every incoming HTTP request and discarded after the response is sent.
This means instance-level fields are reset with every request — any data stored in them
would be lost immediately.

To manage shared, persistent data, fields must be declared `static` so they are shared
across all instances. Using `ConcurrentHashMap` is essential because the JAX-RS runtime
processes requests concurrently across multiple threads. A standard `HashMap` is not
thread-safe — two simultaneous POST requests could both read the old map state, both
generate an ID, and overwrite each other's data. `ConcurrentHashMap` handles concurrent
reads and writes safely without requiring explicit synchronisation blocks.

---

### Part 1.2 — HATEOAS

HATEOAS (Hypermedia As The Engine Of Application State) is considered a hallmark of
advanced REST design because it makes APIs self-discoverable. Instead of clients
relying on external documentation to know the URL structure, the API itself returns
links showing what resources and actions are available from the current state.

For client developers, this means they only need to know the single entry point URL.
The client navigates the API by following links in responses, similar to browsing a
website. This reduces coupling between the client and server — if URLs change, the
server updates the links and clients automatically use the new paths without code changes.
With static documentation, any URL change would require every client to be updated manually.

---

### Part 2.1 — IDs vs Full Objects in Lists

Returning only IDs in a list response is bandwidth-efficient but forces clients to make
one additional HTTP request per item to retrieve details — this is known as the N+1 request
problem. For a list of 100 rooms, that means 101 requests total, creating significant
latency and server load.

Returning full objects in the list means a single request delivers all needed data,
which is better for clients that typically need the full details. The trade-off is a
larger response payload per call. For this API, returning full room objects is the
better choice because campus management clients generally need all room details at once.

---

### Part 2.2 — Idempotency of DELETE

The DELETE operation in this implementation is idempotent. REST defines idempotency
as "multiple identical requests produce the same server-side state", not necessarily
the same response code.

- First DELETE call: room is found, removed from the map, returns `204 No Content`.
- Second DELETE call: room is no longer in the map, returns `404 Not Found`.

The server state is identical after both calls — the resource is absent in both cases.
The response code differs (204 vs 404), but the underlying state does not change on
the second or subsequent calls. This satisfies the REST definition of idempotency.

---

### Part 3.1 — @Consumes Mismatch

When a POST endpoint is annotated with `@Consumes(MediaType.APPLICATION_JSON)` and a
client sends a request with `Content-Type: text/plain` or `Content-Type: application/xml`,
JAX-RS performs content negotiation before invoking the resource method. The framework
detects that the client's declared content type does not match the supported type and
automatically returns `415 Unsupported Media Type` — no custom exception handling code
is required. The resource method is never called in this case.

---

### Part 3.2 — @QueryParam vs @PathParam for Filtering

Using `@QueryParam` for type filtering (`GET /sensors?type=CO2`) is superior to
embedding the filter in the path (`GET /sensors/type/CO2`) for several reasons:

Query parameters are optional — `GET /sensors` still returns all sensors without the
parameter, making it a more versatile endpoint. Path parameters imply that `type` is
a resource identifier, suggesting `/sensors/type/CO2` is a specific resource, which
is semantically incorrect. Query parameters can also be combined for multi-filter
searches (e.g., `?type=CO2&status=ACTIVE`), which path segments cannot do cleanly.
Query parameters are the established REST convention for search and collection filtering.

---

### Part 4.1 — Sub-Resource Locator Pattern

The sub-resource locator pattern improves API maintainability by applying the
Single Responsibility Principle. Rather than having one large `SensorResource`
class with methods for both sensors and their readings, the readings logic is
delegated to a dedicated `SensorReadingResource` class.

The locator method in `SensorResource` has the `@Path` annotation but no HTTP method
annotation — this signals to JAX-RS that it should delegate request handling to
the returned object. Each class has one focused responsibility, making the codebase
easier to read, test, and extend. In large APIs with deep nesting, this prevents
"God Classes" with hundreds of unrelated methods.

---

### Part 5.2 — HTTP 422 vs 404

HTTP 404 (Not Found) means the requested URL resource does not exist — the endpoint
itself is missing. HTTP 422 (Unprocessable Entity) means the request was syntactically
valid and the endpoint exists, but the semantic content of the request body cannot be
processed as-is.

When a client POSTs a new sensor with a `roomId` that references a non-existent room,
the `/sensors` endpoint itself exists (not 404). The problem is that a value *inside*
the valid JSON payload references something that doesn't exist. Returning 422 gives
the client specific information: "your request structure is fine, but this particular
reference is invalid." A 404 would mislead the client into thinking the sensor endpoint
itself is missing.

---

### Part 5.4 — Security Risks of Exposing Stack Traces

Exposing Java stack traces to external API consumers creates several security risks:

1. **Internal path disclosure**: Stack traces reveal full file system paths (e.g.,
   `C:\Users\dev\project\src\...`), exposing server directory structure.
2. **Library and version disclosure**: Stack traces reveal dependency names and versions
   (e.g., `jersey-server-3.1.6`, `jackson-databind-2.15`). Attackers can look up known
   CVEs for those exact versions.
3. **Code structure disclosure**: Class names, method names, and line numbers expose the
   internal application architecture, helping attackers identify where vulnerabilities
   might exist.
4. **Logic flow disclosure**: The call chain in a stack trace reveals the internal
   processing flow, making it easier to construct targeted attacks.

The correct approach is to log the full stack trace server-side and return only a
generic, safe error message to the client.

---

### Part 5.5 — Filters vs Manual Logging

Using JAX-RS filters for cross-cutting concerns like logging follows the DRY (Don't
Repeat Yourself) principle. If logging were added manually to every resource method,
it would need to be added to every current method and every future method added to
the API. Missing one method means a gap in observability.

A single filter class automatically intercepts every request and response across all
endpoints — present and future — without any changes to resource classes. This
centralisation also makes it easy to change the logging format, add correlation IDs,
or disable logging entirely by modifying one class instead of dozens.

---

## Project Structure

```
smart-campus-API/
├── src/main/java/com/smartcampus/api/
│   ├── config/
│   │   ├── MainServer.java
│   │   └── ApplicationConfig.java
│   ├── model/
│   │   ├── Room.java
│   │   ├── Sensor.java
│   │   └── SensorReading.java
│   ├── resource/
│   │   ├── DiscoveryResource.java
│   │   ├── RoomResource.java
│   │   ├── SensorResource.java
│   │   └── SensorReadingResource.java
│   ├── service/
│   │   └── DataStore.java
│   ├── exception/
│   │   ├── RoomNotEmptyException.java
│   │   ├── RoomNotEmptyExceptionMapper.java
│   │   ├── LinkedResourceNotFoundException.java
│   │   ├── LinkedResourceNotFoundExceptionMapper.java
│   │   ├── SensorUnavailableException.java
│   │   ├── SensorUnavailableExceptionMapper.java
│   │   └── GlobalExceptionMapper.java
│   └── filter/
│       └── LoggingFilter.java
└── pom.xml
```