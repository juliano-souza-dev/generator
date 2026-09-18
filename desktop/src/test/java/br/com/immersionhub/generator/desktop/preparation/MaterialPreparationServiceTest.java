package br.com.immersionhub.generator.desktop.preparation;

import br.com.immersionhub.generator.desktop.model.MediaCut;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MaterialPreparationServiceTest {
 @Test void reusesValidSnapshotAndDoesNotRunHeavyStagesAgain() throws Exception {
   Path d=Files.createTempDirectory("prep"); Path s=Files.writeString(d.resolve("source.mp4"),"s"); Path c=Files.writeString(d.resolve("cut.mp4"),"c"); Path wav=Files.writeString(d.resolve("a.wav"),"a");
   MediaCut cut=new MediaCut("id",s,c,0,100,100,Instant.EPOCH);
   Map<String,PreparedMaterial> db=new HashMap<>();
   PreparedMaterialRepository repo=new PreparedMaterialRepository(){public Optional<PreparedMaterial> load(String id){return Optional.ofNullable(db.get(id));} public void save(PreparedMaterial m){db.put(m.id(),m);}};
   int[] calls={0};
   TechnicalAudioExtractor extractor=(mc,out)->{calls[0]++; return wav;};
   AsrEngine asr=new AsrEngine(){public AsrResult transcribeEnglish(Path p){calls[0]++; return new AsrResult("en","hi",List.of(new TimedText("hi",0,100)),List.of());} public String version(){return "w1";}};
   MaterialPreparationService service=new MaterialPreparationService(extractor,asr,repo,d,"p1");
   assertEquals(service.prepare(cut).id(),service.prepare(cut).id());
   assertEquals(2,calls[0]);
 }
 @Test void failedAsrIsNeverPublished() throws Exception {
   Path d=Files.createTempDirectory("prep-fail"); Path s=Files.writeString(d.resolve("source.mp4"),"s"); Path c=Files.writeString(d.resolve("cut.mp4"),"c"); Path wav=Files.writeString(d.resolve("a.wav"),"a");
   MediaCut cut=new MediaCut("id",s,c,0,100,100,Instant.EPOCH); int[] saves={0};
   PreparedMaterialRepository repo=new PreparedMaterialRepository(){public Optional<PreparedMaterial> load(String id){return Optional.empty();} public void save(PreparedMaterial m){saves[0]++;}};
   AsrEngine asr=new AsrEngine(){public AsrResult transcribeEnglish(Path p){throw new IllegalStateException("fail");} public String version(){return "w1";}};
   MaterialPreparationService service=new MaterialPreparationService((mc,out)->wav,asr,repo,d,"p1");
   assertThrows(IllegalStateException.class,()->service.prepare(cut));
   assertEquals(0,saves[0]);
 }
}
