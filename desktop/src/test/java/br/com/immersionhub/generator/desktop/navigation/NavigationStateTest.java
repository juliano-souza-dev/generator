package br.com.immersionhub.generator.desktop.navigation;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NavigationStateTest {
    @Test
    void navigationChangesContentStateWithoutCreatingWindows() {
        NavigationState state = new NavigationState();
        AtomicReference<ScreenId> rendered = new AtomicReference<>();
        state.onChanged(rendered::set);

        state.navigate(ScreenId.WAVE);
        assertEquals(ScreenId.WAVE, state.current());
        assertEquals(ScreenId.WAVE, rendered.get());

        state.navigate(ScreenId.SOURCE);
        assertEquals(ScreenId.SOURCE, state.current());
        assertEquals(ScreenId.SOURCE, rendered.get());
    }
}
