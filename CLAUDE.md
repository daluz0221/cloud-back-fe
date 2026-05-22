# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Sistema de Administración de Tareas Domésticas** is a Spring Boot 3.5.14 REST API for managing household tasks within family groups. It implements a hexagonal architecture (Ports & Adapters) with a use-case-driven design pattern.

- **Language**: Java 21
- **Build System**: Maven (with mvnw wrapper)
- **Database**: PostgreSQL in production, H2 for testing
- **API Documentation**: Swagger/OpenAPI at `/swagger-ui.html`
- **Server Port**: 8080 (configurable in `application.properties`)

## Build and Run Commands

### Build the project
```bash
./mvnw clean package
```

### Run the application
```bash
./mvnw spring-boot:run
```

### Run tests
```bash
./mvnw test
```

### Run a specific test class
```bash
./mvnw test -Dtest=SistemaApplicationTests
```

### Skip tests during build
```bash
./mvnw clean package -DskipTests
```

### View test results
```bash
./mvnw surefire-report:report
```

### Clean build artifacts
```bash
./mvnw clean
```

## Project Structure and Architecture

### Hexagonal Architecture Layers

The codebase follows a **Ports & Adapters (Hexagonal) pattern** with clear separation of concerns:

```
src/main/java/sistema_organizacion/sistema/
├── SistemaApplication.java          (Entry point)
├── entities/                        (Core domain models)
│   ├── Usuario (abstract)
│   ├── JefeDeHogar (Admin user)
│   ├── MiembroHogar (Regular user)
│   ├── GrupoFamiliar
│   ├── Tarea
│   ├── DetalleTarea
│   ├── Rol & RolUsuario
│   ├── Estado & EstadoTarea
│   └── exception/               (Domain exceptions)
├── usecases/                    (Business logic)
│   ├── usuario/                 (User registration & login)
│   ├── grupo/                   (Family group management)
│   ├── tarea/                   (Task management)
│   └── impl/                    (Use case implementations/Interactors)
├── ports/                       (Interface contracts)
│   └── outs/                    (Output ports for repositories)
├── adapters/                    (External interface adapters)
│   ├── controllers/             (REST endpoints)
│   ├── persistence/             (JPA adapters - implements output ports)
│   ├── dto/                     (Data transfer objects)
│   │   ├── request/
│   │   └── response/
│   ├── presenters/              (DTO converters)
│   └── exception/               (Global exception handler)
├── infrastructure/              (Spring configuration)
│   ├── config/                  (Bean definitions, OpenAPI, CORS)
│   └── repositories/            (Spring Data JPA repositories)
└── resources/
    └── application.properties   (Configuration)
```

### Architecture Flow

1. **HTTP Request** → **Controller** (adapters/controllers/)
2. **Controller** creates a Command object and calls a **Use Case**
3. **Use Case** (interface in usecases/) is implemented by an **Interactor**
4. **Interactor** uses **Output Ports** (interface) to access data
5. **Output Port Implementation** (JPA Adapter) queries the database via **JPA Repositories**
6. **Domain Entities** apply business logic and validations
7. **Presenter** converts Entity to **Response DTO**
8. **Response** returned to client

### Key Design Patterns

- **Use Case Pattern**: Each feature is a separate use case (e.g., `RegistrarUsuarioUseCase`, `CrearTareaUseCase`)
- **Command Pattern**: Requests encapsulated as Command objects (e.g., `RegistrarUsuarioCommand`)
- **Port & Adapter Pattern**: Business logic decoupled from persistence via `OutputPort` interfaces
- **Presenter Pattern**: Entities converted to DTOs via Presenter classes for API responses
- **Single Table Inheritance**: `Usuario` uses JPA single table inheritance for `JefeDeHogar` (ADMIN) and `MiembroHogar` (USER) subclasses

### Database Schema

The schema is defined in `create_postgres_schema.sql` and includes:

- **roles**: ADMIN, USER
- **estados**: PENDIENTE, EN_PROCESO, TERMINADA
- **grupos**: Family groups with access codes
- **usuarios**: Abstract base table with single-table inheritance
- **tareas**: Tasks assigned to groups/users
- **detalle_tareas**: Task history/details

Relationships:
- Usuario → Rol (Many-to-One)
- Usuario → GrupoFamiliar (Many-to-One, optional)
- Tarea → Usuario (Many-to-One, optional)
- Tarea → GrupoFamiliar (Many-to-One)
- Tarea → Estado (Many-to-One)
- DetalleTarea → Tarea (Many-to-One)

## Core Use Cases and Endpoints

### Usuario (User Management)
- **HU-01**: User Registration → `POST /api/usuarios/registro`
  - Accepts RegistroRequest with nombre, apellido, correo, username, contrasena, rol
  - Creates JefeDeHogar (ADMIN) or MiembroHogar (USER) entity
  - Validates email format and password strength at entity level

- **HU-10**: Login → `POST /api/usuarios/login`
  - Accepts LoginRequest with correo and contrasena
  - Returns SesionResponse with session details

### GrupoFamiliar (Group Management)
- **Create Group**: `POST /api/grupos`
  - Header: X-Usuario-Id (jefe/admin ID)
  - Creates group with unique access code
  - Only JefeDeHogar can create groups

- **Join Group**: `POST /api/grupos/ingresar`
  - Header: X-Usuario-Id
  - Body: IngresarGrupoRequest with codigoAcceso
  - Allows MiembroHogar to join a group

- **List Groups**: `GET /api/grupos`
  - Returns all family groups

### Tarea (Task Management)
- **HU-11**: Create Task → `POST /api/tareas`
  - Header: X-Usuario-Id (jefe/admin)
  - Creates task with titulo, descripcion, fechaLimite, grupoId
  - Initial state: PENDIENTE

