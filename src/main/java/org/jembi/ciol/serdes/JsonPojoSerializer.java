package org.jembi.ciol.serdes;


import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

import static org.jembi.ciol.utils.AppUtils.OBJECT_MAPPER;

public class JsonPojoSerializer<T> implements Serializer<T> {

    /**
     * Default constructor needed by Kafka
     */
    public JsonPojoSerializer() {
    }

    @Override
    public void configure(Map<String, ?> props, boolean isKey) {
    }

    @Override
    public byte[] serialize(String topic, T data) {
        if (data == null)
            return null;

        try {
            return OBJECT_MAPPER.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new SerializationException("Error serializing JSON message", e);
        }
    }

    @Override
    public void close() {
    }

}
