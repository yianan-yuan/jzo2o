# AIGC Model Fallback and Chat Loading Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent malformed local-model JSON from blocking a user conversation and show only one AI thinking bubble while a streamed reply is pending.

**Architecture:** Keep the existing strict parser for valid JSON objects so invalid recommendation references remain rejected. When both model attempts are syntactically malformed or have a non-object root, create a deterministic demand decision: greetings receive a fixed clarification question and explicit household-service terms become catalog search keywords. The page owns a single pending assistant message and renders its text as the loading label until the first stream delta arrives.

**Tech Stack:** Java 11, Spring Boot, JUnit 5, Mockito, Vue 3 Composition API, uni-app, Node built-in test runner.

## Global Constraints

- Keep `AIGC_MODEL_OUTPUT_INVALID` for valid JSON that violates business rules such as an out-of-range recommendation index.
- Do not add dependencies or alter gateway, Nacos, or database configuration.
- Build the mini-program output with the existing HBuilderX compilation workflow.

---

### Task 1: Safe demand fallback for malformed local-model output

**Files:**
- Modify: `jzo2o-aigc/src/main/java/com/jzo2o/aigc/service/DemandUnderstandingService.java`
- Modify: `jzo2o-aigc/src/test/java/com/jzo2o/aigc/service/DemandUnderstandingServiceTest.java`

**Interfaces:**
- Consumes: `DemandUnderstandingService.understand(AigcSession, String, CancellationToken)` and the existing two-attempt model call.
- Produces: a `DemandDecision` with either a clarification question for greetings or a catalog keyword for explicit needs.

- [ ] **Step 1: Write failing fallback tests**

```java
when(provider.complete(anyList(), anyDouble(), any())).thenReturn("not-json", "[]");
DemandDecision result = service.understand(session(), "你好", new CancellationToken());
assertThat(result.isNeedsClarification()).isTrue();
```

- [ ] **Step 2: Verify the tests fail before the fix**

Run: `mvn -f jzo2o-aigc/pom.xml -Dtest=DemandUnderstandingServiceTest test`

Expected: `AIGC_MODEL_OUTPUT_INVALID` for the two malformed outputs.

- [ ] **Step 3: Implement the minimal fallback boundary**

```java
catch (InvalidModelOutput invalidAgain) {
    if (invalidAgain.isFallbackEligible()) {
        return fallbackDecision(session, userText);
    }
    throw new AigcException(AigcErrorCode.MODEL_OUTPUT_INVALID);
}
```

Mark only malformed/non-object JSON as fallback-eligible. Preserve strict semantic validation for valid JSON objects. Map greetings to the fixed clarification text and recognised terms such as `保洁` and `空调` to search keywords.

- [ ] **Step 4: Verify the focused test suite passes**

Run: `mvn -f jzo2o-aigc/pom.xml -Dtest=DemandUnderstandingServiceTest test`

Expected: all `DemandUnderstandingServiceTest` tests pass, including invalid-index rejection.

### Task 2: Render one pending assistant bubble

**Files:**
- Modify: `project-xzb-xcx-uniapp-java/pages/ai-chat/index.vue`
- Create: `project-xzb-xcx-uniapp-java/pages/ai-chat/chat-message-state.js`
- Create: `project-xzb-xcx-uniapp-java/tests/chat-message-state.test.js`

**Interfaces:**
- Consumes: the existing SSE `delta`, `error`, and `done` events.
- Produces: `createPendingAssistantMessage()` and `appendAssistantDelta(message, text)` for one AI placeholder bubble.

- [ ] **Step 1: Write failing message-state tests**

```js
const message = createPendingAssistantMessage()
assert.equal(displayAssistantContent(message), 'AI 正在思考…')
appendAssistantDelta(message, '您好')
assert.equal(displayAssistantContent(message), '您好')
```

- [ ] **Step 2: Verify the test fails before the helper exists**

Run: `npm test -- --test-name-pattern="pending assistant"`

Expected: module-not-found failure for `chat-message-state.js`.

- [ ] **Step 3: Implement and use the helper**

```js
export const createPendingAssistantMessage = () =>
  ({ role: 'assistant', content: '', pending: true, recommendations: [], stage: '', error: null })
export const appendAssistantDelta = (message, text) => {
  message.pending = false
  message.content += text || ''
}
```

Remove the separate `v-if="loading"` message row. Render `msg.content || 'AI 正在思考…'` only while `msg.pending` is true.

- [ ] **Step 4: Verify mini-program unit tests and compile output**

Run: `npm test`

Expected: all Node tests pass. Then use HBuilderX **编译** so `unpackage/dist/dev/mp-weixin` receives the page update.

### Task 3: Integrated verification

**Files:**
- No source files beyond Tasks 1 and 2.

- [ ] **Step 1: Run the complete AIGC test suite**

Run: `mvn -f jzo2o-aigc/pom.xml test`

Expected: all tests pass.

- [ ] **Step 2: Restart `AigcApplication` in IntelliJ IDEA**

Use the existing `AigcApplication` run configuration and wait for the `Started AigcApplication` log line.

- [ ] **Step 3: Validate in WeChat Developer Tools**

Open AI assistant, send `你好`, and verify there is one pending AI bubble followed by a clarification reply, without `AIGC_MODEL_OUTPUT_INVALID`.
