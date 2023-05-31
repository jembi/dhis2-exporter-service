package org.jembi.ciol.serdes;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

import static org.jembi.ciol.utils.AppUtils.OBJECT_MAPPER;

public class JsonPojoDeserializer<T> implements Deserializer<T> {
    public static final String CLASS_TAG = "JsonPOJOClass";

    private Class<T> tClass;

    /**
     * Default constructor needed by Kafka
     */
    public JsonPojoDeserializer() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public void configure(Map<String, ?> props, boolean isKey) {
        tClass = (Class<T>) props.get(CLASS_TAG);
    }

    @Override
    public T deserialize(String topic, byte[] bytes) {
        if (bytes == null)
            return null;

        T data;
        try {
            data = OBJECT_MAPPER.readValue(bytes, tClass);
        } catch (Exception e) {
            throw new SerializationException(e);
        }

        return data;
    }

    @Override
    public void close() {

    }
}
