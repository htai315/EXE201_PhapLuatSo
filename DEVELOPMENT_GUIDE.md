# 👨‍💻 Development Guide

Hướng dẫn phát triển và đóng góp cho dự án Pháp Luật Số.

## 🎯 Development Workflow

### 1. Setup Development Environment
```bash
# Clone repository
git clone <repository-url>
cd EXE201_PhapLuatSo

# Create .env file
copy .env.example .env

# Install dependencies
mvn clean install

# Run application
mvn spring-boot:run
```

### 2. Create Feature Branch
```bash
git checkout -b feature/your-feature-name
```

### 3. Make Changes
- Write clean code
- Follow coding standards
- Add comments
- Write tests (if applicable)

### 4. Test Locally
```bash
# Run application
mvn spring-boot:run

# Test in browser
http://localhost:8080
```

### 5. Commit Changes
```bash
git add .
git commit -m "feat: add your feature description"
```

### 6. Push & Create PR
```bash
git push origin feature/your-feature-name
```

---

## 📝 Coding Standards

### Java Code Style

#### Naming Conventions
```java
// Classes: PascalCase
public class UserService { }

// Methods: camelCase
public void createUser() { }

// Variables: camelCase
private String userName;

// Constants: UPPER_SNAKE_CASE
private static final String API_KEY = "...";

// Packages: lowercase
package com.htai.exe201phapluatso.auth;
```

#### Code Organization
```java
@Service
public class UserService {
    // 1. Constants
    private static final int MAX_ATTEMPTS = 3;
    
    // 2. Dependencies (injected)
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    
    // 3. Constructor
    public UserService(UserRepo userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }
    
    // 4. Public methods
    public User createUser(CreateUserRequest req) {
        // Implementation
    }
    
    // 5. Private helper methods
    private void validateUser(User user) {
        // Implementation
    }
}
```

#### Comments
```java
/**
 * Create a new user account
 * 
 * @param req User registration request
 * @return Created user
 * @throws BadRequestException if email already exists
 */
public User createUser(CreateUserRequest req) {
    // Check if email exists
    if (userRepo.existsByEmail(req.getEmail())) {
        throw new BadRequestException("Email đã tồn tại");
    }
    
    // Hash password
    String hashedPassword = passwordEncoder.encode(req.getPassword());
    
    // Create user
    User user = new User();
    user.setEmail(req.getEmail());
    user.setPasswordHash(hashedPassword);
    
    return userRepo.save(user);
}
```

### JavaScript Code Style

#### Naming Conventions
```javascript
// Variables: camelCase
const userName = 'John';

// Functions: camelCase
function getUserData() { }

// Constants: UPPER_SNAKE_CASE
const API_BASE_URL = '/api';

// Classes: PascalCase
class UserManager { }
```

#### Code Organization
```javascript
// 1. Constants
const API_BASE = '/api/quiz-sets';

// 2. Global variables
let currentQuizId = null;

// 3. Event listeners
document.addEventListener('DOMContentLoaded', () => {
    initializePage();
});

// 4. Main functions
async function loadQuizzes() {
    // Implementation
}

// 5. Helper functions
function formatDate(date) {
    // Implementation
}
```

#### Async/Await
```javascript
// Good: Use async/await
async function loadData() {
    try {
        const response = await API_CLIENT.get('/api/data');
        const data = await response.json();
        return data;
    } catch (error) {
        console.error('Error:', error);
        Toast.error(error.message);
    }
}

// Avoid: Promise chains
function loadData() {
    return API_CLIENT.get('/api/data')
        .then(response => response.json())
        .then(data => data)
        .catch(error => console.error(error));
}
```

### CSS Code Style

#### Organization
```css
/* 1. Variables */
:root {
    --primary-color: #1a4b84;
    --secondary-color: #2563eb;
}

/* 2. Reset/Base styles */
* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
}

/* 3. Layout */
.container {
    max-width: 1200px;
    margin: 0 auto;
}

/* 4. Components */
.btn-primary {
    background: var(--primary-color);
    color: white;
}

/* 5. Utilities */
.text-center {
    text-align: center;
}

/* 6. Media queries */
@media (max-width: 768px) {
    .container {
        padding: 1rem;
    }
}
```

---

