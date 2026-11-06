# RBAC — Roles & Settings-Driven Permissions (STAFF / ADMIN)

Date: 2026-09-27
Status: Approved (design), implementation in progress

## Goal
Add lightweight, session-style role-based access control so the owner (ADMIN) can hand
limited control to STAFF when away. Two roles: **ADMIN** (always full access) and **STAFF**
(configurable). Which modules a STAFF user sees, and whether they can view/write/delete in
each, is controlled entirely from Settings. ADMIN can also add/manage users from Settings.
No JWT, no Spring Security — enforcement is a lightweight interceptor keyed off the
logged-in user id sent per request.

## Decisions (confirmed)
- BE enforcement: **HandlerInterceptor + `X-User-Id` header** (no Spring Security).
- Passwords: **plaintext** (consistent with existing decision).
- Granularity: **View / Write / Delete per module**.

## Roles
- `Role` constant: `ADMIN`, `STAFF`. `AppUser.role` (existing VARCHAR) validated against these.
- ADMIN → implicit all-true, never restrictable.

## Permission model
One new `system_setting` key **`ROLE_PERMISSIONS`** (JSON, stored in the existing TEXT column):
```json
{ "STAFF": {
  "dashboard":{"view":true,"write":false,"delete":false},
  "sales":{"view":true,"write":true,"delete":false},
  "inventory":{"view":true,"write":false,"delete":false},
  "products":{"view":true,"write":false,"delete":false},
  "investment":{"view":true,"write":false,"delete":false},
  "production":{"view":false,"write":false,"delete":false},
  "employees":{"view":false,"write":false,"delete":false},
  "settings":{"view":false,"write":false,"delete":false} } }
```
- Modules = the 8 menu keys.
- Actions: `view`=GET, `write`=POST/PUT/PATCH, `delete`=DELETE.

## Backend
- `com.skr.erp.common.constants.Role` — ADMIN, STAFF.
- `com.skr.erp.config.PermissionInterceptor` + `com.skr.erp.config.WebConfig` (WebMvcConfigurer).
  - Applies to `/api/**`; skips `/api/auth/**` and CORS preflight (OPTIONS).
  - Reads `X-User-Id`; missing/unknown/inactive → 401. ADMIN → allow.
  - STAFF: path→module + method→action; consult `ROLE_PERMISSIONS`; deny **403** if false/missing.
  - Path→module: `/api/sales`,`/api/customers`,`/api/qr`→sales · `/api/inventory`→inventory ·
    `/api/products`,`/api/product-material-cost`→products · `/api/investments`,`/api/vendors`→investment ·
    `/api/production`→production · `/api/employees`→employees · `/api/dashboard`→dashboard ·
    `/api/settings`,`/api/users`→settings.
  - Permission JSON parsed via Jackson from `SystemSettingRepository`; cached per request read.
- User management `/api/users` (gated as settings-module, so ADMIN-only in practice):
  - GET list, POST create, PUT `/{id}` update (fullName/role/active), PATCH `/{id}/password` reset,
    PATCH `/{id}/status` toggle active.
  - `AppUserRepository`: add `List<AppUser> findAllByOrderByCreatedAtDesc()`, `boolean existsByUsernameIgnoreCase(String)`.
  - DTOs: `CreateUserRequest`, `UpdateUserRequest`, `ResetPasswordRequest`, `UserResponse` (no password out).
  - `UserService` (concrete class, matching the no-interface convention).
- `GlobalExceptionHandler`: add handling for a new `ForbiddenException` (403). 401 continues to
  use `InvalidCredentialsException`.
- Migration **V39** seeds `ROLE_PERMISSIONS` with the STAFF default above.

## Frontend
- `authService.login` already returns `userId`+`role`; AuthContext already stores `user.id`+`user.role`.
- `axios` request interceptor: add `X-User-Id: <user.id>` from `skr_user`.
- `axios` response interceptor: on **403**, show a "no permission" toast; do NOT logout (logout stays 401-only).
- `usePermissions()` (reads `ROLE_PERMISSIONS` via AppSettings + current role): `can(module, action)`,
  `visibleModules`; ADMIN → always true.
- `Sidebar`: filter `MENU_ITEMS` by `can(key,'view')`.
- Route guard: redirect to `/dashboard` when `!can(module,'view')`.
- New Settings section **"Users & Roles"** (ADMIN-only): user table + Add/Edit dialog, and a STAFF
  permission matrix (rows=modules, cols=View/Write/Delete) → saves `ROLE_PERMISSIONS`.
- Primary action buttons per module (create/edit/delete) hidden/disabled via `can(...)`.

## Out of scope
- Password hashing, per-record ownership, audit of permission changes, more than two roles.
