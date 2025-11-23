# Apollo Codebase - AI Coding Instructions

## Project Overview
**Apollo** is a full-stack e-commerce platform with two main components:
- **nightgrass** (backend): Spring Boot 3.3 + Kotlin + WebFlux (reactive)
- **yaojia-ng** (frontend): Angular 20 with standalone components

## Architecture & Data Flow

### Backend (nightgrass)
- **Reactive Stack**: Uses Spring WebFlux + R2DBC for non-blocking database operations
- **REST Controllers** (`rest/`): Public APIs at `/api/product/*` for fetching products, details, and reviews
- **Admin Controllers** (`web/admin/`): Thymeleaf-based views with separate security chain
- **Service Layer** (`service/` + `serviceImpl/`): Business logic split between interfaces and implementations
- **Repository Layer** (`repo/`): R2DBC repositories (async-based, not traditional JPA)
- **Kotlin Pattern**: Constructors use dependency injection directly (no `@Autowired`), class parameters are autowired

Example flow: `RestProductController.productDetail()` → `ProductService.getProductById()` + parallel calls to `ProductPhotoService`, `ProductPropertyService`, `ReviewService` → aggregated response

### Frontend (yaojia-ng)
- **Standalone Components**: No NgModules; each component has `imports: [...]` array
- **State Management**: Uses Angular signals and computed properties (modern approach, not NgRx)
- **Service Layer**: Injectable services in `service/` fetch data and apply transforms
- **Route-based Guards**: `AuthGuard` checks JWT token; protected routes require authentication
- **Cart Persistence**: `CartStore` syncs to localStorage and backend simultaneously

Example flow: `ProductService.getProduct()` → maps response payload with gallery/specs → consumed by `product-detail` component

## Critical Build & Runtime Commands

### Backend (nightgrass)
```bash
# Root: c:\data\github\apollo\nightgrass
mvn clean package        # Build JAR
mvn clean install          # Build JAR
mvn clean install -DskipTests  # Skip tests
mvn spring-boot:run        # Run locally (uses dev profile by default)
mvn test                   # Run tests
```

**Profiles**: Dev/Prod/Secret via `application-{dev,prod,secret}.yml`; activated via `spring.profiles.active` env var

### Frontend (yaojia-ng)
```bash
# Root: c:\data\github\apollo\yaojia-ng
npm install                # Install dependencies
ng serve --configuration=development  # Dev server with dev config
ng serve --configuration=production   # Dev server with prod config
ng serve                   # Dev server at http://localhost:4200
ng build                   # Production build
ng test                    # Unit tests (Karma/Jasmine)
```

## Cart Endpoints

### POST `/api/cart/update` - Bulk Update Cart Items
Updates quantities or removes items from authenticated user's cart. Processes all items in parallel.

**Request Body**:
```json
{
  "items": [
    {
      "itemId": 1,
      "quantity": 3
    },
    {
      "itemId": 2,
      "quantity": 0
    }
  ]
}
```

**Logic**:
- `quantity > 0`: Update item quantity
- `quantity <= 0`: Remove item from cart
- No `itemId`: Skip item (malformed)

**Flow**:
1. Extract user from JWT token via `AuthenticationUtility`
2. Get user's cart via `getCartForUserOrGuest()`
3. Process all items in parallel via `Flux.fromIterable()`:
   - Call `updateQuantity()` for qty > 0
   - Call `removeCartItemFromCart()` for qty <= 0
4. Wait for all operations to complete with `collectList()`
5. Fetch updated cart items and return `CartResponse`

**Response** (200 OK):
```json
{
  "items": [
    {
      "itemId": 1,
      "productId": 5,
      "quantity": 3,
      "price": 29.99
    }
  ]
}
```

**Error Response** (500):
```json
{}
```

### GET `/api/cart/my-cart` - Retrieve User's Cart
Retrieves all cart items for authenticated user.

**Flow**:
1. Extract user from JWT token via `AuthenticationUtility`
2. Get user's cart via `cartService.getCartForUserOrGuest(user)`
3. Fetch all cart items via `cartService.getCartItemsForCart(cartId)`
4. Return `CartResponse` with items list or 401 if auth fails

**Response** (200 OK):
```json
{
  "items": [
    {
      "itemId": 1,
      "productId": 5,
      "quantity": 2,
      "price": 29.99
    }
  ]
}
```

