package org.maggus.myhealthnb.barcode;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
//            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            ;

    public <H extends JabBarcodeHeader, P> String objectToBarcode(H header, P payload) throws IOException {
        StringBuilder sb = new StringBuilder();

        sb.append(PREFIX);
        sb.append(DELIMITER);
        sb.append(getBarcodeFormatId(header != null ? header.getClass() : null, payload.getClass()));
        sb.append(DELIMITER);
        String payloadStr = objectValuesToJsonArrayString(payload);
        if (header != null) {
            header.populate(payload, payloadStr);   // populate the header
            sb.append(objectValuesToJsonArrayString(header));
        }
        sb.append(payloadStr);

        return sb.toString();
    }

    public <H extends JabBarcodeHeader, P> P barcodeToObject(String barcode, Class<H> headerClass, Class<P> payloadClass) throws IOException {
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
        }
        P payload = jsonArrayStringToObject(barcode, payloadClass);
        // validate the header
        if (header != null) {
            header.validate(payload, barcode);
        }
        return payload;
    }

    public String objectValuesToJsonArrayString(Object obj) throws IOException {
        List<Object> beanDataValues = getObjectValues(obj);
        return mapper.writeValueAsString(beanDataValues);
    }

    public <T> T jsonArrayStringToObject(String json, Class<T> clazz) throws IOException {
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
                    for (int i = 0; i < length; i ++) {
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
            declaredFields.addAll(0, Arrays.asList(c.getDeclaredFields()));
        }
        return declaredFields;
    }

    private boolean isJavaLangClass(Class clazz) {
        return clazz != null && (clazz.isPrimitive() || clazz.getName().startsWith("java.")); // TODO: maybe java.lang ?
    }

    private Object getObjectFieldValueByName(Object obj, String fieldName) throws IOException {
        try {
//            return PropertyUtils.getProperty(obj, fieldName); // @#$%^&!, commons-beanutils do not work on Android!
            // FIXME: is that a dirty way of getting a value?
            Method method = obj.getClass().getMethod("get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1));
            return method.invoke(obj);
        } catch (Exception e) {
            throw new IOException("Can't 'get' object \"" + obj.getClass().getSimpleName() + "\" field \"" + fieldName + "\" value", e);
        }
    }

    public static Class<?> getFieldType(Field field){
        Class<?> type = field.getType();
        if(type.isArray()){
            return type.getComponentType();
        }
        else if (List.class.isAssignableFrom(field.getType())) {
            ParameterizedType pt = (ParameterizedType) field.getGenericType();
            return (Class) pt.getActualTypeArguments()[0];
        }
        return type;
    }

    public static long hashString(String data) {
        final long MAX_SAFE_INTEGER = 9007199254740991L;      // 2^53 - 1
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
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
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Exception in hash()", e);
        }
    }

    public static <H extends JabBarcodeHeader, P> long getBarcodeFormatId(Class<H> headerClass, Class<P> payloadClass) {
        StringBuilder sb = new StringBuilder();
        if (headerClass != null) {
            sb.append(headerClass.getSimpleName());
        }
        sb.append(payloadClass.getSimpleName());
        return hashString(sb.toString());
    }
}
