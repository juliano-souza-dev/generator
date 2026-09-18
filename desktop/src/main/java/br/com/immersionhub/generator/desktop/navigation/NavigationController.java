package br.com.immersionhub.generator.desktop.navigation;

import br.com.immersionhub.generator.desktop.model.SourceMedia;
import br.com.immersionhub.generator.desktop.source.SourceAcquisitionService;
import br.com.immersionhub.generator.desktop.source.SourceModule;
import br.com.immersionhub.generator.desktop.ui.AppShell;
import br.com.immersionhub.generator.desktop.ui.SourceView;
import br.com.immersionhub.generator.desktop.ui.WaveView;
import javafx.scene.Node;
import javafx.scene.Parent;

public final class NavigationController {
    private final NavigationState state = new NavigationState();
    private final AppShell shell = new AppShell();
    private final SourceAcquisitionService sourceService = SourceModule.createService();
    private SourceMedia sourceMedia;

    public NavigationController() {
        state.onChanged(this::render);
        shell.setSourceAction(() -> state.navigate(ScreenId.SOURCE));
        shell.setWaveAction(() -> {
            if (sourceMedia != null) state.navigate(ScreenId.WAVE);
        });
        shell.setWaveEnabled(false);
    }

    public Parent root() {
        return shell.root();
    }

    public void showSource() {
        state.navigate(ScreenId.SOURCE);
    }

    private void acceptSource(SourceMedia media) {
        sourceMedia = media;
        shell.setWaveEnabled(true);
        state.navigate(ScreenId.WAVE);
    }

    private void invalidateSource() {
        sourceMedia = null;
        shell.setWaveEnabled(false);
    }

    private SourceView sourceView() {
        return new SourceView(sourceService, sourceMedia, this::acceptSource, this::invalidateSource);
    }

    private void render(ScreenId screen) {
        Node content = switch (screen) {
            case SOURCE -> sourceView().root();
            case WAVE -> {
                if (sourceMedia == null) {
                    yield sourceView().root();
                }
                yield new WaveView(sourceMedia, () -> state.navigate(ScreenId.SOURCE)).root();
            }
        };
        shell.show(content, sourceMedia == null && screen == ScreenId.WAVE ? ScreenId.SOURCE : screen);
    }
}
