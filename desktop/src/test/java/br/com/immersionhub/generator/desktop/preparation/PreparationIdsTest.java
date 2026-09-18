package br.com.immersionhub.generator.desktop.preparation;

import br.com.immersionhub.generator.desktop.model.MediaCut;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class PreparationIdsTest {
 @Test void identityChangesWithCutOrVersion() throws Exception {
   Path d=Files.createTempDirectory("prep-id"); Path s=Files.writeString(d.resolve("source.mp4"),"s"); Path o=Files.writeString(d.resolve("cut.mp4"),"c");
   MediaCut a=new MediaCut("source",s,o,10,110,100,Instant.EPOCH);
   MediaCut b=new MediaCut("source",s,o,10,120,110,Instant.EPOCH);
   assertEquals(PreparationIds.from(a,"1","w1"),PreparationIds.from(a,"1","w1"));
   assertNotEquals(PreparationIds.from(a,"1","w1"),PreparationIds.from(b,"1","w1"));
   assertNotEquals(PreparationIds.from(a,"1","w1"),PreparationIds.from(a,"2","w1"));
   assertNotEquals(PreparationIds.from(a,"1","w1"),PreparationIds.from(a,"1","w2"));
 }
}
