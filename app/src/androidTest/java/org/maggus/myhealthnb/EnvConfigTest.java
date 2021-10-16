package org.maggus.myhealthnb;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.maggus.myhealthnb.api.EnvConfig;

import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
public class EnvConfigTest {

    @Test
    public void loadFromJsObjectTest() {
        String js = "window._env_ = {\n" +
                "  API_URL: \"https://myhealthnb.verosource.com/results-gateway/\",\n" +
                "  DIGITAL_ID_API_URL: \"https://access.id.gnb.ca/as/\",\n" +
                "  DIGITAL_ID_CLIENT_ID: \"covid\",\n" +
                "}";
        EnvConfig envConfig = new EnvConfig();
        envConfig.load(js);

        Assert.assertEquals(3, envConfig.size());
        Assert.assertTrue(envConfig.containsKey("DIGITAL_ID_API_URL"));
    }

    @Test
    public void loadFromJsConstantsTest() {
        String js = "\n" +
                " export const API_URL = \"https://myhealthnb.verosource.com/results-gateway/\";\n" +
                " const DIGITAL_ID_API_URL =\"https://access.id.gnb.ca/as/\";\n" +
                " let   DIGITAL_ID_CLIENT_ID= \"covid\";\n" +
                "";
        EnvConfig envConfig = new EnvConfig();
        envConfig.load(js);

        Assert.assertEquals(3, envConfig.size());
        Assert.assertTrue(envConfig.containsKey("DIGITAL_ID_CLIENT_ID"));
    }
}
