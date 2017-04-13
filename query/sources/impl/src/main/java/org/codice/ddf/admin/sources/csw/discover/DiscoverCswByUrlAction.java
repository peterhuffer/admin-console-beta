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
package org.codice.ddf.admin.sources.csw.discover;

import static org.codice.ddf.admin.sources.commons.SourceCommons.DISCOVERED_SOURCES;

import java.util.List;

import org.codice.ddf.admin.api.fields.Field;
import org.codice.ddf.admin.common.actions.BaseAction;
import org.codice.ddf.admin.common.fields.common.CredentialsField;
import org.codice.ddf.admin.common.fields.common.UrlField;
import org.codice.ddf.admin.sources.csw.utils.CswSourceUtils;
import org.codice.ddf.admin.sources.fields.SourceInfoField;
import org.codice.ddf.admin.sources.utils.DiscoveredUrl;

import com.google.common.collect.ImmutableList;

public class DiscoverCswByUrlAction extends BaseAction<SourceInfoField> {

    public static final String NAME = "discoverCswByUrl";

    public static final String DESCRIPTION =
            "Attempts to discover aCSW source given a URL, and optional username and password.";

    private UrlField endpointUrl = new UrlField("endpointUrl");

    private CredentialsField credentialsField = new CredentialsField();

    private CswSourceUtils cswSourceUtils;

    public DiscoverCswByUrlAction() {
        super(NAME, DESCRIPTION, new SourceInfoField());
        cswSourceUtils = new CswSourceUtils();

        endpointUrl.isRequired(true);
        credentialsField.allFieldsRequired(false);
    }

    @Override
    public List<Field> getArguments() {
        return ImmutableList.of(endpointUrl, credentialsField);
    }

    @Override
    public SourceInfoField performAction() {
        SourceInfoField sourceInfoField = new SourceInfoField();

        String testUrl = endpointUrl.getValue();
        String un = credentialsField.username();
        String pw = credentialsField.password();

        DiscoveredUrl discoveredUrl = cswSourceUtils.getPreferredCswConfig(testUrl, un, pw);
        discoveredUrl.getMessages()
                .forEach(this::addArgumentMessage);

        if (discoveredUrl.get(DISCOVERED_SOURCES) != null) {
            sourceInfoField.configuration(discoveredUrl.get(DISCOVERED_SOURCES));
        }

        return sourceInfoField;
    }
}
