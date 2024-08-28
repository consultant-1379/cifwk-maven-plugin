package com.ericsson.cifwk.maven.plugin;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

class WebClientWrapper {
    private static final String protocol = "https";
    private static final int port = 443;
    private static Log log;
    private static String errorMsg;

    @SuppressWarnings("deprecation")
    public static HttpClient wrapClient(HttpClient base)  throws MojoExecutionException, MojoFailureException{
        ClientConnectionManager clientConnectionMgr = null;
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            X509TrustManager trustManager = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                }
                public void checkServerTrusted(X509Certificate[] xc, String string) throws CertificateException {
                }
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            sslContext.init(null,new TrustManager[]{trustManager}, null);
            SSLSocketFactory sslSocketFactory = new SSLSocketFactory(sslContext);
            sslSocketFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            clientConnectionMgr = base.getConnectionManager();
            SchemeRegistry sr = clientConnectionMgr.getSchemeRegistry();
            sr.register(new Scheme(protocol, sslSocketFactory, port));
            
        } catch (Exception error) {
            errorMsg = "Error setting up the httpClient for REST calls: " + error;
            log.error(errorMsg);
            throw new MojoFailureException(errorMsg);
        }
        return new DefaultHttpClient(clientConnectionMgr, base.getParams());
    }
}
