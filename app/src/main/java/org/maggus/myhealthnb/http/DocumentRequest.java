package org.maggus.myhealthnb.http;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import androidx.annotation.Nullable;

public class DocumentRequest extends StringRequest {

    public DocumentRequest(int method, String url, Response.Listener<Document> listener, @Nullable Response.ErrorListener errorListener) {
        super(method, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    Document doc = Jsoup.parse(response);
                    listener.onResponse(doc);
                } catch (Exception ex) {
                    if (errorListener != null) {
                        errorListener.onErrorResponse(new ParseError(ex));
                    } else {
                        Log.e("http", "Response processing error", ex);
                    }
                }
            }
        }, errorListener);
    }

    @Override
    public String getUrl() {
        String mUrl = super.getUrl();
        if (getMethod() == Method.GET) {
            try {
                Map<String, String> params = getParams();
                if (params != null && !params.isEmpty()) {
                    StringBuilder stringBuilder = new StringBuilder(mUrl);
                    int i = 1;
                    for (Map.Entry<String, String> entry : params.entrySet()) {
                        String key;
                        String value;
                        try {
                            key = URLEncoder.encode(entry.getKey(), "UTF-8");
                            value = URLEncoder.encode(entry.getValue(), "UTF-8");
                            if (i == 1) {
                                stringBuilder.append("?" + key + "=" + value);
                            } else {
                                stringBuilder.append("&" + key + "=" + value);
                            }
                        } catch (UnsupportedEncodingException e) {
                            throw new IllegalArgumentException(e);
                        }
                        i++;
                    }
                    String url = stringBuilder.toString();
                    return url;
                }
            } catch (AuthFailureError authFailureError) {
                throw new IllegalStateException(authFailureError);
            }
        }
        return mUrl;
    }
}
