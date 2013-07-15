package com.box.boxjavalibv2.authorization;

import com.box.boxjavalibv2.interfaces.IAuthData;

public interface OAuthRefreshListener {

    void onRefresh(IAuthData newAuthData);
}
