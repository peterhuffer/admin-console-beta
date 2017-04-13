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
 **/
package org.codice.ddf.admin.sources.csw.persist;

import static org.codice.ddf.admin.sources.fields.type.SourceConfigUnionField.ENDPOINT_URL_FIELD;
import static org.codice.ddf.admin.sources.fields.type.SourceConfigUnionField.FACTORY_PID_FIELD;
import static org.codice.ddf.admin.sources.fields.type.SourceConfigUnionField.SOURCE_HOSTNAME_FIELD;
import static org.codice.ddf.admin.sources.services.CswServiceProperties.CSW_FACTORY_PIDS;
import static org.codice.ddf.admin.sources.services.CswServiceProperties.cswConfigToServiceProps;

import java.util.List;

import org.codice.ddf.admin.api.fields.Field;
import org.codice.ddf.admin.api.fields.ObjectField;
import org.codice.ddf.admin.common.actions.BaseAction;
import org.codice.ddf.admin.configurator.Configurator;
import org.codice.ddf.admin.configurator.ConfiguratorFactory;
import org.codice.ddf.admin.configurator.OperationReport;
import org.codice.ddf.admin.sources.fields.ServicePid;
import org.codice.ddf.admin.sources.fields.SourceInfoField;
import org.codice.ddf.admin.sources.fields.type.CswSourceConfigurationField;

import com.google.common.collect.ImmutableList;

public class SaveCswConfiguration extends BaseAction<SourceInfoField> {

    public static final String NAME = "saveCswSource";

    public static final String DESCRIPTION =
            "Saves a CSW source configuration. If a pid is specified, the source configuration specified by the pid will be updated.";

    private CswSourceConfigurationField config = new CswSourceConfigurationField(CSW_FACTORY_PIDS);

    private ServicePid servicePid = new ServicePid();

    private ConfiguratorFactory configuratorFactory;

    public SaveCswConfiguration(ConfiguratorFactory configuratorFactory) {
        super(NAME, DESCRIPTION, new SourceInfoField());

        config.isRequired(true);
        setRequiredField(config, FACTORY_PID_FIELD, true);
        setRequiredField(config, SOURCE_HOSTNAME_FIELD, true);
        setRequiredField(config, ENDPOINT_URL_FIELD, true);

        this.configuratorFactory = configuratorFactory;
    }

    @Override
    public SourceInfoField performAction() {
        return persist();
    }

    @Override
    public List<Field> getArguments() {
        return ImmutableList.of(config, servicePid);
    }

    // TODO: if pid is provided, attempt to update existing configuration
    public SourceInfoField persist() {
        Configurator configurator = configuratorFactory.getConfigurator();
        configurator.createManagedService(config.factoryPid(), cswConfigToServiceProps(config));
        OperationReport report = configurator.commit("CSW source saved with details: {}",
                config.toString());
        SourceInfoField sourceInfoField = new SourceInfoField();
        sourceInfoField.configuration(config);
        sourceInfoField.isAvaliable(true);
        sourceInfoField.sourceHandlerName(NAME);

        return sourceInfoField;
    }

    // TODO: 4/13/17 refactor this out somewhere
    private void setRequiredField(ObjectField objectField, String fieldName, boolean required) {
        objectField.getFields()
                .stream()
                .filter(field -> field.fieldName()
                        .equals(fieldName))
                .findFirst()
                .ifPresent(field -> field.isRequired(required));
    }
}
