package com.adam.warehouse.service;

import com.adam.warehouse.dao.ApplicationResetDAO;
import com.adam.warehouse.util.PermissionManager;

/** Coordinates the privileged operational-data reset. */
public final class ApplicationResetService {
    private final ApplicationResetDAO resetDAO;

    public ApplicationResetService() {
        this(new ApplicationResetDAO());
    }

    ApplicationResetService(ApplicationResetDAO resetDAO) {
        this.resetDAO = resetDAO;
    }

    public void resetOperationalData() {
        PermissionManager.require(PermissionManager.canOpenSettings(), "reset application data");
        resetDAO.resetOperationalData();
    }
}
