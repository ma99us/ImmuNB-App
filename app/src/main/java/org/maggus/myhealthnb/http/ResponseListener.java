package org.maggus.myhealthnb.http;

import java.util.Map;

public interface ResponseListener<T> {
    /** Called when a response is received. */
    void onResponse(T response, Map<String, String> headers) throws Exception;
}
