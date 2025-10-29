
# 🐙 OctoQuery — JPA @Query Formatter & Projection Generator for IntelliJ IDEA

> A powerful IntelliJ plugin that automatically formats SQL queries inside Spring Data JPA `@Query` annotations and generates Java projection interfaces.

---

## ✨ Overview

**OctoQuery** helps you keep your inline SQL queries clean, readable, and beautifully formatted —
directly inside your IntelliJ editor. It detects any SQL written in your JPA repository methods and applies smart formatting rules.

Ideal for Spring developers who often write queries like this:

```java
@Query("SELECT u FROM User u WHERE u.email LIKE %:email% AND u.active = true ORDER BY u.createdDate DESC")
List<User> findByEmail(@Param("email") String email);
```

With OctoQuery, it becomes instantly readable:

```java
@Query("""
    SELECT u
    FROM User u
    WHERE u.email LIKE %:email%
      AND u.active = true
    ORDER BY u.createdDate DESC
""")
List<User> findByEmail(@Param("email") String email);
```

### 🏗️ DTO Constructor Support & Projection Generation

Generate Java projection interfaces directly from DTO constructors and SQL queries!

```java
@Query("""
    SELECT new com.example.UserDto(
        u.id,
        u.username,
        u.email,
        u.createdAt
    )
    FROM User u
    WHERE u.active = true
""")
List<UserDto> findActiveUsers();
```

With OctoQuery's **Generate Projection Interface** (Alt+Shift+P), it creates:

```java
public interface UserDto {
    Object getId();
    Object getUsername();
    Object getEmail();
    Object getCreatedAt();
}
```

---

## ⚙️ Features

- 🧠 **Auto-detects** SQL inside `@Query`, `@NativeQuery`, and repository methods
- 💅 **Formats & beautifies** inline SQL with consistent indentation and alignment
- 🏗️ **DTO Constructor Support** - Smart formatting for `SELECT new ClassName(...)` syntax
- 🎯 **Projection Interface Generation** - Generate Java interfaces from SQL queries (Alt+Shift+P)
- 🗄️ **Supports multiple dialects** — MySQL, PostgreSQL, Oracle, SQL Server, etc.
- ⚡ **Preserves parameters and placeholders** (`:param`, `?1`, etc.)
- 🌱 **Integrates seamlessly** with Spring Data JPA projects
- 🧩 **Multiple access methods** - Keyboard shortcuts, context menu, and automatic formatting
- 📝 **Smart field extraction** - Handles both `AS` aliases and DTO constructor fields

---

## ⌨️ Usage

### 📝 Formatting SQL Queries

1. Install **OctoQuery** from the [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/28845-octoquery)
2. Open any Spring Data JPA repository file
3. Place your cursor inside an `@Query` string
4. Press **`Ctrl + Alt + L`** or right-click → **"Format SQL Query"**
5. Enjoy your neatly formatted query ✨

### 🎯 Generating Projection Interfaces

1. **Select SQL text** in your editor (including DTO constructor or regular SELECT with AS aliases)
2. Press **`Alt + Shift + P`** or right-click → **"Generate Projection Interface from SQL"**
3. **Enter interface name** (e.g., `UserResponse`, `BookDto`)
4. **Choose package/directory** where to create the interface
5. ✨ **Interface is automatically generated** with proper getter methods!

#### Supported Formats:
- **DTO Constructors**: `SELECT new com.example.Dto(field1, field2)`
- **AS Aliases**: `SELECT table.field1 AS field1, table.field2 AS field2`
- **Mixed**: Both formats in the same query

#### Example:
```java
// Select this SQL:
SELECT new com.example.payload.response.UserResponse(
        u.userId,
        u.username,
        u.email,
        u.active
        )
FROM Users u

// Generated Interface:
public interface UserResponse {
    Object getUserId();
    Object getUsername();
    Object getEmail();
    Object getActive();
}

```

### 🔄 Automatic Formatting

OctoQuery also automatically formats SQL when you:
- Use IntelliJ's code formatting (**Ctrl + Alt + L** on the whole file)
- Save files (if enabled in settings)
- Manually trigger formatting via the context menu

---

## ⌨️ Quick Reference

| Action | Shortcut | Description |
|--------|----------|-------------|
| **Format SQL Query** | `Ctrl + Alt + L` | Format SQL at cursor or all queries in file |
| **Generate Projection Interface** | `Alt + Shift + P` | Generate Java interface from selected SQL |
| **Context Menu** | Right-click | Access both formatting and projection generation |

---

## 🧰 Configuration (optional)

OctoQuery automatically detects SQL dialects and works out of the box.
Advanced configuration will be available in **Settings → Tools → OctoQuery** (coming soon).

---

## 📦 Installation

### From IntelliJ Marketplace
- Go to **Settings → Plugins → Marketplace**
- Search for **“OctoQuery”**
- Click **Install**

### Manual install
Download the latest `.zip` from the [Releases](https://github.com/your-username/octoquery/releases) page,  
then install it via **Settings → Plugins → Install Plugin from Disk...**

---

## 🐙 Why "OctoQuery"?

Because an octopus has eight arms — just like OctoQuery helps you handle every messy SQL statement at once 😄  
It’s small, fast, and smart — your friendly SQL formatter under the sea.

---

## 🧑‍💻 Development

### Local Development

To build and run locally:

```bash
./gradlew buildPlugin
./gradlew runIde
```

Plugin ID: `me.kongkiat.octoquery`
Root project: `octoquery`

### 🚀 Releasing a New Version

When releasing a new version, update the following files:

#### 1. Update Version Number
**File**: `build.gradle.kts`
```kotlin
version = "25.4.1"  // Increment this
```

#### 2. Update Changelog
**File**: `CHANGELOG.md`
```markdown
## v25.4.1

- **New:** Add new features here
- **Enhanced:** Improvements to existing features
- **Fixed:** Bug fixes
- **Documentation:** Documentation updates

---
```

#### 3. Update README (if needed)
**File**: `README.md`
- Add new features to the Features section
- Update usage examples if functionality changed
- **Note**: Avoid adding version-specific "What's New" sections - use CHANGELOG.md instead

#### 4. Build and Test
```bash
# Clean build
./gradlew clean build

# Test plugin
./gradlew runIde

# Build plugin package
./gradlew buildPlugin
```

#### 5. Release Process
1. Commit all changes
2. Create git tag: `git tag v25.4.3`
3. Push tag: `git push origin v25.4.3`
4. Upload plugin to JetBrains Marketplace
5. Create GitHub Release with built plugin `.zip`

### 📋 Release Checklist

- [ ] Update version in `build.gradle.kts`
- [ ] Add changelog entry in `CHANGELOG.md`
- [ ] Update README.md if features changed
- [ ] Run `./gradlew clean build` successfully
- [ ] Test plugin functionality
- [ ] Create git tag and push
- [ ] Upload to Marketplace
- [ ] Create GitHub Release

---

## 🪶 License

MIT © 2025 [Kongkiat](https://github.com/KONGKIAT888/octo-query/blob/main/LICENSE)

---

> “Format your @Query — beautifully, intelligently, and effortlessly.” 🐙💜
