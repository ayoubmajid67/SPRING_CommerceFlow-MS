![1767212063574](image/part9_circuit_breaker/1767212063574.png)
![1767212079211](image/part9_circuit_breaker/1767212079211.png)

we will use resiliency4j 
- rate limiter
- circuit breaker
- bulkhead pattern 
- we will use spring cloud circuit breaker
![1767212327711](image/part9_circuit_breaker/1767212327711.png)


1- add this dependency in the pom.xml of the order service
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
</dependency>

       <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
```

4- add this configuration in the application.properties of the order service
```bash
# Actuator Endpoints :
# Enables the circuit breaker health indicator to be shown in the health endpoint.
management.health.circuitbreakers.enabled=true
# Exposes all Actuator endpoints over HTTP (Web).
management.endpoints.web.exposure.include=*
# Shows full details of the health check (including nested components like db, diskSpace, circuitBreakers).
management.endpoint.health.show-details=always

# Resilience4j Properties :

# Registers the circuit breaker health indicator with Spring Boot Actuator.
resilience4j.circuitbreaker.configs.default.register-health-indicator=true 
# Sets the sliding window type to COUNT_BASED (records the outcome of the last N calls).
resilience4j.circuitbreaker.configs.default.sliding-window-type=COUNT_BASED
# The size of the sliding window (number of calls to record).
resilience4j.circuitbreaker.configs.default.sliding-window-size=10 
# The failure rate threshold in percentage. If 50% of calls fail, the circuit opens.
resilience4j.circuitbreaker.configs.default.failure-rate-threshold=50 
# The time to wait before transitioning from OPEN to HALF_OPEN state.
resilience4j.circuitbreaker.configs.default.wait-duration-in-open-state=5s
# The number of permitted calls when the circuit is in HALF_OPEN state.
resilience4j.circuitbreaker.configs.default.permitted-number-of-calls-in-half-open-state=3
# Automatically transitions to HALF_OPEN state after the wait duration, without waiting for a call.
resilience4j.circuitbreaker.configs.default.automatic-transition-from-open-to-half-open-enabled=true 

