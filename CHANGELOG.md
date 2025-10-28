# 🧩 OctoQuery – Changelog

## v25.4.2 (2025-10-28)
**Major Improvements**

- **New:** Added context menu integration — you can now right-click inside `@Query` or `@NativeQuery` annotations and select **“Format SQL Query”** directly from the editor.  
  _(No more memorizing shortcuts — just right-click and format instantly.)_

- **New:** Added support for formatting standalone `.sql` files — OctoQuery now works seamlessly inside SQL editors as well.

- Improved detection for `@Query` and `@NativeQuery` blocks for more accurate formatting.

- Enhanced DTO constructor formatting (`SELECT new ...`) for cleaner indentation and readability.

- Integrated background listener — automatically reformats SQL after document commits or when saving files.

- Manual action shortcut: **Ctrl + Alt + L** (Format JPA Query).

- Fixed inconsistent indentation when used together with IntelliJ’s built-in formatter.

---

## v25.4.1 (2025-10-27)
**Improvements and Fixes**

- Added automatic SQL reformat after IntelliJ reformat (**Ctrl + Alt + L**).
- Improved detection for `@Query` and `@NativeQuery` blocks.
- Enhanced DTO constructor formatting (`SELECT new ...`) for cleaner indentation.
- Integrated background listener — triggers reformat on save and after document commit.
- Added manual action shortcut: **Alt + Shift + F** (Format JPA Query).
- Fixed inconsistent indentation when using IntelliJ’s built-in formatter.

---

### Summary
OctoQuery now supports both **annotation-based** and **standalone SQL** formatting —  
accessible via **shortcuts**, **menu actions**, or **right-click context menus**.  
Faster, smarter, and more consistent than ever.
