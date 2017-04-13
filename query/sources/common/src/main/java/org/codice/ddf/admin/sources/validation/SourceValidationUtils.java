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
package org.codice.ddf.admin.sources.validation;

import static org.codice.ddf.admin.sources.utils.ValidationUtils.validateString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.admin.api.action.Message;
import org.codice.ddf.admin.common.message.DefaultMessages;
import org.codice.ddf.admin.configurator.Configurator;
import org.codice.ddf.admin.sources.commons.SourceCommons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceValidationUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(SourceValidationUtils.class);

    public static List<Message> validateSourceFactoryPid(String factoryPid, List<String> pidList,
            String fieldId) {
        List<Message> errors = validateString(factoryPid, fieldId);
        if (errors.isEmpty() && !pidList.contains(factoryPid)) {
            errors.add(DefaultMessages.invalidFieldError(fieldId));
        }
        return errors;
    }

    public static List<Message> validateOptionalUsernameAndPassword(String username,
            String password, String userNameId, String passwordId) {
        List<Message> validationResults = new ArrayList<>();
        if (username != null) {
            //            validationResults.addAll(configuration.validate(Arrays.asList(SOURCE_USERNAME,
            //                    SOURCE_USER_PASSWORD)));
        }
        return validationResults;
    }

    /**
     * Validates the {@code sourceName} against the existing source names of the configuration's for
     * the given {@code factoryPids} using the {@code configurator}. An empty {@link List} will be returned
     * if there are no existing source names with with name {@code sourceName}, or a {@link List} containing
     * {@link Message}s if there are errors. If the {@code configurator} is {@code null}, one will
     * be created.
     *
     * @param sourceName   a non null name to validate
     * @param factoryPids  a list of non null factory pids of the configuration to validate names against
     * @param configurator configurator to fetch configurations for the given {@code factoryPids}
     * @return a {@link List} of {@link Message}s containing failure commons, or empty {@link List}
     * if there are no duplicate source names found
     */
    public List<Message> validateSourceName(String sourceName, List<String> factoryPids,
            Configurator configurator, String fieldId) {
        List<Message> errors = validateString(sourceName, fieldId);
        if (!errors.isEmpty()) {
            return errors;
        }

        List<Map<String, Map<String, Object>>> configurations = factoryPids.stream()
                .map(configurator::getManagedServiceConfigs)
                .filter(config -> !config.isEmpty())
                .collect(Collectors.toList());

        for (Map<String, Map<String, Object>> config : configurations) {
            for (Map<String, Object> entry : config.values()) {
                Object id = entry.get("id");
                if (id instanceof String) {
                    String name = (String) id;
                    if (StringUtils.isNotEmpty(name) && sourceName.equals(name)) {
                        LOGGER.debug(
                                "Found duplicate existing source name when creating source with name \"{}\"",
                                sourceName);

                        errors.add(SourceCommons.INVALID_FIELD_MESSAGE);
                        return errors;
                    }
                } else {
                    LOGGER.debug(
                            "Unable to validate duplicity of the incoming configuration's source name \"{}\"",
                            sourceName);
                    errors.add(SourceCommons.INTERNAL_ERROR_MESSAGE);
                    return errors;
                }
            }
        }

        return Collections.emptyList();
    }

    //    public static List<ConfigurationMessage> validateForSourceDiscovery(
    //            SourceConfiguration configuration) {
    //        List<ConfigurationMessage> results = configuration.endpointUrl() == null ?
    //                validateHostnameAndPort(configuration) :
    //                validateEndpointUrl(configuration);
    //        results.addAll(validateOptionalUsernameAndPassword(configuration));
    //        return results;
    //    }
    //
    //    public static List<ConfigurationMessage> validateHostnameAndPort(SourceConfiguration configuration) {
    //        List<ConfigurationMessage> validationResults = new ArrayList<>();
    //        if (configuration.sourceHostName() != null) {
    //            validationResults.addAll(configuration.validate(Arrays.asList(SOURCE_HOSTNAME, PORT)));
    //        } else {
    //            validationResults.add(createInvalidFieldMsg("No hostname provided.", SOURCE_HOSTNAME));
    //        }
    //        return validationResults;
    //    }
    //
    //    public static List<ConfigurationMessage> validateEndpointUrl(SourceConfiguration configuration) {
    //        List<ConfigurationMessage> validationResults = new ArrayList<>();
    //        if (configuration.endpointUrl() != null) {
    //            validationResults.addAll(configuration.validate(Collections.singletonList(ENDPOINT_URL)));
    //        } else {
    //            validationResults.add(createInvalidFieldMsg("No URL provided.", ENDPOINT_URL));
    //        }
    //        return validationResults;
    //    }
}