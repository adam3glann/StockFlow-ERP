package com.adam.warehouse.util;

import com.adam.warehouse.dao.BranchSettingsDAO;
import com.adam.warehouse.model.BranchIdentity;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.application.Platform;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** In-memory, observable copy of branch identity shared by application windows. */
public final class BranchIdentityManager {
    private static final ReadOnlyStringWrapper display = new ReadOnlyStringWrapper();
    private static final ReadOnlyObjectWrapper<BranchIdentity> identity =
            new ReadOnlyObjectWrapper<>(new BranchIdentity("MAIN", "Main Branch", "", 0.0));
    private static final AtomicBoolean refreshStarted = new AtomicBoolean();

    private BranchIdentityManager() { }

    public static void load() {
        set(new BranchSettingsDAO().load());
    }

    /** Refreshes shared branch details changed by another client on this branch server. */
    public static void startAutoRefresh() {
        if (!refreshStarted.compareAndSet(false, true)) return;
        Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "stockflow-branch-settings-refresh");
            thread.setDaemon(true);
            return thread;
        }).scheduleWithFixedDelay(() -> {
            try {
                BranchIdentity branch = new BranchSettingsDAO().load();
                Platform.runLater(() -> set(branch));
            } catch (RuntimeException ignored) {
                // Keep the last known label visible while a brief database outage recovers.
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    public static BranchIdentity get() { return identity.get(); }

    public static ReadOnlyStringProperty displayProperty() { return display.getReadOnlyProperty(); }
    public static ReadOnlyObjectProperty<BranchIdentity> identityProperty() { return identity.getReadOnlyProperty(); }

    public static void set(BranchIdentity branch) {
        identity.set(java.util.Objects.requireNonNull(branch));
        String location = branch.location() == null ? "" : branch.location().trim();
        display.set(location.isBlank() ? branch.name()
                : String.format(LanguageManager.get("branch.header.format"), branch.name(), location));
    }
}
