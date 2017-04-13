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
import static org.codice.ddf.admin.sources.commons.SourceCommons.DISCOVERED_URL;

import java.util.List;

import org.codice.ddf.admin.api.fields.Field;
import org.codice.ddf.admin.common.actions.BaseAction;
import org.codice.ddf.admin.common.fields.common.AddressField;
import org.codice.ddf.admin.common.fields.common.CredentialsField;
import org.codice.ddf.admin.sources.fields.SourceInfoField;
import org.codice.ddf.admin.sources.opensearch.utils.OpenSearchSourceUtils;
import org.codice.ddf.admin.sources.utils.DiscoveredUrl;

import com.google.common.collect.ImmutableList;

public class DiscoverOpenSearchByAddressAction extends BaseAction<SourceInfoField> {

    public static final String ID = "discoverOpenSearchByAddress";

    public static final String DESCRIPTION =
            "Attempts to discover OpenSearch sources with the given hostname and port, and optionally a username and password if authentication is enabled.";

    private AddressField addressField = new AddressField();

    private CredentialsField credentialsField = new CredentialsField();

    private OpenSearchSourceUtils openSearchSourceUtils;

    public DiscoverOpenSearchByAddressAction() {
        super(ID, DESCRIPTION, new SourceInfoField());
        openSearchSourceUtils = new OpenSearchSourceUtils();

        addressField.allFieldsRequired(true);
        credentialsField.allFieldsRequired(false);
    }

    @Override
    public List<Field> getArguments() {
        return ImmutableList.of(addressField, credentialsField);
    }

    @Override
    public SourceInfoField performAction() {
        SourceInfoField sourceInfoField = new SourceInfoField();
        sourceInfoField.isAvaliable(true);
        sourceInfoField.sourceHandlerName(ID);

        String un = credentialsField.username();
        String pw = credentialsField.password();

        String testUrl;

        DiscoveredUrl discoveredUrl =
                openSearchSourceUtils.discoverOpenSearchUrl(addressField.hostname(),
                        addressField.port(),
                        un,
                        pw);

        testUrl = discoveredUrl.get(DISCOVERED_URL);
        discoveredUrl.getMessages()
                .forEach(this::addArgumentMessage);

        DiscoveredUrl anotherOne = openSearchSourceUtils.getOpenSearchConfig(testUrl, un, pw);
        anotherOne.getMessages()
                .forEach(this::addArgumentMessage);

        if (anotherOne.get(DISCOVERED_SOURCES) != null) {
            sourceInfoField.configuration(anotherOne.get(DISCOVERED_SOURCES));
        }

        return sourceInfoField;
    }
}
