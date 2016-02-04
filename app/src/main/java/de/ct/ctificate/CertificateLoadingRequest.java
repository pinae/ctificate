package de.ct.ctificate;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ExpandableListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * This task loads certificate information from a SSL host.
 */
public class CertificateLoadingRequest extends AsyncTask<String, Void, JSONObject> {
    private ExpandableListView list;
    private Context context;

    public CertificateLoadingRequest(Context context, ExpandableListView list) {
        this.context = context;
        this.list = list;
    }

    private JSONObject getErrorResponse(String error) {
        JSONObject errorResponse = new JSONObject();
        try {
            errorResponse.put("Error", error);
        } catch (JSONException jsonError) {
            jsonError.printStackTrace();
        }
        return errorResponse;
    }

    private SSLContext getSSLContext() throws
            NoSuchAlgorithmException,
            KeyManagementException,
            KeyStoreException,
            CertificateException,
            IOException {
        SSLContext sslcontext = SSLContext.getInstance("TLS");
        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}

                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }
        };
        sslcontext.init(null, trustAllCerts, null);
        return sslcontext;
    }

    private String formatWithColons(String input) {
        String remainder = input;
        String output = "";
        while (remainder.length() > 2) {
            output += remainder.substring(0, 2) + ":";
            remainder = remainder.substring(2);
        }
        return output + remainder;
    }

    @Override
    protected JSONObject doInBackground(String... params) {
        try {
            URL url = new URL(params[0]);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setSSLSocketFactory(getSSLContext().getSocketFactory());
            connection.connect();
            Certificate[] chain = connection.getServerCertificates();
            JSONObject result = new JSONObject();
            JSONArray jsonChain = new JSONArray();
            for (Certificate cert : chain) {
                JSONObject jsonCert = new JSONObject();
                Pattern subjectPattern = Pattern.compile("CN=([^,]+)");
                Matcher m = subjectPattern.matcher(
                        ((X509Certificate) cert).getSubjectDN().getName());
                if (m.find()) {
                    jsonCert.put("subject", m.group(1));
                } else {
                    jsonCert.put("subject", ((X509Certificate) cert).getSubjectDN().getName());
                }
                jsonCert.put("description", cert.toString());
                byte[] binaryCertificate = cert.getEncoded();
                jsonCert.put("pem", "-----BEGIN CERTIFICATE-----\n" +
                        Base64.encodeToString(binaryCertificate, Base64.DEFAULT) +
                        "\n-----END CERTIFICATE-----");
                MessageDigest digest = MessageDigest.getInstance("MD5");
                digest.update(binaryCertificate);
                jsonCert.put("md5", this.formatWithColons(Hextools.bytesToHex(digest.digest())));
                digest = MessageDigest.getInstance("SHA1");
                digest.update(binaryCertificate);
                jsonCert.put("sha1", this.formatWithColons(Hextools.bytesToHex(digest.digest())));
                digest = MessageDigest.getInstance("SHA256");
                digest.update(binaryCertificate);
                jsonCert.put("sha256", this.formatWithColons(Hextools.bytesToHex(digest.digest())));
                jsonChain.put(jsonCert);
            }
            result.put("chain", jsonChain);
            return result;
        } catch (MalformedURLException urlError) {
            urlError.printStackTrace();
            return this.getErrorResponse("malformatted url");
        } catch (IOException ioError) {
            ioError.printStackTrace();
            return this.getErrorResponse("io error while opening connection");
        } catch (NoSuchAlgorithmException algorithmError) {
            algorithmError.printStackTrace();
            return this.getErrorResponse("no tls support");
        } catch (KeyManagementException keyManagementError) {
            keyManagementError.printStackTrace();
            return this.getErrorResponse("broken key management");
        } catch (KeyStoreException keyStoreError) {
            keyStoreError.printStackTrace();
            return this.getErrorResponse("broken key store");
        } catch (CertificateException certificateError) {
            certificateError.printStackTrace();
            return this.getErrorResponse("invalid certificate");
        } catch (JSONException jsonError) {
            jsonError.printStackTrace();
            return this.getErrorResponse("json problem");
        }
    }

    @Override
    protected void onPostExecute(JSONObject result) {
        if (result.has("error")) {
            try {
                String error = result.getString("error");
                Log.e("error", error);
            } catch (JSONException jsonError) {
                jsonError.printStackTrace();
            }
        } else if (result.has("chain")) {
            try {
                JSONArray chain = result.getJSONArray("chain");
                this.list.setAdapter(new CertificateChainAdapter(
                        (LayoutInflater) this.context.getSystemService(
                                Context.LAYOUT_INFLATER_SERVICE),
                        chain));
            } catch (JSONException jsonError) {
                jsonError.printStackTrace();
            }
        }
    }
}
