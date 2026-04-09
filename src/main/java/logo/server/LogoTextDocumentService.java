package logo.server;

import logo.analysis.DocumentState;
import logo.features.SemanticTokensHandler;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.LanguageClient;
import java.util.List;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class LogoTextDocumentService implements TextDocumentService {

    private final Map<String, DocumentState> documents = new ConcurrentHashMap<>();
    private LanguageClient client;

    public void setClient(LanguageClient client) {
        this.client = client;
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        String uri  = params.getTextDocument().getUri();
        String text = params.getTextDocument().getText();
        documents.put(uri, DocumentState.parse(uri, text));
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        String uri  = params.getTextDocument().getUri();
        String text = params.getContentChanges().get(0).getText();
        documents.put(uri, DocumentState.parse(uri, text));
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
