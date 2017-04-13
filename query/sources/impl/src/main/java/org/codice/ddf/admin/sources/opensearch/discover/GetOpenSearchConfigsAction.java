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
package org.codice.ddf.admin.sources.opensearch.discover;

import static org.codice.ddf.admin.sources.services.OpenSearchServiceProperties.OPENSEARCH_FACTORY_PIDS;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.admin.api.fields.Field;
import org.codice.ddf.admin.common.actions.BaseAction;
import org.codice.ddf.admin.configurator.Configurator;
import org.codice.ddf.admin.configurator.ConfiguratorFactory;
import org.codice.ddf.admin.sources.fields.ServicePid;
import org.codice.ddf.admin.sources.fields.SourceInfoField;
import org.codice.ddf.admin.sources.fields.SourceInfoListField;
import org.codice.ddf.admin.sources.services.OpenSearchServiceProperties;

import com.google.common.collect.ImmutableList;

public class GetOpenSearchConfigsAction extends BaseAction<SourceInfoListField> {

    public static final String ID = "openSearchConfigs";

    public static final String DESCRIPTION =
            "Retrieves all currently configured OpenSearch sources. If a source pid is specified, only that source configuration will be returned.";

    private ServicePid servicePid = new ServicePid();

    private ConfiguratorFactory configuratorFactory;

    public GetOpenSearchConfigsAction(ConfiguratorFactory configuratorFactory) {
        super(ID, DESCRIPTION, new SourceInfoListField());
        this.configuratorFactory = configuratorFactory;

        servicePid.isRequired(false);
    }

    @Override
    public List<Field> getArguments() {
        return ImmutableList.of(servicePid);
    }

    @Override
    public SourceInfoListField performAction() {
        SourceInfoListField sourceInfoListField = new SourceInfoListField();
        Configurator configurator = configuratorFactory.getConfigurator();

        OPENSEARCH_FACTORY_PIDS.stream()
                .flatMap(factoryPid -> configurator.getManagedServiceConfigs(factoryPid)
                        .values()
                        .stream())
                .map(OpenSearchServiceProperties::servicePropsToOpenSearchConfig)
                .filter(config -> {
                    if (StringUtils.isNotEmpty(servicePid.getValue())) {
                        return config.servicePid()
                                .equals(servicePid.getValue());
                    }
                    return true;
                })
                .forEach(config -> {
                    SourceInfoField sourceInfoField = new SourceInfoField();
                    sourceInfoField.configuration(config);
                    sourceInfoField.sourceHandlerName(ID);
                    sourceInfoField.isAvaliable(true);
                    sourceInfoListField.add(sourceInfoField);
                });

        return sourceInfoListField;
    }
}
