package de.ct.ctificate;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * Loads data over https with a certain CA-certificate.
 */
public class MyCertRequest extends AsyncTask<String, Void, Integer> {

    TextView resultView;
    String pem;

    public MyCertRequest(String certificatePem, TextView view) {
        this.pem = certificatePem;
        this.resultView = view;
    }

    private KeyStore buildKeystore() throws
            CertificateException, KeyStoreException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        InputStream caInput;
        try {
            caInput = new ByteArrayInputStream(this.pem.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            caInput = new ByteArrayInputStream(this.pem.getBytes());
        }
        Certificate ca;
        try {
            ca = cf.generateCertificate(caInput);
        } finally {
            try {
                caInput.close();
            } catch (IOException ioError) {
                ioError.printStackTrace();
            }
        }
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try {
            keyStore.load(null, null);
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
        keyStore.setCertificateEntry("ca", ca);
        return keyStore;
    }

    private TrustManager[] getTrustManagers() throws
            NoSuchAlgorithmException, KeyStoreException, CertificateException {
        String trustManagerAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(trustManagerAlgorithm);
        tmf.init(buildKeystore());
        return tmf.getTrustManagers();
    }

    private SSLContext getSSLContext() throws
            NoSuchAlgorithmException, CertificateException,
            KeyStoreException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, getTrustManagers(), null);
        return sslContext;
    }

    private HttpsURLConnection establishHttpsConnection(String urlString) throws
            CertificateException, MalformedURLException,
            NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
        URL url = new URL(urlString);
        try {
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setSSLSocketFactory(getSSLContext().getSocketFactory());
            connection.setDoOutput(true);
            connection.setDoInput(true);
            return connection;
        } catch (IOException ioError) {
            ioError.printStackTrace();
            throw new MalformedURLException(ioError.getMessage());
        }
    }

    @Override
    protected Integer doInBackground(String... params) {
        HttpsURLConnection connection;
        try {
            connection = establishHttpsConnection(params[0]);
            DataOutputStream requestStream = new DataOutputStream(connection.getOutputStream());
            requestStream.writeBytes(params[1]);
            requestStream.flush();
            requestStream.close();
            InputStream responseStream = connection.getInputStream();
            InputStreamReader responseReader = new InputStreamReader(responseStream);
            BufferedReader bufferedResponseReader = new BufferedReader(responseReader);
            String response = "";
            String responseLine;
            while ((responseLine = bufferedResponseReader.readLine()) != null) {
                response += responseLine;
            }
            Log.d("response", response);
            return 0;
        } catch (CertificateException certificateError) {
            certificateError.printStackTrace();
            return 1;
        } catch (IOException ioError) {
            ioError.printStackTrace();
            return 2;
        } catch (KeyStoreException keyStoreError) {
            keyStoreError.printStackTrace();
            return 3;
        } catch (KeyManagementException keyManagementError) {
            keyManagementError.printStackTrace();
            return 4;
        } catch (NoSuchAlgorithmException algorithmError) {
            algorithmError.printStackTrace();
            return 5;
        }
    }

    @Override
    protected void onPostExecute(Integer status) {
        switch (status) {
            case 0:
                this.resultView.setText(R.string.successfullyTested);
                break;
            case 1:
                this.resultView.setText(R.string.testFailed);
                break;
            case 2:
                this.resultView.setText(R.string.testIoProblem);
                break;
            case 3:
                this.resultView.setText(R.string.testKeyStoreProblem);
                break;
            case 4:
                this.resultView.setText(R.string.testKeyManagementProblem);
                break;
            case 5:
                this.resultView.setText(R.string.testNoSuchAlgorithm);
                break;
        }
    }
}
