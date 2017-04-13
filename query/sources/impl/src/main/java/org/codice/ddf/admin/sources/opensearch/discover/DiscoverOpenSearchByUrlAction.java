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

import static org.codice.ddf.admin.sources.commons.SourceCommons.DISCOVERED_SOURCES;

import java.util.List;

import org.codice.ddf.admin.api.fields.Field;
import org.codice.ddf.admin.common.actions.BaseAction;
import org.codice.ddf.admin.common.fields.common.CredentialsField;
import org.codice.ddf.admin.common.fields.common.UrlField;
import org.codice.ddf.admin.sources.fields.SourceInfoField;
import org.codice.ddf.admin.sources.opensearch.utils.OpenSearchSourceUtils;
import org.codice.ddf.admin.sources.utils.DiscoveredUrl;

import com.google.common.collect.ImmutableList;

public class DiscoverOpenSearchByUrlAction extends BaseAction<SourceInfoField> {
    public static final String NAME = "discoverOpenSearchByUrl";

    public static final String DESCRIPTION =
            "Attempts to discover an OpenSearch source given a URL, and optional username and password.";

    private UrlField endpointUrl = new UrlField("endpointUrl");

    private CredentialsField credentialsField = new CredentialsField();

    private OpenSearchSourceUtils openSearchSourceUtils;

    public DiscoverOpenSearchByUrlAction() {
        super(NAME, DESCRIPTION, new SourceInfoField());
        openSearchSourceUtils = new OpenSearchSourceUtils();

        endpointUrl.isRequired(true);
        credentialsField.allFieldsRequired(false);
    }

    @Override
    public List<Field> getArguments() {
        return ImmutableList.of(endpointUrl, credentialsField);
    }

    @Override
    public SourceInfoField performAction() {
        String testUrl = endpointUrl.getValue();
        String un = credentialsField.username();
        String pw = credentialsField.password();

        DiscoveredUrl discoveredUrl = openSearchSourceUtils.getOpenSearchConfig(testUrl, un, pw);
        discoveredUrl.getMessages()
                .forEach(this::addArgumentMessage);

        if (discoveredUrl.get(DISCOVERED_SOURCES) != null) {
            SourceInfoField sourceInfoField = new SourceInfoField();
            sourceInfoField.configuration(discoveredUrl.get(DISCOVERED_SOURCES));
            return sourceInfoField;
        }

        return null;
    }
}