### DELETE `/api/cart/clear` - Clear User's Cart
Clears all items from authenticated user's cart and resets total to zero.

**Flow**:
1. Extract user from JWT token via `AuthenticationUtility`
2. Delete all cart items via repository
3. Reset cart total to zero
4. Broadcast cart update via sink (for real-time updates)
5. Return 204 No Content on success

### Pattern: Authenticated Cart Operations
All cart endpoints follow this pattern:
1. Extract user from `ServerWebExchange` (no database lookup - JWT claims only)
2. Retrieve cart via `getCartForUserOrGuest(user)`
3. Perform operation (get items, clear, update)
4. Log success/failure with user ID
5. Error handling: 401 if auth fails, 500 if operation fails

## Cart Operations Example - Full Stack

The `/api/cart/clear` and `/api/cart/my-cart` endpoints demonstrate full-stack patterns:

**Backend Utility** (`AuthenticationUtility` - optimized auth component)
- `@Component` for reusability across multiple controllers
- Method: `extractUserFromExchange(exchange: ServerWebExchange): Mono<User>`

- **KEY OPTIMIZATION**: Constructs `User` directly from JWT claims (no database lookups)
- Avoids N+1 database queries: each request is O(1) time regardless of concurrency
- Encapsulates: token extraction → JWT validation → user object construction
- Comprehensive logging for debugging token issues

**JWT Claims Strategy**
- JWT should embed essential user identity: `id` (OAuth ID), `name`, `email`, `avatar`
- Token validation confirms authenticity; no DB lookup needed for request handling
- For operations requiring additional user fields not in JWT, lazy-load them in service layer only when needed

**Backend Endpoint** (`CartApiController.clearCart()`)
1. Injects `AuthenticationUtility` (not another controller or repository)
2. Calls utility: `authenticationUtility.extractUserFromExchange(exchange)`
3. User object created from JWT claims (instant, no DB)
4. Delegates to `cartService.clearCart(user)` (reactive operation)
5. Service deletes items, resets total, broadcasts update
6. Returns 204 No Content on success; 401 if token invalid

**Pattern**: Controllers depend on Utilities/Services, never other Controllers. Avoid N+1 queries via token-based user construction.

**Frontend** (`CartStore.clearCart()`)
1. Clears local signal: `items.set([])`
2. Calls `cartService.clearCart()` (DELETE `/api/cart/clear`)
3. Only syncs if `authService.isLoggedIn()` (respects auth state)
4. Saves empty cart to localStorage
5. Error handling: catches backend failures, continues with local state

## Authentication & Security

### OAuth2 User Persistence
When user authenticates via OAuth2 provider (Google, WeChat, etc.):
1. **Extract** user info from OAuth2 attributes (id, name, email, avatar)
2. **Save/Update** user in database via `UserService.saveOrUpdateOAuth2User()`
   - Creates new user if OAuth ID not found
   - Updates profile (name, email, avatar) if user already exists
   - Enables server-side user management, orders, cart, reviews
3. **Generate** JWT token pair with user data from saved user
4. **Redirect** to frontend with access token; refresh token in HttpOnly cookie

**Files**: `AuthenticationSuccessHandler` → calls `UserService.saveOrUpdateOAuth2User()` → `UserRepository.save()`

### JWT Token Generation
- **Access Token** (15 min): Contains `id`, `name`, `email`, `avatar`, `provider` claims
  - Used by `AuthenticationUtility` to construct User objects (zero-copy, no DB lookup)
  - Frontend adds to every request via `authInterceptor`
- **Refresh Token** (7 days): Contains only `id` and `jti` (JWT ID)
  - Stored in secure HttpOnly cookie (cannot be accessed by JavaScript)
  - Used to obtain new token pairs when access token expires

### Backend
- **OAuth2 + JWT**: `SecurityConfig` splits authentication:
  - `/admin/**` paths: Form-based login (admin authentication manager)
  - Other paths: JWT Bearer token validation via `JwtDecoder`
- **Token Generation**: `CustomJwtEncoder` enforces HS256 algorithm with shared secret
- **User Persistence**: OAuth2 users saved to database to enable order/cart/review tracking
- **Optimization**: JWT carries user identity (no DB lookup per request)

