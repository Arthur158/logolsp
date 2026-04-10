# Logo LSP

A Language Server Protocol (LSP) implementation for the LOGO programming language, built with Java, ANTLR4, and LSP4J.

---

## Features

- **Syntax highlighting** — keywords, functions, variables, numbers, strings and comments each rendered in distinct colors
- **Go-to-declaration** — jump to the declaration of any procedure or variable, scoped with function closure and position of the declaration
- **Diagnostics** — red underlines on calls to undefined procedures and references to undeclared variables
- **Code actions** — quick fix suggestions that replace a mistyped name with the lexically closest declared name
- **Signature change** — simple signature change feature: change the order of the parameters in a procedure declaration, and automatically change order in calls to that procedure

---

## Project Structure

```
src/main/java/logo/
├── Main.java
├── server/
│   ├── LogoLanguageServer.java
│   ├── LogoTextDocumentService.java
│   └── LogoWorkspaceService.java
├── analysis/
│   ├── DocumentState.java
│   ├── SymbolTable.java
│   ├── SymbolTableBuilder.java
│   └── EditDistance.java
└── features/
    ├── SemanticTokensHandler.java
    ├── DefinitionHandler.java
    ├── DiagnosticsHandler.java
    └── CodeActionHandler.java

src/main/antlr/logo/parser/
└── Logo.g4
```

---

## What Each File Does

### `Main.java`
Entry point. Launches the LSP server over **stdio**, which is how LSP clients like LSP4IJ communicate with it. Wires together the server and the LSP4J launcher. All communication happens over stdin/stdout as JSON-RPC — nothing should ever be printed to stdout directly as it would corrupt the protocol.

### `server/LogoLanguageServer.java`
The top-level server class. Implements LSP4J's `LanguageServer` interface. Responds to the `initialize` handshake and tells the client what features are supported: semantic tokens for syntax highlighting, goto-definition, diagnostics, and code actions. Declares the semantic token legend — the list of token type names (`keyword`, `function`, `variable`, `number`, `string`, `comment`) that map indices in the token data to colors in the editor.

### `server/LogoTextDocumentService.java`
Handles all `textDocument/*` requests from the client. Listens for file open/change/close events, re-parses the document each time via `parseAndPublish`, and stores the result in a map keyed by file URI. On every parse it pushes fresh diagnostics to the client. Delegates feature requests to the appropriate handler classes.

### `server/LogoWorkspaceService.java`
Stub implementation of LSP4J's `WorkspaceService`. Required by the interface but not used in the current implementation.

### `analysis/DocumentState.java`
The core parsing class. Takes raw document text, runs it through the ANTLR-generated `LogoLexer` and `LogoParser`, builds a parse tree, then runs `SymbolTableBuilder` over it to collect all declarations and references. Stores the token stream, parse tree, and symbol table together. Re-created from scratch on every document change. It is also responsible for giving the client parsing errors (gathered at parseAndPublish in `server/LogoTextDocumentService.java`)

### `analysis/SymbolTable.java`
Stores all procedure and variable declarations found in the document, each mapped by name to the `Range` (line + column) where they were declared. Also stores all procedure call references and variable references as `SymbolReference` records. Provides lookup methods used by goto-definition, diagnostics, and code actions. For variable, can check if the variable was declared at a given point (where a reference would be) by using procedure scope information and order of declarations/references

### `analysis/SymbolTableBuilder.java`
Extends ANTLR's `LogoBaseVisitor` and walks the parse tree to populate the `SymbolTable`. Visits `procedureDef` nodes to record procedure declarations, `param` nodes to record procedure parameters as variable declarations, `makeStmt`/`localMakeStmt` nodes to record variable assignments, `procedureCall` nodes to record call references, and `variable` nodes to record variable use references.

### `analysis/EditDistance.java`
Utility class that computes the Levenshtein edit distance between two strings. Used by `CodeActionHandler` to find the closest declared name to a mistyped one. Only suggests a correction if the distance is 3 or fewer — beyond that the names are too different to be a likely typo.

### `features/SemanticTokensHandler.java`
Produces the semantic token data sent to the client for syntax highlighting. Walks every token in the ANTLR token stream and encodes them in the LSP delta-encoded integer format (5 integers per token: delta line, delta column, length, token type index, modifiers). Assigns token types based on the ANTLR token type. Variable references (`:name`) are detected by checking if the previous token was a colon.

### `features/DefinitionHandler.java`
Handles goto-definition. Given a cursor position, finds which token the cursor is on by scanning the token stream. If the previous token was `:`, treats it as a variable reference and looks it up in the symbol table. Otherwise tries to find it as a procedure name first, then as a variable. Returns a `Location` pointing to the declaration range in the same file.

