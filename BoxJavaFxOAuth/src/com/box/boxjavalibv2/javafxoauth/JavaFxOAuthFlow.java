package com.box.boxjavalibv2.javafxoauth;

import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;

import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;

import com.box.boxjavalibv2.BoxClient;
import com.box.boxjavalibv2.authorization.OAuthDataMessage;
import com.box.boxjavalibv2.authorization.OAuthWebViewData;
import com.box.boxjavalibv2.dao.BoxOAuthToken;
import com.box.boxjavalibv2.events.OAuthEvent;
import com.box.boxjavalibv2.interfaces.IAuthFlowListener;
import com.box.boxjavalibv2.interfaces.IAuthFlowUI;
import com.box.boxjavalibv2.requests.requestobjects.BoxOAuthRequestObject;
import com.box.restclientv2.exceptions.BoxRestException;
import com.box.restclientv2.httpclientsupport.HttpClientURIBuilder;

/**
 * Java OAuth UI running on javafx. Note all UI's are on javafx specific thread. The IAuthFlowListener call backs will also be executed in this thread. In case
 * the other part of your application runs on a java swing thread, you want to handle carefully so that your logic triggering java swing data change should be
 * put into a java swing thread. For example by using SwingUtilities.invokeLater(runnable) method.
 */
public class JavaFxOAuthFlow implements IAuthFlowUI {

    private final AtomicBoolean oauthCreated = new AtomicBoolean(false);
    private OAuthWebViewData mWebViewData;
    private BoxClient client;
    private WebEngine webEngine;
    private WebView webView;
    private final IAuthFlowListener oauthListener;
    private final double maxWidth;
    private final double maxHeight;
    private final double minWidth;
    private final double minHeight;

    public JavaFxOAuthFlow(double webViewMaxWidth, double webViewMaxHeight, double webViewMinWidth, double webViewMinHeight, IAuthFlowListener listener) {
        this.oauthListener = listener;
        this.maxWidth = webViewMaxWidth;
        this.maxHeight = webViewMaxHeight;
        this.minWidth = webViewMinWidth;
        this.minHeight = webViewMinHeight;
    }

    public WebView getWebView() {
        return webView;
    }

    /**
     * Always use this, this handles the issue that javafx needs to run in it's own thread.
     * 
     * @param panel
     * @param clientId
     * @param clientSecret
     */
    public void initAuthAndRun(final JFXPanel panel, final String clientId, final String clientSecret) {
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
                initializeAuthFlow(new BoxClient(clientId, clientSecret), null);
                Group group = new Group();
                Scene scene = new Scene(group);

                panel.setScene(scene);
                group.getChildren().add(getWebView());

                authenticate(oauthListener);
            }
        });
    }

    @Override
    public void initializeAuthFlow(BoxClient boxClient, Object applicationContext) {
        client = boxClient;
        mWebViewData = new OAuthWebViewData(boxClient.getOAuthDataController());
        webView = new WebView();
        webView.setMinSize(minWidth, minHeight);
        webView.setMaxSize(maxWidth, maxHeight);
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);
        webEngine.setOnStatusChanged(createEventHandler());
    }

    @Override
    public void authenticate(IAuthFlowListener listener) {
        try {
            webEngine.load(mWebViewData.buildUrl().toString());
        }
        catch (URISyntaxException e) {
            oauthListener.onAuthFlowException(e);
        }
    }

    private void exitSuccess(final BoxOAuthToken token) {
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
                try {
                    oauthListener.onAuthFlowEvent(OAuthEvent.OAUTH_CREATED, new OAuthDataMessage(token));
                }
                catch (BoxRestException e) {
                    oauthListener.onAuthFlowException(e);
                }
                Platform.exit();
            }
        });

    }

    private void exitException(final Exception e) {
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
                oauthListener.onAuthFlowException(e);
                Platform.exit();
            }
        });
    }

    private EventHandler<WebEvent<String>> createEventHandler() {
        return new EventHandler<WebEvent<String>>() {

            @Override
            public void handle(WebEvent<String> event) {
                if (event.getSource() instanceof WebEngine) {
                    WebEngine engine = (WebEngine) event.getSource();
                    String url = engine.getLocation();
                    String code = getResponseValueFromUrl(url);
                    if (StringUtils.isNotEmpty(code)) {
                        webEngine.getLoadWorker().cancel();
                        startCreateOAuth(code);
                    }
                }
            }
        };
    }

    private void startCreateOAuth(final String code) {
        if (oauthCreated.getAndSet(true)) {
            return;
        }

        Thread t = new Thread() {

            @Override
            public void run() {
                BoxOAuthToken oauth = null;
                try {
                    oauth = client.getOAuthManager().createOAuth(
                        BoxOAuthRequestObject.createOAuthRequestObject(code, mWebViewData.getClientId(), mWebViewData.getClientSecret(),
                            mWebViewData.getRedirectUrl()));
                    exitSuccess(oauth);
                }
                catch (Exception e) {
                    exitException(e);
                }
            }
        };
        t.start();

        while (t.isAlive()) {
        }

    }

    /**
     * Get response value.
     * 
     * @param url
     *            url
     * @return response value
     * @throws URISyntaxException
     *             exception
     */
    private String getResponseValueFromUrl(final String url) {
        HttpClientURIBuilder builder;
        try {
            builder = new HttpClientURIBuilder(url);
        }
        catch (URISyntaxException e) {
            return null;
        }

        List<NameValuePair> query = builder.getQueryParams();
        for (NameValuePair pair : query) {
            if (pair.getName().equalsIgnoreCase(mWebViewData.getResponseType())) {
                return pair.getValue();
            }
        }
        return null;
    }
}