## 🏗️ Project Structure

### Backend Structure
```
src/main/java/com/htai/exe201phapluatso/
├── ai/                     # AI services
│   ├── controller/
│   ├── service/
│   └── dto/
├── auth/                   # Authentication
│   ├── controller/
│   ├── service/
│   ├── entity/
│   ├── repo/
│   ├── dto/
│   ├── security/
│   └── oauth2/
├── common/                 # Shared code
│   ├── exception/
│   └── GlobalExceptionHandler.java
├── config/                 # Configuration
│   ├── SecurityConfig.java
│   ├── CorsConfig.java
│   └── DotEnvEnvironmentPostProcessor.java
├── credit/                 # Credit system
│   ├── controller/
│   ├── service/
│   ├── entity/
│   └── repo/
├── legal/                  # Legal features
│   ├── controller/
│   ├── service/
│   ├── entity/
│   ├── repo/
│   └── config/
├── payment/                # Payment
│   ├── controller/
│   ├── service/
│   ├── entity/
│   ├── repo/
│   └── util/
└── quiz/                   # Quiz management
    ├── controller/
    ├── service/
    ├── entity/
    ├── repo/
    └── dto/
```

### Frontend Structure
```
src/main/resources/static/
├── html/                   # HTML pages
│   ├── index.html
│   ├── login.html
│   ├── register.html
│   ├── quiz-*.html
│   ├── legal-*.html
│   └── ...
├── css/                    # Stylesheets
│   ├── common.css          # Shared styles
│   ├── quiz-*.css
│   ├── legal-*.css
│   └── ...
└── scripts/                # JavaScript
    ├── api-client.js       # API wrapper
    ├── error-handler.js    # Error handling
    ├── toast-notification.js
    ├── credits-counter.js
    └── ...
```

---

## 🔧 Common Development Tasks

### Add New API Endpoint

#### 1. Create DTO
```java
// src/main/java/.../dto/CreateQuizRequest.java
@Data
public class CreateQuizRequest {
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
}
```

#### 2. Add Service Method
```java
// src/main/java/.../service/QuizService.java
@Service
public class QuizService {
    @Transactional
    public QuizSet createQuiz(Long userId, CreateQuizRequest req) {
        QuizSet quiz = new QuizSet();
        quiz.setUserId(userId);
        quiz.setTitle(req.getTitle());
        quiz.setDescription(req.getDescription());
        return quizRepo.save(quiz);
    }
}
```

#### 3. Add Controller Endpoint
```java
// src/main/java/.../controller/QuizController.java
@RestController
@RequestMapping("/api/quiz-sets")
public class QuizController {
    
    @PostMapping
    public ResponseEntity<?> createQuiz(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateQuizRequest req) {
        
        QuizSet quiz = quizService.createQuiz(principal.getId(), req);
        return ResponseEntity.ok(quiz);
    }
}
```

#### 4. Test with Frontend
```javascript
// JavaScript
async function createQuiz() {
    try {
        const response = await API_CLIENT.post('/api/quiz-sets', {
            title: 'New Quiz',
            description: 'Description'
        });
        
        const quiz = await response.json();
        console.log('Created:', quiz);
    } catch (error) {
        Toast.error(error.message);
    }
}
```

### Add New Database Table

#### 1. Create Entity
```java
@Entity
@Table(name = "quiz_tags")
@Data
public class QuizTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
```

#### 2. Create Migration
```sql
-- src/main/resources/db/migration/V2__add_quiz_tags.sql
CREATE TABLE quiz_tags (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    created_at DATETIME2 DEFAULT GETDATE()
);

CREATE INDEX idx_quiz_tags_name ON quiz_tags(name);
```

#### 3. Create Repository
```java
public interface QuizTagRepo extends JpaRepository<QuizTag, Long> {
    Optional<QuizTag> findByName(String name);
}
```

### Add New Frontend Page

#### 1. Create HTML
```html
<!-- src/main/resources/static/html/new-page.html -->
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>New Page</title>
    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/new-page.css">
</head>
<body>
    <!-- Content -->
    
    <script src="/scripts/api-client.js"></script>
    <script src="/scripts/new-page.js"></script>
</body>
</html>
```

#### 2. Create CSS
```css
/* src/main/resources/static/css/new-page.css */
.page-container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 2rem;
}
```

