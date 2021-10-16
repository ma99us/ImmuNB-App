package org.maggus.myhealthnb;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Assert;
import org.junit.Test;
import org.maggus.myhealthnb.barcode.ChecksumHeader;
import org.maggus.myhealthnb.barcode.JabBarcode;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class JabBarcodeTest {

    @Test
    public void basicBarcodeTest(){
        DummyDTO dto = DummyDTO.makeDummyDTO(false, false);

//        ObjectMapper objectMapper = new ObjectMapper();
//        Map<String, Object> objData = objectMapper.convertValue(dto, LinkedHashMap.class);

        JabBarcode jabBarcode = new JabBarcode();

        // generate barcode
        String barcode = null;
        try {
            barcode = jabBarcode.objectToBarcode(null, dto);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail();
        }

        System.out.println(barcode);
        Assert.assertNotNull(barcode);
        Assert.assertTrue(barcode.startsWith(JabBarcode.PREFIX + JabBarcode.DELIMITER));

        // parse it back
        DummyDTO res = null;
        try {
            res = jabBarcode.barcodeToObject(barcode, null, DummyDTO.class);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail();
        }

        System.out.println(res);
        Assert.assertNotNull(res);
        Assert.assertEquals(dto, res);
    }

    @Test
    public void checksumHeaderBarcodeTest(){
        DummyDTO dto = DummyDTO.makeDummyDTO(false, false);

        checksumHeaderBarcodeTest(dto, DummyDTO.class);
    }

    @Test
    public void checksumHeaderCompositionDtoBarcodeTest(){
        DummyDTO dto = DummyDTO.makeDummyDTO(true, false);

        checksumHeaderBarcodeTest(dto, DummyDTO.class);
    }

    @Test
    public void checksumHeaderCollectionsDtoBarcodeTest(){
        DummyDTO dto = DummyDTO.makeDummyDTO(false, true);

        checksumHeaderBarcodeTest(dto, DummyDTO.class);
    }

    @Test
    public void checksumHeaderCollectionsCompositionDtoBarcodeTest(){
        DummyDTO dto = DummyDTO.makeDummyDTO(true, true);

        checksumHeaderBarcodeTest(dto, DummyDTO.class);
    }

    private<T> T checksumHeaderBarcodeTest(T dto, Class<T> clazz){
        ChecksumHeader header = new ChecksumHeader();
        JabBarcode jabBarcode = new JabBarcode();

        // generate barcode
        String barcode = null;
        try {
            barcode = jabBarcode.objectToBarcode(header, dto);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail();
        }

        System.out.println(barcode);
        Assert.assertNotNull(barcode);
        Assert.assertTrue(barcode.startsWith(JabBarcode.PREFIX + JabBarcode.DELIMITER));

        // parse it back
        T res = null;
        try {
            res = jabBarcode.barcodeToObject(barcode, ChecksumHeader.class, clazz);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail();
        }

        System.out.println(res);
        Assert.assertNotNull(res);
        Assert.assertEquals(dto, res);

        return res;
    }
}
