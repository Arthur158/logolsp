package logo.server;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LogoLanguageServer implements LanguageServer, LanguageClientAware {

    private final LogoTextDocumentService textDocumentService = new LogoTextDocumentService();
    private LanguageClient client;

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        ServerCapabilities caps = new ServerCapabilities();
        CodeActionOptions codeActionOptions = new CodeActionOptions();
        codeActionOptions.setCodeActionKinds(List.of(CodeActionKind.QuickFix));
        caps.setCodeActionProvider(codeActionOptions);
        caps.setTextDocumentSync(TextDocumentSyncKind.Full);
        caps.setDefinitionProvider(true);

        SemanticTokensWithRegistrationOptions semanticOpts = new SemanticTokensWithRegistrationOptions();
        semanticOpts.setFull(Either.forLeft(true));
        semanticOpts.setLegend(new SemanticTokensLegend(
            List.of("keyword", "function", "variable", "number", "string", "comment"),
            List.of()
        ));
        caps.setSemanticTokensProvider(semanticOpts);

        return CompletableFuture.completedFuture(new InitializeResult(caps));
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit() {
        System.exit(0);
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return textDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return new LogoWorkspaceService();
    }

    @Override
    public void connect(LanguageClient client) {
        this.client = client;
        textDocumentService.setClient(client);
    }
}