# Resilience4j Timeout Properties:
# The time to wait for a permission before timing out.
resilience4j.ratelimiter.configs.default.timeout-duration=3s
```
Perfect — let’s do this **clean, deep, and systematic**, and I’ll include the **logic + examples** for **every property**, so you fully understand **what happens at runtime**.

I’ll explain them in the **order the circuit breaker actually uses them**.

---

# 🧠 Resilience4j Circuit Breaker & Rate Limiter

### Full Explanation with Logic + Examples

---

## 1️⃣ `register-health-indicator`

```properties
resilience4j.circuitbreaker.configs.default.register-health-indicator=true
```

### What it does

Registers the **circuit breaker status** in **Spring Boot Actuator**.

### Internal logic

* Circuit breaker exposes its state (`CLOSED`, `OPEN`, `HALF_OPEN`)
* Actuator collects this state
* `/actuator/health` includes it

### Example

```json
{
  "status": "DOWN",
  "components": {
    "circuitBreakers": {
      "status": "DOWN",
      "details": {
        "productService": {
          "state": "OPEN",
          "failureRate": "60%"
        }
      }
    }
  }
}
```

### Why it matters

* Monitoring (Prometheus, Grafana)
* Ops visibility
* Eureka can mark service as unhealthy

---

## 2️⃣ `sliding-window-type`

```properties
resilience4j.circuitbreaker.configs.default.sliding-window-type=COUNT_BASED
```

### What it does

Defines **how failures are measured**.

### Logic

Two modes exist:

| Type          | Logic                       |
| ------------- | --------------------------- |
| `COUNT_BASED` | Last **N calls**            |
| `TIME_BASED`  | Calls in last **T seconds** |

### Your config (COUNT_BASED)

Resilience4j only cares about the **last N calls**, no time.

### Example

```
Calls history: [✔ ❌ ❌ ✔ ✔ ❌ ✔ ✔ ❌ ❌]
```

Only count = important, not when they happened.

---

## 3️⃣ `sliding-window-size`

```properties
resilience4j.circuitbreaker.configs.default.sliding-window-size=10
```

### What it does

Defines **N** = number of calls to observe.

### Logic

* Every request outcome is recorded
* Oldest call is removed when size exceeds 10

### Example

```
Last 10 calls:
✔ ✔ ❌ ❌ ❌ ✔ ✔ ❌ ✔ ❌
```

Failures = 5 / 10

---

## 4️⃣ `failure-rate-threshold`

```properties
resilience4j.circuitbreaker.configs.default.failure-rate-threshold=50
```

### What it does

Defines **when the circuit opens**.

### Logic

```
failureRate = (failedCalls / totalCalls) × 100
```

If:

```
failureRate ≥ 50%
→ circuit transitions CLOSED → OPEN
```

### Example

| Total Calls | Failures | Failure Rate | Result |
| ----------- | -------- | ------------ | ------ |
| 10          | 4        | 40%          | CLOSED |
| 10          | 5        | 50%          | OPEN   |
| 10          | 7        | 70%          | OPEN   |

---

## 5️⃣ `wait-duration-in-open-state`

```properties
resilience4j.circuitbreaker.configs.default.wait-duration-in-open-state=5s
```

### What it does

Defines **how long the circuit stays OPEN**.

### Logic

When OPEN:

* All calls fail immediately
* No HTTP request is sent downstream

### Timeline example

```
T0  → Circuit opens
T0–T5s → All calls rejected
T5s → Eligible for HALF_OPEN
```

### Why critical

* Prevents hammering a broken service
* Stops cascading failures

---

## 6️⃣ `automatic-transition-from-open-to-half-open-enabled`

```properties
resilience4j.circuitbreaker.configs.default.automatic-transition-from-open-to-half-open-enabled=true
```

### What it does

Automatically moves:

```
OPEN → HALF_OPEN
```

after `wait-duration`.

### Logic

If `true`:

* Transition happens **without traffic**
  If `false`:
* Transition happens **only when a request arrives**

### Example

| Setting | Behavior                    |
| ------- | --------------------------- |
| true    | Recovery starts immediately |
| false   | Recovery waits for traffic  |

---

## 7️⃣ `permitted-number-of-calls-in-half-open-state`

```properties
resilience4j.circuitbreaker.configs.default.permitted-number-of-calls-in-half-open-state=3
```

### What it does

Limits **how many test calls** are allowed during recovery.

### 🔴 VERY IMPORTANT

> ❌ NOT per second
> ❌ NOT rate-based
> ✅ TOTAL calls per HALF_OPEN cycle

### Logic

```
HALF_OPEN:
Allow exactly 3 calls
Evaluate results
Decide → CLOSED or OPEN
```

### Example

```
HALF_OPEN starts
✔ Call 1 → success
✔ Call 2 → success
❌ Call 3 → failure

→ Circuit goes back to OPEN
```

Or:

```
✔ ✔ ✔ → CLOSED
```

### Why critical

* Prevents traffic flood
* Controlled recovery testing

---

## 8️⃣ Circuit Breaker State Machine (Full Flow)

```
CLOSED
  │ (failure rate ≥ 50%)
  ▼
OPEN
  │ (after 5s)
  ▼
HALF_OPEN
  │ (3 test calls)
  ├─ success → CLOSED
  └─ failure → OPEN
```

---

# ⏱️ Rate Limiter Property

## 9️⃣ `timeout-duration`

```properties
resilience4j.ratelimiter.configs.default.timeout-duration=3s
```

### What it does

How long a request waits for **rate-limiter permission**.

### Logic

If rate limit is exceeded:

* Request waits up to 3 seconds
* If no permit → request fails

### Example

```
Rate limit = 10 req/sec
11th request arrives

→ waits 3s
→ if permit frees → allowed
→ else → rejected
```

### Difference vs Circuit Breaker

| Feature          | Circuit Breaker | Rate Limiter   |
| ---------------- | --------------- | -------------- |
| Protects against | Failures        | Traffic spikes |
| Time-based       | ❌               | ✅              |
| HALF_OPEN        | ✅               | ❌              |
| Per second logic | ❌               | ✅              |

---

# 🧩 How Everything Works Together (Gateway)

```
Client
  ↓
Rate Limiter (traffic control)
  ↓
Circuit Breaker (failure control)
  ↓
LoadBalancer (instance selection)
  ↓
