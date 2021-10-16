package org.maggus.myhealthnb.http;

import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

public class JsonRequest<R, T> extends JsonObjectRequest {
    private static final ObjectMapper mapper = new ObjectMapper();
    @Getter
    private Map<String, String> responseHeaders;

    /**
     * Make a GET request and return a parsed object from JSON.
     *
     * @param url              URL of the request to make
     * @param responseDtoClass Relevant class object, for json response parsing
     */
    public JsonRequest(int method, String url, R requestDto, Class<T> responseDtoClass,
                       ResponseListener<T> responseListener, Response.ErrorListener errorListener) {
        super(method, url, beanToJsonObject(requestDto, errorListener), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    T data = jsonObjectToBean(response.optJSONObject("data").optJSONObject("data"), responseDtoClass, errorListener);
                    Map<String, String> headers = jsonObjectToBean(response.optJSONObject("headers"), Map.class, errorListener);
                    responseListener.onResponse(data, headers);
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
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        responseHeaders = response.headers != null ? new HashMap<>(response.headers) : null;
        try {
            String jsonString = new String(response.data,
                            HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));

            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("data", new JSONObject(jsonString));
            jsonResponse.put("headers", new JSONObject(response.headers));

            return Response.success(jsonResponse,
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException je) {
            return Response.error(new ParseError(je));
        }
    }

    public static JSONObject beanToJsonObject(Object data, Response.ErrorListener errorListener) {
        if (data == null) {
            return null;
        }
        try {
            return new JSONObject(mapper.writeValueAsString(data));
        } catch (Exception e) {
            if (errorListener != null) {
                errorListener.onErrorResponse(new ParseError(e));
                return null;
            }
            throw new IllegalArgumentException(e);
        }
    }

    public static <T> T jsonObjectToBean(JSONObject json, Class<T> clazz, Response.ErrorListener errorListener) {
        if (json == null) {
            return null;
        }
        try {
            return mapper.readValue(json.toString(), clazz);
        } catch (JsonProcessingException e) {
            if (errorListener != null) {
                errorListener.onErrorResponse(new ParseError(e));
                return null;
            }
            throw new IllegalArgumentException(e);
        }
    }
}
