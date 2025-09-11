# Temporary Note

> There are extension updates (Account and Leaderboard) in the codebase for which the README has yet to be updated. Please refer to the code for the latest changes.
I will clean up the README once the code is finalized and fully tested, and once I have updated all files with javadoc comments.
I will also create a new sequence diagram with plantUML, separate from existing ones, to illustrate the new features.

> Note that the fronted has not been updated yet, however, the extension features do not affect its previous functionality.
I will update the frontend once I have fully cleaned up the backend codebase.

The changes include the following files:
```bash
new file:   src/main/java/com/nikoladesnica/mastermind/api/AccountController.java
modified:   src/main/java/com/nikoladesnica/mastermind/api/GameController.java
new file:   src/main/java/com/nikoladesnica/mastermind/api/LeaderboardController.java
modified:   src/main/java/com/nikoladesnica/mastermind/api/RoomController.java
new file:   src/main/java/com/nikoladesnica/mastermind/api/dto/CreateAccountRequest.java
new file:   src/main/java/com/nikoladesnica/mastermind/api/dto/CreateAccountResponse.java
new file:   src/main/java/com/nikoladesnica/mastermind/api/dto/LeaderboardEntry.java
new file:   src/main/java/com/nikoladesnica/mastermind/api/dto/LoginRequest.java
new file:   src/main/java/com/nikoladesnica/mastermind/api/dto/LoginResponse.java
new file:   src/main/java/com/nikoladesnica/mastermind/domain/model/Account.java
new file:   src/main/java/com/nikoladesnica/mastermind/domain/model/Score.java
new file:   src/main/java/com/nikoladesnica/mastermind/domain/model/Session.java
new file:   src/main/java/com/nikoladesnica/mastermind/domain/ports/AccountRepository.java
new file:   src/main/java/com/nikoladesnica/mastermind/domain/ports/LeaderboardRepository.java
new file:   src/main/java/com/nikoladesnica/mastermind/domain/ports/SessionRepository.java
new file:   src/main/java/com/nikoladesnica/mastermind/domain/service/AccountService.java
modified:   src/main/java/com/nikoladesnica/mastermind/infra/config/BeanConfig.java
new file:   src/main/java/com/nikoladesnica/mastermind/infra/config/LeaderboardProperties.java
new file:   src/main/java/com/nikoladesnica/mastermind/infra/repo/InMemoryAccountRepository.java
new file:   src/main/java/com/nikoladesnica/mastermind/infra/repo/InMemoryLeaderboard.java
new file:   src/main/java/com/nikoladesnica/mastermind/infra/repo/InMemorySessionRepository.java
modified:   src/main/resources/application.yml
```
# Mastermind — Backend (Spring Boot + Hexagonal)

