package com.box.boxjavalibv2.requests;

import com.box.boxjavalibv2.dao.BoxResourceType;
import com.box.boxjavalibv2.requests.requestobjects.BoxDefaultRequestObject;
import com.box.restclientv2.RestMethod;
import com.box.restclientv2.exceptions.BoxRestException;
import com.box.restclientv2.interfaces.IBoxConfig;
import com.box.restclientv2.requests.DefaultBoxRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Request to create a shared link for a file or folder.
 */
public class CreateSharedLinkRequest extends DefaultBoxRequest {

    private static final String URI = "/%s/%s";

    /**
     * Constructor.
     * 
     * @param config
     *            config
     * @param objectMapper
     *            object mapper
     * @param id
     *            id of the item
     * @param requestObject
     *            request object
     * @param type
     *            type of the item
     * 
     * @throws BoxRestException
     *             exception
     */
    public CreateSharedLinkRequest(final IBoxConfig config, final ObjectMapper objectMapper, final String id, BoxDefaultRequestObject requestObject,
        final BoxResourceType type) throws BoxRestException {
        super(config, objectMapper, getUri(id, type), RestMethod.PUT, requestObject);
    }

    /**
     * Get uri.
     * 
     * @param fileFolderId
     *            id of this file/folder
     * @param type
     *            whether it is folder
     * @return uri
     */
    public static String getUri(final String fileFolderId, final BoxResourceType type) {
        return String.format(URI, type.toPluralString(), fileFolderId);
    }
}
