# Logo LSP

A minimal Language Server Protocol (LSP) implementation for the LOGO programming language, built with Java, ANTLR4, and LSP4J.

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
│   └── SymbolTableBuilder.java
└── features/
    └── SemanticTokensHandler.java

src/main/antlr/logo/parser/
└── Logo.g4
```

---

## What Each File Does

### `Main.java`
Entry point. Launches the LSP server over **stdio**, which is how LSP clients like LSP4IJ communicate with it. Wires together the server and the LSP4J launcher.

### `server/LogoLanguageServer.java`
The top-level server class. Implements LSP4J's `LanguageServer` interface. Responds to the `initialize` handshake and tells the client what features are supported (currently: semantic tokens for syntax highlighting). Also holds a reference to the language client for sending notifications back.

### `server/LogoTextDocumentService.java`
Handles all `textDocument/*` requests from the client. Listens for file open/change/close events, re-parses the document each time, and stores the result in a map keyed by file URI. Handles the `textDocument/semanticTokens/full` request by delegating to `SemanticTokensHandler`.

### `server/LogoWorkspaceService.java`
Stub implementation of LSP4J's `WorkspaceService`. Required by the interface but not used yet. Will eventually handle workspace-wide events like watched file changes.

### `analysis/DocumentState.java`
The core parsing class. Takes raw document text, runs it through the ANTLR-generated `LogoLexer` and `LogoParser`, and stores the resulting parse tree and token stream. Re-created from scratch on every document change.

### `analysis/SymbolTable.java`
Currently a stub. Will eventually store all procedure and variable declarations found in the document, along with their positions, to support features like goto-definition and hover.

### `analysis/SymbolTableBuilder.java`
Currently a stub. Will extend `LogoBaseVisitor` and walk the ANTLR parse tree to populate the `SymbolTable` with declarations and references.

### `features/SemanticTokensHandler.java`
Produces the semantic token data sent to the client for syntax highlighting. Walks every token in the ANTLR token stream and encodes them in the LSP delta-encoded integer format (5 integers per token: delta line, delta column, length, token type index, modifiers). Currently marks every token as type `keyword` (index 0).

### `src/main/antlr/logo/parser/Logo.g4`
The ANTLR4 grammar for the LOGO language. Defines the lexer rules (keywords, identifiers, numbers, symbols) and parser rules (procedure definitions, repeat/if/while loops, expressions, variable references). ANTLR generates `LogoLexer.java`, `LogoParser.java`, and `LogoBaseVisitor.java` from this file at build time.

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
5. Under the **Server** tab, set **Command** to:
   ```
   java -jar /absolute/path/to/build/libs/logo-lsp.jar
   ```
6. Under the **Mappings** tab, click `+`, choose **File name patterns**, and add `*.logo`
7. Click **OK** and open any `.logo` file

The server starts automatically when a `.logo` file is opened.

### Restarting after a rebuild

The server does not auto-update when the JAR changes. After running `./gradlew shadowJar`, restart the server from the LSP4IJ tool window in the bottom bar.

---

## Changing the Highlight Color

Semantic token colors are controlled by your IntelliJ color scheme, not the LSP server itself. To change the color of highlighted tokens:

`Settings → Editor → Color Scheme → Language Defaults → Semantic token types → keyword`

Set the foreground color to whatever you want. This affects all files using the `keyword` semantic token type, so you may want to scope it to Logo specifically once more token types are added.
