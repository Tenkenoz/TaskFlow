# TaskFlow

Gestor de tareas full-stack con autenticación **JWT**. Cada usuario ve y administra únicamente sus propias tareas.

Backend **Spring Boot 3 + JPA + Spring Security**, frontend **React + TypeScript**, base de datos **PostgreSQL** y orquestación con **Docker Compose**. Todo se levanta con un solo comando.

```bash
docker compose up --build
```

| Servicio | URL |
| --- | --- |
| Frontend | http://localhost:3000 |
| Backend | http://localhost:8080 |
| Health check | http://localhost:8080/actuator/health |
| PostgreSQL | `localhost:5432` · db/user/pass: `taskflow` |

> La primera vez tarda unos minutos mientras se descargan imágenes y dependencias. Las siguientes ejecuciones usan la caché de Docker.

---

## Funcionalidades

- Registro y login con **JWT** (contraseñas hasheadas con BCrypt).
- CRUD de tareas: crear, listar, ver, editar, eliminar y completar/reabrir.
- Filtros por **estado** (`TODO`, `IN_PROGRESS`, `DONE`) y **prioridad** (`LOW`, `MEDIUM`, `HIGH`).
- Cada usuario solo accede a sus propias tareas (relación `@OneToMany` User → Tasks).
- DTOs de entrada y salida: las entidades JPA nunca se exponen.
- Frontend React que consume la API con **axios**, guarda el token y maneja expiración/401.
- UI responsive con estados de carga, vacío y error.

## Stack

| Capa | Tecnología |
| --- | --- |
| Backend | Java 21 · Spring Boot 3.5 · Spring Web · Spring Data JPA · Spring Security · Bean Validation · Actuator |
| Auth | JWT (jjwt 0.12) · BCrypt |
| Tests | JUnit 5 · Mockito · AssertJ |
| Frontend | React 19 · TypeScript · Vite · React Router · axios |
| Base de datos | PostgreSQL 16 |
| Infra | Docker multi-stage · Docker Compose · Nginx · GitHub Actions |

---

## Arquitectura backend (SOLID)

```text
HTTP → Controller → Service → Repository → Entity
                        │
                        ├── DTOs (request/response)
                        ├── JwtService / JwtAuthenticationFilter
                        └── Excepciones de dominio → GlobalExceptionHandler
```

- **Controller**: solo traduce HTTP (validación, status codes) y delega. No contiene lógica de negocio.
- **Service**: reglas de negocio y límites transaccionales. Es la capa cubierta por los tests unitarios.
- **Repository**: interfaces de Spring Data JPA; no hay SQL de negocio en los servicios.
- **Entity**: modelo JPA (`User`, `Task`).
- **DTO**: `TaskRequest`, `TaskResponse`, `RegisterRequest`, `LoginRequest`, `AuthResponse`, `UserResponse`.
- **Seguridad**: filtro stateless que valida el JWT y construye un `UserPrincipal`; reglas de autorización en `SecurityConfig`.
- **Errores**: `@RestControllerAdvice` con respuestas JSON consistentes (400, 401, 404, 409, 500).

### Modelo de datos

```text
User 1 ────── * Task
  id              id
  name            title
  email (unique)  description
  password        status   (TODO | IN_PROGRESS | DONE)
  createdAt       priority (LOW | MEDIUM | HIGH)
                  dueDate
                  createdAt / updatedAt
                  user_id (FK, NOT NULL)
```

El repositorio expone consultas siempre acotadas por `userId` (`findByIdAndUserId`, `findByUserIdAndStatus...`), de modo que el aislamiento entre usuarios se aplica también a nivel de consulta.

---

## API

Todas las rutas de tareas requieren `Authorization: Bearer <token>`.

