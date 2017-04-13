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
package org.codice.ddf.admin.sources.csw.discover;

import static org.codice.ddf.admin.sources.commons.SourceCommons.DISCOVERED_SOURCES;
import static org.codice.ddf.admin.sources.commons.SourceCommons.DISCOVERED_URL;

import java.util.List;

import org.codice.ddf.admin.api.fields.Field;
import org.codice.ddf.admin.common.actions.BaseAction;
import org.codice.ddf.admin.common.fields.common.AddressField;
import org.codice.ddf.admin.common.fields.common.CredentialsField;
import org.codice.ddf.admin.sources.csw.utils.CswSourceUtils;
import org.codice.ddf.admin.sources.fields.SourceInfoField;
import org.codice.ddf.admin.sources.utils.DiscoveredUrl;

import com.google.common.collect.ImmutableList;

public class DiscoverCswByAddressAction extends BaseAction<SourceInfoField> {

    public static final String ID = "discoverCswByAddress";

    public static final String DESCRIPTION =
            "Attempts to discover CSW sources with the given hostname and port, and optionally a username and password if authentication is enabled.";

    private AddressField addressField = new AddressField();

    private CredentialsField credentialsField = new CredentialsField();

    private CswSourceUtils cswSourceUtils;

    public DiscoverCswByAddressAction() {
        super(ID, DESCRIPTION, new SourceInfoField());
        cswSourceUtils = new CswSourceUtils();

        credentialsField.allFieldsRequired(false);
        addressField.allFieldsRequired(true);
    }

    @Override
    public List<Field> getArguments() {
        return ImmutableList.of(addressField, credentialsField);
    }

    @Override
    public SourceInfoField performAction() {
        String un = credentialsField.username();
        String pw = credentialsField.password();

        String testUrl;

        DiscoveredUrl discoveredUrl = cswSourceUtils.discoverCswUrl(addressField.hostname(),
                addressField.port(),
                un,
                pw);

        testUrl = discoveredUrl.get(DISCOVERED_URL);
        discoveredUrl.getMessages().forEach(this::addReturnValueMessage);

        discoveredUrl = cswSourceUtils.getPreferredCswConfig(testUrl, un, pw);
        discoveredUrl.getMessages().forEach(this::addReturnValueMessage);

        if(discoveredUrl.get(DISCOVERED_SOURCES) != null) {
            SourceInfoField sourceInfoField = new SourceInfoField();
            sourceInfoField.isAvaliable(true);
            sourceInfoField.sourceHandlerName(ID);
            sourceInfoField.configuration(discoveredUrl.get(DISCOVERED_SOURCES));
            return sourceInfoField;
        }

        return null;
    }
}
