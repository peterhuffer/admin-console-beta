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
package org.codice.ddf.admin.sources.wfs.utils;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.codice.ddf.admin.sources.commons.SourceCommons.DISCOVERED_SOURCES;
import static org.codice.ddf.admin.sources.commons.SourceCommons.DISCOVERED_URL;
import static org.codice.ddf.admin.sources.commons.SourceCommons.SOURCES_NAMESPACE_CONTEXT;
import static org.codice.ddf.admin.sources.commons.SourceCommons.createDocument;
import static org.codice.ddf.admin.sources.services.WfsServiceProperties.WFS1_FACTORY_PID;
import static org.codice.ddf.admin.sources.services.WfsServiceProperties.WFS2_FACTORY_PID;

import java.util.Collections;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.codice.ddf.admin.sources.commons.SourceCommons;
import org.codice.ddf.admin.sources.fields.type.WfsSourceConfigurationField;
import org.codice.ddf.admin.sources.utils.DiscoveredUrl;
import org.codice.ddf.admin.sources.utils.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.google.common.collect.ImmutableList;

public class WfsSourceUtils {

    // TODO fix javadoc in this class

    private static final Logger LOGGER = LoggerFactory.getLogger(WfsSourceUtils.class);

    public static final String GET_CAPABILITIES_PARAMS = "?service=WFS&request=GetCapabilities";

    private static final List<String> WFS_MIME_TYPES = ImmutableList.of("text/xml",
            "application/xml",
            "text/xml; charset=UTF-8",
            "application/xml; charset=UTF-8");

    private static final String ACCEPT_VERSION_PARAMS = "&AcceptVersions=2.0.0,1.0.0";

    private static final List<String> URL_FORMATS = ImmutableList.of("https://%s:%d/services/wfs",
            "https://%s:%d/wfs",
            "http://%s:%d/services/wfs",
            "http://%s:%d/wfs",
            "http://%s:%d/geoserver/wfs"); // TODO: remove this, for testing

    private static final String WFS_VERSION_EXP = "/wfs:WFS_Capabilities/attribute::version";

    private final RequestUtils requestUtils;

    public WfsSourceUtils() {
        this(new RequestUtils());
    }

    public WfsSourceUtils(RequestUtils requestUtils) {
        this.requestUtils = requestUtils;
    }

    /**
     * Attempts to verify the given URL as a functional WFS endpoint
     * SUCCESS TYPES - VERIFIED_CAPABILITIES,
     * FAILURE TYPES - CANNOT_CONNECT, CERT_ERROR, UNKNOWN_ENDPOINT
     * RETURN TYPES -  DISCOVERED_URL
     *
     * @param url      URL to probe for WFS capabilities
     * @param username Optional username to add to Basic Auth header
     * @param password Optional password to add to Basic Auth header
     * @return report
     */
    public DiscoveredUrl sendWfsCapabilitiesRequest(String url, String username, String password) {
        String reqUrl = url + GET_CAPABILITIES_PARAMS + ACCEPT_VERSION_PARAMS;
        DiscoveredUrl requestResults = requestUtils.sendGetRequest(reqUrl, username, password);
        if (requestResults.hasErrors()) {
            return requestResults;
        }
        int statusCode = requestResults.get(RequestUtils.STATUS_CODE);
        String contentType = requestResults.get(RequestUtils.CONTENT_TYPE);

        if (statusCode == HTTP_OK && WFS_MIME_TYPES.contains(contentType)) {
            requestResults.put(DISCOVERED_URL, url);
            return requestResults;
        }
        requestResults.addMessage(SourceCommons.UNKNOWN_ENDPOINT_MESSAGE);
        return requestResults;
    }

    /**
     * Attempts to discover a WFS endpoint at a given hostname and port
     * SUCCESS TYPES - VERIFIED_CAPABILITIES,
     * FAILURE TYPES - UNKNOWN_ENDPOINT
     * RETURN TYPES - DISCOVERED_URL
     *
     * @param hostname Hostname to probe for WFS capabilities
     * @param port     Port over which to connect to host
     * @param username Optional username to add to Basic Auth header
     * @param password Optional username to add to Basic Auth header
     * @return report
     */
    public DiscoveredUrl discoverWfsUrl(String hostname, int port, String username,
            String password) {
        return URL_FORMATS.stream()
                .map(formatUrl -> String.format(formatUrl, hostname, port))
                .map(url -> sendWfsCapabilitiesRequest(url, username, password))
                .filter(report -> !report.hasErrors())
                .findFirst()
                .orElse(new DiscoveredUrl(Collections.singletonList(SourceCommons.UNKNOWN_ENDPOINT_MESSAGE)));
    }

    /**
     * Attempts to create a WFS configuration from the given url.
     * SUCCESS TYPES - CONFIG_CREATED
     * FAILURE TYPES - CERT_ERROR, UNKNOWN_ENDPOINT, CANNOT_CONNECT
     * RETURN TYPES - DISCOVERED_SOURCES
     *
     * @param url      WFS URL to probe for a configuration
     * @param username Optional username to add to Basic Auth header
     * @param password Optional password to add to Basic Auth header
     * @return report
     */
    public DiscoveredUrl getPreferredWfsConfig(String url, String username, String password) {
        DiscoveredUrl requestReport = sendWfsCapabilitiesRequest(url, username, password);

        if (requestReport.hasErrors()) {
            return requestReport;
        }

        DiscoveredUrl results = new DiscoveredUrl();
        String requestBody = requestReport.get(RequestUtils.CONTENT);
        Document capabilitiesXml;
        try {
            capabilitiesXml = createDocument(requestBody);
        } catch (Exception e) {
            LOGGER.debug("Failed to read response from WFS endpoint.");
            results.addMessage(SourceCommons.INTERNAL_ERROR_MESSAGE);
            return results;
        }

        WfsSourceConfigurationField preferredConfig = new WfsSourceConfigurationField();
        preferredConfig.endpointUrl(url)
                .credentials()
                .username(username)
                .password(password);

        XPath xpath = XPathFactory.newInstance()
                .newXPath();
        xpath.setNamespaceContext(SOURCES_NAMESPACE_CONTEXT);
        String wfsVersion;
        try {
            wfsVersion = xpath.compile(WFS_VERSION_EXP)
                    .evaluate(capabilitiesXml);
        } catch (XPathExpressionException e) {
            LOGGER.debug("Failed to parse XML response.");
            results.addMessage(SourceCommons.INTERNAL_ERROR_MESSAGE);
            return results;
        }
        switch (wfsVersion) {
        case "2.0.0":
            results.put(DISCOVERED_SOURCES, preferredConfig.factoryPid(WFS2_FACTORY_PID));
            return results;
        case "1.0.0":
            results.put(DISCOVERED_SOURCES, preferredConfig.factoryPid(WFS1_FACTORY_PID));
            return results;
        default:
            LOGGER.debug("Unsupported WFS version discovered.");
            results.addMessage(SourceCommons.UNKNOWN_ENDPOINT_MESSAGE);
            return results;
        }
    }
}