Service
```

---
Great question — this one is **very important** and often misunderstood.
Let’s break it **precisely**, with **logic + real examples**, and also show **where it fits in the architecture**.

---

# ⏱️ `TimeLimiter` – `timeout-duration`

```properties
resilience4j.timelimiter.configs.default.timeout-duration=3s
```

---

## 1️⃣ What this property does (simple)

> **Limits how long your service waits for a response from another service.**

If the downstream service **does not respond within 3 seconds**, the call is:

* **Canceled**
* **Marked as a failure**
* **Triggers fallback / circuit breaker logic**

---

## 2️⃣ What problem it solves

Without a TimeLimiter:

* Threads block
* Gateway becomes slow
* Load balancer queues grow
* Circuit breaker reacts **too late**

TimeLimiter protects **latency**, not failure rate.

---

## 3️⃣ Internal logic (VERY IMPORTANT)

TimeLimiter works by **wrapping async calls**:

* `CompletableFuture`
* `Mono` / `Flux` (WebFlux)
* Async executors

⚠️ **It does NOT work with blocking MVC calls unless explicitly wrapped async**

---

## 4️⃣ Execution flow with TimeLimiter

```
Request sent
  ↓
Start timer (3s)
  ↓
Wait for response
  ↓
If response < 3s → SUCCESS
If response ≥ 3s → TIMEOUT exception
```

Timeout exception:

```
java.util.concurrent.TimeoutException
```

This counts as:

* ❌ failure for Circuit Breaker
* ❌ rejected call for client
* ✅ fallback triggered

---

## 5️⃣ Example timeline

### Case 1: Fast service

```
Service responds in 1.2s
→ Request succeeds
→ Circuit breaker sees SUCCESS
```

### Case 2: Slow service

```
Service responds in 4s
→ TimeLimiter stops at 3s
→ TimeoutException thrown
→ Circuit breaker records FAILURE
```

---

## 6️⃣ Relationship with Circuit Breaker (CRITICAL)

TimeLimiter **feeds failures into the circuit breaker**.

### Combined logic

```
TimeLimiter (3s)
  ↓
CircuitBreaker
  ↓
Failure rate calculated
```

So if many calls exceed 3 seconds:

```
timeouts ↑
failure rate ↑
circuit opens
```

---

## 7️⃣ Why TimeLimiter is NOT Retry

| Feature           | TimeLimiter     | Retry           |
| ----------------- | --------------- | --------------- |
| Purpose           | Control latency | Re-attempt call |
| Stops slow calls  | ✅               | ❌               |
| Generates failure | ✅               | Sometimes       |
| Adds traffic      | ❌               | ✅               |

TimeLimiter **cuts** slow calls
Retry **adds** more calls

---

## 8️⃣ Where it sits in the Gateway flow

```
Client
  ↓
RateLimiter (traffic)
  ↓
TimeLimiter (latency)
  ↓
CircuitBreaker (stability)
  ↓
LoadBalancer
  ↓
