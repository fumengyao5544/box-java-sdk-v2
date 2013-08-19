package com.box.boxjavalibv2.requests.requestobjects;

import java.text.ParseException;
import java.util.Date;

import com.box.boxjavalibv2.dao.BoxSharedLink;
import com.box.boxjavalibv2.dao.BoxSharedLinkPermissions;
import com.box.boxjavalibv2.utils.ISO8601DateParser;

// CHECKSTYLE:OFF
/**
 * Entity for shared link.
 */
public class BoxSharedLinkRequestObject extends BoxDefaultRequestObject {

    private BoxSharedLinkRequestObject() {
    }

    /**
     * SharedLinkRequestObject for create sharedlink request.
     * 
     * @param accessLevel
     *            access(String can be the strings defined in {@link com.box.boxjavalibv2.dao.BoxSharedLinkAccess}.)
     * @return SharedLinkRequestObject
     */
    public static BoxSharedLinkRequestObject createSharedLinkRequestObject(final String accessLevel) {
        return (new BoxSharedLinkRequestObject()).setAccess(accessLevel);
    }

    /**
     * Get access String.
     * 
     * @return access
     */
    public String getAccess() {
        return (String) get(BoxSharedLink.FIELD_ACCESS);
    }

    /**
     * Set access String. This defines who has access to the link. String can be the strings defined in {@link com.box.boxjavalibv2.dao.BoxSharedLinkAccess}.
     * 
     * @param accessLevel
     *            access
     * @return
     */
    public BoxSharedLinkRequestObject setAccess(final String accessLevel) {
        put(BoxSharedLink.FIELD_ACCESS, accessLevel);
        return this;
    }

    /**
     * Get the time to unshare the link. This returns a String and can be parsed into {@link java.util.Date} by
     * {@link com.box.boxjavalibv2.utils.ISO8601DateParser}
     * 
     * @return time to unshare the link
     * @throws ParseException
     */
    public Date getUnshared_at() throws ParseException {
        return ISO8601DateParser.parseSilently((String) get(BoxSharedLink.FIELD_UNSHARED_AT));
    }

    /**
     * Set the time to unshare the link. This String is an ISO8601 time String and can be generated from {@link java.util.Date} by
     * {@link com.box.boxjavalibv2.utils.ISO8601DateParser}
     * 
     * @param unsharedAt
     *            time to unshare the link
     * @return
     */
    public BoxSharedLinkRequestObject setUnshared_at(final Date unsharedAt) {
        String date = unsharedAt != null ? ISO8601DateParser.toString(unsharedAt) : "null";
        put(BoxSharedLink.FIELD_UNSHARED_AT, date);
        return this;
    }

    /**
     * Get permissions.
     * 
     * @return permissions
     */
    public BoxSharedLinkPermissions getPermissions() {
        return (BoxSharedLinkPermissions) get(BoxSharedLink.FIELD_PERMISSIONS);
    }

    /**
     * Set permissions.
     * 
     * @param permissionsEntity
     *            permissions
     * @return
     */
    public BoxSharedLinkRequestObject setPermissions(final BoxSharedLinkPermissions permissionsEntity) {
        put(BoxSharedLink.FIELD_PERMISSIONS, permissionsEntity);
        return this;
    }
}
