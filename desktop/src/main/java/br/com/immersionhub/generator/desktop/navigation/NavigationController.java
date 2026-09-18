package br.com.immersionhub.generator.desktop.navigation;

import br.com.immersionhub.generator.desktop.ui.AppShell;
import br.com.immersionhub.generator.desktop.ui.SourceView;
import br.com.immersionhub.generator.desktop.ui.WaveView;
import javafx.scene.Node;
import javafx.scene.Parent;

public final class NavigationController {
    private final NavigationState state = new NavigationState();
    private final AppShell shell = new AppShell();

    public NavigationController() {
        state.onChanged(this::render);
        shell.setSourceAction(() -> state.navigate(ScreenId.SOURCE));
        shell.setWaveAction(() -> state.navigate(ScreenId.WAVE));
    }

    public Parent root() {
        return shell.root();
    }

    public void showSource() {
        state.navigate(ScreenId.SOURCE);
    }

    private void render(ScreenId screen) {
        Node content = switch (screen) {
            case SOURCE -> new SourceView(() -> state.navigate(ScreenId.WAVE)).root();
            case WAVE -> new WaveView(() -> state.navigate(ScreenId.SOURCE)).root();
        };
        shell.show(content, screen);
    }
}
