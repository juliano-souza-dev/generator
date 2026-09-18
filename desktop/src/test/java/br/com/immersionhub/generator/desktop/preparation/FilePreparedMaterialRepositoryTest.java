package br.com.immersionhub.generator.desktop.preparation;

import br.com.immersionhub.generator.desktop.model.MediaCut;
import org.junit.jupiter.api.Test;
import java.nio.file.*; import java.time.Instant; import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FilePreparedMaterialRepositoryTest {
 @Test void persistsAndReloadsSnapshotOutsideInstall() throws Exception {
  Path root=Files.createTempDirectory("repo"), source=Files.writeString(root.resolve("source.mp4"),"s"), cut=Files.writeString(root.resolve("cut.mp4"),"c"), audio=Files.writeString(root.resolve("a.wav"),"a");
  MediaCut mc=new MediaCut("source",source,cut,0,100,100,Instant.EPOCH);
  PreparedMaterial m=new PreparedMaterial("id","p1","w1",mc,audio,new AsrResult("en","hello",List.of(new TimedText("hello",0,100)),List.of()),Instant.EPOCH);
  FilePreparedMaterialRepository repo=new FilePreparedMaterialRepository(root.resolve("prepared"));
  repo.save(m); PreparedMaterial loaded=repo.load("id").orElseThrow();
  assertEquals("hello",loaded.transcription().text()); assertEquals(m.id(),loaded.id());
 }
}
