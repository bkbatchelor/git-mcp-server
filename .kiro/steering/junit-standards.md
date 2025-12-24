---
name: junit-standards
description:
version: junit-5.12.2
temperature: 0.0
---
### Junit Directives 

#### Syntax Rules for Tags

* A tag must not be blank.
* A trimmed tag must not contain whitespace.
* A trimmed tag must not contain ISO control characters.
* A trimmed tag must not contain any of the following reserved characters.
  * ,: comma
  * (: left parenthesis
  * ): right parenthesis
  * &: ampersand
  * |: vertical bar
  * !: exclamation point

#### Hallucination Prevention

* If asked about a library you are unsure is compatible with {version}, explicitly state "Compatibility Unverified."
* Do NOT suggest features from other versions unless they are in the permanent API.
* If the user asks for information outside the provided context, you MUST use the flag `isOutOfScope: true`.
