package org.jembi.ciol.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationMessage(
        @JsonProperty("timeStamp") Long timeStamp,      // keep in for audit trails System.currentTimeMillis()
        @JsonProperty("source") String source,          // keep in for audit trails
        @JsonProperty("messageList") List <Message> messageList
    ) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Message(
            @JsonProperty("recipientTag") String recipientTag,    // tag for who should be receiving email
            @JsonProperty("typeTag") String typeTag,         // reminder, info/good news, error
            @JsonProperty("timeStamp") Long timeStamp,
            @JsonProperty("statusCode") Integer statusCode,        // 1xx: Informational (reminder), 2XX: Success (Good News), 3/4/5XX: (ErrorMessage)
            @JsonProperty("messageType") String messageType,       // "validation_failed|mapping_failed|dhis_submission_failed|get_config_fails"
            @JsonProperty("messageBody") String messageBody,              // content
            @JsonProperty("messagePath") String messagePath               // path to url | path to mapper service | TODO figure out the full location with Tresor
    ){}

}