> A well-tested engine for the Mastermind game. This repo is **fully backend**, focusing on clarity, testability, and extensibility. The UI is implemented separately and hosted at **[https://mastermind.nikoladesnica.com](https://mastermind.nikoladesnica.com)**, where you can hook up a custom API base (if you choose to run this project locally). If so, use **ngrok** to expose your backend. By default, the frontend points to my hosted backend at **[https://mastermind-backend.nikoladesnica.com](https://mastermind-backend.nikoladesnica.com)**, which redirects to my DigitalOcean server (running Docker). To verify your own local instance, update the frontend API base as described in the fronted web app where you can verify that requests go to your ngrok URL instead of my hosted backend (to test my submission).

---

## 0) TL;DR — Run & Test

**Prereqs**

* Java 17+ (`java -version`)
* Maven 3.9+ (`mvn -version`)

**Run**

```bash
mvn -q spring-boot:run
# or
mvn -q clean package && java -jar target/mastermind-0.0.1-SNAPSHOT.jar
```

**Test**

```bash
mvn -q test
```

**Base URL:** `http://localhost:8080`

> **Why port 8080?** Spring Boot starts an embedded web server (Tomcat) that **binds** to a TCP port to receive HTTP requests. 8080 is a common default. You can change it in `src/main/resources/application.yml` via `server.port: 8080`, or at runtime with `-Dserver.port=9090`.

---

## 1) What This Is (and Why)

**Goal**

* Let a player start a Mastermind game and submit guesses until they **WON** or **LOST**.
* Showcase backend potential for my **LinkedIn REACH interview**: clean design, input validation, error handling, and tests.
* **Extension:** adds a lightweight **multiplayer room** mode where a host creates a room, players join, and everyone races on the same secret.

**Emphasis**

* **Server-side logic** over UI
* **Testability** and **extensibility** (easy to add features quickly)
* Clean separation of **domain** and **adapters**

---

## 2) Architecture (Hexagonal / Ports & Adapters)

* Put **business rules** (the “domain”) in the center.
* The domain depends only on **interfaces** (ports), not on frameworks.
* Frameworks and I/O (HTTP, DB, random.org) live at the **edges** as **adapters** that implement those ports.
* **Benefit:** swap infrastructure (e.g., in-memory → Postgres; local RNG → random.org) **without touching core rules**.

**Layout**

```
com.nikoladesnica.mastermind
├─ api/               # REST controllers + DTOs (HTTP adapter)
│  ├─ GameController.java
│  └─ RoomController.java          # (extension)
│
├─ domain/            # Pure domain: model, ports, services (no Spring)
│  ├─ model/          # Code, Guess, Feedback, Game, GameStatus, + Player/Room (extension)
│  ├─ ports/          # GameRepository, SecretCodeGenerator, + RoomRepository (extension)
│  ├─ service/        # GuessEvaluator, GameService, + RoomService (extension)
│  └─ errors/         # BadRequestException, NotFoundException, + ForbiddenException (extension)
│
├─ infra/             # Adapters + config (in-memory repos, random.org or local RNG)
│  ├─ config/         # GameProperties, RandomOrg config
│  └─ repo/           # InMemoryGameRepository, + InMemoryRoomRepository (extension)
│
└─ MastermindApplication.java
```

---

## 3) Build & Run (Step-by-Step)

### 3.1 Prerequisites

* **Java** 17+ installed and on `PATH`
* **Maven** 3.9+
* TCP **port 8080** available (or change it)

### 3.2 Commands

```bash
# Build
mvn -q clean package

# Run (dev)
mvn -q spring-boot:run

# Run (jar)
java -jar target/mastermind-0.0.1-SNAPSHOT.jar
```

### 3.3 Configuration (`src/main/resources/application.yml`)

```yaml
server:
  port: 8080

mastermind:
  code-length: 4
  min-digit: 0
  max-digit: 7
  attempts: 10
  allow-duplicates: true
  use-random-org: true
  random-org:
    timeout-ms: 1200
    base-url: https://www.random.org/integers/
```

* `use-random-org: true` = true randomness; transparent fallback to local RNG on errors/timeouts.
* Force local RNG with `use-random-org: false`.

---

## 4) How to Play — **Single-player API**

> All examples use `curl`. If you have `jq`, pretty-print with `| jq`.

### 4.1 Start a game

```bash
curl -s -X POST http://localhost:8080/api/games \
  -H 'Content-Type: application/json' \
  -d '{}'
```

**Response**

```json
{ "gameId":"<uuid>", "attemptsLeft":10, "status":"IN_PROGRESS" }
```

### 4.2 Submit a guess

```bash
curl -s -X POST http://localhost:8080/api/games/<gameId>/guesses \
  -H 'Content-Type: application/json' \
  -d '{"digits":[0,1,2,3]}'
```

**Snapshot (********`GameView`****\*\*\*\*\*\*\*\*)**

```json
{
  "gameId":"<uuid>",
  "status":"IN_PROGRESS|WON|LOST",
  "attemptsLeft":9,
  "canGuess": true,
  "message": null,
  "historyCount": 1,
  "history":[
    {"guess":[0,1,2,3], "correctPositions":2, "correctNumbers":4, "at":"2025-...Z"}
  ]
}
```

> `correctNumbers` counts **all** digit matches regardless of position (includes the exact matches), while `correctPositions` counts index-exact matches. Win = `correctPositions == code-length`.

### 4.3 Get game state

```bash
curl -s http://localhost:8080/api/games/<gameId>
```

---

## 5) **Extension:** Multiplayer Rooms (API)

A host creates a room, players join (each gets a token), the host starts, and everyone plays against the **same secret**. The server tracks per-player progress and produces a **leaderboard**.

**Headers & tokens**

* Host must pass: `X-Host-Token: <hostToken>`
* Players must pass: `X-Player-Id: <playerId>` and `X-Player-Token: <playerToken>`
* Treat tokens like secrets; don’t log them.

### 5.1 Create a room (host)

```bash
curl -s -X POST http://localhost:8080/api/rooms | jq
```

**Response (********`CreateRoomResponse`****\*\*\*\*\*\*\*\*)**

```json
{ "roomId":"<uuid>", "hostToken":"<uuid>" }
```

### 5.2 Join the room (player)

```bash
curl -s -X POST http://localhost:8080/api/rooms/<roomId>/join \
  -H 'Content-Type: application/json' \
  -d '{"name":"Alice"}' | jq
```

**Response (********`JoinRoomResponse`****\*\*\*\*\*\*\*\*)**

```json
{ "roomId":"<roomId>", "playerId":"<uuid>", "playerToken":"<uuid>" }
```

> Join is allowed only while the room is in `WAITING` state.

### 5.3 Start the room (host)

```bash
curl -s -X POST http://localhost:8080/api/rooms/<roomId>/start \
  -H "X-Host-Token: <hostToken>" | jq
```

Returns a `RoomView` snapshot with `state:"RUNNING"` and `players`.

### 5.4 Submit a guess (player)

```bash
curl -s -X POST http://localhost:8080/api/rooms/<roomId>/guesses \
  -H 'Content-Type: application/json' \
  -H "X-Player-Id: <playerId>" \
  -H "X-Player-Token: <playerToken>" \
  -d '{"digits":[0,1,2,3]}' | jq
```

**Behavior**

* If a player **wins**, they get `status:"WON"`, and the room moves to `FINISHED`.
* If a player runs out of attempts: `status:"LOST"`. If **all** players finish, the room moves to `FINISHED`.
* After `FINISHED`, further guesses return a **frozen** snapshot; no new history is appended.

### 5.5 Get room state

```bash
curl -s http://localhost:8080/api/rooms/<roomId> | jq
```

**`RoomView`** summary

```json
{
  "roomId":"<uuid>",
  "state":"WAITING|RUNNING|FINISHED",
  "createdAt":"2025-...Z",
  "startedAt":"2025-...Z",
  "finishedAt":"2025-...Z",
  "players":[
    {
      "playerId":"<uuid>",
      "name":"Alice",
      "status":"IN_PROGRESS|WON|LOST",
      "attemptsLeft":9,
      "history":[
        {"guess":[0,0,0,0], "correctPositions":0, "correctNumbers":1, "at":"2025-...Z"}
      ]
    }
  ],
  "leaderboard":[
    {
      "playerId":"<uuid>",
      "name":"Alice",
      "status":"WON|IN_PROGRESS|LOST",
      "attemptsUsed":1,
      "elapsedSeconds":12
    }
  ]
}
```

**Leaderboard ordering**

1. `WON` first, then `IN_PROGRESS`, then `LOST`
2. Fewer `attemptsUsed` is better
3. Lower `elapsedSeconds` is better (nulls sort last)

---

## 6) New Multiplayer Admin Flows (Lobby-only)

All of the following are valid only while `state=WAITING`.

### 6.1 Leave room (player)

```bash
POST /api/rooms/{roomId}/leave
Headers:
  X-Player-Id: <playerId>
  X-Player-Token: <playerToken>
Body: {}
→ RoomView
```

* **WAITING:** player is removed from the lobby.
* **RUNNING:** player is marked **LOST** (if still `IN_PROGRESS`); if everyone is finished, room becomes `FINISHED`.
* **FINISHED:** no-op.

### 6.2 Claim host (race-safe)

```bash
POST /api/rooms/{roomId}/promote-host
Headers:
  X-Player-Id: <playerId>
  X-Player-Token: <playerToken>
Body: {}
→ { roomId, hostToken }
```

* First caller **wins**; others receive **403**.
* Frontend should only surface this option when “host seems gone”.

### 6.3 Assign host (current host picks the new host)

```bash
POST /api/rooms/{roomId}/assign-host/{targetPlayerId}
Headers:
  X-Host-Token: <currentHostToken>
Body: {}
→ { roomId, hostToken }  # the new host’s token
```

* After this, the **old host token is invalid**.

### 6.4 Kick a player (host-only)

```bash
POST /api/rooms/{roomId}/kick/{targetPlayerId}
Headers:
  X-Host-Token: <currentHostToken>
Body: {}
→ RoomView  # target is removed from players
```

* Only in `WAITING`.

> **Note on host leaving:** If the host leaves while `WAITING` and at least one player remains, the backend **auto-rotates** host to a random player and issues a fresh `hostToken` (returned in the next room fetch). The manual `promote-host`/`assign-host` flows remain available for explicit control.

---

## 7) Error Contract (HTTP codes & why)

**Payload shape**

```json
{ "timestamp":"2025-...Z", "status":400, "error":"Bad Request", "message":"...", "path":"/api/..." }
```

**Codes used**

* **400 Bad Request** – semantic validation errors (digits out of range, wrong guess length, room not joinable, etc.)
* **403 Forbidden** – bad/invalid **token** (host or player) or “first-claim lost” on `promote-host`
* **404 Not Found** – `gameId`/`roomId`/`playerId` doesn’t exist
* **200 OK** – after finish, guesses return frozen state (idempotent, simpler clients)

---

## 8) Domain Rules

**Secret code**

* Length = `code-length` (default 4)
* Each digit ∈ \[`min-digit`..`max-digit`] (default 0..7)
* Duplicates allowed if `allow-duplicates: true`

**Feedback semantics**

* `correctPositions`: digits that match in value **and** index
* `correctNumbers`: **total** digits that appear in the secret **regardless of position** (includes the exact matches)
* Win: `correctPositions == code-length`

**Single-player lifecycle**

* Attempts decrement per guess; final status: `WON`/`LOST`
* After finish, POST returns frozen snapshot

**Multiplayer lifecycle**

* Room states: `WAITING` → (`start`) → `RUNNING` → (`winner or all finished`) → `FINISHED`
* Each player tracks their own attempts and history
* Room finishes immediately on first `WON` or when **all** players are done
* `leave` in `RUNNING` marks player **LOST** and may finish the room

---

## 9) Frontend & Connectivity (ngrok-friendly)

**How I tested it**

* I am hosting the frontend at [https://mastermind.nikoladesnica.com](https://mastermind.nikoladesnica.com).
* For the backend running on my machine (`localhost:8080`), I exposed it using **ngrok** and pasted the ngrok HTTPS URL into the UI’s **API Base URL** configuration.
* The UI stores that value in `localStorage` (key: `mm.apiBase`). You can change it anytime from the UI.

**Your setup (simple path)**

1. Run the backend locally (or on your server).

  * Default port: **8080** (change via `server.port` in `application.yml` or `-Dserver.port=...`).
2. Expose the backend with **ngrok**:

   ```bash
   ngrok http http://localhost:8080
   # copy the https URL, e.g., https://abc123.ngrok-free.app
   ```
3. Open the frontend and set **API Base URL** to that ngrok URL (e.g., `https://abc123.ngrok-free.app`).
4. If you ever see an ngrok interstitial page (HTML warning), make sure your frontend requests include the header:

   `ngrok-skip-browser-warning: true`

**Hosted backend (optional)**

* If you deploy the backend to a server (e.g., DigitalOcean droplet), point a subdomain (e.g., `mastermind-backend.yourdomain.com`) to it, and paste that URL into the UI’s **API Base URL**.
* You can still keep ngrok for local dev.

---

## 10) Tests (What’s covered)

```bash
mvn -q test
```

**Unit (domain)**

* `GuessEvaluatorTest`: feedback correctness (incl. duplicates)
* `GameServiceTest`: attempts, win/freeze, range validation
* **(extension)** `RoomServiceTest`: join/start/guess flow, token checks, input validation, finish rules, leave

**Integration (API)**

* `GameControllerTest` (MockMvc): start/guess/get, invalid payloads (400), not found (404)
* **(extension)** `RoomControllerTest` (MockMvc): create/join/start/guess/freeze, invalid digits (400), wrong host token (403), room not found (404), leave, **promote-host**, **assign-host**, **kick**

> Tests use a fixed secret generator (`[0,1,3,2]`) for determinism and assert the “frozen after finish” behavior by comparing history lengths.

---

## 11) Design Choices & Rationale

* **Hexagonal**: logic isolated from frameworks; tests are fast and focused.
* **In-memory repos**: simple demo; trivially replaceable with a DB adapter.
* **Random.org + fallback**: availability-first design.
* **Uniform snapshots** from POST/GET endpoints simplify clients.
* **Explicit error mapping** via `ApiErrorHandler`: 400 (validation), 403 (token / claim failure), 404 (missing resource).
* **Rooms**: minimal surface area, token-based auth per role; concurrency handled by synchronizing on `Room` during mutations.
* **Race-safe host claim**: `promote-host` is first-come-first-serve; the UI hides it unless the host appears gone to avoid chaos.

---

## 12) Personal Notes & Lessons (from development)

* I already knew **Maven**, but I didn’t know **Spring Boot** at all. I taught myself Spring Boot and many other parts of the ecosystem quickly for this project to demonstrate fast learning.
* I used **ngrok** heavily to make my local backend reachable from the hosted frontend; the UI lets you paste the API base URL and stores it in local storage.
* I hosted the frontend on a subdomain and kept the backend local at times — the **API Configuration** control made that painless.
* I hit the ngrok warning page once; adding the header `ngrok-skip-browser-warning: true` fixed the “HTML instead of JSON” issue.
* I added a **leave** endpoint to keep the lobby and race state consistent (players should stop blocking others when they go).
* I implemented **resume game** in the frontend for single-player (stored `gameId` in localStorage). For multiplayer, I chose not to “resume as a player” after leaving because it would fight the finish rules; instead, you can still **view** the room state.
* Potential ops workaround while using in-memory repos: occasionally restart the process to clear old rooms if you’re running a long-lived demo.