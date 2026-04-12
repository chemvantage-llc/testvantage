# TestVantage Rebuild — Implementation Plan

## Summary

Rebuild TestVantage from a manual-click LMS simulator into an **automated regression tester** that acts as a certified LMS platform, sending LTI 1.3 messages to ChemVantage endpoints and verifying responses programmatically. The current code conflates LMS simulation with test triggering in a flat package with no test orchestration. This plan separates the codebase into three clear layers: **platform simulation** (mock LMS endpoints that ChemVantage calls back to), **test runner engine** (orchestrates multi-step scenario execution), and **test servlets** (one per ChemVantage major endpoint, each containing multiple scenario definitions).

**Mode:** Rewrite (user confirms repo is headed wrong direction; most code deleted or rewritten)

**Targets under test:**
- `https://www.chemvantage.org` (production)
- `https://dev-vantage-hrd.appspot.com` (dev)

---

## 1. Proposed Target Architecture

### Package Structure

```
src/main/java/org/testvantage/
├── platform/                    # Layer 1: LMS Platform Simulation
│   ├── JwksServlet.java         #   KEEP+REWRITE — serves platform public keys
│   ├── OAuth2TokenServlet.java  #   KEEP+REWRITE — issues access tokens to ChemVantage
│   ├── NrpsMockServlet.java     #   KEEP+REWRITE — mock membership/roster responses
│   ├── AgsMockServlet.java      #   KEEP+REWRITE — mock line items + score sink
│   ├── OidcConfigServlet.java   #   KEEP+REWRITE — .well-known/openid-configuration
│   ├── OidcAuthServlet.java     #   KEEP+REWRITE — generates id_token, POSTs to ChemVantage
│   ├── OidcLoginServlet.java    #   DELETE — not needed (we initiate, not receive, login)
│   └── KeyManager.java          #   KEEP — RSA key management (minor cleanup only)
│
├── runner/                      # Layer 2: Test Engine
│   ├── TestRunner.java          #   NEW — orchestrates scenario execution
│   ├── TestScenario.java        #   NEW — defines a single test (steps, expected outcomes)
│   ├── TestStep.java            #   NEW — one HTTP request/response in a multi-step flow
│   ├── TestRun.java             #   NEW — execution record (replaces TestResult)
│   ├── StepResult.java          #   NEW — result of one step within a run
│   ├── FlowState.java           #   NEW — carries state between steps (tokens, IDs, etc.)
│   ├── Assertions.java          #   NEW — reusable assertion helpers for LTI responses
│   └── HttpClient.java          #   NEW — wraps HTTP calls to ChemVantage with logging
│
├── tests/                       # Layer 3: Test Servlets (one per CV endpoint)
│   ├── JwksTestServlet.java     #   NEW — tests /jwks endpoint
│   ├── AuthTokenTestServlet.java#   NEW — tests /auth/token endpoint
│   ├── LaunchTestServlet.java   #   NEW — tests /lti/launch (8 types × 2 roles)
│   ├── DeepLinksTestServlet.java#   NEW — tests /lti/deeplinks
│   ├── RegistrationTestServlet.java # NEW — tests /lti/registration
│   └── ScoringTestServlet.java  #   NEW — multi-stage: launch → submit → verify score
│
├── model/                       # Layer 4: Data / Persistence
│   ├── TestRunEntity.java       #   NEW — Objectify entity for test run persistence
│   ├── ScenarioRegistry.java    #   NEW — static registry of all known scenarios
│   └── TargetConfig.java        #   NEW — prod vs dev target URL + credentials
│
├── web/                         # Layer 5: UI / API
│   ├── DashboardServlet.java    #   NEW — replaces HomeServlet; shows run history/status
│   ├── RunApiServlet.java       #   NEW — JSON API to trigger runs & get results
│   └── ReportServlet.java       #   REWRITE from TestReportServlet
│
├── LtiMessageBuilder.java      #   KEEP+REWRITE — parameterized for any assignment type/role
├── OfyService.java              #   KEEP — Objectify wiring
└── ObjectifyBootstrapListener.java # KEEP — entity registration (update entity list)
```

