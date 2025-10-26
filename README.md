
# 🐙 OctoQuery — JPA @Query Formatter for IntelliJ IDEA

> A lightweight IntelliJ plugin that automatically formats SQL queries inside Spring Data JPA `@Query` annotations.

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

---

## ⚙️ Features

- 🧠 **Auto-detects** SQL inside `@Query`, `@NativeQuery`, and repository methods
- 💅 **Formats & beautifies** inline SQL with consistent indentation and alignment
- 🗄️ **Supports multiple dialects** — MySQL, PostgreSQL, Oracle, SQL Server, etc.
- ⚡ **Preserves parameters and placeholders** (`:param`, `?1`, etc.)
- 🌱 **Integrates seamlessly** with Spring Data JPA projects
- 🧩 Works directly with your editor shortcut (default: `Alt + Shift + F`)

---

## ⌨️ Usage

1. Install **OctoQuery** from the [JetBrains Marketplace](https://plugins.jetbrains.com)
2. Open any Spring Data JPA repository file
3. Place your cursor inside an `@Query` string
4. Press **`Ctrl + Alt + L`**
5. Enjoy your neatly formatted query ✨

---

## 🧰 Configuration (optional)

OctoQuery automatically detects SQL dialects,  
but you can tweak behavior through **Settings → Tools → OctoQuery** (coming soon).

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

To build and run locally:

```bash
./gradlew buildPlugin
./gradlew runIde
```

Plugin ID: `me.kongkiat.octoquery`  
Root project: `octoquery`

---

## 🪶 License

MIT © 2025 [Kongkiat](https://github.com/KONGKIAT888/octo-query/blob/main/LICENSE)

---

> “Format your @Query — beautifully, intelligently, and effortlessly.” 🐙💜
