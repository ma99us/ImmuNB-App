package org.maggus.myhealthnb.barcode;

import android.util.Base64;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.maggus.myhealthnb.barcode.headers.BarcodeHeader;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

/**
 * My custom barcode format called JAB (Json Array Barcode) looks like this:
 * JAB|12345678|["abc", 123, ...]["string field", 567,]
 * which are:
 * JAB|barcode id|[coma-separated header fields values][coma-separated payload fields values].
 * The data in square brackets is a json array of the bean fields values (no fields names).
 * The barcode id is a combined checksum of Header and Payload classes names.
 */
public class JabBarcode {
    public static final String PREFIX = "JAB";
    public static final String DELIMITER = "|";
    private static final Hasher hasher = new Hasher();
    @Getter
    private final Registry registry = new Registry();

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
//            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            ;

    /**
     * Quick check that barcode looks like a correct format
     *
     * @param barcode
     * @return possible JabBarcode, and parsing could be attempted
     */
    public boolean isPossibleJabBarcode(String barcode) {
        if (barcode != null
                && barcode.startsWith(PREFIX + DELIMITER)
                && barcode.indexOf("|", (PREFIX + DELIMITER).length() + 1) > 0
                && barcode.indexOf("[") > 0
                && barcode.endsWith("]")) {
            return true;
        }
        return false;
    }

    public long findJabBarcodeFormatId(String barcode) {
        if (!isPossibleJabBarcode(barcode)) {
            return -1L; // bad format
        }
        try {
            barcode = barcode.substring((PREFIX + DELIMITER).length());
            int p0 = barcode.indexOf('|');
            if (p0 <= 0) {
                return -1L;  // no format id
            }
            return Long.parseLong(barcode.substring(0, p0));
        } catch (Exception ex) {
            // bad format id value
            return -1L;
        }
    }

    public <H extends BarcodeHeader, P> String objectToBarcode(H header, P payload) throws IOException {
        StringBuilder sb = new StringBuilder();

        sb.append(PREFIX);
        sb.append(DELIMITER);
        sb.append(getBarcodeFormatId(header != null ? header.getClass() : null, payload.getClass()));
        sb.append(DELIMITER);
        String payloadStr = objectValuesToJsonArrayString(payload);
        if (header != null) {
            // populate the header
            header.populate(payload, payloadStr);
            // encrypt the payload
            payloadStr = header.obfuscate(payload, payloadStr);
            sb.append(objectValuesToJsonArrayString(header));
        }
        sb.append(payloadStr);

        return sb.toString();
    }

    public Object barcodeToObject(String barcode) throws IOException {
        long formatId = findJabBarcodeFormatId(barcode);
        if (formatId == -1) {
            throw new IOException("Unrecognized barcode format");
        }
        Registry.JabBarcodeFormat<?, ?> format = registry.findFormat(formatId);
        if (format == null) {
            throw new IOException("Unregistered barcode format id: " + formatId);
        }
        return barcodeToObject(barcode, format.getHeaderClass(), format.getPayloadClass());
    }

    public <H extends BarcodeHeader, P> P barcodeToObject(String barcode, Class<H> headerClass, Class<P> payloadClass) throws IOException {
        if (barcode == null) {
            return null;
        }
        if (!barcode.startsWith(PREFIX + DELIMITER)) {
            throw new IOException("Unrecognized barcode prefix");
        }
        barcode = barcode.substring((PREFIX + DELIMITER).length());
        int p0 = barcode.indexOf('|');
        if (p0 <= 0) {
            throw new IOException("Unrecognized barcode format; no checksum");
        }
        long barcodeFormatId = Long.parseLong(barcode.substring(0, p0));
        long formatId = getBarcodeFormatId(headerClass, payloadClass);
        if (barcodeFormatId != formatId) {
            throw new IOException("Barcode format ID mismatch; expected " + formatId + ", but got " + barcodeFormatId);
        }
        barcode = barcode.substring(p0 + 1);
        H header = null;
        if (headerClass != null) {
            int p1 = barcode.indexOf("][");// FIXME: not very reliable
            if (p1 <= 0) {
                throw new IOException("Bad barcode format; can not parse header");
            }
            header = jsonArrayStringToObject(barcode.substring(0, p1 + 1), headerClass);
            barcode = barcode.substring(p1 + 1);
            // decrypt barcode payload
            barcode = header.deobfuscate(null, barcode);
        }
        P payload = jsonArrayStringToObject(barcode, payloadClass);
        if (header != null) {
            // validate checksum
            header.validate(payload, barcode);
        }
        return payload;
    }

    private String objectValuesToJsonArrayString(Object obj) throws IOException {
        List<Object> beanDataValues = getObjectValues(obj);
        return mapper.writeValueAsString(beanDataValues);
    }