| Método | Ruta | Descripción |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Crea la cuenta y devuelve `{ token, tokenType, user }` |
| `POST` | `/api/auth/login` | Login y devolución del token |
| `GET` | `/api/tasks?status=&priority=` | Lista las tareas del usuario (filtros opcionales) |
| `POST` | `/api/tasks` | Crea una tarea |
| `GET` | `/api/tasks/{id}` | Detalle de una tarea propia |
| `PUT` | `/api/tasks/{id}` | Actualiza una tarea propia |
| `PATCH` | `/api/tasks/{id}/toggle` | Alterna `DONE` ↔ `TODO` |
| `DELETE` | `/api/tasks/{id}` | Elimina una tarea propia |
| `GET` | `/actuator/health` | Health check (público, usado por Docker y CI) |

### Ejemplo

```bash
# Registro
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"name":"Erick","email":"erick@example.com","password":"secret123"}' | jq -r .token)

# Crear tarea
curl -s -X POST http://localhost:8080/api/tasks \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Preparar entrevista","description":"Repasar SOLID","priority":"HIGH","dueDate":"2026-11-15"}'

# Filtrar
curl -s "http://localhost:8080/api/tasks?status=TODO&priority=HIGH" \
  -H "Authorization: Bearer $TOKEN"
```

Errores de validación:

```json
{
  "status": 400,
  "message": "La petición contiene datos inválidos",
  "timestamp": "2026-10-29T04:00:00Z",
  "errors": { "email": "El email no es válido" }
}
```

---

## Tests y TDD

Los tests del `TaskService` y `AuthService` están escritos con JUnit 5 + Mockito, simulando los repositorios. No necesitan base de datos ni contexto de Spring, por lo que son rápidos y deterministas.

```bash
cd backend
./mvnw test
```

Cobertura de casos:

- Creación con valores por defecto y con valores explícitos, asignación de dueño.
- Usuario inexistente al crear.
- Filtros por estado, prioridad y combinación de ambos.
- Aislamiento: una tarea de otro usuario se trata como `404`.
- Actualización, completado/reapertura y eliminación.
- Registro: normalización de email, hash de contraseña y conflicto por email duplicado.
- Login: credenciales válidas, contraseña incorrecta y usuario inexistente.
- JWT: token válido, manipulado, expirado y firmado con otra clave.

---

## Docker

`docker-compose.yml` orquesta tres contenedores:

| Servicio | Imagen | Puerto | Rol |
| --- | --- | --- | --- |
| `db` | `postgres:16-alpine` | `5432` | PostgreSQL + volumen `pgdata` |
| `backend` | build `backend/Dockerfile` | `8080` | Spring Boot (JAR) |
| `frontend` | build `frontend/Dockerfile` | `3000→80` | React compilado servido por Nginx |

- **Backend multi-stage**: `maven:3.9-eclipse-temurin-21` compila; `eclipse-temurin:21-jre-alpine` solo lleva el JRE y el JAR. Copia el `pom.xml` antes que el código y ejecuta `dependency:go-offline` para cachear dependencias. Corre como usuario no root e incluye `HEALTHCHECK` contra `/actuator/health`.
- **Frontend multi-stage**: `node:22-alpine` ejecuta `npm ci` + `npm run build`; `nginx:alpine` sirve los estáticos y hace proxy de `/api/` hacia `backend:8080`.
- **Arranque ordenado**: `db` tiene healthcheck con `pg_isready`; `backend` espera `service_healthy`; `frontend` espera a que el backend esté healthy.
- **Configuración por entorno**: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET`.

Comandos útiles:

```bash
docker compose up --build -d     # levantar en segundo plano
docker compose ps                # estado y healthchecks
docker compose logs -f backend   # logs del backend
docker compose down              # parar (conserva datos)
docker compose down -v           # parar y borrar la base de datos
```

### Desarrollo local (sin Docker completo)

```bash
# Base de datos
docker compose up -d db

# Backend
cd backend && ./mvnw spring-boot:run