#### 3. Create JavaScript
```javascript
// src/main/resources/static/scripts/new-page.js
document.addEventListener('DOMContentLoaded', () => {
    initializePage();
});

async function initializePage() {
    // Load data
    const data = await loadData();
    
    // Render UI
    renderData(data);
}
```

---

## 🧪 Testing

### Manual Testing Checklist

#### Authentication
- [ ] Register new user
- [ ] Login with email
- [ ] Login with Google
- [ ] Token refresh works
- [ ] Logout clears tokens

#### Quiz Features
- [ ] Create quiz set (free)
- [ ] Add questions
- [ ] Edit questions
- [ ] Delete questions
- [ ] Start exam
- [ ] Submit exam
- [ ] View results
- [ ] View history

#### AI Features
- [ ] AI chat (costs credit)
- [ ] AI quiz generation (costs credit)
- [ ] Error when no credits
- [ ] Credits deducted correctly

#### Payment
- [ ] Create payment
- [ ] Redirect to VNPay
- [ ] Pay with test card
- [ ] Return to success page
- [ ] Credits added

### Testing Tools

#### Postman
Import collection and test APIs:
```bash
# Import postman_collection.json
# Set environment variables
# Run tests
```

#### Browser DevTools
- Console: Check for errors
- Network: Monitor API calls
- Application: Check localStorage

---

## 🐛 Debugging

### Backend Debugging

#### Enable Debug Logging
```properties
# application.properties
logging.level.com.htai.exe201phapluatso=DEBUG
logging.level.org.springframework.web=DEBUG
```

#### IntelliJ Debugger
1. Set breakpoints
2. Run in Debug mode (Shift+F9)
3. Step through code

### Frontend Debugging

#### Console Logging
```javascript
console.log('Data:', data);
console.error('Error:', error);
console.table(array);
```

#### Network Tab
- Check request/response
- Check status codes
- Check headers

---

## 📦 Dependencies Management

### Add New Maven Dependency
```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.example</groupId>
    <artifactId>example-lib</artifactId>
    <version>1.0.0</version>
</dependency>
```

Then run:
```bash
mvn clean install
```

### Update Dependencies
```bash
# Check for updates
mvn versions:display-dependency-updates

# Update specific dependency
mvn versions:use-latest-versions
```

---

## 🚀 Deployment

### Build for Production
```bash
# Build JAR
mvn clean package -DskipTests

# JAR location
target/exe201phapluatso-0.0.1-SNAPSHOT.jar
```

### Run in Production
```bash
# Set environment variables
export SPRING_PROFILES_ACTIVE=production
export DB_URL=...
export DB_USERNAME=...
export DB_PASSWORD=...

# Run JAR
java -jar target/exe201phapluatso-0.0.1-SNAPSHOT.jar
```

---

## 📚 Useful Commands

### Maven
```bash
mvn clean                    # Clean target folder
mvn compile                  # Compile code
mvn test                     # Run tests
mvn package                  # Build JAR
mvn spring-boot:run          # Run application
mvn dependency:tree          # Show dependencies
```

### Git
```bash
git status                   # Check status
git add .                    # Stage all changes
git commit -m "message"      # Commit
git push                     # Push to remote
git pull                     # Pull from remote
git checkout -b feature/x    # Create branch
git merge feature/x          # Merge branch
```

### Database
```sql
-- Check tables
SELECT * FROM INFORMATION_SCHEMA.TABLES;

-- Check migrations
SELECT * FROM flyway_schema_history;

-- Reset database (DANGER!)
DROP DATABASE phapluatso;
CREATE DATABASE phapluatso;
```

---

## 🤝 Contributing Guidelines

### Before Submitting PR
- [ ] Code follows style guide
- [ ] No console.log in production code
- [ ] No commented-out code
- [ ] All files properly formatted
- [ ] Tested locally
- [ ] No merge conflicts

### PR Description Template
```markdown
## What
Brief description of changes

## Why
Reason for changes

## How
Technical details

## Testing
How to test the changes

## Screenshots
(if UI changes)
```

---

## 📞 Need Help?

- Check documentation first
- Search existing issues
- Ask in team chat
- Create new issue

Happy coding! 🚀
