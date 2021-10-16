package org.maggus.myhealthnb.api.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 *         "userId": "qweqwe",
 *         "username": "email@mail.com",
 *         "email": "email@mail.com",
 *         "activeFlag": null,
 *         "userLocked": null,
 *         "authInfo": {
 *             "extraAuthorities": [],
 *             "roles": [
 *                 {
 *                     "id": "verosource_roles_results_gateway_user",
 *                     "authorities": [
 *                         {
 *                             "id": "verosource/vsf/results/gateway/results/read"
 *                         },
 *                         {
 *                             "id": "verosource/vsf/results/gateway/dependents/read"
 *                         },
 *                         {
 *                             "id": "verosource/vsf/results/gateway/dependents/create"
 *                         },
 *                         {
 *                             "id": "verosource/vsf/results/gateway/demographics/read"
 *                         },
 *                         {
 *                             "id": "verosource/vsf/results/gateway/immunization/read"
 *                         }
 *                     ]
 *                 }
 *             ],
 *             "groups": []
 *         },
 *         "identifiers": [
 *             {
 *                 "identifier": "123456",
 *                 "type": "snb_digital_id_oidc_authorization_registration_flow"
 *             }
 *         ],
 *         "createdTimestamp": null,
 *         "updatedTimestamp": null
 */

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserAuthorizationDTO {
    private String userId;
    private String username;
    private String email;
    private AuthInfo authInfo;
    private Identifier[] identifiers;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuthInfo {
        private Role[] roles;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Role {
            private String id;
            private Authority[] authorities;

            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Authority {
                private String id;
            }
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Identifier {
        private String identifier;
        private String type;
    }
}
