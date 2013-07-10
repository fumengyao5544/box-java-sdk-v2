package com.box.boxjavalibv2.jacksonparser;

import com.box.boxjavalibv2.dao.BoxCollaboration;
import com.box.boxjavalibv2.dao.BoxCollection;
import com.box.boxjavalibv2.dao.BoxComment;
import com.box.boxjavalibv2.dao.BoxEmailAlias;
import com.box.boxjavalibv2.dao.BoxEvent;
import com.box.boxjavalibv2.dao.BoxFile;
import com.box.boxjavalibv2.dao.BoxFileVersion;
import com.box.boxjavalibv2.dao.BoxFolder;
import com.box.boxjavalibv2.dao.BoxItem;
import com.box.boxjavalibv2.dao.BoxLock;
import com.box.boxjavalibv2.dao.BoxOAuthToken;
import com.box.boxjavalibv2.dao.BoxPreview;
import com.box.boxjavalibv2.dao.BoxRealTimeServer;
import com.box.boxjavalibv2.dao.BoxResourceType;
import com.box.boxjavalibv2.dao.BoxServerError;
import com.box.boxjavalibv2.dao.BoxTypedObject;
import com.box.boxjavalibv2.dao.BoxUser;
import com.box.boxjavalibv2.dao.BoxWebLink;
import com.box.boxjavalibv2.interfaces.IBoxResourceHub;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;

public class BoxResourceHub implements IBoxResourceHub {

    private final ObjectMapper mObjectMapper;

    public BoxResourceHub() {
        mObjectMapper = new ObjectMapper();
        mObjectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        mObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        registerBoxObjectsSubtypes();
    }

    @Override
    public ObjectMapper getObjectMapper() {
        return mObjectMapper;
    }

    protected void registerBoxObjectsSubtypes() {
        for (BoxResourceType type : BoxResourceType.values()) {
            mObjectMapper.registerSubtypes(new NamedType(getClass(type), type.toString()));
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Class getClass(BoxResourceType type) {
        switch (type) {
            case FILE:
                return BoxFile.class;
            case PREVIEW:
                return BoxPreview.class;
            case FOLDER:
                return BoxFolder.class;
            case WEB_LINK:
                return BoxWebLink.class;
            case USER:
                return BoxUser.class;
            case FILE_VERSION:
                return BoxFileVersion.class;
            case ITEM:
                return BoxItem.class;
            case COMMENT:
                return BoxComment.class;
            case COLLABORATION:
                return BoxCollaboration.class;
            case EMAIL_ALIAS:
                return BoxEmailAlias.class;
            case OAUTH_DATA:
                return BoxOAuthToken.class;
            case EVENT:
                return BoxEvent.class;
            case REALTIME_SERVER:
                return BoxRealTimeServer.class;
            case LOCK:
                return BoxLock.class;
            case ERROR:
                return BoxServerError.class;
            case ITEMS:
            case FILES:
            case USERS:
            case COMMENTS:
            case FILE_VERSIONS:
            case COLLABORATIONS:
            case EMAIL_ALIASES:
                return BoxCollection.class;
            default:
                return BoxTypedObject.class;
        }
    }
}