- **HU-12**: List Tasks → `GET /api/tareas` (implied by controller structure)
  - Lists tasks by group

- **HU-13**: Modify Task → `PUT /api/tareas/{tareaId}`
  - Header: X-Usuario-Id
  - Modifies task state, assignment, or details
  - Returns ModificarTareaResponse

- **View Task Details**: `GET /api/tareas/{tareaId}/detalles`
  - Returns task with all DetalleTarea entries

### DetalleTarea (Task Details)
- Create task detail/history entry
- Tracks task updates with descripcion, observacion, and fecha_actualizacion

## Important Implementation Notes

### Entity Validation
- All entities perform validation in constructors and methods
- Custom exceptions thrown in `entities/exception/` for domain-specific errors
- GlobalExceptionHandler catches and maps exceptions to appropriate HTTP status codes

### User Types and Roles
- **JefeDeHogar (Admin)**: Can create groups, create tasks, modify tasks, assign members
  - Estado: SIN_GRUPO or ACTIVO
  - Method: `crearGrupo(GrupoFamiliar)` - creates a group and sets estado to ACTIVO

- **MiembroHogar (User)**: Can join groups, view tasks, provide task updates
  - Estado: SIN_GRUPO or ACTIVO
  - Method: `asignarGrupo(GrupoFamiliar)` - joins a group; throws exception if already in group

### Request Headers
Controllers expect `X-Usuario-Id` header for authenticated operations. This header contains the Long ID of the authenticated user and is used to:
- Verify permissions (admin vs regular member)
- Track task assignments
- Link entities to the current user

### DTOs and Mapping
- MapStruct is configured (v1.5.5.Final) for automatic entity-to-DTO mapping
- Presenters (GrupoFamiliarPresenter, TareaPresenter, UsuarioPresenter) manually map entities to response DTOs
- DTOs use getter/setter pattern (not Lombok)

### Exception Handling
Domain-specific exceptions are defined in `entities/exception/`:
- CorreoInvalidoException
- ContrasenaInvalidaException
- CredencialesInvalidasException
- GrupoFamiliarNoEncontradoException
- MiembroYaEnGrupoException
- NombreTareaDuplicadoException
- TareaInvalidaException
- And others...

GlobalExceptionHandler in `adapters/exception/` catches these and returns ErrorResponse with appropriate HTTP status codes.

### Bean Configuration
All use cases are wired as Spring Beans in `infrastructure/config/BeanConfig.java`. Each use case bean accepts its required output ports (repositories) as constructor parameters. This enables:
- Dependency injection and loose coupling
- Easy testing by mocking output ports
- Clear visibility of use case dependencies

## Configuration

### application.properties
- `spring.datasource.url`: PostgreSQL connection (uses render.com in production)
- `spring.jpa.hibernate.ddl-auto=update`: Auto-update schema on startup
- `spring.jpa.show-sql=true`: Log SQL statements (useful for debugging)
- `springdoc.swagger-ui.path=/swagger-ui.html`: Swagger UI endpoint

### Database Setup
Before first run:
1. Ensure PostgreSQL is running and accessible
2. Update database credentials in `application.properties`
3. Run the application - Hibernate will create/update schema based on entities and `create_postgres_schema.sql`
4. Or manually execute `create_postgres_schema.sql` for initial setup with sample data

### CORS Configuration
CorsConfig allows cross-origin requests (check `infrastructure/config/CorsConfig.java` for allowed origins)

## Testing

- **Test Framework**: JUnit 5 (spring-boot-starter-test)
- **Current Test Coverage**: Minimal (only contextLoads test in SistemaApplicationTests)
- **Test Location**: `src/test/java/`
- **Recommended Test Structure**: Mirror the architecture layers with tests for:
  - Interactors (use case logic)
  - Adapters (controller endpoints, repository queries)
  - Entities (domain validations)

## Common Development Tasks

### Adding a New Use Case
1. Create the use case interface in `usecases/{feature}/`
2. Create a Command class in same directory
3. Create Interactor implementation in `usecases/{feature}/impl/`
4. Add output port methods to `ports/outs/` if needed
5. Implement output port in `adapters/persistence/`
6. Create JPA Repository in `infrastructure/repositories/`
7. Wire bean in `infrastructure/config/BeanConfig.java`
8. Create controller endpoint in `adapters/controllers/`
9. Create request/response DTOs in `adapters/dto/`
10. Create presenter method in `adapters/presenters/`

### Adding a New Entity
1. Define entity class in `entities/` with JPA annotations
2. Create JPA Repository in `infrastructure/repositories/`
3. Add output port methods if needed
4. Create DTOs and presenter mappings
5. Update schema in `create_postgres_schema.sql`

### Debugging Database Issues
- Check `application.properties` for correct database URL and credentials
- Enable SQL logging: `spring.jpa.show-sql=true` (already enabled)
- Check Hibernate dialect: `spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect`
- Review entity relationships for cascade and fetch strategies

## Development Dependencies

- **Lombok** (v1.18.30): Annotation processor for boilerplate code reduction
- **MapStruct** (v1.5.5.Final): Code generation for DTO mappings
- **springdoc-openapi** (v2.8.6): Swagger/OpenAPI documentation
- **spring-dotenv** (v4.0.0): Environment variable management from .env files
- **PostgreSQL Driver**: Runtime dependency for PostgreSQL
- **H2 Database**: Test/fallback in-memory database

## Integration Notes

The application exposes a RESTful API with OpenAPI documentation. All endpoints require proper headers (especially `X-Usuario-Id` for authenticated operations) and return structured responses with error handling.
