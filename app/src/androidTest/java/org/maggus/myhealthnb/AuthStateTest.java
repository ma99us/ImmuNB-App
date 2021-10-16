package org.maggus.myhealthnb;

import android.content.Context;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.maggus.myhealthnb.api.AuthState;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

@RunWith(AndroidJUnit4.class)
public class AuthStateTest {

    @Test
    public void makeDeviceIdTest() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String str = AuthState.generateDeviceId(appContext);
        System.out.println(str);
        Assert.assertNotNull(str);
    }

    @Test
    public void generateRandomStringTest() {
        String str = AuthState.generateRandomString(8);

        System.out.println(str);
        Assert.assertNotNull(str);
        Assert.assertEquals(8, str.length());
        Assert.assertFalse(str.contains("-"));
    }

    @Test
    public void base64UrlEncodeStringTest() {
        String str = AuthState.generateRandomString(16);
        str = AuthState.base64UrlEncodeString(str);

        System.out.println(str);
        Assert.assertNotNull(str);
        Assert.assertFalse(str.contains("+"));
        Assert.assertFalse(str.contains("/"));
        Assert.assertFalse(str.contains("="));
    }
}
