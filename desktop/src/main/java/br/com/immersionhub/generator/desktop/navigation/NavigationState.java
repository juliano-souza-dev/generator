package br.com.immersionhub.generator.desktop.navigation;

import java.util.Objects;
import java.util.function.Consumer;

public final class NavigationState {
    private ScreenId current = ScreenId.SOURCE;
    private Consumer<ScreenId> listener = ignored -> {};

    public ScreenId current() {
        return current;
    }

    public void onChanged(Consumer<ScreenId> listener) {
        this.listener = Objects.requireNonNull(listener);
    }

    public void navigate(ScreenId next) {
        current = Objects.requireNonNull(next);
        listener.accept(current);
    }
}
