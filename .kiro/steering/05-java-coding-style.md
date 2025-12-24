---
name: java-coding-style
description: Enforces Java coding standards including naming conventions, formatting, imports, and file structure. Use when writing, refactoring, or reviewing Java code for the Kiro project.
temperature: 0.0
---

## 1. Source File Structure & Basics

* **File Naming:** The filename must match the case-sensitive name of the single top-level class with a `.java` extension.
* **Encoding:** All files must be encoded in **UTF-8**.
* **Whitespace:** * Use the ASCII horizontal space character (0x20) only.
* **No Tabs:** Tab characters are strictly forbidden for indentation.


* **File Order:** Sections must appear in this order, separated by exactly one blank line:

  1. License/Copyright
  2. Package declaration
  3. Import statements
  4. Exactly one top-level class

## 2. Imports & Modules

* **No Wildcards:** Never use wildcard (`*`) imports, including static ones.
* **Static Imports:** * Place all static imports in a single block before non-static imports.
* Do **not** use static imports for static nested classes; use normal imports.

* **Ordering:** Within a group (static vs. non-static), names must be in ASCII sort order.
* **Module Directives:** Follow the strict order: `requires` → `exports` → `opens` → `uses` → `provides`.

## 3. Formatting & Braces

* **Indentation:**
* **Block Indent:** +2 spaces for every new block.
* **Continuation Indent:** +4 spaces (minimum) for line-wrapping.

* **Braces:**

  * **Always Use Braces:** Required for `if`, `else`, `for`, `do`, and `while`, even if the body is empty or a single line.
  * **K&R Style:** No line break before `{`; line break after `{`; line break before `}`.
  * **Column Limit:** Code must not exceed **100 characters** per line.
  * **Line-wrapping:** Prefer breaking at the highest syntactic level.

## 4. Naming Conventions

Identifiers must use only ASCII letters, digits, and underscores (no `mPrefix` or `s_suffix`).

| Entity | Style | Example |
| --- | --- | --- |
| **Packages / Modules** | lowercase (no underscores) | `com.google.example` |
| **Classes / Records** | UpperCamelCase | `DataProcessor` |
| **Methods** | lowerCamelCase | `calculateTotal()` |
| **Constants** | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| **Parameters / Fields** | lowerCamelCase | `userName` |

## 5. Programming Practices

* **Method Overloading:** Keep overloaded methods/constructors in a contiguous group; do not split them with other members.
* **Annotations:** The `@Override` annotation must be used whenever it is legal.
* **Exception Handling:** Caught exceptions must **never** be ignored (empty catch blocks) without a significant, documented reason.
* **Javadoc:**
  * Must start with a brief **summary fragment** (noun or verb phrase), not a full sentence.
  * Follow standard block formatting with properly wrapped text.