### Responsibilities

| Layer | Responsibility |
|-------|---------------|
| **platform/** | Endpoints that ChemVantage calls back to during LTI flows. These simulate a real LMS. They must be spec-compliant but are not the code being tested. |
| **runner/** | Generic test engine. Knows how to execute a `TestScenario` (a list of `TestStep`s), carry `FlowState` between steps, record `StepResult`s, and persist a `TestRun`. |
| **tests/** | One servlet per ChemVantage endpoint family. Each servlet registers N scenarios and exposes GET (list scenarios) + POST (run one/all). Each scenario is a sequence of steps. |
| **model/** | Objectify entities and configuration POJOs. |
| **web/** | Human-facing dashboard and JSON API for external triggering (CI, cron). |

---

## 2. Keep / Delete / Rewrite Matrix

| Current File | Decision | Rationale |
|---|---|---|
| `HomeServlet.java` | **DELETE** | Replaced by `DashboardServlet`; current UI is static buttons, not a test dashboard |
| `RegistrationServlet.java` | **DELETE** | Current code does registration *of this platform*. New `RegistrationTestServlet` will *test* ChemVantage's registration endpoint |
| `OIDCLoginServlet.java` | **DELETE** | We are the platform (initiator), not the tool (receiver). Not needed |
| `OIDCAuthServlet.java` | **REWRITE → `platform/OidcAuthServlet`** | Core logic needed (generate id_token, form-POST to CV) but must be parameterized for role/assignment type |
| `LaunchServlet.java` | **DELETE** | Manual trigger page. Replaced by `LaunchTestServlet` which drives automated scenarios |
| `DeepLinkingServlet.java` | **DELETE** | Only displays raw JWT. New test servlet will parse, validate, and assert on deep link responses |
| `JWKSServlet.java` | **REWRITE → `platform/JwksServlet`** | Logic is correct; move to `platform/` package, add key rotation support |
| `NRPSServlet.java` | **REWRITE → `platform/NrpsMockServlet`** | Move to `platform/`; make roster configurable per scenario |
| `AGSServlet.java` | **REWRITE → `platform/AgsMockServlet`** | Move to `platform/`; add score capture for verification in scoring tests |
| `OAuth2TokenServlet.java` | **REWRITE → `platform/OAuth2TokenServlet`** | Move to `platform/`; add token validation, scope enforcement |
| `OIDCConfigurationServlet.java` | **REWRITE → `platform/OidcConfigServlet`** | Move to `platform/`; minor |
| `LTIMessageBuilder.java` | **REWRITE** | Must accept assignment_type, role, custom params, resource_link_id as parameters instead of hardcoding |
| `KeyManager.java` | **KEEP** | Works correctly. Move to `platform/` package |
| `TestResult.java` | **DELETE** | Replaced by `TestRunEntity` with richer data model |
| `TestReportServlet.java` | **REWRITE → `web/ReportServlet`** | Must render new data model |
| `OfyService.java` | **KEEP** | Update entity registrations |
| `ObjectifyBootstrapListener.java` | **KEEP** | Update entity registrations |
| `pom.xml` | **KEEP+UPDATE** | Same deps; bump versions, add test deps |
| `web.xml` | **REWRITE** | New servlet mappings for new package layout |
| `app.yaml` / `appengine-web.xml` | **KEEP** | No changes needed |
| `deploy.sh` | **KEEP** | No changes needed |
| `README.md` | **REWRITE** | Update architecture docs |
| `CONFIGURATION.md` | **REWRITE** | Update for new flow |
| `TEST_PLAN.md` | **REWRITE** | See section 8 below |
| `QUICKSTART.md` | **REWRITE** | Update for new UI/API |

**Summary:** 6 files deleted, 10 rewritten, 3 kept as-is, ~15 new files created.

---

## 3. Data Model

### TestScenario (in-memory, registered at startup)

```java
public class TestScenario {
    String id;              // e.g. "launch-homework-student"
    String suiteId;         // e.g. "launch"
    String name;            // e.g. "Launch Homework as Student"
    String description;
    AssignmentType assignmentType;  // HOMEWORK, QUIZ, EXAM, PRACTICE_EXAM,
                                    // VIDEO_QUIZ, POLL, PLACEMENT_EXAM, SMART_TEXT
    Role role;              // STUDENT, INSTRUCTOR
    List<TestStep> steps;   // ordered steps in this scenario
    Map<String, String> tags; // for filtering: {"endpoint": "/lti/launch", "multi-stage": "false"}
}
```

### TestStep (in-memory, part of scenario definition)

```java
public class TestStep {
    int order;
    String name;                // e.g. "Initiate OIDC Login"
    StepType type;              // OIDC_LOGIN, LTI_LAUNCH, DEEP_LINK_REQUEST,
                                // DEEP_LINK_RESPONSE, SUBMIT_ANSWER, FETCH_SCORE,
                                // REGISTRATION_INITIATE, REGISTRATION_COMPLETE,
                                // HTTP_GET, HTTP_POST
    String targetUrlTemplate;   // e.g. "${targetBase}/lti/launch" 
    Map<String, String> params; // step-specific parameters
    List<Assertion> assertions; // what to check in the response
}

public class Assertion {
    AssertionType type;  // STATUS_CODE, HEADER_CONTAINS, BODY_JSON_PATH,
                         // JWT_CLAIM_EQUALS, JWT_SIGNATURE_VALID, REDIRECT_TO
    String path;         // e.g. "$.message_type" or "Content-Type"
    String expected;     // e.g. "LtiDeepLinkingResponse"
}
```

### FlowState (in-memory, per test run)

```java
public class FlowState {
    Map<String, String> variables;  // carried between steps
    // Populated by steps, consumed by later steps. Examples:
    //   "id_token"         → JWT from OIDC auth
    //   "access_token"     → OAuth2 token from /auth/token
    //   "resource_link_id" → from deep linking response
    //   "lineitem_url"     → from AGS response
    //   "assignment_id"    → from launch response
    //   "nonce", "state"   → OIDC flow state
}
```

### TestRun / StepResult (persisted via Objectify)

```java
@Entity
public class TestRunEntity {
    @Id Long id;                       // auto-generated
    @Index String scenarioId;          // "launch-homework-student"
    @Index String suiteId;             // "launch"
    @Index String targetBase;          // "https://www.chemvantage.org"
    @Index Date startTime;
    Date endTime;
    long durationMs;
    @Index RunStatus status;           // RUNNING, PASSED, FAILED, ERROR
    List<StepResultData> stepResults;  // embedded
    String errorMessage;               // top-level failure reason if ERROR
}

public class StepResultData {  // embedded in TestRunEntity, not a separate entity
    int stepOrder;
    String stepName;
    Date timestamp;
    long durationMs;
    StepStatus status;          // PASSED, FAILED, SKIPPED, ERROR
    int httpStatusCode;
    String responseSnippet;     // first 2KB of response for debugging
    String failureReason;       // if FAILED/ERROR
}
```

### TargetConfig

```java
public class TargetConfig {
    String name;        // "production" or "dev"
    String baseUrl;     // "https://www.chemvantage.org" or "https://dev-vantage-hrd.appspot.com"
    String clientId;    // issued during registration
    String deploymentId;
    // Loaded from Datastore or environment variables
}
```

### Assignment Types (enum)

```java
public enum AssignmentType {
    HOMEWORK, QUIZ, EXAM, PRACTICE_EXAM, VIDEO_QUIZ, POLL, PLACEMENT_EXAM, SMART_TEXT
}
```

---

## 4. Endpoint Contracts

### Platform Simulation Endpoints (called BY ChemVantage)

These are unchanged from current design — they simulate a real LMS:

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/.well-known/openid-configuration` | OIDC discovery |
| GET | `/jwks` | Platform public keys |
| POST | `/oauth2/token` | Issue access tokens |
| GET | `/nrps/context/{ctx}/memberships` | Mock roster |
| GET/POST | `/ags/context/{ctx}/lineitems` | Mock gradebook |
| POST | `/ags/context/{ctx}/lineitems/{id}/scores` | Receive scores |

### Test Runner API (new — exposed for dashboard and CI)

#### List all scenarios
```
GET /api/scenarios
Query params: ?suite=launch&assignmentType=HOMEWORK&role=STUDENT

Response 200:
{
  "scenarios": [
    {
      "id": "launch-homework-student",
      "suite": "launch",
      "name": "Launch Homework as Student",
      "assignmentType": "HOMEWORK",
      "role": "STUDENT",
      "stepCount": 3,
      "tags": {"multi-stage": "false"}
    },
    ...
  ]
}
```

#### Run one or more scenarios
```
POST /api/run
Content-Type: application/json

{
  "target": "production",             // or "dev"
  "scenarioIds": ["launch-homework-student"],  // omit to run all
  "suiteFilter": "launch",            // optional: run all in suite
  "tags": {"multi-stage": "true"}     // optional: filter by tags
}

Response 202:
{
  "runBatchId": "batch-1234",
  "scenarioCount": 16,
  "status": "RUNNING"
}
```

#### Get run results
```
GET /api/runs?batchId=batch-1234
GET /api/runs?suite=launch&status=FAILED&limit=50

Response 200:
{
  "runs": [
    {
      "id": 5678,
      "scenarioId": "launch-homework-student",
      "target": "https://www.chemvantage.org",
      "status": "PASSED",
      "durationMs": 1230,
      "startTime": "2026-04-11T10:00:00Z",
      "steps": [
        {"order": 1, "name": "Initiate OIDC Login", "status": "PASSED", "durationMs": 200},
        {"order": 2, "name": "POST id_token to /lti/launch", "status": "PASSED", "durationMs": 800},
        {"order": 3, "name": "Verify response HTML", "status": "PASSED", "durationMs": 230}
      ]
    }
  ]
}
```

#### Get single run detail
```
GET /api/runs/{runId}

Response 200: (same shape as single item above, plus full responseSnippets)
```

### Test Servlet Endpoints (trigger tests from browser)

Each test servlet provides an HTML UI for interactive use:

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/test/jwks` | List JWKS test scenarios, show results |
| POST | `/test/jwks` | Run JWKS test scenarios |
| GET | `/test/auth-token` | List auth/token test scenarios |
| POST | `/test/auth-token` | Run auth/token test scenarios |
| GET | `/test/launch` | List launch scenarios (8 types × 2 roles) |
| POST | `/test/launch` | Run launch scenarios |
| GET | `/test/deeplinks` | List deep linking scenarios |
| POST | `/test/deeplinks` | Run deep linking scenarios |
| GET | `/test/registration` | List registration scenarios |
| POST | `/test/registration` | Run registration scenarios |
| GET | `/test/scoring` | List multi-stage scoring scenarios |
| POST | `/test/scoring` | Run scoring scenarios |

---

## 5. Multi-Stage Flow Examples

### Example: Launch Homework → Submit Answer → Verify Score

```
Scenario: "scoring-homework-student"
Steps:
  1. Initiate OIDC flow → get state, nonce
  2. Generate id_token JWT (role=Learner, assignment_type=Homework, 
     resource_link_id=hw-001, include AGS claim)
  3. POST id_token to ChemVantage /lti/launch
     → Assert 200, parse response for assignment form
     → Extract: assignment_id, form_action URL
     → Store in FlowState: assignment_id, submit_url
  4. POST student answer to submit_url (from FlowState)
     → Assert 200, response indicates submission received
     → Extract: score acknowledgment
  5. Wait 2 seconds (ChemVantage grade processing)
  6. GET score from AGS mock servlet's captured scores
     → Assert: score received for correct user + lineitem
     → Assert: score value within expected range
```

### Example: Deep Linking → Launch Created Resource

```
Scenario: "deeplink-then-launch-quiz"
Steps:
  1. Generate DeepLinkingRequest JWT (role=Instructor)
  2. POST to ChemVantage /lti/deeplinks  
     → Assert: 200, content selection UI returned
     → Extract: form action URL, session cookies
  3. POST content selection (select Quiz type)
     → Assert: redirect to deep_link_return_url
     → Extract: deep linking response JWT
  4. Parse returned content_items
     → Assert: contains LtiResourceLink with custom params
     → Store: resource_link_id, custom.assignment_type
  5. Generate new id_token using extracted resource_link_id
  6. POST to ChemVantage /lti/launch with new token
     → Assert: 200, quiz content displayed
```

---

## 6. Step-by-Step Migration Plan

### Phase 0: Foundation (Week 1)
**Goal:** New package structure compiles and deploys with zero test functionality.

1. Create new package directories: `platform/`, `runner/`, `tests/`, `model/`, `web/`
2. Move `KeyManager.java` → `platform/KeyManager.java`
3. Move `JWKSServlet.java` → `platform/JwksServlet.java` (rename, repackage)
4. Move `OAuth2TokenServlet.java` → `platform/OAuth2TokenServlet.java`
5. Move `OIDCConfigurationServlet.java` → `platform/OidcConfigServlet.java`
6. Move `OIDCAuthServlet.java` → `platform/OidcAuthServlet.java`
7. Create stub `web/DashboardServlet.java` (returns "TestVantage v2 — coming soon")
8. Update `web.xml` with new servlet-class paths
9. Update `OfyService.java` and `ObjectifyBootstrapListener.java` imports
10. Delete: `HomeServlet`, `OIDCLoginServlet`, `LaunchServlet`, `DeepLinkingServlet`
11. Verify: `mvn clean package` succeeds, deploy works, `/jwks` and `/.well-known/openid-configuration` respond

**Acceptance criteria:**
- [ ] App deploys and serves `/jwks`, `/.well-known/openid-configuration`, `/oauth2/token`
- [ ] No compilation errors
- [ ] Old dead servlets removed

---

### Phase 1: Test Engine Core (Week 2)
**Goal:** `TestRunner` can execute a scenario definition and persist results.

1. Create `runner/TestScenario.java`, `runner/TestStep.java`, `runner/FlowState.java`
2. Create `runner/Assertions.java` — status code, JSON path, JWT claim checks
3. Create `runner/HttpClient.java` — wrapper around Google HTTP Client with request/response logging
4. Create `runner/StepResult.java`, `runner/TestRun.java`
5. Create `model/TestRunEntity.java` (Objectify entity replacing `TestResult`)
6. Delete `TestResult.java`
7. Create `runner/TestRunner.java` — accepts a `TestScenario` + `TargetConfig`, executes each step sequentially, carries `FlowState`, returns `TestRun`
8. Create `model/TargetConfig.java` with hardcoded prod/dev URLs
9. Register `TestRunEntity` in `OfyService`

**Acceptance criteria:**
- [ ] `TestRunner` can execute a dummy 2-step scenario against a mock endpoint
- [ ] Results persisted to Datastore
- [ ] `FlowState` correctly passes variables between steps

---

### Phase 2: Parameterized LTI Message Builder (Week 2-3)
**Goal:** `LTIMessageBuilder` generates correct JWTs for any assignment type, role, and launch configuration.

1. Rewrite `LTIMessageBuilder` to accept a parameter object:
   ```java
   public class LaunchParams {
       AssignmentType assignmentType;
       Role role;
       String resourceLinkId;
       String contextId;
       boolean includeNrps;
       boolean includeAgs;
       Map<String, String> customParams;
   }
   ```
2. Create `model/AssignmentType.java` enum (8 types)
3. Create `model/Role.java` enum (STUDENT, INSTRUCTOR)
4. Update `platform/OidcAuthServlet.java` to use new parameterized builder
5. Add support for deep linking request JWTs with configurable `accept_types`

**Acceptance criteria:**
- [ ] Can generate valid JWTs for all 8 assignment types × 2 roles
- [ ] Deep linking request JWTs include correct `deep_linking_settings`
- [ ] All JWTs pass signature verification via `/jwks`

---

### Phase 3: Platform Mock Improvements (Week 3)
**Goal:** Mock endpoints are configurable per-scenario and capture data for assertions.

1. Rewrite `platform/NrpsMockServlet.java` — make roster configurable via `ScenarioContext`
2. Rewrite `platform/AgsMockServlet.java` — capture posted scores into a concurrent map keyed by (scenarioRunId, userId, lineItemId) so scoring tests can verify them
3. Rewrite `platform/OAuth2TokenServlet.java` — validate `client_assertion` JWT, enforce scope
4. Add `platform/ScenarioContext.java` — thread-local or request-attribute based context that tells mock servlets which scenario is running

**Acceptance criteria:**
- [ ] AGS mock captures scores and exposes them to the test runner
- [ ] NRPS mock can return different rosters per scenario
- [ ] OAuth2 token endpoint validates client credentials

---

### Phase 4: First Test Servlet — JWKS Tests (Week 3)
**Goal:** First real test servlet operating end-to-end.

1. Create `tests/JwksTestServlet.java`
2. Define scenarios:
   - Fetch `/jwks` from ChemVantage, verify JSON structure
   - Verify `kid` matches keys used in CV's JWT signatures
   - Verify RSA key can validate a sample JWT from CV
3. Create `model/ScenarioRegistry.java` — startup registration of all scenarios
4. Wire into `web.xml` at `/test/jwks`

**Acceptance criteria:**
- [ ] `GET /test/jwks` shows scenario list
- [ ] `POST /test/jwks` runs all JWKS scenarios against dev target
- [ ] Results visible on page and persisted

---

### Phase 5: Launch Test Servlet (Week 4)
**Goal:** Full launch testing across all assignment types and roles.

1. Create `tests/LaunchTestServlet.java`
2. Define 16 scenarios (8 assignment types × 2 roles):
   - Each scenario: initiate OIDC → generate id_token → POST to `/lti/launch` → assert response
3. Assertions per scenario:
   - HTTP 200
   - Response contains expected assignment content
   - No error messages in response body
4. Wire at `/test/launch`

**Acceptance criteria:**
- [ ] All 16 launch variants execute
- [ ] Each records pass/fail with response snippet
- [ ] Dashboard shows launch suite summary

---

### Phase 6: Deep Linking + Registration Tests (Week 5)
**Goal:** Deep linking and dynamic registration test coverage.

1. Create `tests/DeepLinksTestServlet.java`
   - Scenario per assignment type: send deep linking request → parse content items returned → verify structure
   - Scenario: deep link then launch created resource
2. Create `tests/RegistrationTestServlet.java`
   - Scenario: initiate dynamic registration → verify returned client_id
   - Scenario: re-registration (idempotency check)
3. Wire at `/test/deeplinks` and `/test/registration`

**Acceptance criteria:**
- [ ] Deep linking returns valid content_items for each type
- [ ] Registration flow completes and credentials are usable
- [ ] Multi-step deep-link-then-launch scenario passes

---

### Phase 7: Scoring + Multi-Stage Tests (Week 5-6)
**Goal:** End-to-end scoring: launch → submit answer → verify score passback.

1. Create `tests/ScoringTestServlet.java`
2. Create `tests/AuthTokenTestServlet.java`
   - Scenario: request token with valid client_credentials → verify token structure
   - Scenario: request token with bad credentials → verify error
3. Scoring scenarios (per assignment type where applicable):
   - Launch as student → submit answer → wait → verify AGS score received
4. Implement wait/retry logic in `TestRunner` for async score passback

**Acceptance criteria:**
- [ ] Score passback captured by AGS mock
- [ ] Correct score values for correct/incorrect answers
- [ ] Auth token tests cover happy path + error cases

---

### Phase 8: Dashboard + API + Docs (Week 6-7)
**Goal:** Production-ready UI, JSON API, and documentation.

1. Build `web/DashboardServlet.java`:
   - Suite overview: % pass per suite, last run time
   - Drill-down: individual scenario results
   - Run controls: run all, run suite, run single scenario
   - Target selector: prod vs dev
2. Build `web/RunApiServlet.java` — JSON API (see contracts in section 4)
3. Rewrite `web/ReportServlet.java` — HTML report for sharing
4. Rewrite `README.md`, `CONFIGURATION.md`, `QUICKSTART.md`, `TEST_PLAN.md`

**Acceptance criteria:**
- [ ] Dashboard shows all suites with pass/fail status
- [ ] API can trigger runs and return JSON results
- [ ] Docs accurate for new architecture

---

## 7. Risks and Mitigations

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| **ChemVantage response format undocumented** — we don't know exact HTML/JSON structure returned by each endpoint | Assertions may be too brittle or too loose | High | Phase 4 starts with JWKS (well-defined JSON). Capture raw responses first to build assertion library. Use substring/contains assertions before exact-match. |
| **Multi-stage flow timing** — score passback may be async with variable delay | Scoring tests flaky | Medium | Implement configurable retry with backoff in `TestRunner`. Allow max-wait-seconds per step. |
| **ChemVantage OIDC flow requires actual browser redirects** — some steps may not work with plain HTTP client | Launch tests fail at redirect chain | Medium | Phase 2 validates with simple POST-based flows first. If redirects needed, add redirect-following in `HttpClient`. Worst case: use HtmlUnit for browser simulation. |
| **Dynamic Registration requires interactive UI step** — CV may present a confirmation dialog | Registration test can't be fully automated | Medium | Support semi-automated: test initiates registration, pauses for human click, then verifies result. Tag these as `manual-step-required`. |
| **Objectify entity migration** — deleting `TestResult` may lose existing data | Minor — no production data of value | Low | This is a test tool; data loss is acceptable. No migration needed. |
| **App Engine cold starts** — platform mock endpoints might timeout if CV calls back during a cold start | Intermittent test failures | Low | Keep mock servlets minimal. Set `min_instances: 1` in `app.yaml` for warm starts. |
| **Single-threaded Objectify sessions** — concurrent test runs may conflict | Race conditions in test results | Medium | Use batch IDs to isolate runs. TestRunner acquires FlowState per-scenario, not shared. |

---

## 8. Suggested TEST_PLAN.md Structure

Replace the current manual-checkbox format with a machine-readable structure aligned to the new architecture:

```markdown
# TestVantage — Regression Test Plan

## Test Suites

### Suite: JWKS (`/test/jwks`)
| ID | Scenario | Steps | Expected |
|----|----------|-------|----------|
| jwks-001 | Fetch CV JWKS | GET /jwks | 200, valid JWK Set JSON, ≥1 RSA key |
| jwks-002 | Key matches JWT signature | GET /jwks, verify against sample JWT | kid matches, signature valid |

### Suite: Auth Token (`/test/auth-token`)  
| ID | Scenario | Steps | Expected |
|----|----------|-------|----------|
| auth-001 | Valid client_credentials | POST /auth/token | 200, Bearer token, expires_in > 0 |
| auth-002 | Invalid credentials | POST /auth/token with bad secret | 401 |
| auth-003 | Missing grant_type | POST /auth/token without grant_type | 400 |

### Suite: Launch (`/test/launch`)
| ID | Scenario | Assignment Type | Role | Steps | Expected |
|----|----------|----------------|------|-------|----------|
| launch-001 | Homework Student | HOMEWORK | STUDENT | OIDC → id_token → POST /lti/launch | 200, homework content |
| launch-002 | Homework Instructor | HOMEWORK | INSTRUCTOR | OIDC → id_token → POST /lti/launch | 200, instructor view |
| launch-003 | Quiz Student | QUIZ | STUDENT | ... | ... |
| ... | (8 types × 2 roles = 16 scenarios) | | | | |

### Suite: Deep Links (`/test/deeplinks`)
| ID | Scenario | Steps | Expected |
|----|----------|-------|----------|
| dl-001 | Homework deep link | DeepLinkingRequest → content_items | LtiResourceLink with assignment_type=Homework |
| dl-002 | Deep link then launch | DeepLink → extract resource_link → Launch | Quiz content displayed |
| ... | (one per assignment type + multi-stage combos) | | |

### Suite: Registration (`/test/registration`)
| ID | Scenario | Steps | Expected |
|----|----------|-------|----------|
| reg-001 | Dynamic registration | POST platform metadata | client_id returned |
| reg-002 | Re-registration | POST again | same or new client_id, no error |

### Suite: Scoring (`/test/scoring`)
| ID | Scenario | Assignment Type | Steps | Expected |
|----|----------|----------------|-------|----------|
| score-001 | Homework scoring | HOMEWORK | Launch → submit → wait → check AGS | Score ≥ 0, ≤ scoreMaximum |
| score-002 | Quiz scoring | QUIZ | Launch → submit → wait → check AGS | Score recorded |

## Automation
- All scenarios are executable via `POST /api/run`
- Results persisted and viewable at `/test-report` and `GET /api/runs`
- CI trigger: `curl -X POST https://test-vantage.appspot.com/api/run?target=dev`
```

---

## 9. First Sprint Backlog (10-15 Tasks)

Sprint goal: **Phase 0 + Phase 1 complete — new package structure deployed, test engine core working.**

| # | Task | Est. | Acceptance |
|---|------|------|------------|
| 1 | Create package directories: `platform/`, `runner/`, `tests/`, `model/`, `web/` | S | Directories exist |
| 2 | Move `KeyManager` → `platform/KeyManager`; update imports | S | Compiles |
| 3 | Move `JWKSServlet` → `platform/JwksServlet`; update package + web.xml | S | `/jwks` responds |
| 4 | Move `OAuth2TokenServlet` → `platform/OAuth2TokenServlet` | S | `/oauth2/token` responds |
| 5 | Move `OIDCConfigurationServlet` → `platform/OidcConfigServlet` | S | `/.well-known/openid-configuration` responds |
| 6 | Move `OIDCAuthServlet` → `platform/OidcAuthServlet` | S | Compiles |
| 7 | Move `NRPSServlet` → `platform/NrpsMockServlet` | S | `/nrps/*` responds |
| 8 | Move `AGSServlet` → `platform/AgsMockServlet` | S | `/ags/*` responds |
| 9 | Delete `HomeServlet`, `LaunchServlet`, `DeepLinkingServlet`, `OIDCLoginServlet`, `TestResult`; create stub `DashboardServlet` | M | App deploys, `/` shows stub page |
| 10 | Rewrite `web.xml` for new servlet-class paths | M | All endpoints mapped correctly |
| 11 | Create `runner/TestScenario`, `TestStep`, `FlowState`, `StepResult`, `TestRun` POJOs | M | Classes compile with all fields from data model |
| 12 | Create `runner/HttpClient` — wrapper around Google HTTP Client | M | Can make GET/POST to external URL, returns status + body |
| 13 | Create `runner/Assertions` — `assertStatusCode()`, `assertJsonPath()`, `assertJwtClaim()` | M | Unit-testable assertion methods |
| 14 | Create `runner/TestRunner` — execute scenario, carry FlowState, return TestRun | L | Runs a 2-step dummy scenario end-to-end |
| 15 | Create `model/TestRunEntity` + register in Objectify; persist a test run | M | Run saved and loadable from Datastore |

**Size key:** S = < 1 hour, M = 1-3 hours, L = 3-6 hours

---

## 10. Constitution Check

- **Java 17 + Servlet 4.0 + Maven:** Maintained
- **Google App Engine Standard:** Maintained  
- **Objectify for persistence:** Maintained
- **No new frameworks added:** Pure servlet + existing deps
- **Security:** JWT signing/verification already present; no new auth surface added. Platform mock endpoints should validate Bearer tokens properly (addressed in Phase 3).
- **Deployment:** Same `deploy.sh` and `mvn appengine:deploy` workflow
