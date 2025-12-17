# Repository Guidelines (Backend)

This file applies to the `jipjung-backend/` tree.

## Quick Commands
- `./mvnw spring-boot:run` — run locally (default profile is `h2`, set in `src/main/resources/application.properties`).
- `./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql` — run against MySQL profile (requires a local `src/main/resources/application-mysql.properties` + DB).
- `./mvnw clean package` — build a jar (runs tests unless you pass `-DskipTests`).
- `./mvnw test` — run tests.
- `./mvnw test -Dtest=ClassName` — run a specific test class.

## Local URLs (Default)
- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- H2 Console (H2 profile): `http://localhost:8080/h2-console` (`jdbc:h2:mem:jipjung`, user `sa`, empty password)

## Requirements
- Java `17` (see `pom.xml`).

## Project Structure & Module Organization
- Entry point: `src/main/java/com/jipjung/project/JipJungApplication.java`
- HTTP layer: `src/main/java/com/jipjung/project/controller`
- DTOs: `src/main/java/com/jipjung/project/controller/dto/request` and `src/main/java/com/jipjung/project/controller/dto/response`
- Business logic: `src/main/java/com/jipjung/project/service`
- Domain models: `src/main/java/com/jipjung/project/domain` (MyBatis type aliases: `mybatis.type-aliases-package`)
- DSR engine: `src/main/java/com/jipjung/project/dsr` (calculator/policy/result objects)
- AI DTOs: `src/main/java/com/jipjung/project/ai` (used by AI manager flow)
- Persistence: `src/main/java/com/jipjung/project/repository` (MyBatis) + XML mappers in `src/main/resources/mapper/**/*.xml`
- Cross-cutting: `src/main/java/com/jipjung/project/global` (exception handling, OpenAPI helpers, response wrappers)
- Auth/JWT: `src/main/java/com/jipjung/project/config/jwt` (filters/handlers)
- Security rules: `src/main/java/com/jipjung/project/config/SecurityConfig.java`
- API contract reference: `REST_API.md`

## Database & Schema Notes
- Seed/schema scripts live in `src/main/resources/schema-*.sql` and `src/main/resources/data-*.sql`.
- Prefer the schema scripts as the source of truth if docs disagree (e.g. older docs mention `apartment_transaction`, current schema/mappers use `apartment_deal`).

## Configuration & Secrets
- Spring loads an optional dotenv file via `spring.config.import=optional:file:.env[.properties]` (do not commit secrets).
- JWT settings live in `src/main/resources/application.properties`; update `SecurityConfig` when adding new public/authenticated routes.
- `src/main/resources/application-mysql.properties` is intentionally gitignored; create it locally when running with the `mysql` profile.

## AI Integration (Spring AI)
- Uses `spring-ai-starter-model-vertex-ai-gemini` and reads `GCP_PROJECT_ID` / `GCP_LOCATION` from env.
- Keep AI calls behind feature flags / config when possible; avoid introducing runtime dependencies on external services for local dev/tests.
