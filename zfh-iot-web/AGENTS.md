# Agent Instructions for ZFH IoT Platform

This is a full-stack IoT platform with a Vue 3 frontend and Spring Boot backend.

## Project Structure

```
zfh-iot/
├── zfh-iot-frontend/     # Vue 3 + Vite frontend
└── zfh-iot-backend/      # Spring Boot 3.2 + Java 17 backend
```

## Build Commands

### Frontend (`zfh-iot-frontend/`)
```bash
npm install       # Install dependencies
npm run dev       # Development server (port 3000)
npm run build     # Production build (outputs to dist/)
npm run preview   # Preview production build
```

### Backend (`zfh-iot-backend/`)
```bash
mvn spring-boot:run     # Run development server (port 8080)
mvn package             # Build JAR (outputs to target/)
mvn test                # Run all tests
mvn test -Dtest=ClassName#methodName    # Run single test
mvn clean compile       # Clean and compile
```

## Code Style Guidelines

### Frontend (Vue 3)

**Framework & Style:**
- Vue 3 with Composition API and `<script setup>` syntax
- Element Plus for UI components
- Pinia for state management (using Composition API style)
- Vue Router for navigation
- Axios for HTTP requests

**Imports:**
- Use `@/` alias for src directory imports
- Group imports: Vue core → third-party → project aliases
- Example:
```javascript
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'
```

**Naming Conventions:**
- Components: PascalCase (e.g., `UserLogin.vue`)
- Composables: camelCase starting with `use` (e.g., `useAuth.js`)
- Stores: camelCase ending with `Store` (e.g., `userStore`)
- API functions: camelCase (e.g., `getUserInfo`)
- Vue files: PascalCase matching component name

**Component Structure:**
```vue
<template>
  <!-- template content -->
</template>

<script setup>
// imports
// reactive state
// computed properties
// methods
// lifecycle hooks
</script>

<style scoped lang="scss">
/* component styles */
</style>
```

**API Pattern:**
- Place API calls in `src/api/` directory
- Export functions that return axios promises
- Use request utility from `@/utils/request`

**State Management (Pinia):**
- Use Composition API style with `defineStore`
- Store files in `src/stores/`
- Export store as `useXxxStore`

**Error Handling:**
- Use Element Plus `ElMessage` for user feedback
- Handle async errors with try/catch
- 401 responses trigger automatic logout via request interceptor

### Backend (Spring Boot)

**Framework & Style:**
- Spring Boot 3.2 with Java 17
- MyBatis Plus for ORM
- Shiro for security/authorization
- Lombok for boilerplate reduction
- JWT for authentication

**Package Structure:**
```
com.zfh.iot/
├── common/           # Shared utilities, exceptions, result wrappers
├── config/           # Configuration classes
└── modules/          # Business modules
    └── {module}/
        ├── controller/   # REST controllers
        ├── service/      # Business logic (interface + impl)
        ├── mapper/       # MyBatis mappers
        └── entity/       # Entity classes
```

**Naming Conventions:**
- Classes: PascalCase
- Methods/variables: camelCase
- Constants: UPPER_SNAKE_CASE
- Controllers: suffixed with `Controller`
- Services: Interface with `Service`, implementation with `ServiceImpl`
- Entities: prefixed with domain (e.g., `IotGateway`)

**Controller Pattern:**
- Use `@RestController` and `@RequestMapping`
- Constructor injection with `@RequiredArgsConstructor`
- Return `Result<T>` wrapper for all responses
- Use `@RequiresPermissions` for authorization
- Use standard HTTP methods: GET (list/retrieve), POST (create), PUT (update), DELETE (delete)

**Entity Pattern:**
- Use Lombok `@Data` annotation
- Use MyBatis Plus annotations (`@TableName`, `@TableId`, `@TableLogic`)
- Use `LocalDateTime` for timestamps
- Include `deleted` field with `@TableLogic` for soft delete

**API Response:**
- Always wrap responses in `Result<T>`
- Use `Result.success(data)` or `Result.error(message)`

**Error Handling:**
- Business exceptions extend `BusinessException`
- Global exception handler in `GlobalExceptionHandler`
- Use appropriate HTTP status codes

## Key Libraries

### Frontend
- vue@^3.4, vue-router@^4, pinia@^2
- element-plus@^2.5 (UI components)
- axios@^1.6 (HTTP client)
- echarts@^5.4 (charts)
- dayjs@^1.11 (date handling)
- js-cookie@^3 (cookie management)

### Backend
- spring-boot-starter-web@3.2.0
- mybatis-plus-boot-starter@3.5.5
- shiro-spring-boot-web-starter@1.12.0
- jjwt@0.12.3 (JWT handling)
- druid-spring-boot-starter@1.2.20 (connection pool)
- taos-jdbcdriver@3.2.7 (TDengine)
- lombok (boilerplate reduction)

## Development Workflow

1. Backend runs on port 8080
2. Frontend dev server runs on port 3000 with API proxy to backend
3. Frontend uses `/api/v1` base path for all API calls
4. Authentication via JWT token stored in cookies
