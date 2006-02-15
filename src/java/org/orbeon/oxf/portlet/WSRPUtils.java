/**
 *  Copyright (C) 2004 Orbeon, Inc.
 *
 *  This program is free software; you can redistribute it and/or modify it under the terms of the
 *  GNU Lesser General Public License as published by the Free Software Foundation; either version
 *  2.1 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 *
 *  The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
 */
package org.orbeon.oxf.portlet;

import org.orbeon.oxf.common.OXFException;
import org.orbeon.oxf.util.NetUtils;
import org.orbeon.oxf.xml.XMLUtils;

import javax.portlet.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;

public class WSRPUtils {
    public static final int URL_TYPE_BLOCKING_ACTION = 1;
    public static final int URL_TYPE_RENDER = 2;
    public static final int URL_TYPE_RESOURCE = 3;

    public static final String BASE_TAG = "wsrp_rewrite";
    public static final String START_TAG = BASE_TAG + "?";
    public static final String END_TAG = "/" + BASE_TAG;
    public static final String PREFIX_TAG = BASE_TAG + "_";

    public static final String URL_TYPE_PARAM = "wsrp-urlType";
    public static final String MODE_PARAM = "wsrp-mode";
    public static final String WINDOW_STATE_PARAM = "wsrp-windowState";
    public static final String NAVIGATIONAL_STATE_PARAM = "wsrp-navigationalState";
    public static final String URL_PARAM = "wsrp-url";
    public static final String REQUIRES_REWRITE_PARAM = "wsrp-requiresRewrite";

    public static final String URL_TYPE_BLOCKING_ACTION_STRING = "blockingAction";
    public static final String URL_TYPE_RENDER_STRING = "render";
    public static final String URL_TYPE_RESOURCE_STRING = "resource";

    private static final int BASE_TAG_LENGTH = BASE_TAG.length();
    private static final int START_TAG_LENGTH = START_TAG.length();
    private static final int END_TAG_LENGTH = END_TAG.length();
    private static final int PREFIX_TAG_LENGTH = PREFIX_TAG.length();

    public static String encodePortletURL(int urlType, String navigationalState, String mode, String windowState, String fragmentId, boolean secure) {
        StringBuffer sb = new StringBuffer(START_TAG);
        sb.append(URL_TYPE_PARAM);
        sb.append('=');

        String urlTypeString = null;
        if (urlType == URL_TYPE_BLOCKING_ACTION)
            urlTypeString = URL_TYPE_BLOCKING_ACTION_STRING;
        else if (urlType == URL_TYPE_RENDER)
            urlTypeString = URL_TYPE_RENDER_STRING;
        else
            throw new IllegalArgumentException();

        sb.append(urlTypeString);

        // Encode mode
        if (mode != null) {
            sb.append('&');
            sb.append(MODE_PARAM);
            sb.append('=');
            sb.append(mode);
        }

        // Encode window state
        if (windowState != null) {
            sb.append('&');
            sb.append(WINDOW_STATE_PARAM);
            sb.append('=');
            sb.append(windowState);
        }

        // Encode navigational state
        if (navigationalState != null) {
            try {
                sb.append('&');
                sb.append(NAVIGATIONAL_STATE_PARAM);
                sb.append('=');
                sb.append(URLEncoder.encode(navigationalState, "utf-8"));
            } catch (UnsupportedEncodingException e) {
                throw new OXFException(e);
            }
        }

        sb.append(END_TAG);

        return sb.toString();
    }

    public static String encodeResourceURL(String url, boolean requiresRewrite) {
        StringBuffer sb = new StringBuffer(START_TAG);
        sb.append(URL_TYPE_PARAM);
        sb.append('=');

        sb.append(URL_TYPE_RESOURCE_STRING);

        // Encode URL
        if (url != null) {
            try {
                sb.append('&');
                sb.append(URL_PARAM);
                sb.append('=');
                sb.append(URLEncoder.encode(url, "utf-8"));

                sb.append('&');
                sb.append(REQUIRES_REWRITE_PARAM);
                sb.append('=');
                sb.append(new Boolean(requiresRewrite).toString());
            } catch (UnsupportedEncodingException e) {
                throw new OXFException(e);
            }
        }

        sb.append(END_TAG);

        return sb.toString();
    }

    public static String encodeNamespacePrefix() {
        return PREFIX_TAG;
    }

