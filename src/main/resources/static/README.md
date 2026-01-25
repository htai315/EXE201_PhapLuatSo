Frontend Standards — AppRuntime & API client
===========================================

Purpose
- Standardize auth bootstrap and API client usage across static frontend.

Canonical rules
- The canonical global API client is `window.apiClient` (set by `/scripts/api-client.js`).
- DO NOT assign or rely on `window.API_CLIENT` (forbidden).
- Use `window.AppRuntime` helpers (provided in `/scripts/app-runtime.js`) for:
  - `AppRuntime.domReady()`
  - `AppRuntime.authReady()`
  - `AppRuntime.getClient()` / `AppRuntime.requireClient(context)`
  - `AppRuntime.safe(label, fn)`
  - `AppRuntime.getMe(client)` and `AppRuntime.isAdmin(me)`

Patterns
- AUTH-BOOT (page needs token on load):
  (async ()=>{ await AppRuntime.domReady(); await AppRuntime.authReady(); const client = AppRuntime.requireClient('Page'); const me = await AppRuntime.safe('Page:getMe', ()=>AppRuntime.getMe(client)); /* ... */ })();

- PUBLIC (no token needed):
  (async ()=>{ await AppRuntime.domReady(); const client = AppRuntime.getClient(); if (!client) { console.warn('client missing'); return; } const data = await AppRuntime.safe('Page:get', ()=>client.get('/api/public')); })();

Guard checks (do not call the auth me endpoint directly)
- ALWAYS use `AppRuntime.getMe(client)` or `AppRuntime.ensureAdmin(client)`.
- Avoid embedding literal auth endpoints in pages; use `AppRuntime.getMe()` instead.

Dev checks (run locally)
- rg "API_CLIENT\\." src/main/resources/static || true
- rg "window\\.API_CLIENT" src/main/resources/static || true
- rg "auth/me" src/main/resources/static || true

If any match appears, update the file to use AppRuntime.getClient() / AppRuntime.getMe() and re-run the grep.


