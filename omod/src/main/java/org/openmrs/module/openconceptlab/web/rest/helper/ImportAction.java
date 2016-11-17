package org.openmrs.module.openconceptlab.web.rest.helper;

import org.openmrs.module.openconceptlab.Import;

/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * v. 2.0. If a copy of the MPL was not distributed with this file, You
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
public class ImportAction {

    private Import anImport;

    private boolean ignoreAllErrors;

    public ImportAction() {}

    public ImportAction(boolean ignoreAllErrors) {
        this.ignoreAllErrors = ignoreAllErrors;
    }

    public Import getAnImport() {
        return anImport;
    }

    public void setAnImport(Import anImport) {
        this.anImport = anImport;
    }

    public boolean isIgnoreAllErrors() {
        return ignoreAllErrors;
    }

    public void setIgnoreAllErrors(boolean ignoreAllErrors) {
        this.ignoreAllErrors = ignoreAllErrors;
    }
}
