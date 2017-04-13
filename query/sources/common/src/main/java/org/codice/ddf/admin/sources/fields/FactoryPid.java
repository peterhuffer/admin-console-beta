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
package org.codice.ddf.admin.sources.fields;

import static org.codice.ddf.admin.sources.validation.SourceValidationUtils.validateSourceFactoryPid;

import java.util.ArrayList;
import java.util.List;

import org.codice.ddf.admin.api.action.Message;
import org.codice.ddf.admin.common.fields.base.scalar.StringField;

public class FactoryPid extends StringField {
    public static final String DEFAULT_FIELD_NAME = "factoryPid";

    public static final String FIELD_TYPE_NAME = "FactoryPid";

    public static final String DESCRIPTION =
            "A unique ID used for persisting a configuration with a factory.";

    List<String> validFactoryPids;

    public FactoryPid() {
        this(DEFAULT_FIELD_NAME);
    }

    public FactoryPid(String fieldName) {
        super(fieldName, FIELD_TYPE_NAME, DESCRIPTION);
        validFactoryPids = new ArrayList<>();
    }

    @Override
    public List<Message> validate() {
        List<Message> messages = new ArrayList<>();
        messages.addAll(validateSourceFactoryPid(getValue(), validFactoryPids, fieldName()));
        return messages;
    }

    public FactoryPid setValidFactoryPids(List<String> factoryPids) {
        validFactoryPids = factoryPids;
        return this;
    }
}
