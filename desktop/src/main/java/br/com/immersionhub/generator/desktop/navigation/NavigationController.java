package br.com.immersionhub.generator.desktop.navigation;

import br.com.immersionhub.generator.desktop.editorial.EditorialMaterial;
import br.com.immersionhub.generator.desktop.editorial.EditorialModule;
import br.com.immersionhub.generator.desktop.editorial.EditorialReviewService;
import br.com.immersionhub.generator.desktop.editorial.WordReviewPosition;
import br.com.immersionhub.generator.desktop.editorial.WordReviewService;
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
import br.com.immersionhub.generator.desktop.ui.EditorialReviewUnavailableView;
import br.com.immersionhub.generator.desktop.ui.EditorialReviewView;
import br.com.immersionhub.generator.desktop.ui.GroqSettingsDialog;
import br.com.immersionhub.generator.desktop.ui.HomeView;
import br.com.immersionhub.generator.desktop.ui.PreparationView;
import br.com.immersionhub.generator.desktop.ui.SourceView;
import br.com.immersionhub.generator.desktop.ui.TranslationView;
import br.com.immersionhub.generator.desktop.ui.WaveView;
import br.com.immersionhub.generator.desktop.ui.WordReviewView;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.stage.Window;

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

    private EditorialReviewService editorialReviewService;
    private EditorialMaterial editorialMaterial;
    private int editorialCueOrder = 1;
    private WordReviewService wordReviewService;
    private WordReviewPosition wordReviewPosition;

    private boolean preparationAutoStart;
    private boolean preparationNeedsRecovery;
    private boolean translationNeedsRecovery;
    private boolean editorialNeedsRecovery;

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
        shell.setReviewAction(() -> {
            if (translationMaterial != null) state.navigate(ScreenId.EDITORIAL_REVIEW);
        });
        shell.setWordReviewAction(() -> {
            if (editorialMaterial != null && editorialMaterial.cuesApproved()) {
                ensureWordReviewInitialized();
                state.navigate(ScreenId.WORD_REVIEW);
            }
        });
        shell.setConfigAction(this::openGroqConfig);

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

    private boolean openGroqConfig() {
        Window owner = shell.root().getScene() == null ? null : shell.root().getScene().getWindow();
        return GroqSettingsDialog.show(owner);
    }

    private void startNewProject() {
        projectState = null;
        sourceMedia = null;
        mediaCut = null;
        alignedMaterial = null;
        translationMaterial = null;
        resetEditorial();
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
        resetEditorial();

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
            } else if (translationMaterial != null) {
                initializeEditorialReview();
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
            case EDITORIAL_REVIEW -> {
                if (editorialMaterial != null && editorialMaterial.cuesApproved()) {
                    ensureWordReviewInitialized();
                    state.navigate(ScreenId.WORD_REVIEW);
                } else {
                    state.navigate(ScreenId.EDITORIAL_REVIEW);
                }
            }
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
            resetEditorial();
        } else if (projectState.sourceMedia().isEmpty()) {
            ProjectState repaired = projectState.withSource(media);
            persist(repaired);
            projectState = repaired;
            mediaCut = null;
            alignedMaterial = null;
            translationMaterial = null;
            resetEditorial();
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
        resetEditorial();
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
        resetEditorial();
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
        resetEditorial();
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
        initializeEditorialReview();
        refreshNavigationAvailability();
        state.navigate(ScreenId.EDITORIAL_REVIEW);
    }

    private void acceptEditorialProgress(EditorialMaterial material, Integer cueOrder) {
        editorialMaterial = material;
        editorialCueOrder = cueOrder == null ? 1 : cueOrder;
        wordReviewPosition = null;

        if (editorialMaterial.cuesApproved()) {
            ensureWordReviewInitialized();
        } else {
            wordReviewService = null;
        }

        if (projectState != null) {
            ProjectState next = projectState.withEditorialActivity();
            persist(next);
            projectState = next;
        }
        refreshNavigationAvailability();
    }

    private void acceptWordReviewProgress(EditorialMaterial material, WordReviewPosition position) {
        editorialMaterial = material;
        wordReviewPosition = position;

        if (projectState != null) {
            ProjectState next = projectState.withEditorialActivity();
            persist(next);
            projectState = next;
        }
        refreshNavigationAvailability();
    }

    private void initializeEditorialReview() {
        editorialReviewService = null;
        editorialMaterial = null;
        editorialCueOrder = 1;
        wordReviewService = null;
        wordReviewPosition = null;
        editorialNeedsRecovery = false;

        if (projectState == null || translationMaterial == null) return;

        try {
            editorialReviewService = EditorialModule.createService(projectState.projectId());
            editorialMaterial = editorialReviewService.loadOrCreate(translationMaterial);
            editorialCueOrder = editorialReviewService.loadCursor(editorialMaterial);
            if (editorialMaterial.cuesApproved()) {
                ensureWordReviewInitialized();
            }
        } catch (Exception exception) {
            editorialMaterial = null;
            editorialCueOrder = 1;
            editorialNeedsRecovery = true;
        }
    }

    private void ensureWordReviewInitialized() {
        if (projectState == null || editorialMaterial == null || !editorialMaterial.cuesApproved()) {
            wordReviewService = null;
            wordReviewPosition = null;
            return;
        }
        if (wordReviewService == null) {
            wordReviewService = EditorialModule.createWordReviewService(projectState.projectId());
        }
        if (wordReviewPosition == null) {
            wordReviewPosition = wordReviewService.loadCursor(editorialMaterial);
        }
    }

    private void retryEditorialReview() {
        initializeEditorialReview();
        refreshNavigationAvailability();
        state.navigate(ScreenId.EDITORIAL_REVIEW);
    }

    private void resetEditorial() {
        editorialReviewService = null;
        editorialMaterial = null;
        editorialCueOrder = 1;
        wordReviewService = null;
        wordReviewPosition = null;
        editorialNeedsRecovery = false;
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
        shell.setReviewEnabled(translationMaterial != null);
        shell.setWordReviewEnabled(editorialMaterial != null && editorialMaterial.cuesApproved());
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
        } else if (screen == ScreenId.TRANSLATION) {
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
                    () -> state.navigate(ScreenId.PREPARATION),
                    this::openGroqConfig
                ).root();
            }
        } else if (screen == ScreenId.EDITORIAL_REVIEW) {
            if (translationMaterial == null || projectState == null || mediaCut == null) {
                shown = ScreenId.TRANSLATION;
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
                        () -> state.navigate(ScreenId.PREPARATION),
                        this::openGroqConfig
                    ).root();
                }
            } else if (editorialNeedsRecovery || editorialMaterial == null || editorialReviewService == null) {
                content = new EditorialReviewUnavailableView(
                    this::retryEditorialReview,
                    () -> state.navigate(ScreenId.TRANSLATION)
                ).root();
            } else {
                content = new EditorialReviewView(
                    editorialReviewService,
                    translationMaterial,
                    editorialMaterial,
                    editorialCueOrder,
                    mediaCut.outputPath(),
                    this::acceptEditorialProgress,
                    () -> state.navigate(ScreenId.TRANSLATION)
                ).root();
            }
        } else {
            if (editorialMaterial == null || !editorialMaterial.cuesApproved()) {
                shown = ScreenId.EDITORIAL_REVIEW;
                if (translationMaterial == null || editorialReviewService == null) {
                    content = new EditorialReviewUnavailableView(
                        this::retryEditorialReview,
                        () -> state.navigate(ScreenId.TRANSLATION)
                    ).root();
                } else {
                    content = new EditorialReviewView(
                        editorialReviewService,
                        translationMaterial,
                        editorialMaterial,
                        editorialCueOrder,
                        mediaCut.outputPath(),
                        this::acceptEditorialProgress,
                        () -> state.navigate(ScreenId.TRANSLATION)
                    ).root();
                }
            } else {
                ensureWordReviewInitialized();
                content = new WordReviewView(
                    wordReviewService,
                    editorialMaterial,
                    wordReviewPosition,
                    this::acceptWordReviewProgress,
                    () -> state.navigate(ScreenId.EDITORIAL_REVIEW)
                ).root();
            }
        }

        shell.show(content, shown);
    }
}