# Frontend (Vite hace proxy de /api a localhost:8080)
cd frontend && npm install && npm run dev
```

---

## CI (GitHub Actions)

`.github/workflows/ci.yml` se ejecuta en cada push y pull request a `main`/`master`:

1. **backend-tests**: JDK 21 + caché Maven → `./mvnw -B test` y publica los reportes Surefire.
2. **frontend-build**: Node 22 + `npm ci` → `tsc --noEmit && vite build`.
3. **docker-smoke**: construye las imágenes, levanta el stack completo y valida de punta a punta:
   - registro y login,
   - creación, listado y completado de una tarea,
   - `401` sin token,
   - frontend servido por Nginx y proxy `/api`,
   - logs y `docker compose down -v` al terminar.

Así el pipeline no solo testea: verifica que la aplicación realmente arranca y funciona containerizada.

Para publicarlo:

```bash
git init
git add .
git commit -m "feat: TaskFlow full-stack con JWT, Docker y CI"
git branch -M main
git remote add origin git@github.com:<tu-usuario>/taskflow.git
git push -u origin main
```

El workflow se activa automáticamente en el primer push.

---

## Estructura

```text
.
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/taskflow/
│       │   ├── config/        # SecurityConfig (stateless, CORS, BCrypt)
│       │   ├── controller/    # AuthController, TaskController
│       │   ├── domain/        # User, Task, enums
│       │   ├── dto/           # requests/responses (records)
│       │   ├── exception/     # excepciones + GlobalExceptionHandler
│       │   ├── repository/    # Spring Data JPA
│       │   ├── security/      # JwtService, filtro, UserPrincipal
│       │   └── service/       # AuthService, TaskService
│       └── test/java/com/taskflow/
│           ├── service/       # TaskServiceTest (TDD), AuthServiceTest
│           └── security/      # JwtServiceTest
├── frontend/
│   ├── Dockerfile
│   ├── nginx.conf
│   └── src/
│       ├── api/               # cliente axios + manejo de errores
│       ├── components/        # AuthLayout, Header, TaskForm, TaskCard, FilterBar
│       ├── context/           # AuthContext (token + usuario)
│       ├── lib/               # etiquetas y formato de fechas
│       ├── pages/             # Login, Register, Dashboard
│       └── styles.css
├── .github/workflows/ci.yml
└── docker-compose.yml
```

---

## Variables de entorno

| Variable | Default | Descripción |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/taskflow` | URL JDBC |
| `SPRING_DATASOURCE_USERNAME` | `taskflow` | Usuario de PostgreSQL |
| `SPRING_DATASOURCE_PASSWORD` | `taskflow` | Contraseña de PostgreSQL |
| `JWT_SECRET` | secreto de desarrollo | Clave HS256 (mínimo 32 bytes). **Cámbiala en producción** |
| `JWT_EXPIRATION_MS` | `86400000` (24 h) | Vida del token |
| `SERVER_PORT` | `8080` | Puerto del backend |

## Notas de diseño

- El token se guarda en `localStorage`; un interceptor de axios lo añade a cada petición y, ante un `401`, limpia la sesión y redirige al login.
- `PATCH /api/tasks/{id}/toggle` evita enviar toda la tarea solo para cambiar el estado.
- `open-in-view` está desactivado y las respuestas usan DTOs para no serializar entidades ni relaciones lazy.
- `ddl-auto: update` mantiene el proyecto simple; para producción se recomendaría Flyway/Liquibase con migraciones versionadas.

## Troubleshooting

- **`release version 21 not supported` al correr `./mvnw test`**: tienes un JRE pero no un JDK. Instala `openjdk-21-jdk` o ejecuta los tests con Docker:
  ```bash
  docker run --rm -v "$PWD/backend":/app -v "$HOME/.m2":/root/.m2 \
    -w /app maven:3.9-eclipse-temurin-21 mvn -B test
  ```
- **Puertos ocupados** (`3000`, `8080` o `5432`): cambia el mapeo en `docker-compose.yml` o detén el proceso que los usa.
- **Quiero reiniciar la base de datos**: `docker compose down -v` (borra el volumen `pgdata`) y vuelve a levantar.
- **El frontend carga pero la API falla**: revisa `docker compose ps` y `docker compose logs backend`; Nginx necesita que el backend esté healthy.
