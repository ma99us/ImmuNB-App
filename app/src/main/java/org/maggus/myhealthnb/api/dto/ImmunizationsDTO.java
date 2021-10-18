package org.maggus.myhealthnb.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Arrays;

import androidx.annotation.NonNull;
import lombok.Data;

/**
 * "patientImmunization": {
 * "firstName": "Mike",
 * "lastName": "Reddragon",
 * "dateOfBirth": "1980-01-01",
 * "hcn": "123 456 789",
 * "hcnIssuer": "NB",
 * "immunizations": [
 * {
 * "doseNumber": "1",
 * "vaccinationDate": "2021-04-01",
 * "tradeName": "COVID-19 Moderna mRNA-ARNm-1273 MT",
 * "agentName": "COVID-19 mRNA",
 * "serviceLocation": "Jean Coutu",
 * "serviceProvider": "Some Doctor"
 * },
 * {
 * "doseNumber": "2",
 * "vaccinationDate": "2021-06-01",
 * "tradeName": "COVID-19 Moderna mRNA-ARNm-1273 MT",
 * "agentName": "COVID-19 mRNA",
 * "serviceLocation": "Some Pharmacy",
 * "serviceProvider": "Cool Doctor"
 * }
 * ]
 * }
 */

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImmunizationsDTO {
    private PatientImmunizationDTO patientImmunization;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PatientImmunizationDTO implements Filterable<PatientImmunizationDTO> {
        private String firstName;
        private String lastName;
        private String dateOfBirth;
        private String hcn;
        private String hcnIssuer;
        private ImmunizationDTO[] immunizations;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ImmunizationDTO implements Filterable<ImmunizationDTO> {
            private String doseNumber;
            private String vaccinationDate;
            private String tradeName;
            private String agentName;
            private String serviceLocation;
            private String serviceProvider;

            @NonNull
            @Override
            public ImmunizationDTO filter() {
                ImmunizationDTO filtered = new ImmunizationDTO();
                filtered.doseNumber = this.doseNumber;
                filtered.vaccinationDate = this.vaccinationDate;
                filtered.tradeName = this.tradeName;
                return filtered;
            }
        }

        @NonNull
        @Override
        public PatientImmunizationDTO filter() {
            PatientImmunizationDTO filtered = new PatientImmunizationDTO();
            filtered.firstName = this.firstName;
            filtered.lastName = this.lastName;
            filtered.dateOfBirth = this.dateOfBirth;
            if (this.immunizations != null) {
                filtered.immunizations = new ImmunizationDTO[this.immunizations.length];
                for (int i = 0; i < filtered.immunizations.length; i++) {
                    filtered.immunizations[i] = this.immunizations[i].filter();
                }
            }
            return filtered;
        }
    }

    public static ImmunizationsDTO buildDummyDTO(boolean unvaccinated, boolean partial, boolean recent) {
        ImmunizationsDTO.PatientImmunizationDTO patientImmunizationDTO = new ImmunizationsDTO.PatientImmunizationDTO();
        patientImmunizationDTO.setFirstName("Mike");
        patientImmunizationDTO.setLastName("Reddragon");
        patientImmunizationDTO.setDateOfBirth("1980-01-01");
        patientImmunizationDTO.setHcn("123 456 789");
        patientImmunizationDTO.setHcnIssuer("NB");

        PatientImmunizationDTO.ImmunizationDTO[] immunizationDTOs = null;
        if (!unvaccinated) {
            int recSize = unvaccinated ? 0 : (partial ? 1 : 2);
            immunizationDTOs = new PatientImmunizationDTO.ImmunizationDTO[recSize];
            immunizationDTOs[0] = new PatientImmunizationDTO.ImmunizationDTO();
            immunizationDTOs[0].setDoseNumber("1");
            immunizationDTOs[0].setVaccinationDate("2021-04-01");
            immunizationDTOs[0].setTradeName("COVID-19 Moderna mRNA-ARNm-1273 MT");
            immunizationDTOs[0].setAgentName("COVID-19 mRNA");
            immunizationDTOs[0].setServiceLocation("Jean Coutu");
            immunizationDTOs[0].setServiceProvider("Some Doctor");

            if (!partial) {
                immunizationDTOs[1] = new PatientImmunizationDTO.ImmunizationDTO();
                immunizationDTOs[1].setDoseNumber("2");
                immunizationDTOs[1].setVaccinationDate("2021-06-01");   //TODO: use recent date if 'recent' set
                immunizationDTOs[1].setTradeName("COVID-19 Moderna mRNA-ARNm-1273 MT");
                immunizationDTOs[1].setAgentName("COVID-19 mRNA");
                immunizationDTOs[1].setServiceLocation("Some Pharmacy");
                immunizationDTOs[1].setServiceProvider("Cool Doctor");
            }
        }

        patientImmunizationDTO.setImmunizations(immunizationDTOs);
        ImmunizationsDTO immunizationsDTO = new ImmunizationsDTO();
        immunizationsDTO.setPatientImmunization(patientImmunizationDTO);

        return immunizationsDTO;
    }
}
