package com.capitall.config;

import org.springframework.stereotype.Component;

@Component
public class MaintenanceModeState {
    private boolean maintenanceMode = false;

    public boolean isMaintenanceMode() {
        return maintenanceMode;
    }

    public void setMaintenanceMode(boolean maintenanceMode) {
        this.maintenanceMode = maintenanceMode;
    }
}
