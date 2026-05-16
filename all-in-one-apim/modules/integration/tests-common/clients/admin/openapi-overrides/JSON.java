/*
 * Applied after OpenAPI codegen (see pom.xml). Gson date adapters accept epoch millis (JSON number
 * or string) because some Admin API payloads serialize date-time fields that way; stock okhttp-gson
 * JSON.java only accepts ISO-8601 strings.
 */
package org.wso2.am.integration.clients.admin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.bind.util.ISO8601Utils;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.google.gson.JsonElement;
import io.gsonfire.GsonFireBuilder;
import io.gsonfire.TypeSelector;

import org.wso2.am.integration.clients.admin.api.dto.*;
import okio.ByteString;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JSON {

    private static final Map<String, Class<?>> CLASS_BY_DISCRIMINATOR_VALUE;

    static {
        Map<String, Class<?>> m = new HashMap<String, Class<?>>();
        m.put("AdvancedThrottlePolicyInfo", AdvancedThrottlePolicyInfoDTO.class);
        m.put("AdvancedThrottlePolicy", AdvancedThrottlePolicyDTO.class);
        m.put("ApplicationThrottlePolicy", ApplicationThrottlePolicyDTO.class);
        m.put("SubscriptionThrottlePolicy", SubscriptionThrottlePolicyDTO.class);
        m.put("CustomRule", CustomRuleDTO.class);
        m.put("ThrottlePolicy", ThrottlePolicyDTO.class);
        CLASS_BY_DISCRIMINATOR_VALUE = Collections.unmodifiableMap(m);
    }

    private Gson gson;
    private boolean isLenientOnJson = false;
    private DateTypeAdapter dateTypeAdapter = new DateTypeAdapter();
    private SqlDateTypeAdapter sqlDateTypeAdapter = new SqlDateTypeAdapter();
    private ByteArrayAdapter byteArrayAdapter = new ByteArrayAdapter();

    public static GsonBuilder createGson() {
        GsonFireBuilder fireBuilder = new GsonFireBuilder()
                .registerTypeSelector(ThrottlePolicyDTO.class, new TypeSelector() {
                    @Override
                    public Class getClassForElement(JsonElement readElement) {
                        return getClassByDiscriminator(CLASS_BY_DISCRIMINATOR_VALUE,
                                getDiscriminatorValue(readElement, "type"));
                    }
          })
        ;
        GsonBuilder builder = fireBuilder.createGsonBuilder();
        return builder;
    }

    private static String getDiscriminatorValue(JsonElement readElement, String discriminatorField) {
        JsonElement element = readElement.getAsJsonObject().get(discriminatorField);
        if (null == element) {
            throw new IllegalArgumentException("missing discriminator field: <" + discriminatorField + ">");
        }
        return element.getAsString();
    }

    /**
     * Returns the Java class that implements the OpenAPI schema for the specified discriminator value.
     *
     * @param classByDiscriminatorValue The map of discriminator values to Java classes.
     * @param discriminatorValue The value of the OpenAPI discriminator in the input data.
     * @return The Java class that implements the OpenAPI schema
     */
    private static Class getClassByDiscriminator(Map classByDiscriminatorValue, String discriminatorValue) {
        Class clazz = (Class) classByDiscriminatorValue.get(discriminatorValue);
        if (null == clazz) {
            throw new IllegalArgumentException("cannot determine model class of name: <" + discriminatorValue + ">");
        }
        return clazz;
    }

    public JSON() {
        gson = createGson()
            .registerTypeAdapter(Date.class, dateTypeAdapter)
            .registerTypeAdapter(java.sql.Date.class, sqlDateTypeAdapter)
            .registerTypeAdapter(byte[].class, byteArrayAdapter)
            .create();
    }

    /**
     * Get Gson.
     *
     * @return Gson
     */
    public Gson getGson() {
        return gson;
    }

    /**
     * Set Gson.
     *
     * @param gson Gson
     * @return JSON
     */
    public JSON setGson(Gson gson) {
        this.gson = gson;
        return this;
    }

    public JSON setLenientOnJson(boolean lenientOnJson) {
        isLenientOnJson = lenientOnJson;
        return this;
    }

    /**
     * Serialize the given Java object into JSON string.
     *
     * @param obj Object
     * @return String representation of the JSON
     */
    public String serialize(Object obj) {
        return gson.toJson(obj);
    }

    /**
     * Deserialize the given JSON string to Java object.
     *
     * @param <T>        Type
     * @param body       The JSON string
     * @param returnType The type to deserialize into
     * @return The deserialized Java object
     */
    @SuppressWarnings("unchecked")
    public <T> T deserialize(String body, Type returnType) {
        try {
            if (isLenientOnJson) {
                JsonReader jsonReader = new JsonReader(new StringReader(body));
                jsonReader.setLenient(true);
                return gson.fromJson(jsonReader, returnType);
            } else {
                return gson.fromJson(body, returnType);
            }
        } catch (JsonParseException e) {
            if (returnType.equals(String.class)) {
                return (T) body;
            } else {
                throw (e);
            }
        }
    }

    /**
     * Gson TypeAdapter for Byte Array type
     */
    public class ByteArrayAdapter extends TypeAdapter<byte[]> {

        @Override
        public void write(JsonWriter out, byte[] value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(ByteString.of(value).base64());
            }
        }

        @Override
        public byte[] read(JsonReader in) throws IOException {
            JsonToken token = in.peek();
            if (token == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            if (token != JsonToken.STRING) {
                throw new JsonParseException(
                        "Expected STRING or NULL for byte[] property, got " + token);
            }
            String bytesAsBase64 = in.nextString();
            ByteString byteString = ByteString.decodeBase64(bytesAsBase64);
            if (byteString == null) {
                throw new JsonParseException(
                        "Invalid base64 content for byte[] property: " + bytesAsBase64);
            }
            return byteString.toByteArray();
        }
    }

    /**
     * Gson TypeAdapter for java.sql.Date type
     * If the dateFormat is null, a simple "yyyy-MM-dd" format will be used
     * (more efficient than SimpleDateFormat).
     */
    public static class SqlDateTypeAdapter extends TypeAdapter<java.sql.Date> {

        private DateFormat dateFormat;

        public SqlDateTypeAdapter() {}

        public SqlDateTypeAdapter(DateFormat dateFormat) {
            this.dateFormat = dateFormat;
        }

        public void setFormat(DateFormat dateFormat) {
            this.dateFormat = dateFormat;
        }

        @Override
        public void write(JsonWriter out, java.sql.Date date) throws IOException {
            if (date == null) {
                out.nullValue();
            } else {
                String value;
                if (dateFormat != null) {
                    value = dateFormat.format(date);
                } else {
                    value = date.toString();
                }
                out.value(value);
            }
        }

        @Override
        public java.sql.Date read(JsonReader in) throws IOException {
            switch (in.peek()) {
                case NULL:
                    in.nextNull();
                    return null;
                case NUMBER:
                    return parseSqlDateFromJsonNumber(in);
                default:
                    String date = in.nextString();
                    try {
                        java.sql.Date fromEpoch = parseSqlDateFromEpochString(date);
                        if (fromEpoch != null) {
                            return fromEpoch;
                        }
                        if (dateFormat != null) {
                            return new java.sql.Date(dateFormat.parse(date).getTime());
                        }
                        return new java.sql.Date(ISO8601Utils.parse(date, new ParsePosition(0)).getTime());
                    } catch (ParseException e) {
                        throw new JsonParseException(e);
                    }
            }
        }

        private java.sql.Date parseSqlDateFromJsonNumber(JsonReader in) throws IOException {
            String epochRaw = in.nextString();
            try {
                return new java.sql.Date(Long.parseLong(epochRaw));
            } catch (NumberFormatException e) {
                try {
                    double d = Double.parseDouble(epochRaw);
                    return new java.sql.Date((long) d);
                } catch (NumberFormatException e2) {
                    throw new JsonParseException(
                            "Cannot parse sql.Date from JSON number: " + epochRaw, e2);
                }
            }
        }

        private java.sql.Date parseSqlDateFromEpochString(String raw) {
            if (raw == null) {
                return null;
            }
            String date = raw.trim();
            if (date.isEmpty()) {
                return null;
            }
            if (date.matches("^-?\\d+$")) {
                return new java.sql.Date(Long.parseLong(date));
            }
            try {
                double asDouble = Double.parseDouble(date);
                if (!Double.isNaN(asDouble) && !Double.isInfinite(asDouble)) {
                    return new java.sql.Date((long) asDouble);
                }
            } catch (NumberFormatException ignored) {
            }
            return null;
        }
    }

    /**
     * Gson TypeAdapter for java.util.Date type
     * If the dateFormat is null, ISO8601Utils will be used.
     */
    public static class DateTypeAdapter extends TypeAdapter<Date> {

        private DateFormat dateFormat;

        public DateTypeAdapter() {}

        public DateTypeAdapter(DateFormat dateFormat) {
            this.dateFormat = dateFormat;
        }

        public void setFormat(DateFormat dateFormat) {
            this.dateFormat = dateFormat;
        }

        @Override
        public void write(JsonWriter out, Date date) throws IOException {
            if (date == null) {
                out.nullValue();
            } else {
                String value;
                if (dateFormat != null) {
                    value = dateFormat.format(date);
                } else {
                    value = ISO8601Utils.format(date, true);
                }
                out.value(value);
            }
        }

        @Override
        public Date read(JsonReader in) throws IOException {
            try {
                switch (in.peek()) {
                    case NULL:
                        in.nextNull();
                        return null;
                    case NUMBER:
                        return parseUtilDateFromJsonNumber(in);
                    default:
                        String date = in.nextString();
                        try {
                            Date fromEpoch = parseUtilDateFromEpochString(date);
                            if (fromEpoch != null) {
                                return fromEpoch;
                            }
                            if (dateFormat != null) {
                                return dateFormat.parse(date);
                            }
                            return ISO8601Utils.parse(date, new ParsePosition(0));
                        } catch (ParseException e) {
                            throw new JsonParseException(e);
                        }
                }
            } catch (IllegalArgumentException e) {
                throw new JsonParseException(e);
            }
        }

        private Date parseUtilDateFromJsonNumber(JsonReader in) throws IOException {
            String epochRaw = in.nextString();
            try {
                return new Date(Long.parseLong(epochRaw));
            } catch (NumberFormatException e) {
                try {
                    double d = Double.parseDouble(epochRaw);
                    return new Date((long) d);
                } catch (NumberFormatException e2) {
                    throw new JsonParseException(
                            "Cannot parse java.util.Date from JSON number: " + epochRaw, e2);
                }
            }
        }

        private Date parseUtilDateFromEpochString(String raw) {
            if (raw == null) {
                return null;
            }
            String date = raw.trim();
            if (date.isEmpty()) {
                return null;
            }
            if (date.matches("^-?\\d+$")) {
                return new Date(Long.parseLong(date));
            }
            try {
                double asDouble = Double.parseDouble(date);
                if (!Double.isNaN(asDouble) && !Double.isInfinite(asDouble)) {
                    return new Date((long) asDouble);
                }
            } catch (NumberFormatException ignored) {
            }
            return null;
        }
    }

    public JSON setDateFormat(DateFormat dateFormat) {
        dateTypeAdapter.setFormat(dateFormat);
        return this;
    }

    public JSON setSqlDateFormat(DateFormat dateFormat) {
        sqlDateTypeAdapter.setFormat(dateFormat);
        return this;
    }

}
