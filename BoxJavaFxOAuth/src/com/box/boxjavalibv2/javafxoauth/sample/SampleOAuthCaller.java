package com.box.boxjavalibv2.javafxoauth.sample;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;

import javafx.embed.swing.JFXPanel;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.box.boxjavalibv2.authorization.OAuthDataMessage;
import com.box.boxjavalibv2.events.OAuthEvent;
import com.box.boxjavalibv2.interfaces.IAuthEvent;
import com.box.boxjavalibv2.interfaces.IAuthFlowListener;
import com.box.boxjavalibv2.interfaces.IAuthFlowMessage;
import com.box.boxjavalibv2.javafxoauth.JavaFxOAuthFlow;

/**
 * A sample app using the javafx OAuth UI. This app itself is on java swing.
 */
public class SampleOAuthCaller {

    // TODO: use your own client id and client secret.
    private static final String id = "";
    private static final String secret = "";

    public static void main(String[] args) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final JFrame f = new JFrame();
        f.setExtendedState(JFrame.MAXIMIZED_BOTH);
        final JFXPanel panel = new JFXPanel();
        f.add(panel);
        f.pack();
        f.setVisible(true);

        JavaFxOAuthFlow oauthflow = new JavaFxOAuthFlow(screenSize.width, screenSize.height, screenSize.width, screenSize.height, createAuthFlowListener(f));
        oauthflow.initAuthAndRun(panel, id, secret);
    }

    private static IAuthFlowListener createAuthFlowListener(final Component parentUIComponent) {
        return new IAuthFlowListener() {

            @Override
            public void onAuthFlowMessage(IAuthFlowMessage message) {
            }

            @Override
            public void onAuthFlowException(Exception e) {
            }

            @Override
            public void onAuthFlowEvent(IAuthEvent state, IAuthFlowMessage message) {
                if (state == OAuthEvent.OAUTH_CREATED) {
                    OAuthDataMessage msg = (OAuthDataMessage) message;
                    final String display = "tokens: " + msg.getData().getAccessToken() + "," + msg.getData().getRefreshToken();
                    // Note this event is fired on javafx thread, if your app is like this sample trying to do something on java swing thread on
                    // receiving the message, you need to use SwingUtilities.invokeLater.
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            parentUIComponent.setVisible(false);
                            JOptionPane.showMessageDialog(parentUIComponent, display);
                        }
                    });
                }
            }
        };
    }
}
