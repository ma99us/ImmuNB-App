package org.maggus.myhealthnb.api;

import android.util.Log;

import java.util.Properties;

public class EnvConfig extends Properties {

    public synchronized void load(String javaScript) {
        int p0 = javaScript.indexOf('{');
        int p1 = javaScript.indexOf('}');
        if (p0 > 0 && p1 > p0) {
            loadFromJsObject(javaScript.substring(p0 + 1, p1));
        } else {
            loadFromJsConstants(javaScript);
        }
        Log.d("EnvConfig", "Loaded " + size() + " records");
    }

    private void loadFromJsObject(String javaScript) {
        String[] rows = javaScript.split(",");
        for (String row : rows) {
            int p3 = row.indexOf(":");   //split(":");
            if (p3 > 0) {
                String key = row.substring(0, p3).trim();
                String value = row.substring(p3 + 1).trim().replaceAll("^\"|\"$", "");
                put(key, value);
            }
        }
    }

    private void loadFromJsConstants(String javaScript) {
        String[] rows = javaScript.split(";");
        for (String row : rows) {
            int p3 = row.indexOf("=");   //split(":");
            if (p3 > 0) {
                String key = row.substring(0, p3).trim();
                int p4 = key.lastIndexOf(" ");
                if (p4 > 0) {
                    key = key.substring(p4).trim();
                }
                String value = row.substring(p3 + 1).trim().replaceAll("^\"|\"$", "");
                put(key, value);
            }
        }
    }
}
