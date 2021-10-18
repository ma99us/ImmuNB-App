package org.maggus.myhealthnb;

import org.junit.Assert;
import org.junit.Test;
import org.maggus.myhealthnb.api.dto.ImmunizationsDTO;

public class FilterableTest {

    @Test
    public void filteredImmunizationsTest() {
        ImmunizationsDTO immunizationsDTO = ImmunizationsDTO.buildDummyDTO(false, false, false);
        ImmunizationsDTO.PatientImmunizationDTO dto = immunizationsDTO.getPatientImmunization();
        Assert.assertNotNull(dto);
        Assert.assertNotNull(dto.getHcn());
        Assert.assertNotNull(dto.getImmunizations()[0].getAgentName());

        ImmunizationsDTO.PatientImmunizationDTO filtered = dto.filter();

        System.out.println(filtered);
        Assert.assertNotNull(filtered);
        Assert.assertNull(filtered.getHcn());
        Assert.assertNull(filtered.getImmunizations()[0].getAgentName());
    }
}