    /**
     * This method parses a string containing WSRP Consumer URL and namespace
     * encodings and encode the URLs and namespaces as per the Portlet API.
     *
     * FIXME: Right now, we do not support any type of escaping.
     */
    public static void write(RenderResponse response, String s) throws IOException {
        int stringLength = s.length();
        int currentIndex = 0;
        int index;
        Writer writer = response.getWriter();
        while ((index = s.indexOf(BASE_TAG, currentIndex)) != -1) {
            // Write up to the current mark
            writer.write(s, currentIndex, index - currentIndex);

            // Check if escaping is requested
            if (index + BASE_TAG_LENGTH * 2 <= stringLength
                    && s.substring(index + BASE_TAG_LENGTH, index + BASE_TAG_LENGTH * 2).equals(BASE_TAG)) {
                // Write escaped tag, update index and keep looking
                writer.write(BASE_TAG);
                currentIndex = index + BASE_TAG_LENGTH * 2;
                continue;
            }

            if (index < stringLength - BASE_TAG_LENGTH && s.charAt(index + BASE_TAG_LENGTH) == '?') {
                // URL encoding
                // Find the matching end mark
                int endIndex = s.indexOf(END_TAG, index);
                if (endIndex == -1)
                    throw new OXFException("Missing end tag for WSRP encoded URL.");
                String encodedURL = s.substring(index + START_TAG_LENGTH, endIndex);
//                System.out.println("XXX Found WSRP-encoded URL: " + encodedURL);

                currentIndex = endIndex + END_TAG_LENGTH;

                writer.write(decodePortletURL(encodedURL, response));

            } else if (index < stringLength - BASE_TAG_LENGTH && s.charAt(index + BASE_TAG_LENGTH) == '_') {
                // Namespace encoding
                writer.write(response.getNamespace());
                currentIndex = index + PREFIX_TAG_LENGTH;
            } else {
                throw new OXFException("Invalid wsrp rewrite tagging.");
            }
        }
        // Write remainder of string
        if (currentIndex < stringLength) {
            writer.write(s, currentIndex, s.length() - currentIndex);
        }
    }

    private static String decodePortletURL(String encodedURL, RenderResponse response) {
        // Parse URL
        Map wsrpParameters = NetUtils.decodeQueryString(encodedURL, true);

        // Check URL type and create URL
        try {
            String[] urlTypeValues = (String[]) wsrpParameters.get(URL_TYPE_PARAM);
            if (urlTypeValues == null)
                throw new OXFException("Missing URL type for WSRP encoded URL: " + encodedURL);
            String urlTypeValue = urlTypeValues[0];

            if (urlTypeValue.equals(URL_TYPE_RESOURCE_STRING)) {
                // Case of a resource
                String[] urlValues = (String[]) wsrpParameters.get(URL_PARAM);
                if (urlValues != null) {
                    String url = null;
                    try {
                        url = response.encodeURL(URLDecoder.decode(urlValues[0], "utf-8"));
                    } catch (UnsupportedEncodingException e) {
                        // Should not happen
                        throw new OXFException(e);
                    }
                    return XMLUtils.escapeXML(url);
                }
                // TODO: We ignore a request to rewrite the resource
                return "";
            } else {
                // Case of a render or action request
                // create a PortletURL
                PortletURL portletURL = null;
                if (urlTypeValue.equals(URL_TYPE_BLOCKING_ACTION_STRING))
                    portletURL = response.createActionURL();
                else if (urlTypeValue.equals(URL_TYPE_RENDER_STRING))
                    portletURL = response.createRenderURL();
                else
                    throw new OXFException("Invalid URL type for WSRP encoded URL: " + encodedURL);

                // Get portlet mode
                String[] portletModeValues = (String[]) wsrpParameters.get(MODE_PARAM);
                if (portletModeValues != null) {
                    portletURL.setPortletMode(new PortletMode(portletModeValues[0]));
                }

                // Get window state
                String[] windowStateValues = (String[]) wsrpParameters.get(WINDOW_STATE_PARAM);
                if (windowStateValues != null) {
                    portletURL.setWindowState(new WindowState(windowStateValues[0]));
                }

                // Get navigational state
                String[] navigationalStateValues = (String[]) wsrpParameters.get(NAVIGATIONAL_STATE_PARAM);
                if (navigationalStateValues != null) {
                    String decodedNavigationalState = null;
                    try {
                        decodedNavigationalState = URLDecoder.decode(navigationalStateValues[0], "utf-8");
                    } catch (UnsupportedEncodingException e) {
                        // Should not happen
                        throw new OXFException(e);
                    }
                    Map navigationParameters = NetUtils.decodeQueryString(decodedNavigationalState, true);
                    portletURL.setParameters(navigationParameters);
                }

                // TODO: wsrp-fragmentID
                // TODO: wsrp-secureURL

                // Write resulting encoded PortletURL
                return portletURL.toString();
            }
        } catch (PortletException e) {
            throw new OXFException(e);
        }
    }
}