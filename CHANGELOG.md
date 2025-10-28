---

<!-- JetBrains Marketplace change notes -->
<ul>
  <li><b>New:</b> Added context menu integration — you can now right-click inside <code>@Query</code> or <code>@NativeQuery</code> annotations and select <b>"Format SQL Query"</b> directly from the editor.</li>
  <li><b>New:</b> Added support for formatting standalone <code>.sql</code> files — OctoQuery now works seamlessly inside SQL editors as well.</li>
  <li>Improved detection for <code>@Query</code> and <code>@NativeQuery</code> blocks for more accurate formatting.</li>
  <li>Enhanced DTO constructor formatting (<code>SELECT new ...</code>) for cleaner indentation and readability.</li>
  <li>Integrated background listener — automatically reformats SQL after document commits or when saving files.</li>
  <li>Manual action shortcut: <kbd>Ctrl</kbd> + <kbd>Alt</kbd> + <kbd>L</kbd> (Format JPA Query).</li>
  <li>Fixed inconsistent indentation when used together with IntelliJ’s built-in formatter.</li>
</ul>
<p><b>OctoQuery</b> now supports both annotation-based and standalone SQL formatting — accessible via shortcuts, menus, or right-click context actions.</p>

---

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