Service
```

---

## 9️⃣ MVC Gateway vs WebFlux Gateway (VERY IMPORTANT)

### ❌ MVC (blocking)

TimeLimiter:

* ❌ Not effective unless wrapped in async
* Thread still blocked

### ✅ WebFlux (reactive)

TimeLimiter:

* ✅ Fully non-blocking
* Cancels subscription

👉 **Best practice**:
TimeLimiter is **designed for reactive or async flows**

---

## 🔧 MVC Gateway – How to make it work properly

You must use:

```java
CompletableFuture.supplyAsync(() -> service.call())
```

or:

```java
@Async
public CompletableFuture<Response> call()
```

Otherwise:

> The timeout happens logically, but the thread remains blocked.

---

## 10️⃣ Example configuration (full)

```properties
resilience4j.timelimiter.configs.default.timeout-duration=3s
resilience4j.circuitbreaker.configs.default.failure-rate-threshold=50
resilience4j.circuitbreaker.configs.default.sliding-window-size=10
```

---

## 11️⃣ Real-world example (microservices)

### Scenario

* Product service is slow due to DB lock
* Response time: 6–8 seconds

### What happens

```
TimeLimiter → stops at 3s
CircuitBreaker → counts failures
After 5 failures → circuit OPEN
Gateway → instant fallback
System stays responsive
```

---

## 12️⃣ What happens WITHOUT TimeLimiter (danger)

* Gateway threads blocked
* Load balancer queue grows
* Other services impacted
* Full system slowdown (cascading failure)

---

## 🧠 One-line mental model

> **TimeLimiter answers:**
> “How long am I allowed to wait?”

---


---

## 1️⃣ Max Attempts

```properties
resilience4j.retry.configs.default.max-attempts=3
```

### What it does

Defines **how many total attempts** are made for a single request.

⚠️ **Total attempts**, not retries.

### Logic

```
Attempt #1 → original call
Attempt #2 → retry
Attempt #3 → retry
→ then stop
```

So:

* **2 retries**
* **3 total attempts**

---

### Example

If Product Service returns 500:

```
Call #1 → 500
Call #2 → 500
Call #3 → 200
→ SUCCESS
```

If all fail:

```
Call #1 → fail
Call #2 → fail
Call #3 → fail
→ failure returned
```

---

### Why this matters

* Too low → temporary glitches not recovered
* Too high → traffic explosion

---

## 2️⃣ Wait Duration

```properties
resilience4j.retry.configs.default.wait-duration=2s
```

### What it does

Defines **how long Retry waits between attempts**.

### Logic

```
Attempt #1 fails
→ wait 2s
→ Attempt #2
→ wait 2s
→ Attempt #3
```

This delay:

* Gives downstream service time to recover
* Avoids immediate retry storms

---

### Example timeline

```
t=0s   Attempt 1 → fail
t=2s   Attempt 2 → fail
t=4s   Attempt 3 → success
```

Total retry time ≈ **4 seconds** (excluding call duration).

---

## 3️⃣ What Retry considers a failure

Retry triggers on:

* Exceptions
* HTTP 5xx
* Timeouts (from TimeLimiter)

❌ Does NOT retry:

* 4xx client errors (by default)
* Validation failures

---

## 4️⃣ Retry + TimeLimiter interaction (CRITICAL)

Retry is applied **after TimeLimiter fails**.

### Example

```properties
timelimiter.timeout-duration=3s
retry.max-attempts=3
retry.wait-duration=2s
```

Worst case duration:

```
(3s timeout × 3 attempts) + (2s × 2 waits)
= 9s + 4s
= 13 seconds
```

⚠️ This is why Retry must be **carefully limited**.

---

## 5️⃣ Retry + Circuit Breaker interaction

Retry **feeds failures into the circuit breaker**.

### Bad configuration example

```
Retry = 5
CircuitBreaker window = 10
```

Result:

```
2 user requests → 10 failures
→ Circuit opens too fast
```

---

## 6️⃣ Retry vs Circuit Breaker (VERY IMPORTANT)

| Feature            | Retry                         | Circuit Breaker      |
| ------------------ | ----------------------------- | -------------------- |
| Purpose            | Recover from transient errors | Stop failing service |
| Sends more traffic | ✅ Yes                         | ❌ No                 |
| Protects service   | ❌ No                          | ✅ Yes                |
| Risk               | Retry storm                   | None                 |

👉 **Retry must always be used with a Circuit Breaker**

---

## 7️⃣ Retry in Gateway Architecture

```
Client
  ↓
Gateway
  ↓
RateLimiter
  ↓
TimeLimiter
  ↓
Retry
  ↓
CircuitBreaker
  ↓
LoadBalancer
  ↓
Service
```

---

## 8️⃣ When to use Retry (BEST PRACTICE)

✅ Use Retry when:

* Network glitches
* Temporary service startup issues
* Occasional timeouts

❌ Avoid Retry when:

* Service is overloaded
* Errors are deterministic (validation)
* Long-running requests

---

## 9️⃣ MVC Gateway vs WebFlux Gateway

| Gateway Type | Retry   |
| ------------ | ------- |
| MVC          | ✅ Works |
| WebFlux      | ✅ Works |

But:

* MVC blocks threads
* WebFlux retries non-blocking

---

## 🧠 One-sentence mental model

> **Retry answers:**
> “Maybe the service will work if I try again.”

---

## 🎯 Final recommendation (production)

```properties
retry.max-attempts=2 or 3
retry.wait-duration=500ms – 2s
always combine with circuit breaker
```

---
Excellent addition — this property is **very important** and often **missed**, and your comment shows you already understand the internal logic 👍
Below is the **clean, complete explanation**, fully integrated with the **circuit breaker evaluation flow**, examples, and architectural impact.

---

# 🧠 Circuit Breaker – `minimum-number-of-calls`

```properties
resilience4j.circuitbreaker.configs.default.minimum-number-of-calls=5
```

---

## 1️⃣ What this property does (core idea)

> Defines the **minimum number of calls** that must be recorded **before the circuit breaker starts evaluating the failure rate**.

Until this number is reached:

* ❌ **No OPEN decision**
* ❌ Failure rate is **not calculated**
* ✅ Circuit stays **CLOSED**

---

## 2️⃣ Why this property exists

Without this property:

* Early failures could **open the circuit too soon**
* Small traffic bursts cause **false positives**
* Startup phase becomes unstable

This property prevents **premature circuit opening**.

---

## 3️⃣ How it works with sliding window (CRITICAL LOGIC)

Your config:

```properties
sliding-window-size = 10
minimum-number-of-calls = 5
failure-rate-threshold = 50%
```

### Evaluation logic:

```
Calls recorded < 5
→ DO NOT calculate failure rate
→ Circuit stays CLOSED

