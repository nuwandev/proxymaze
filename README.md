# ProxyMaze

Core monitoring implementation for ProxyMaze.

## Build

```bash
mvn -q -DskipTests compile
```

## Run

```bash
mvn spring-boot:run
```

## Implemented core pieces

- in-memory proxy/state store
- HTTP probe client
- background monitoring engine
- alert lifecycle evaluation
- metrics snapshot service

Controllers and request DTOs are intentionally left for the API layer.

