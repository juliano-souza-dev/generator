package br.com.immersionhub.generator.desktop.preparation;

import br.com.immersionhub.generator.desktop.model.MediaCut;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

public final class FilePreparedMaterialRepository implements PreparedMaterialRepository {
    private final Path root;
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public FilePreparedMaterialRepository(Path root) { this.root=root.toAbsolutePath().normalize(); }

    @Override public Optional<PreparedMaterial> load(String id) {
        Path json=root.resolve(id).resolve("prepared-material.json");
        if(!Files.isRegularFile(json)) return Optional.empty();
        try {
            Snapshot s=mapper.readValue(json.toFile(),Snapshot.class);
            Path source=Path.of(s.sourcePath); Path cut=Path.of(s.cutPath); Path audio=Path.of(s.technicalAudio);
            if(!Files.isRegularFile(source)||!Files.isRegularFile(cut)||!Files.isRegularFile(audio)) return Optional.empty();
            MediaCut mc=new MediaCut(s.sourceId,source,cut,s.startMs,s.endMs,s.durationMs,Instant.parse(s.cutCreatedAt));
            AsrResult asr=new AsrResult(s.language,s.text,toTimed(s.segments),toTimed(s.words));
            PreparedMaterial material=new PreparedMaterial(s.id,s.pipelineVersion,s.asrVersion,mc,audio,asr,Instant.parse(s.createdAt));
            if(!id.equals(material.id())) return Optional.empty();
            return Optional.of(material);
        } catch(Exception ignored) { return Optional.empty(); }
    }

    @Override public void save(PreparedMaterial m) throws Exception {
        Path dir=root.resolve(m.id()); Files.createDirectories(dir);
        Path tmp=dir.resolve("prepared-material.tmp.json"), out=dir.resolve("prepared-material.json");
        Snapshot s=Snapshot.from(m); mapper.writeValue(tmp.toFile(),s);
        Files.move(tmp,out,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
    }

    private static List<TimedText> toTimed(List<Timed> in){ return in==null?List.of():in.stream().map(x->new TimedText(x.text,x.startMs,x.endMs)).toList(); }

    public static final class Timed { public String text; public long startMs; public long endMs; public Timed(){} Timed(TimedText x){text=x.text();startMs=x.startMs();endMs=x.endMs();}}
    public static final class Snapshot {
        public String id,pipelineVersion,asrVersion,sourceId,sourcePath,cutPath,technicalAudio,cutCreatedAt,createdAt,language,text;
        public long startMs,endMs,durationMs; public List<Timed> segments,words;
        public Snapshot(){}
        static Snapshot from(PreparedMaterial m){ Snapshot s=new Snapshot(); var c=m.mediaCut(); s.id=m.id();s.pipelineVersion=m.pipelineVersion();s.asrVersion=m.asrVersion();s.sourceId=c.sourceId();s.sourcePath=c.sourcePath().toString();s.cutPath=c.outputPath().toString();s.startMs=c.startMs();s.endMs=c.endMs();s.durationMs=c.durationMs();s.cutCreatedAt=c.createdAt().toString();s.technicalAudio=m.technicalAudio().toString();s.createdAt=m.createdAt().toString();s.language=m.transcription().language();s.text=m.transcription().text();s.segments=m.transcription().segments().stream().map(Timed::new).toList();s.words=m.transcription().words().stream().map(Timed::new).toList();return s;}
    }
}