    private <T> T jsonArrayStringToObject(String json, Class<T> clazz) throws IOException {
        List<Object> beanDataValues = mapper.readValue(json, ArrayList.class);
        return parseObjectValues(beanDataValues, clazz);
    }

    private <T> T parseObjectValues(List<Object> beanDataValues, Class<T> clazz) throws IOException {
        Map<String, Object> beanData = parseMapValues(beanDataValues, clazz);
        return mapper.convertValue(beanData, clazz);
    }

    private <T> Map<String, Object> parseMapValues(List<Object> beanDataValues, Class<T> clazz) throws IOException {
        int fieldIdx = 0;
        LinkedHashMap<String, Object> objectData = new LinkedHashMap<String, Object>();
        List<Field> fieldNames = getObjectFieldNames(clazz); // actual bean fields in proper order
        for (Object value : beanDataValues) {
            if (fieldIdx < fieldNames.size()) {
                Field field = fieldNames.get(fieldIdx);
                Class<?> fieldType = getFieldType(field);
                if (value instanceof List && !isJavaLangClass(fieldType)) {
                    // not a java.lang field, probably nested java bean, parse with Value Mapper recursively
                    if (field.getType().isArray() || List.class.isAssignableFrom(field.getType())) {
                        // list or array of non-java.lang types
                        List<Object> arrValue = new ArrayList<Object>();
                        for (Object elem : (List) value) {
                            arrValue.add(parseObjectValues((List) elem, fieldType));
                        }
                        value = arrValue;
                    } else {
                        // single composition object
                        value = parseObjectValues((List) value, fieldType);
                    }
                }

                objectData.put(field.getName(), value);
                fieldIdx++;
            }
        }
        return objectData;
    }

    private List<Object> getObjectValues(Object obj) throws IOException {
        Map<String, Object> objData = mapper.convertValue(obj, LinkedHashMap.class);
        return getMapValues(objData, obj);
    }

    private List<Object> getMapValues(Map<String, Object> objData, Object obj) throws IOException {
        List<Object> dataValues = new ArrayList<>();
        List<Field> fieldNames = getObjectFieldNames(obj.getClass());
        for (Field field : fieldNames) {
            Object value = objData.get(field.getName());
            Class<?> fieldType = getFieldType(field);
            if (value instanceof Map && !isJavaLangClass(fieldType)) {
                // not a java.lang property, probably nested java bean, serialize with Value Mapper recursively
                Object objectValue = getObjectFieldValueByName(obj, field.getName());
                value = getObjectValues(objectValue);
            } else if (value instanceof List && !isJavaLangClass(fieldType)) {
                // array of not a java.lang objects, probably array of java beans, serialize each element with Value Mapper recursively
                Object objectValue = getObjectFieldValueByName(obj, field.getName());
                List<Object> arrValue = new ArrayList<Object>();
                if (objectValue.getClass().isArray()) {
                    int length = Array.getLength(objectValue);
                    for (int i = 0; i < length; i++) {
                        Object elem = Array.get(objectValue, i);
                        arrValue.add(getObjectValues(elem));
                    }
                } else if (objectValue instanceof List) {
                    for (Object elem : (List) objectValue) {
                        arrValue.add(getObjectValues(elem));
                    }
                }
                value = arrValue;
            }
            dataValues.add(value);
        }
        return dataValues;
    }

    private List<Field> getObjectFieldNames(Class objClass) {
        List<Field> declaredFields = new ArrayList<>();
        for (Class<?> c = objClass; c != null; c = c.getSuperclass()) {
            if (c.equals(Object.class)) {
                continue;
            }
            List<Field> fields = new ArrayList<>(Arrays.asList(c.getDeclaredFields()));
            // Unfortunately, some JVMs do not guarantee DeclaredFields order, so we have to sort fields ourselfs
            Collections.sort(fields, new Comparator<Field>() {
                public int compare(Field f1, Field f2) {
                    // sort each class fields names alphabetically
                    return f1.getName().compareTo(f2.getName());
                }
            });
            // parent class fields always go before child's fields
            declaredFields.addAll(0, fields);
        }
        return declaredFields;
    }

    private boolean isJavaLangClass(Class clazz) {
        return clazz != null && (clazz.isPrimitive() || clazz.getName().startsWith("java.")); // TODO: maybe java.lang ?
    }

