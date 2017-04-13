/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.admin.sources.csw.persist;

import static org.codice.ddf.admin.sources.services.CswServiceProperties.servicePropsToCswConfig;

import java.util.List;
import java.util.Map;

import org.codice.ddf.admin.api.fields.Field;
import org.codice.ddf.admin.common.actions.BaseAction;
import org.codice.ddf.admin.configurator.Configurator;
import org.codice.ddf.admin.configurator.ConfiguratorFactory;
import org.codice.ddf.admin.configurator.OperationReport;
import org.codice.ddf.admin.sources.fields.ServicePid;
import org.codice.ddf.admin.sources.fields.SourceInfoField;

import com.google.common.collect.ImmutableList;

public class DeleteCswConfiguration extends BaseAction<SourceInfoField> {

    public static final String ID = "deleteCswSource";

    public static final String DESCRIPTION =
            "Deletes a CSW source configuration and returns the deleted configuration.";

    private ServicePid servicePid = new ServicePid();

    private ConfiguratorFactory configuratorFactory;

    public DeleteCswConfiguration(ConfiguratorFactory configuratorFactory) {
        super(ID, DESCRIPTION, new SourceInfoField());
        this.configuratorFactory = configuratorFactory;

        servicePid.isRequired(true);
    }

    @Override
    public SourceInfoField performAction() {
        return persist();
    }

    @Override
    public List<Field> getArguments() {
        return ImmutableList.of(servicePid);
    }

    public SourceInfoField persist() {
        Configurator configurator = configuratorFactory.getConfigurator();
        Map<String, Object> configToDelete = configurator.getConfig(servicePid.getValue());

        if (configToDelete.isEmpty()) {
            // TODO add error
        }

        configurator.deleteManagedService(servicePid.getValue());
        OperationReport report = configurator.commit("Deleted CSW source with pid [{}].",
                servicePid.getValue());

        if (report.containsFailedResults()) {
            // TODO add error
        }

        SourceInfoField sourceInfoField = new SourceInfoField();
        sourceInfoField.configuration(servicePropsToCswConfig(configToDelete));
        sourceInfoField.sourceHandlerName(ID);
        sourceInfoField.isAvaliable(false);

        return sourceInfoField;
    }

}
