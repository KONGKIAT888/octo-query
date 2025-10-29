# 🧩 OctoQuery – Changelog

## v25.4.3

- **New:** Added DTO Constructor Support in Generate Projection Interface — Now supports generating Java interfaces from DTO constructors without requiring `AS` aliases.
  - Automatically extracts field names from `SELECT new ClassName(field1, field2, ...)` syntax
  - Handles complex field expressions like `tab1.field1` → `getField1()`
  - Maintains full compatibility with existing `AS` alias-based interface generation
- **Enhanced:** Improved field extraction algorithm to handle nested parentheses and complex expressions in DTO constructors
- **Fixed:** Resolved "No 'AS' found in the selected SQL SELECT list" error when working with DTO constructors
- **Improved:** Better handling of string literals and function calls in parameter parsing
- **Documentation:** Added comprehensive code comments and JavaDoc documentation for better maintainability

---

## v25.4.2 

- **New:** Added context menu integration — you can now right-click inside `@Query` or `@NativeQuery` annotations and select **“Format SQL Query”** directly from the editor.  
  _(No more memorizing shortcuts — just right-click and format instantly.)_
- **New:** Added support for formatting standalone `.sql` files — OctoQuery now works seamlessly inside SQL editors as well.
- Improved detection for `@Query` and `@NativeQuery` blocks for more accurate formatting.
- Enhanced DTO constructor formatting (`SELECT new ...`) for cleaner indentation and readability.
- Integrated background listener — automatically reformats SQL after document commits or when saving files.
- Manual action shortcut: **Ctrl + Alt + L** (Format JPA Query).
- Fixed inconsistent indentation when used together with IntelliJ’s built-in formatter.

---

## v25.4.1 

- Added automatic SQL reformat after IntelliJ reformat (**Ctrl + Alt + L**).
- Improved detection for `@Query` and `@NativeQuery` blocks.
- Enhanced DTO constructor formatting (`SELECT new ...`) for cleaner indentation.
- Integrated background listener — triggers reformat on save and after document commit.
- Added a manual action shortcut: **Alt + Shift + F** (Format JPA Query).
- Fixed inconsistent indentation when using IntelliJ’s built-in formatter.

---

### Summary
`OctoQuery` now supports both **annotation-based** and **standalone SQL** formatting —  
accessible via **shortcuts**, **menu actions**, or **right-click context menus**.  
Faster, smarter, and more consistent than ever.
