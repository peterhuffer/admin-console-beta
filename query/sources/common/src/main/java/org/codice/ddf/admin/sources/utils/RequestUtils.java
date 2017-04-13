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
package org.codice.ddf.admin.sources.utils;

import static java.net.HttpURLConnection.HTTP_OK;

import static org.codice.ddf.admin.common.message.DefaultMessages.cannotConnectError;
import static org.codice.ddf.admin.common.message.DefaultMessages.certError;
import static org.codice.ddf.admin.common.message.DefaultMessages.unstrustedCaWarning;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HTTP;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.codice.ddf.admin.api.action.Message;
import org.codice.ddf.admin.sources.commons.SourceCommons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.HttpHeaders;

public class RequestUtils {

    // TODO finish javadocs for this class

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestUtils.class);

    public static final String CONTENT_TYPE = "contentType";

    public static final String CONTENT = "content";

    public static final String STATUS_CODE = "statusCode";

    private static final String HTTPS = "https";

    private static final String NONE = "NONE";

    private static final int PING_TIMEOUT = 500;

    /**
     * Sends a get request to the specified URL. It does NOT check the response status code or body.
     * <p>
     * SUCCESS TYPES - EXECUTED_REQUEST
     * WARNING TYPES - UNTRUSTED_CA
     * FAILURE TYPES - CANNOT_CONNECT, CERT_ERROR
     * RETURN TYPES -  CONTENT_TYPE, CONTENT, STATUS_CODE
     *
     * @param url      URL to send Get request to
     * @param userName optional username to add to Basic Auth header
     * @param password optional password to add to Basic Auth header
     * @return report
     */
    public DiscoveredUrl sendGetRequest(String url, String userName, String password) {
        Optional<Message> result = endpointIsReachable(url);
        if (result.isPresent() && result.get()
                .getType() == Message.MessageType.ERROR) {
            return new DiscoveredUrl(Collections.singletonList(result.get()));
        }
        HttpGet request = new HttpGet(url);

        if (url.startsWith(HTTPS) && userName != null && password != null) {
            String auth = Base64.getEncoder()
                    .encodeToString((userName + ":" + password).getBytes());
            request.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + auth);
        }
        return sendHttpRequest(request);
    }

    /**
     * Sends a post request to the specified url. Does not check response code or body.
     *
     * @param url         URL to send Post request to
     * @param username    Optional username to add to Basic Auth header
     * @param password    Optional password to add to Basic Auth header
     * @param contentType Mime type of the post body
     * @param content     Body of the post request
     * @return A {@link DiscoveredUrl} containing containing a discovered URL on success,
     * or an {@link org.codice.ddf.admin.common.message.ErrorMessage} on failure.
     */
    public DiscoveredUrl sendPostRequest(String url, String username, String password,
            String contentType, String content) {
        Optional<Message> message = endpointIsReachable(url);
        if (message.isPresent() && message.get()
                .getType() == Message.MessageType.ERROR) {
            return new DiscoveredUrl(Collections.singletonList(message.get()));
        }

        HttpPost post = new HttpPost(url);
        post.setHeader(HTTP.CONTENT_TYPE, contentType);
        if (url.startsWith("https") && username != null && password != null) {
            String auth = Base64.getEncoder()
                    .encodeToString((username + ":" + password).getBytes());
            post.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + auth);
        }
        try {
            post.setEntity(new StringEntity(content));
        } catch (UnsupportedEncodingException e) {
            return new DiscoveredUrl(Collections.singletonList(SourceCommons.INTERNAL_ERROR_MESSAGE));
        }
        return sendHttpRequest(post);
    }

    protected DiscoveredUrl sendHttpRequest(HttpRequestBase request) {
        DiscoveredUrl discoveredUrl = new DiscoveredUrl();
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = getHttpClient(false);
            response = client.execute(request);
            if (response.getStatusLine()
                    .getStatusCode() == HTTP_OK) {
                LOGGER.debug("Found valid endpoint at [{}].",
                        request.getURI()
                                .toString());
                discoveredUrl.setResponseProperties(responseToMap(response));
                return discoveredUrl;
            } else {
                LOGGER.debug("Received status code [{}] with [{}]",
                        response.getStatusLine()
                                .getStatusCode(),
                        request.getURI()
                                .toString());
                discoveredUrl.addMessage(cannotConnectError("dummyValue"));
                return discoveredUrl;
            }
        } catch (SSLPeerUnverifiedException e) {
            LOGGER.debug("Failed cert check at [{}].",
                    request.getURI()
                            .toString());
            discoveredUrl.addMessage(certError("dummyValue"));
            return discoveredUrl;
        } catch (IOException e) {
            closeClientAndResponse(client, response);
            try {
                client = getHttpClient(true);
                response = client.execute(request);
                if (response.getStatusLine()
                        .getStatusCode() == HTTP_OK) {
                    LOGGER.debug("Untrusted cert from [{}].",
                            request.getURI()
                                    .toString());
                    discoveredUrl.addMessage(unstrustedCaWarning("dummyValue"));
                    discoveredUrl.setResponseProperties(responseToMap(response));
                    return discoveredUrl;
                } else {
                    LOGGER.debug("Failed to connect with cert at [{}]",
                            request.getURI()
                                    .toString());
                    discoveredUrl.addMessage(cannotConnectError("dummyValue"));
                    return discoveredUrl;
                }
            } catch (Exception e1) {
                discoveredUrl.addMessage(cannotConnectError("dummyValue"));
                return discoveredUrl;
            }
        } finally {
            closeClientAndResponse(client, response);
        }
    }

    /**
     * Attempts to open a connection with the given url.
     *
     * @param url - the URL to connect to
     * @return an empty {@link Optional} on connection success, otherwise an {@link Optional} containing a
     * {@link SourceCommons#CANNOT_CONNECT_MESSAGE}
     */
    public Optional<Message> endpointIsReachable(String url) {
        try {
            URLConnection urlConnection = (new URL(url).openConnection());
            urlConnection.setConnectTimeout(PING_TIMEOUT);
            urlConnection.connect();
            LOGGER.debug("Successfully reached {}.", url);
            return Optional.empty();
        } catch (IOException e) {
            LOGGER.debug("Failed to reach {}, returning an error.", url, e);
            return Optional.of(cannotConnectError("exampleValue"));
        }
    }

    /**
     * Attempts to open a connection with the given hostname and port.
     *
     * @param hostname the hostname to connect to
     * @param port     the port to connect to
     * @return an empty {@link Optional} on connection success, otherwise an {@link Optional} containing a
     * {@link SourceCommons#CANNOT_CONNECT_MESSAGE}
     */
    public Optional<Message> endpointIsReachable(String hostname, int port) {
        try (Socket connection = new Socket()) {
            connection.connect(new InetSocketAddress(hostname, port), PING_TIMEOUT);
            connection.close();
            LOGGER.debug("Successfully reached hostname [{}] and port [{]}.", hostname, port);
            return Optional.empty();
        } catch (IOException e) {
            LOGGER.debug("Failed to reach reached hostname [{}] and port [{]}.", hostname, port);
            return Optional.of(cannotConnectError("dummyValue"));
        }
    }

    protected CloseableHttpClient getHttpClient(boolean trustAnyCA) {
        HttpClientBuilder builder = HttpClientBuilder.create()
                .disableAutomaticRetries()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(PING_TIMEOUT)
                        .setSocketTimeout(PING_TIMEOUT)
                        .build());
        if (trustAnyCA) {
            try {
                builder.setSSLSocketFactory(new SSLConnectionSocketFactory(SSLContexts.custom()
                        .loadTrustMaterial(null, (chain, authType) -> true)
                        .build()));
            } catch (Exception e) {
                LOGGER.debug("Unable to load TrustMaterial.");
            }
        }
        return builder.build();
    }

    protected Map<String, Object> responseToMap(CloseableHttpResponse response) throws IOException {
        Map<String, Object> requestResults = new HashMap<>();
        requestResults.put(STATUS_CODE,
                response.getStatusLine()
                        .getStatusCode());

        String contentType = response.getEntity()
                .getContentType() == null ?
                NONE :
                response.getEntity()
                        .getContentType()
                        .getValue();
        requestResults.put(CONTENT_TYPE, contentType);
        requestResults.put(CONTENT, EntityUtils.toString(response.getEntity()));
        return requestResults;
    }

    protected void closeClientAndResponse(CloseableHttpClient client,
            CloseableHttpResponse response) {
        try {
            if (client != null) {
                client.close();
            }
            if (response != null) {
                response.close();
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to close client or response.");
        }
    }
}
