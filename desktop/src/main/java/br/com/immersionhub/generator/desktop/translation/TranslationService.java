package br.com.immersionhub.generator.desktop.translation;

import br.com.immersionhub.generator.desktop.preparation.AlignedMaterial;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class TranslationService {
    private final GroqClient groq;
    private final TranslationChunker chunker;
    private final GroqTranslationResponseCodec groqCodec;
    private final ExternalAiPackageService external;
    private final TranslationMaterialRepository repository;
    private final TranslationLogger logger;
    private final Path translationDirectory;

    public TranslationService(
        GroqClient groq,
        TranslationChunker chunker,
        GroqTranslationResponseCodec groqCodec,
        ExternalAiPackageService external,
        TranslationMaterialRepository repository,
        TranslationLogger logger,
        Path translationDirectory
    ) {
        this.groq = groq;
        this.chunker = Objects.requireNonNull(chunker);
        this.groqCodec = Objects.requireNonNull(groqCodec);
        this.external = Objects.requireNonNull(external);
        this.repository = Objects.requireNonNull(repository);
        this.logger = Objects.requireNonNull(logger);
        this.translationDirectory = Objects.requireNonNull(translationDirectory).toAbsolutePath().normalize();
    }

    public TranslationOutcome translate(
        AlignedMaterial aligned,
        Consumer<TranslationStage> progress
    ) throws Exception {
        Consumer<TranslationStage> listener = progress == null ? ignored -> {} : progress;
        TranslationMaterial base = TranslationMaterialFactory.base(aligned);

        listener.accept(TranslationStage.PREPARING_PACKAGE);
        ExternalAiPackage externalPackage = external.prepare(
            base,
            aligned.preparedMaterial().technicalAudio(),
            translationDirectory
        );
        logger.info("translation.external-package.ready materialId=" + base.id());

        var cached = repository.load(base);
        if (cached.isPresent() && cached.get().complete()) {
            listener.accept(TranslationStage.READY);
            logger.info("translation.cache.ready materialId=" + cached.get().id());
            return new TranslationOutcome(base, cached.get(), externalPackage, "");
        }

        if (groq == null) {
            listener.accept(TranslationStage.EXTERNAL_REQUIRED);
            logger.warn("translation.groq.unavailable materialId=" + base.id(), null);
            return new TranslationOutcome(
                base,
                null,
                externalPackage,
                "Tradução automática não configurada."
            );
        }

        try {
            GroqModelInfo model = groq.modelInfo();
            List<List<TranslationCue>> chunks = chunker.chunk(base, model);
            List<TranslatedCue> translated = new ArrayList<>();

            for (int index = 0; index < chunks.size(); index++) {
                List<TranslationCue> chunk = chunks.get(index);
                listener.accept(TranslationStage.TRANSLATING);
                logger.info(
                    "translation.groq.chunk.start materialId=" + base.id()
                        + " chunk=" + (index + 1) + "/" + chunks.size()
                        + " cues=" + chunk.size()
                );

                GroqCompletion completion = groq.translate(chunk);
                List<TranslatedCue> chunkResult = groqCodec.parse(completion.content(), chunk);
                translated.addAll(chunkResult);

                logger.info(
                    "translation.groq.chunk.ready materialId=" + base.id()
                        + " chunk=" + (index + 1) + "/" + chunks.size()
                );
            }

            listener.accept(TranslationStage.VALIDATING);
            TranslationMaterial result = TranslationMaterialFactory.apply(
                base,
                translated,
                TranslationSource.GROQ
            );
            repository.save(result);
            listener.accept(TranslationStage.READY);
            logger.info("translation.groq.ready materialId=" + result.id() + " cues=" + result.cues().size());
            return new TranslationOutcome(base, result, externalPackage, "");
        } catch (Exception failure) {
            logger.warn("translation.groq.fallback materialId=" + base.id(), failure);
            listener.accept(TranslationStage.EXTERNAL_REQUIRED);
            return new TranslationOutcome(
                base,
                null,
                externalPackage,
                "A tradução automática não pôde concluir todo o material."
            );
        }
    }

    public TranslationMaterial importExternal(
        Path returnedFile,
        TranslationMaterial expectedBase
    ) throws Exception {
        TranslationMaterial result = external.importReturn(returnedFile, expectedBase);
        repository.save(result);
        logger.info("translation.external.ready materialId=" + result.id() + " cues=" + result.cues().size());
        return result;
    }
}