### `features/DiagnosticsHandler.java`
Computes diagnostics by cross-referencing the symbol table. Checks every procedure call reference against declared procedures and every variable reference against declared variables. Unknown built-in commands (like `forward`, `repeat`, `right` etc.) are excluded via a hardcoded set so they are not incorrectly flagged. Returns a list of `Diagnostic` objects with error severity and a descriptive message.

### `features/CodeActionHandler.java`
Handles quick fix suggestions. When the client requests code actions for a range containing a diagnostic, this handler reads the diagnostic message to extract the mistyped name, then uses `EditDistance.findClosest` to find the nearest declared name. If a close enough match exists it returns a `CodeAction` of kind `QuickFix` containing a `TextEdit` that replaces the bad token with the correct one.

### `src/main/antlr/logo/parser/Logo.g4`
The ANTLR4 grammar for the LOGO language. Defines lexer rules (keywords, identifiers, numbers, symbols) and parser rules (procedure definitions, repeat/if/while/for loops, expressions, variable references). Covers the full Turtle Academy command set including `FOREVER`, `WHILE`, `UNTIL`, `DO.WHILE`, `DO.UNTIL`, `FOR`, `MAKE`, `LOCALMAKE`, `OUTPUT`, and `STOP`. ANTLR generates `LogoLexer.java`, `LogoParser.java`, and `LogoBaseVisitor.java` from this file at build time — these are never edited directly.

---

## Building

Requires Java 21 and Gradle (or use the included wrapper).

```bash
# Generate ANTLR sources
./gradlew generateGrammarSource

# Build the fat JAR (includes all dependencies)
./gradlew shadowJar
```

Output: `build/libs/logo-lsp.jar`

---

## Connecting to LSP4IJ (IntelliJ)

1. Install the **LSP4IJ** plugin via `Settings → Plugins → Marketplace`
2. Go to `Settings → Languages & Frameworks → Language Servers`
3. Click `+` to add a new server
4. Set **Name** to `Logo`
5. Under the **Server** tab set **Command** to:
   ```
   java -jar /absolute/path/to/build/libs/logo-lsp.jar
   ```
6. Under the **Mappings** tab click `+`, choose **File name patterns**, and add `*.logo`
7. Click **OK** and open any `.logo` file

The server starts automatically when a `.logo` file is opened.

### Restarting after a rebuild

The server does not auto-update when the JAR changes. After running `./gradlew shadowJar`, restart the server from the LSP4IJ tool window in the bottom bar of IntelliJ.

---

## Testing Goto-Definition

Open a `.logo` file, click on a procedure call or variable reference, and press **Ctrl+B** (or right-click → Go To → Declaration).

```logo
to square :size
  repeat 4 [forward :size right 90]
end

make "mySize 50

square :mySize
```

- Click `square` on the last line → jumps to `to square :size`
- Click `:mySize` → jumps to `make "mySize 50`
- Click `:size` inside the body → jumps to `:size` in the parameter list

---

## Testing Diagnostics and Code Actions

```logo
to square :size
  repeat 4 [forward :size right 90]
end

make "mySize 50

; typo in procedure name
triange 50

; typo in variable name
forward :mySoze
```

- `triange` gets a red underline — "Undefined procedure: triange"
- `:mySoze` gets a red underline — "Undefined variable: mySoze"
- Click the lightbulb (or press **Alt+Enter**) on either underline to see the quick fix suggestion
- Selecting the fix rewrites the token to the closest declared name

## Testing Signature Change

```logo
to mydraw :count :color :size
  repeat :count [forward :size setpc :color]
end

(mydraw 33 10 500)
mydraw 334 10 500
```

- `mydraw` gets 2 code actions: make color the first parameter or make size the first parameter
- Apply either
- The parameters change order, and each of the calls to mydraw also sees its arguments order change

## Small issue

Unfortunately, a decision I took early on, which is to make the following syntax rule:
```
procedureCall
    : name=IDENT expr*
    ;
```
Turned out to be a problem when doing several call in a row, where the second call and its arguments would all be seen as arguments to the first call. To solve this, one can surround the first call with parenthesis. For example:
```
to mydraw :count :color :size
  repeat :count [forward :size setpc :color]
end

mydraw 33 10 500
mydraw 334 10 500
```
Becomes:
```
to mydraw :count :color :size
  repeat :count [forward :size setpc :color]
end

(mydraw 33 10 500)
mydraw 334 10 500
```

---

## Changing Highlight Colors

Semantic token colors are controlled by your IntelliJ color scheme, not the server. To override them:

`Settings → Editor → Color Scheme → Language Defaults → Semantic token types`

Each token type name declared in the legend (`keyword`, `function`, `variable`, `number`, `string`, `comment`) can be assigned a custom foreground color there.
