# la-water-watch

## Install

You need **Java 25**. Maven comes with the project (`./mvnw`), so you don't install it separately.

macOS (Homebrew):

```bash
brew install openjdk@25
java -version   # should print 25
```

Other systems: install a JDK 25 build (for example from https://adoptium.net) and make sure `java -version` prints 25.

## Run

```bash
cd water-api
./mvnw spring-boot:run
```

The first start downloads the data (about 20 seconds) into `water-api/data/water.db`. Later starts reuse it.

To force a fresh download:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--app.data.force-refresh=true
```

The API runs at http://localhost:8080. Quick check:

```bash
curl localhost:8080/api/meta
```

### API contract (Swagger)

With the backend running, open **http://localhost:8080/swagger-ui.html** to see every endpoint, its parameters and response fields, and to try requests from the browser. The raw OpenAPI spec is at http://localhost:8080/v3/api-docs (import it into Postman, or generate a typed client from it).

Errors always come back in the same shape:

```json
{ "status": 404, "error": "Not Found", "message": "No water system with PWSID 'X'. ...", "timestamp": "2026-11-07T23:39:55Z" }
```

The one exception is 429 (rate limit), which only has `{ "status": 429, "message": "Too many requests, please try again later." }`.

## Test

```bash
cd water-api
./mvnw test
```