Calls recorded ≥ 5
→ Start calculating failure rate
→ OPEN if threshold exceeded
```

---

## 4️⃣ Timeline example (VERY IMPORTANT)

### Case: first requests after deployment

| Call # | Result | Total Calls | Failure Rate | Circuit |
| ------ | ------ | ----------- | ------------ | ------- |
| 1      | ❌      | 1           | —            | CLOSED  |
| 2      | ❌      | 2           | —            | CLOSED  |
| 3      | ❌      | 3           | —            | CLOSED  |
| 4      | ❌      | 4           | —            | CLOSED  |
| 5      | ❌      | 5           | 100%         | 🔴 OPEN |

✔ Circuit opens **only after 5 calls**, not after 1 or 2

---

## 5️⃣ What happens without this property (danger)

Default behavior:

```
sliding-window-size = 10
minimum-number-of-calls = 10
```

So:

* Circuit waits for **all 10 calls**
* Slow reaction to real failures

Your config improves **responsiveness**.

---

## 6️⃣ Why 5 is a good number (best practice)

| Value | Effect         |
| ----- | -------------- |
| 1–2   | Too aggressive |
| 5     | ✅ Balanced     |
| 10    | Conservative   |

5 allows:

* Early detection
* Still statistically meaningful

---

## 7️⃣ Interaction with Retry (VERY IMPORTANT)

With Retry:

```
1 client request → 3 retry attempts
```

That means:

```
2 client requests → 6 circuit breaker calls
```

So with:

```properties
minimum-number-of-calls=5
```

👉 The circuit may open **after only 2 user requests**

⚠️ This is expected and correct — retries are real calls.

---

## 8️⃣ Interaction with TimeLimiter

Timeouts count as failures.

Example:

```
5 timeouts ≥ 3s
→ failure rate = 100%
→ circuit OPEN
```

This prevents slow services from killing the gateway.

---

## 9️⃣ Interaction with HALF_OPEN state

This property applies to:

* ✅ CLOSED state only

It does **not** affect:

* HALF_OPEN evaluation
* Permitted calls logic

HALF_OPEN uses:

```properties
permitted-number-of-calls-in-half-open-state
```

---

## 🧠 Mental model (memorize)

> **minimum-number-of-calls answers:**
> “Do I have enough data to judge this service?”

---

## 🎯 Final integrated circuit breaker logic (FULL FLOW)

```
Request arrives
  ↓
Record outcome
  ↓
If calls < minimum-number-of-calls → ignore
  ↓
Else:
   calculate failure rate over sliding window
   if failureRate ≥ threshold → OPEN
```

---

## ✅ Final recommended circuit breaker config (production)

```properties
sliding-window-type=COUNT_BASED
sliding-window-size=10
minimum-number-of-calls=5
failure-rate-threshold=50
wait-duration-in-open-state=5s
permitted-number-of-calls-in-half-open-state=3
automatic-transition-from-open-to-half-open-enabled=true
```

---

## 🎓 One-line PFE-ready explanation

> The `minimum-number-of-calls` property prevents premature circuit breaker activation by ensuring failure rate evaluation starts only after a statistically meaningful number of requests.

---

## 🎯 Final Mental Model (Memorize This)
* **TimeLimiter** → “How long am I allowed to wait?”
* **RateLimiter** → “Can I send this request now?”
* **CircuitBreaker** → “Should I even try?”
* **LoadBalancer** → “Where do I send it?”
* **Eureka** → “What instances exist?”

---
