package br.com.immersionhub.generator.desktop.navigation;

import br.com.immersionhub.generator.desktop.model.MediaCut;
import br.com.immersionhub.generator.desktop.model.SourceMedia;
import br.com.immersionhub.generator.desktop.preparation.AlignedMaterial;
import br.com.immersionhub.generator.desktop.preparation.PreparationModule;
import br.com.immersionhub.generator.desktop.preparation.PreparationPipeline;
import br.com.immersionhub.generator.desktop.project.ProjectModule;
import br.com.immersionhub.generator.desktop.project.ProjectRepository;
import br.com.immersionhub.generator.desktop.project.ProjectStage;
import br.com.immersionhub.generator.desktop.project.ProjectState;
import br.com.immersionhub.generator.desktop.source.SourceAcquisitionService;
import br.com.immersionhub.generator.desktop.source.SourceModule;
import br.com.immersionhub.generator.desktop.timing.MediaProcessor;
import br.com.immersionhub.generator.desktop.timing.TimingModule;
import br.com.immersionhub.generator.desktop.translation.TranslationMaterial;
import br.com.immersionhub.generator.desktop.translation.TranslationModule;
import br.com.immersionhub.generator.desktop.ui.AppShell;
import br.com.immersionhub.generator.desktop.ui.HomeView;
import br.com.immersionhub.generator.desktop.ui.PreparationView;
import br.com.immersionhub.generator.desktop.ui.SourceView;
import br.com.immersionhub.generator.desktop.ui.TranslationView;
import br.com.immersionhub.generator.desktop.ui.WaveView;
import javafx.scene.Node;
import javafx.scene.Parent;

public final class NavigationController {
    private final NavigationState state = new NavigationState();
    private final AppShell shell = new AppShell();
    private final SourceAcquisitionService sourceService = SourceModule.createService();
    private final MediaProcessor mediaProcessor = TimingModule.createProcessor();
    private final ProjectRepository projectRepository = ProjectModule.createRepository();

    private PreparationPipeline preparationPipeline;
    private ProjectState projectState;
    private SourceMedia sourceMedia;
    private MediaCut mediaCut;
    private AlignedMaterial alignedMaterial;
    private TranslationMaterial translationMaterial;
    private boolean preparationAutoStart;
    private boolean preparationNeedsRecovery;
    private boolean translationNeedsRecovery;

    public NavigationController() {
        state.onChanged(this::render);

        shell.setHomeAction(() -> state.navigate(ScreenId.HOME));
        shell.setSourceAction(() -> state.navigate(ScreenId.SOURCE));
        shell.setWaveAction(() -> {
            if (sourceMedia != null) state.navigate(ScreenId.WAVE);
        });
        shell.setPreparationAction(() -> {
            if (mediaCut != null) {
                preparationAutoStart = false;
                state.navigate(ScreenId.PREPARATION);
            }
        });
        shell.setTranslationAction(() -> {
            if (alignedMaterial != null) state.navigate(ScreenId.TRANSLATION);
        });

        refreshNavigationAvailability();
    }

    public Parent root() {
        return shell.root();
    }

    public void showHome() {
        state.navigate(ScreenId.HOME);
    }

    public void showSource() {
        startNewProject();
    }

    private void startNewProject() {
        projectState = null;
        sourceMedia = null;
        mediaCut = null;
        alignedMaterial = null;
        translationMaterial = null;
        preparationAutoStart = false;
        preparationNeedsRecovery = false;
        translationNeedsRecovery = false;
        refreshNavigationAvailability();
        state.navigate(ScreenId.SOURCE);
    }

    private void openProject(ProjectState project) {
        projectState = project;
        sourceMedia = project.sourceMedia().orElse(null);
        mediaCut = project.mediaCut().orElse(null);

        alignedMaterial = project.preparationCompleted() && mediaCut != null
            ? PreparationModule.loadAligned(
                project.preparedMaterialId(),
                project.alignedMaterialId(),
                mediaCut
            ).orElse(null)
            : null;

        preparationNeedsRecovery = project.preparationCompleted()
            && mediaCut != null
            && alignedMaterial == null;

        if (preparationNeedsRecovery) {
            ProjectState recovered = project.withoutPreparation();
            persist(recovered);
            projectState = recovered;
            translationMaterial = null;
            translationNeedsRecovery = false;
        } else {
            translationMaterial = project.translationCompleted() && alignedMaterial != null
                ? TranslationModule.loadTranslation(project.projectId(), alignedMaterial).orElse(null)
                : null;

            translationNeedsRecovery = project.translationCompleted()
                && alignedMaterial != null
                && translationMaterial == null;

            if (translationNeedsRecovery) {
                ProjectState recovered = project.withoutTranslation();
                persist(recovered);
                projectState = recovered;
            }
        }

        preparationAutoStart = false;
        refreshNavigationAvailability();

        ProjectStage resume = projectState.minimumResumeStage();
        switch (resume) {
            case SOURCE -> state.navigate(ScreenId.SOURCE);
            case WAVE -> state.navigate(ScreenId.WAVE);
            case PREPARATION -> state.navigate(ScreenId.PREPARATION);
            case TRANSLATION -> state.navigate(ScreenId.TRANSLATION);
        }
    }

