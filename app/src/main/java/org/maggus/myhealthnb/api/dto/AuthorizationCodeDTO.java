package org.maggus.myhealthnb.api.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorizationCodeDTO {
    private String type;
    private CredentialsDTO credentials;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CredentialsDTO {
        private String authorizationCode;
        private String codeVerifier;
        private String nonce;
    }
}

