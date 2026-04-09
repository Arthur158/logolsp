package logo;

import logo.server.LogoLanguageServer;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.jsonrpc.Launcher;

public class Main {
    public static void main(String[] args) throws Exception {
        LogoLanguageServer server = new LogoLanguageServer();
        Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(
            server, System.in, System.out
        );
        server.connect(launcher.getRemoteProxy());
        launcher.startListening().get();
    }
}