    private Object getObjectFieldValueByName(Object obj, String fieldName) throws IOException {
        try {
//            return PropertyUtils.getProperty(obj, fieldName); // Unfortunately, commons-beanutils do not work on Android!

            // TODO: is there a better cross-platform way to getting a field value from object?
            try {
                // try to find a 'getter' for this field first
                Method method = obj.getClass().getMethod("get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1));
                return method.invoke(obj);
            } catch (NoSuchMethodException ex) {
                // if no 'getter', then try to access field directly. This will fail if it is not public field.
                Class<?> clazz = obj.getClass();
                Field field = clazz.getField(fieldName);
                return field.get(obj);
            }
        } catch (Exception e) {
            throw new IOException("Can't access object's \"" + obj.getClass().getSimpleName() + "\" field \"" + fieldName + "\" value", e);
        }
    }

    private Class<?> getFieldType(Field field) {
        Class<?> type = field.getType();
        if (type.isArray()) {
            return type.getComponentType();
        } else if (List.class.isAssignableFrom(field.getType())) {
            ParameterizedType pt = (ParameterizedType) field.getGenericType();
            return (Class) pt.getActualTypeArguments()[0];
        }
        return type;
    }

    public static <H extends BarcodeHeader, P> long getBarcodeFormatId(Class<H> headerClass, Class<P> payloadClass) {
        StringBuilder sb = new StringBuilder();
        if (headerClass != null) {
            sb.append(headerClass.getSimpleName());
        }
        sb.append(payloadClass.getSimpleName());
        return hasher.hashString(sb.toString());
    }

    /**
     * Simple platform-independent way to create hashes from strings.
     */
    public static class Hasher {
        private final String ALGORITHM = "SHA-256"; // default
        private final long MAX_SAFE_INTEGER = 9007199254740991L;      // 2^53 - 1 is the maximum "safe" integer for json/javascript

        public long hashString(String data) {
            try {
                MessageDigest md = MessageDigest.getInstance(ALGORITHM);
                byte[] digest = md.digest(data.getBytes(StandardCharsets.UTF_8));
                if (digest.length < 8) {      // resulting digest should be at least 8 bytes in length
                    throw new IllegalStateException("Digest is too short");
                }
                // only use 8 most significant bytes
                long msb = 0;
                for (int i = 0; i < 8; i++) {
                    msb = (msb << 8) | (digest[i] & 0xff);
                }
                return Math.abs(msb) % MAX_SAFE_INTEGER;  // unsigned and not larger then MAX_SAFE_INTEGER
            } catch (Exception e) {
                throw new IllegalArgumentException("Hash Error", e);
            }
        }
    }

    /**
     * Simple platform-independent way to encrypt/decrypt strings.
     */
    public static class Crypto {
        private final String ALGORITHM = "Blowfish"; // default
        private final String MODE = "Blowfish/CBC/PKCS5Padding"; // default
        private final int IV_LEN = 8;   //TODO: IV bytes are generated from the Key, is that good enough?

        public byte[] wrapBytes(byte[] sBytes, int rLen) {
            int sLen = sBytes.length;
            byte[] rBytes = new byte[rLen];
            for (int step = 0, s = 0, r = 0; step < Math.max(rLen, sLen); step++, s++, r++) {
                s %= sLen;
                r %= rLen;
                rBytes[r] ^= sBytes[s];
            }
            return rBytes;
        }

        public String encryptString(String value, String key) {
            try {
                SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), ALGORITHM);
                Cipher cipher = Cipher.getInstance(MODE);
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(wrapBytes(key.getBytes(), IV_LEN)));
                byte[] encrypted = cipher.doFinal(value.getBytes());
                return bytesToString(encrypted);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Encryption Error", ex);
            }
        }

        public String decryptString(String value, String key) {
            try {
                byte[] encrypted = stringToBytes(value);
                SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), ALGORITHM);
                Cipher cipher = Cipher.getInstance(MODE);
                cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(wrapBytes(key.getBytes(), IV_LEN)));
                return new String(cipher.doFinal(encrypted));
            } catch (Exception ex) {
                throw new IllegalArgumentException("Encryption Error", ex);
            }
        }

        public String bytesToString(byte[] bytes){
            return Base64.encodeToString(bytes, Base64.NO_WRAP);        //TODO: URL_SAFE ?
        }

        public byte[] stringToBytes(String string) {
            return Base64.decode(string, Base64.NO_WRAP);       //TODO: URL_SAFE ?
        }

    }

    /**
     * Simple collection of registered barcode formats.
     * Finds java bean classes from the barcode format id.
     */
    public static class Registry {
        private final Map<Long, JabBarcodeFormat<?, ?>> formats = new HashMap<>();

        public synchronized <H extends BarcodeHeader, P> Registry registerFormat(Class<H> headerClass, Class<P> payloadClass) {
            long barcodeFormatId = getBarcodeFormatId(headerClass, payloadClass);
            formats.put(barcodeFormatId, new JabBarcodeFormat(headerClass, payloadClass));
            return this;
        }

        public synchronized JabBarcodeFormat<?, ?> findFormat(long id) {
            return formats.get(id);
        }

        @Data
        @AllArgsConstructor
        public static class JabBarcodeFormat<H extends BarcodeHeader, P> {
            private final Class<H> headerClass;
            private final Class<P> payloadClass;
        }
    }
}
