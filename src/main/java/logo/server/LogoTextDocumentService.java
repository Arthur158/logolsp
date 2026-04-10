package logo.server;

import logo.analysis.DocumentState;
import logo.features.DefinitionHandler;
import logo.features.SemanticTokensHandler;
import logo.features.CodeActionHandler;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import logo.features.DiagnosticsHandler;

import org.eclipse.lsp4j.Diagnostic;
import java.util.ArrayList;

public class LogoTextDocumentService implements TextDocumentService {

    private final Map<String, DocumentState> documents = new ConcurrentHashMap<>();
    private LanguageClient client;

    public void setClient(LanguageClient client) {
        this.client = client;
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        parseAndPublish(
            params.getTextDocument().getUri(),
            params.getTextDocument().getText()
        );
    }

    @Override
    public CompletableFuture<List<Either<Command, CodeAction>>>
    codeAction(CodeActionParams params) {
        DocumentState doc = documents.get(params.getTextDocument().getUri());
        if (doc == null) return CompletableFuture.completedFuture(List.of());
        return CompletableFuture.completedFuture(
            CodeActionHandler.compute(doc, params)
        );
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        parseAndPublish(
            params.getTextDocument().getUri(),
            params.getContentChanges().get(0).getText()
        );
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>>
    definition(DefinitionParams params) {
        DocumentState doc = documents.get(params.getTextDocument().getUri());
        if (doc == null) return CompletableFuture.completedFuture(Either.forLeft(List.of()));

        Location loc = DefinitionHandler.findDefinition(doc, params.getPosition());
        if (loc == null) return CompletableFuture.completedFuture(Either.forLeft(List.of()));

        return CompletableFuture.completedFuture(Either.forLeft(List.of(loc)));
    }

    // private void parseAndPublish(String uri, String text) {
    //     DocumentState doc = DocumentState.parse(uri, text);
    //     documents.put(uri, doc);
    //
    //     DocumentState.log("=== Parsed: " + uri);
    //     DocumentState.log(doc.printTree());
    //
    //     List<Diagnostic> diagnostics = DiagnosticsHandler.compute(doc);
    //     DocumentState.log("Diagnostics: " + diagnostics.size());
    //     client.publishDiagnostics(new PublishDiagnosticsParams(uri, diagnostics));
    // }

    private void parseAndPublish(String uri, String text) {
        DocumentState doc = DocumentState.parse(uri, text);
        documents.put(uri, doc);

        List<Diagnostic> diagnostics = new ArrayList<>();
        diagnostics.addAll(doc.parseErrors);                  // syntax errors
        diagnostics.addAll(DiagnosticsHandler.compute(doc));  // undefined symbols

        client.publishDiagnostics(new PublishDiagnosticsParams(uri, diagnostics));
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        documents.remove(params.getTextDocument().getUri());
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {}

    @Override
    public CompletableFuture<SemanticTokens> semanticTokensFull(SemanticTokensParams params) {
        DocumentState doc = documents.get(params.getTextDocument().getUri());
        if (doc == null) return CompletableFuture.completedFuture(new SemanticTokens(List.of()));
        return CompletableFuture.completedFuture(
            SemanticTokensHandler.computeTokens(doc)
        );
    }
}
