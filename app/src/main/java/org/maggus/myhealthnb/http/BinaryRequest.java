package org.maggus.myhealthnb.http;

import android.util.Log;

import com.android.volley.ClientError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.util.Map;

public class BinaryRequest extends Request<byte[]> {
    private final Response.Listener<byte[]> mListener;
    private Map<String, String> mParams;

    public BinaryRequest(int method, String mUrl, Response.Listener<byte[]> listener,
                         Response.ErrorListener errorListener) {
        super(method, mUrl, errorListener);
        // this request would never use cache.
        setShouldCache(false);
        mListener = listener;
    }

    @Override
    protected void deliverResponse(byte[] response) {
        try {
            mListener.onResponse(response);
        } catch (Exception ex) {
            Response.ErrorListener errorListener = getErrorListener();
            if (errorListener != null) {
                errorListener.onErrorResponse(new ParseError(ex));
            } else {
                Log.e("http", "Response processing error", ex);
            }
        }
    }

    @Override
    protected Response<byte[]> parseNetworkResponse(NetworkResponse response) {
        //Pass the response data here
        return Response.success(response.data, HttpHeaderParser.parseCacheHeaders(response));
    }
}