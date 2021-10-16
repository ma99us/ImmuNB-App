package org.maggus.myhealthnb.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 *         "patientImmunization": {
 *             "firstName": "Mike",
 *             "lastName": "Reddragon",
 *             "dateOfBirth": "1980-01-01",
 *             "hcn": "123 456 789",
 *             "hcnIssuer": "NB",
 *             "immunizations": [
 *                 {
 *                     "doseNumber": "1",
 *                     "vaccinationDate": "2021-04-01",
 *                     "tradeName": "COVID-19 Moderna mRNA-ARNm-1273 MT",
 *                     "agentName": "COVID-19 mRNA",
 *                     "serviceLocation": "Jean Coutu",
 *                     "serviceProvider": "Some Doctor"
 *                 },
 *                 {
 *                     "doseNumber": "2",
 *                     "vaccinationDate": "2021-06-01",
 *                     "tradeName": "COVID-19 Moderna mRNA-ARNm-1273 MT",
 *                     "agentName": "COVID-19 mRNA",
 *                     "serviceLocation": "Some Pharmacy",
 *                     "serviceProvider": "Cool Doctor"
 *                 }
 *             ]
 *         }
 */

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImmunizationsDTO {
    private PatientImmunizationDTO patientImmunization;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PatientImmunizationDTO {
        private String firstName;
        private String lastName;
        private String dateOfBirth;
        private String hcn;
        private String hcnIssuer;
        private ImmunizationDTO[] immunizations;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ImmunizationDTO {
            private String doseNumber;
            private String vaccinationDate;
            private String tradeName;
            private String agentName;
            private String serviceLocation;
            private String serviceProvider;
        }
    }

    public static  ImmunizationsDTO buildDummyDTO() {
        ImmunizationsDTO.PatientImmunizationDTO patientImmunizationDTO = new ImmunizationsDTO.PatientImmunizationDTO();
        patientImmunizationDTO.setFirstName("Mike");
        patientImmunizationDTO.setLastName("Reddragon");
        patientImmunizationDTO.setDateOfBirth("1980-01-01");
        patientImmunizationDTO.setHcn("123 456 789");
        patientImmunizationDTO.setHcnIssuer("NB");

        PatientImmunizationDTO.ImmunizationDTO[] immunizationDTOs = new PatientImmunizationDTO.ImmunizationDTO[2];
        immunizationDTOs[0] = new PatientImmunizationDTO.ImmunizationDTO();
        immunizationDTOs[0].setDoseNumber("1");
        immunizationDTOs[0].setVaccinationDate("2021-04-01");
        immunizationDTOs[0].setTradeName("COVID-19 Moderna mRNA-ARNm-1273 MT");
        immunizationDTOs[0].setAgentName("COVID-19 mRNA");
        immunizationDTOs[0].setServiceLocation("Jean Coutu");
        immunizationDTOs[0].setServiceProvider("Some Doctor");

        immunizationDTOs[1] = new PatientImmunizationDTO.ImmunizationDTO();
        immunizationDTOs[1].setDoseNumber("2");
        immunizationDTOs[1].setVaccinationDate("2021-06-01");
        immunizationDTOs[1].setTradeName("COVID-19 Moderna mRNA-ARNm-1273 MT");
        immunizationDTOs[1].setAgentName("COVID-19 mRNA");
        immunizationDTOs[1].setServiceLocation("Some Pharmacy");
        immunizationDTOs[1].setServiceProvider("Cool Doctor");

        patientImmunizationDTO.setImmunizations(immunizationDTOs);
        ImmunizationsDTO immunizationsDTO = new ImmunizationsDTO();
        immunizationsDTO.setPatientImmunization(patientImmunizationDTO);

        return immunizationsDTO;
    }
}