### Frontend
- **HTTP Interceptor**: `authInterceptor` automatically adds `Authorization: Bearer {token}` header to protected requests
- **Public Endpoints**: `/oauth2`, `/login`, `/api/product` bypass token requirement
- **Token Refresh**: On 401 error, calls `AuthService.refreshTokenAndRetry()` with interceptor


### Key Files
- Backend config: `config/JwtConfig.kt`, `config/SecurityConfig.kt`
- Frontend interceptor: `auth/auth-interceptor.ts`
- Service: `auth/service/auth-service.ts`

## Database & ORM

- **R2DBC** (reactive JDBC): All repositories return `Mono<T>` or `Flux<T>` (Reactor types), NOT `Optional<T>`
- **MySQL with async driver**: `r2dbc-mysql` handles connection pooling
- **Schema & Data**: `schema.sql`, `data.sql` in resources for initialization
- **Transactions**: Use `@Transactional` on service methods that modify multiple tables

**Pattern**: Never use `.block()` in services; always return Mono/Flux for non-blocking semantics

## Code Patterns & Conventions

### Backend (Kotlin/Spring Boot)
1. **Repository calls always async**: `userRepository.findByEmail(email): Mono<User>`
2. **Mono.zip for parallel operations**: Aggregate multiple service calls; see `RestProductController.productDetail()`
3. **DTO pattern**: Transfer objects in `dto/` folder; map domain models before returning
4. **Service interfaces**: `IProductService`, `ICartService` in `service/`; implementations in `serviceImpl/`

### Frontend (Angular)
1. **Signals over RxJS**: Prefer `signal()` and `computed()` for state; use RxJS pipes sparingly
2. **Async pipes in templates**: Bind observables directly; never subscribe in components
3. **Service methods return observables**: All HTTP calls stay in stream until consumed
4. **Standalone first**: New components get `standalone: true` with explicit `imports`
5. **Material Design**: Components use `@angular/material` for consistent UI

### Cross-Cutting
- **Environment variables**: Frontend at `environments/`, backend at `application-*.yml`
- **Error handling**: Backend throws custom exceptions with `@ExceptionHandler` in `GlobalExceptionHandler`; frontend handles via interceptor

## External Integrations

- **Stripe**: Payment processing via `StripeService` (Kotlin backend)
- **Aliyun OSS**: Cloud storage for media via `AliyunOssService`
- **WeChat OAuth2**: OAuth2 client registration + custom resolver in `config/ReactiveWeChatAuthorizationRequestResolver.kt`
- **Referral System**: `ReferralRepository` + `ReferralRewardRepository` for user incentive tracking

## Testing

- **Backend**: JUnit 5 + Spring Security test + Reactor test; use `StepVerifier` for Mono/Flux assertions
- **Frontend**: Karma test runner; import Material modules in test setup
- **No E2E tests** currently configured; consider adding Cypress or Playwright

## Common Gotchas

1. **Blocking calls in reactive code**: Never call `.block()` on Mono in production services
2. **CORS configuration**: Check `CorsConfig.kt` if frontend cannot reach backend
3. **JWT secret**: Stored in `application-secret.yml`; must be injected via `@Value`
4. **Cart sync**: Ensure both localStorage and backend are updated; see `CartStore` sync logic
5. **Thymeleaf + REST**: Admin views use Thymeleaf; public API is JSON-only REST at `/api`

## File Organization Summary

```
nightgrass/
├── rest/             → REST controllers for public APIs
├── web/              → Thymeleaf controllers for admin/legal
├── service/          → Service interfaces
├── serviceImpl/       → Service implementations
├── repo/             → R2DBC repositories
├── config/           → Security, JWT, CORS config
├── dto/              → Data transfer objects
└── resources/        → SQL, YAML config, static assets

yaojia-ng/src/app/
├── service/          → HTTP services (inject HttpClient)
├── auth/             → Authentication & JWT logic
├── cart/             → Cart state (signals) & service
├── guards/           → Route guards (AuthGuard)
├── model/            → TypeScript interfaces
└── [feature]/        → Component folders (standalone components)
```

---
**Last Updated**: 2025-11-22 | **Branch**: eagle | **Team**: Verify sections for accuracy before major refactors
