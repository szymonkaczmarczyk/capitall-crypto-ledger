package com.capitall.config;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class MaintenanceModeState {
    private final AtomicBoolean maintenanceMode = new AtomicBoolean(false);

    public boolean isMaintenanceMode() {
        return maintenanceMode.get();
    }

    public void setMaintenanceMode(boolean maintenanceMode) {
        this.maintenanceMode.set(maintenanceMode);
    }
}
