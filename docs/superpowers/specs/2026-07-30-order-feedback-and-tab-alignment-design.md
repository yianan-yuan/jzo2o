# Order feedback and tab alignment design

## Scope

Repair the service-worker order experience and the customer mini-program order header without changing order APIs or status values.

## Worker App

- Replace the legacy success/failure face-image dialog used after order actions with a shared warm result-card structure: a small semantic status mark, title, optional API message, and one confirmation action.
- Apply the same result-card pattern to the existing rob-order success and failure path first. Other action pages continue to use their existing toast behavior unless they already render this dialog structure.
- Give the worker order page a dedicated empty-state variant. It will say that the currently selected order category has no records, rather than referring to waiting online for rob orders.
- Keep the existing API requests, refresh event, and confirmation flow unchanged.

## Customer Mini-program

- Make the five order-status tabs equal-width inside their warm container so the active underline sits under the label rather than near the screen edge.
- Position the reusable Navbar history control from the measured mini-program capsule boundary, keeping it visible without overlapping the WeChat controls.
- Preserve all current order-status routing and filter values.

## Verification

- Add source-contract tests that prevent the legacy result image and generic worker-order empty copy from returning.
- Add source-contract tests for the five-column customer status strip and capsule-aware history placement.
- Run both mini-program and worker App contract test suites and `git diff --check`.
