package org.maggus.myhealthnb.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 *         "demographic": {
 *             "firstName": "Mike",
 *             "lastName": "Reddragon",
 *             "hcn": "123 456 789",
 *             "hcnIssuer": "NB",
 *             "dateOfBirth": "1980-01-01",
 *             "userInfoId": "d1dsd12dd-dwsqd1-d1212d-1d12d",
 *             "vsfUserId": "d1d24d-1ddd21d-34d12d34d-1d1dd"
 *         }
 */

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DemographicsDTO {
    private DemographicDTO demographic;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DemographicDTO {
        private String firstName;
        private String lastName;
        private String hcn;
        private String hcnIssuer;
        private String dateOfBirth;
        private String userInfoId;
        private String vsfUserId;
    }
}
