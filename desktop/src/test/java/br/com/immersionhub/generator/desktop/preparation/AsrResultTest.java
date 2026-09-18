package br.com.immersionhub.generator.desktop.preparation;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AsrResultTest {
 @Test void rejectsTimingBeyondCut() {
   AsrResult r=new AsrResult("en","hello",List.of(new TimedText("hello",0,101)),List.of());
   assertThrows(IllegalArgumentException.class,()->r.validate(100));
 }
 @Test void acceptsMonotonicTimeline() {
   AsrResult r=new AsrResult("en","hello world",List.of(new TimedText("hello world",0,100)),List.of(new TimedText("hello",0,40),new TimedText("world",40,100)));
   assertDoesNotThrow(()->r.validate(100));
 }
}
