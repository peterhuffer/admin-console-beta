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
package org.codice.ddf.admin.sources.wfs.persist;

import static org.codice.ddf.admin.sources.fields.type.SourceConfigUnionField.ENDPOINT_URL_FIELD;
import static org.codice.ddf.admin.sources.fields.type.SourceConfigUnionField.FACTORY_PID_FIELD;
import static org.codice.ddf.admin.sources.fields.type.SourceConfigUnionField.SOURCE_HOSTNAME_FIELD;
import static org.codice.ddf.admin.sources.services.WfsServiceProperties.WFS_FACTORY_PIDS;
import static org.codice.ddf.admin.sources.services.WfsServiceProperties.wfsConfigToServiceProps;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.admin.api.fields.Field;
import org.codice.ddf.admin.api.fields.ObjectField;
import org.codice.ddf.admin.common.actions.BaseAction;
import org.codice.ddf.admin.configurator.Configurator;
import org.codice.ddf.admin.configurator.ConfiguratorFactory;
import org.codice.ddf.admin.configurator.OperationReport;
import org.codice.ddf.admin.sources.commons.SourceCommons;
import org.codice.ddf.admin.sources.fields.ServicePid;
import org.codice.ddf.admin.sources.fields.SourceInfoField;
import org.codice.ddf.admin.sources.fields.type.WfsSourceConfigurationField;

import com.google.common.collect.ImmutableList;

public class SaveWfsConfiguration extends BaseAction<SourceInfoField> {

    private static final String NAME = "saveWfsSource";

    private static final String DESCRIPTION =
            "Saves a WFS source configuration. If a pid is specified, the source configuration specified by the pid will be updated.";

    private WfsSourceConfigurationField config = new WfsSourceConfigurationField(WFS_FACTORY_PIDS);

    private ServicePid servicePid = new ServicePid();

    private ConfiguratorFactory configuratorFactory;

    public SaveWfsConfiguration(ConfiguratorFactory configuratorFactory) {
        super(NAME, DESCRIPTION, new SourceInfoField());

        config.isRequired(true);
        setRequiredField(config, FACTORY_PID_FIELD, true);
        setRequiredField(config, SOURCE_HOSTNAME_FIELD, true);
        setRequiredField(config, ENDPOINT_URL_FIELD, true);

        this.configuratorFactory = configuratorFactory;
    }

    @Override
    public List<Field> getArguments() {
        return ImmutableList.of(config, servicePid);
    }

    @Override
    public SourceInfoField performAction() {
        if (StringUtils.isNotEmpty(servicePid.getValue())) {
            return updateExistingConfig();
        } else {
            return persistNewConfig();
        }
    }

    private SourceInfoField updateExistingConfig() {
        Map<String, Object> newConfig = wfsConfigToServiceProps(config);
        Configurator configurator = configuratorFactory.getConfigurator();

        configurator.updateConfigFile(servicePid.getValue(), newConfig, true);
        OperationReport report = configurator.commit("Updated config with pid [{}].",
                servicePid.getValue());

        if (report.containsFailedResults()) {
            super.addReturnValueMessage(SourceCommons.FAIL_CONFIG_PERSIST_MESSAGE);
        }

        SourceInfoField sourceInfoField = new SourceInfoField();
        sourceInfoField.sourceHandlerName(NAME);
        sourceInfoField.isAvaliable(true);
        sourceInfoField.configuration(config);

        return sourceInfoField;
    }

    private SourceInfoField persistNewConfig() {
        Configurator configurator = configuratorFactory.getConfigurator();
        configurator.createManagedService(config.factoryPid(), wfsConfigToServiceProps(config));
        OperationReport report = configurator.commit("WFS source saved with details: {}",
                config.toString());

        if (report.containsFailedResults()) {
            super.addReturnValueMessage(SourceCommons.FAIL_CONFIG_PERSIST_MESSAGE);
        }

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