    private void acceptSource(SourceMedia media) {
        boolean samePersistedSource = projectState != null
            && projectState.sourceId().equals(media.sourceId());

        if (!samePersistedSource) {
            ProjectState next = ProjectState.start(media);
            persist(next);
            projectState = next;
            mediaCut = null;
            alignedMaterial = null;
            translationMaterial = null;
        } else if (projectState.sourceMedia().isEmpty()) {
            ProjectState repaired = projectState.withSource(media);
            persist(repaired);
            projectState = repaired;
            mediaCut = null;
            alignedMaterial = null;
            translationMaterial = null;
        }

        sourceMedia = media;
        preparationAutoStart = false;
        preparationNeedsRecovery = false;
        translationNeedsRecovery = false;
        refreshNavigationAvailability();
        state.navigate(ScreenId.WAVE);
    }

    private void invalidateSource() {
        sourceMedia = null;
        mediaCut = null;
        alignedMaterial = null;
        translationMaterial = null;
        preparationAutoStart = false;
        preparationNeedsRecovery = false;
        translationNeedsRecovery = false;
        refreshNavigationAvailability();
    }

    private void acceptCut(MediaCut cut) {
        if (projectState == null) {
            throw new IllegalStateException("Projeto ativo não encontrado.");
        }

        ProjectState next = projectState.withCut(cut);
        persist(next);

        projectState = next;
        mediaCut = cut;
        alignedMaterial = null;
        translationMaterial = null;
        preparationAutoStart = true;
        preparationNeedsRecovery = false;
        translationNeedsRecovery = false;
        refreshNavigationAvailability();
        state.navigate(ScreenId.PREPARATION);
    }

    private void acceptPrepared(AlignedMaterial material) {
        if (projectState == null) {
            throw new IllegalStateException("Projeto ativo não encontrado.");
        }

        ProjectState next = projectState.withPrepared(material);
        persist(next);

        projectState = next;
        alignedMaterial = material;
        translationMaterial = null;
        preparationAutoStart = false;
        preparationNeedsRecovery = false;
        translationNeedsRecovery = false;
        refreshNavigationAvailability();
        state.navigate(ScreenId.TRANSLATION);
    }

    private void acceptTranslation(TranslationMaterial material) {
        if (projectState == null) {
            throw new IllegalStateException("Projeto ativo não encontrado.");
        }

        ProjectState next = projectState.withTranslation(material);
        persist(next);

        projectState = next;
        translationMaterial = material;
        translationNeedsRecovery = false;
        refreshNavigationAvailability();
    }

    private void persist(ProjectState project) {
        try {
            projectRepository.save(project);
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível salvar o andamento do projeto.", exception);
        }
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

    private WaveView waveView() {
        if (projectState == null || sourceMedia == null) {
            throw new IllegalStateException("Projeto e fonte são obrigatórios para abrir o Wave.");
        }

        return new WaveView(
            sourceMedia,
            mediaProcessor,
            TimingModule.createRepository(projectState.projectId()),
            TimingModule.createDraftRepository(projectState.projectId()),
            TimingModule.projectTimingDir(projectState.projectId()),
            mediaCut,
            this::acceptCut,
            () -> state.navigate(ScreenId.SOURCE)
        );
    }

    private void refreshNavigationAvailability() {
        shell.setWaveEnabled(sourceMedia != null);
        shell.setPreparationEnabled(mediaCut != null);
        shell.setTranslationEnabled(alignedMaterial != null);
    }

    private void render(ScreenId screen) {
        ScreenId shown = screen;
        Node content;

        if (screen == ScreenId.HOME) {
            content = new HomeView(
                projectRepository.list(),
                this::openProject,
                this::startNewProject
            ).root();
        } else if (screen == ScreenId.SOURCE) {
            content = sourceView().root();
        } else if (screen == ScreenId.WAVE) {
            if (sourceMedia == null || projectState == null) {
                shown = ScreenId.SOURCE;
                content = sourceView().root();
            } else {
                content = waveView().root();
            }
        } else if (screen == ScreenId.PREPARATION) {
            if (mediaCut == null || projectState == null) {
                if (sourceMedia == null || projectState == null) {
                    shown = ScreenId.SOURCE;
                    content = sourceView().root();
                } else {
                    shown = ScreenId.WAVE;
                    content = waveView().root();
                }
            } else {
                boolean autoStart = preparationAutoStart;
                preparationAutoStart = false;
                content = new PreparationView(
                    mediaCut,
                    preparationPipeline(),
                    this::acceptPrepared,
                    () -> state.navigate(ScreenId.WAVE),
                    alignedMaterial,
                    autoStart,
                    preparationNeedsRecovery
                ).root();
            }
        } else {
            if (alignedMaterial == null || projectState == null) {
                shown = ScreenId.PREPARATION;
                content = new PreparationView(
                    mediaCut,
                    preparationPipeline(),
                    this::acceptPrepared,
                    () -> state.navigate(ScreenId.WAVE),
                    alignedMaterial,
                    false,
                    preparationNeedsRecovery
                ).root();
            } else {
                content = new TranslationView(
                    projectState.projectId(),
                    alignedMaterial,
                    translationMaterial,
                    this::acceptTranslation,
                    () -> state.navigate(ScreenId.PREPARATION)
                ).root();
            }
        }

        shell.show(content, shown);
    }
}
