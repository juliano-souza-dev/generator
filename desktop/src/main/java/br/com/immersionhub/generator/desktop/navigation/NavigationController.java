package br.com.immersionhub.generator.desktop.navigation;

import br.com.immersionhub.generator.desktop.model.MediaCut;
import br.com.immersionhub.generator.desktop.model.SourceMedia;
import br.com.immersionhub.generator.desktop.preparation.AlignedMaterial;
import br.com.immersionhub.generator.desktop.preparation.PreparationModule;
import br.com.immersionhub.generator.desktop.preparation.PreparationPipeline;
import br.com.immersionhub.generator.desktop.source.SourceAcquisitionService;
import br.com.immersionhub.generator.desktop.source.SourceModule;
import br.com.immersionhub.generator.desktop.timing.MediaCutRepository;
import br.com.immersionhub.generator.desktop.timing.MediaProcessor;
import br.com.immersionhub.generator.desktop.timing.TimingDraftRepository;
import br.com.immersionhub.generator.desktop.timing.TimingModule;
import br.com.immersionhub.generator.desktop.ui.AppShell;
import br.com.immersionhub.generator.desktop.ui.PreparationView;
import br.com.immersionhub.generator.desktop.ui.SourceView;
import br.com.immersionhub.generator.desktop.ui.WaveView;
import javafx.scene.Node;
import javafx.scene.Parent;

public final class NavigationController {
    private final NavigationState state = new NavigationState();
    private final AppShell shell = new AppShell();
    private final SourceAcquisitionService sourceService = SourceModule.createService();
    private final MediaProcessor mediaProcessor = TimingModule.createProcessor();
    private final MediaCutRepository mediaCutRepository = TimingModule.createRepository();
    private final TimingDraftRepository timingDraftRepository = TimingModule.createDraftRepository();

    private PreparationPipeline preparationPipeline;
    private SourceMedia sourceMedia;
    private MediaCut mediaCut;
    private AlignedMaterial alignedMaterial;

    public NavigationController() {
        state.onChanged(this::render);
        shell.setSourceAction(() -> state.navigate(ScreenId.SOURCE));
        shell.setWaveAction(() -> {
            if (sourceMedia != null) state.navigate(ScreenId.WAVE);
        });
        shell.setPreparationAction(() -> {
            if (mediaCut != null) state.navigate(ScreenId.PREPARATION);
        });
        shell.setWaveEnabled(false);
        shell.setPreparationEnabled(false);
    }

    public Parent root() {
        return shell.root();
    }

    public void showSource() {
        state.navigate(ScreenId.SOURCE);
    }

    private void acceptSource(SourceMedia media) {
        if (sourceMedia == null || !sourceMedia.sourceId().equals(media.sourceId())) {
            mediaCut = null;
            alignedMaterial = null;
            shell.setPreparationEnabled(false);
        }
        sourceMedia = media;
        shell.setWaveEnabled(true);
        state.navigate(ScreenId.WAVE);
    }

    private void invalidateSource() {
        sourceMedia = null;
        mediaCut = null;
        alignedMaterial = null;
        shell.setWaveEnabled(false);
        shell.setPreparationEnabled(false);
    }

    private void acceptCut(MediaCut cut) {
        mediaCut = cut;
        alignedMaterial = null;
        shell.setPreparationEnabled(true);
        state.navigate(ScreenId.PREPARATION);
    }

    private void acceptPrepared(AlignedMaterial material) {
        alignedMaterial = material;
    }

    private PreparationPipeline preparationPipeline() {
        if (preparationPipeline == null) {
            preparationPipeline = PreparationModule.createPipeline();
        }
        return preparationPipeline;
    }

    private SourceView sourceView() {
        return new SourceView(sourceService, sourceMedia, this::acceptSource, this::invalidateSource);
    }

    private void render(ScreenId screen) {
        ScreenId shown = screen;
        Node content;

        if (screen == ScreenId.SOURCE) {
            content = sourceView().root();
        } else if (screen == ScreenId.WAVE) {
            if (sourceMedia == null) {
                shown = ScreenId.SOURCE;
                content = sourceView().root();
            } else {
                content = new WaveView(
                    sourceMedia,
                    mediaProcessor,
                    mediaCutRepository,
                    timingDraftRepository,
                    mediaCut,
                    this::acceptCut,
                    () -> state.navigate(ScreenId.SOURCE)
                ).root();
            }
        } else {
            if (mediaCut == null) {
                if (sourceMedia == null) {
                    shown = ScreenId.SOURCE;
                    content = sourceView().root();
                } else {
                    shown = ScreenId.WAVE;
                    content = new WaveView(
                        sourceMedia,
                        mediaProcessor,
                        mediaCutRepository,
                        timingDraftRepository,
                        mediaCut,
                        this::acceptCut,
                        () -> state.navigate(ScreenId.SOURCE)
                    ).root();
                }
            } else {
                content = new PreparationView(
                    mediaCut,
                    preparationPipeline(),
                    this::acceptPrepared,
                    () -> state.navigate(ScreenId.WAVE)
                ).root();
            }
        }

        shell.show(content, shown);
    }
}
