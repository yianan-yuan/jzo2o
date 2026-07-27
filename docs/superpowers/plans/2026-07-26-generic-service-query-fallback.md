# Generic Service Query Fallback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make generic service questions return real city services without depending on unstable model JSON.

**Architecture:** Add a small intent recognizer in `DemandUnderstandingService` before the existing model call. It produces a resolved decision with an empty keyword so the existing catalog, authority filtering, selection fallback, and SSE response pipeline remain unchanged.

**Tech Stack:** Java, Spring Boot, JUnit 5, Mockito, AssertJ.

## Global Constraints

- Keep SSE streaming unchanged.
- Only bypass the model for generic service-list questions.
- Never recommend services outside the existing catalog authority path.

---

### Task 1: Recognize generic service-list questions before the model call

**Files:**
- Modify: `jzo2o-aigc/src/main/java/com/jzo2o/aigc/service/DemandUnderstandingService.java`
- Test: `jzo2o-aigc/src/test/java/com/jzo2o/aigc/service/DemandUnderstandingServiceTest.java`

**Interfaces:**
- Consumes: `understand(AigcSession, String, CancellationToken)`.
- Produces: `DemandDecision` with `needsClarification=false` and empty `searchKeyword` for generic service-list questions.

- [ ] **Step 1: Write the failing test**

```java
@Test
void shouldListServicesWithoutCallingModelForGenericServiceQuery() {
    DemandDecision result = service.understand(session(), "有什么服务", new CancellationToken());
    assertThat(result.isNeedsClarification()).isFalse();
    assertThat(result.getProfile().getSearchKeyword()).isEmpty();
    verify(provider, never()).complete(anyList(), anyDouble(), any());
}
```

- [ ] **Step 2: Run the focused test to verify it fails**

Run: `mvn -f jzo2o-aigc/pom.xml -Dtest=DemandUnderstandingServiceTest#shouldListServicesWithoutCallingModelForGenericServiceQuery test`

Expected: FAIL because the current implementation calls the provider for this input.

- [ ] **Step 3: Write the minimal implementation**

```java
if (isGreeting(userText.trim()) || isGenericServiceQuery(userText.trim())) {
    return fallbackDecision(session, userText);
}
```

Extend `fallbackDecision` so generic service queries set an empty keyword and `needsClarification=false`; add only the fixed Chinese query phrases required for this behavior.

- [ ] **Step 4: Run the focused test to verify it passes**

Run: `mvn -f jzo2o-aigc/pom.xml -Dtest=DemandUnderstandingServiceTest#shouldListServicesWithoutCallingModelForGenericServiceQuery test`

Expected: PASS.

- [ ] **Step 5: Run the module test suite**

Run: `mvn -f jzo2o-aigc/pom.xml test`

Expected: BUILD SUCCESS with no test failures.

- [ ] **Step 6: Verify through the mini program**

Restart `AigcApplication`, open a new AI chat session, send “有什么服务”, and verify the streamed reply contains real service cards and no `AIGC_MODEL_OUTPUT_INVALID`.
