package org.maggus.myhealthnb;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.maggus.myhealthnb.barcode.headers.CryptoChecksumHeader;
import org.maggus.myhealthnb.barcode.headers.ChecksumHeader;
import org.maggus.myhealthnb.barcode.JabBarcode;
import org.maggus.myhealthnb.barcode.headers.BarcodeHeader;
import org.maggus.myhealthnb.dummy.DummyDTO;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
public class JabBarcodeTest {

    @Test
    public void isPossibleJabBarcodeTest() {
        DummyDTO dto = DummyDTO.makeDummyDTO(false, false);

        JabBarcode jabBarcode = new JabBarcode();
        Assert.assertEquals(false, jabBarcode.isPossibleJabBarcode(null));
        Assert.assertEquals(false, jabBarcode.isPossibleJabBarcode("akdjhfo237fh8fgo8dgfvb587gtb"));

        // generate barcode
        String barcode = null;
        try {
            barcode = jabBarcode.objectToBarcode(null, dto);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail();
        }

        Assert.assertEquals(true, jabBarcode.isPossibleJabBarcode(barcode));
    }

    @Test
    public void findJabBarcodeFormatIdTest() {
        DummyDTO dto = DummyDTO.makeDummyDTO(false, false);
        ChecksumHeader header = new ChecksumHeader();

        JabBarcode jabBarcode = new JabBarcode();
        Assert.assertEquals(-1, jabBarcode.findJabBarcodeFormatId(null));
        Assert.assertEquals(-1, jabBarcode.findJabBarcodeFormatId("akdjhfo237fh8fgo8dgfvb587gtb"));

        long simpleBarcodeFormatId = JabBarcode.getBarcodeFormatId(null, dto.getClass());
        long checksumBarcodeFormatId = JabBarcode.getBarcodeFormatId(header.getClass(), dto.getClass());

        // generate barcode
        String barcode = null, csBarcode = null;
        try {
            barcode = jabBarcode.objectToBarcode(null, dto);
            csBarcode = jabBarcode.objectToBarcode(header, dto);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail();
        }

        Assert.assertEquals(simpleBarcodeFormatId, jabBarcode.findJabBarcodeFormatId(barcode));
        Assert.assertEquals(checksumBarcodeFormatId, jabBarcode.findJabBarcodeFormatId(csBarcode));
    }

    @Test
    public void registeredBarcodeFormatTest() {
        DummyDTO dto = DummyDTO.makeDummyDTO(false, false);

        JabBarcode jabBarcode = new JabBarcode();

        // generate barcodes
        String sBarcode = null, csBarcode = null;
        try {
            sBarcode = jabBarcode.objectToBarcode(null, dto);
            csBarcode = jabBarcode.objectToBarcode(new ChecksumHeader(), dto);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail();
        }

        // register barcode formats
        jabBarcode.getRegistry().registerFormat(ChecksumHeader.class, DummyDTO.class);

        // parse it back
        try {
            jabBarcode.barcodeToObject(sBarcode);   // should throw an exception
            Assert.fail();
        } catch (IOException e) {
            System.out.println("Expected exception: " + e.getMessage());
        }

        DummyDTO res = null;
        try {
            res = (DummyDTO) jabBarcode.barcodeToObject(csBarcode);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }

        Assert.assertEquals(dto, res);
    }

    @Test
    public void basicBarcodeTest() {
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
    public void checksumHeaderBarcodeTest() {
        DummyDTO dto = DummyDTO.makeDummyDTO(false, false);
        ChecksumHeader header = new ChecksumHeader();

        barcodeTest(header, ChecksumHeader.class, dto, DummyDTO.class);
    }

    @Test
    public void checksumHeaderCompositionDtoBarcodeTest() {
        DummyDTO dto = DummyDTO.makeDummyDTO(true, false);
        ChecksumHeader header = new ChecksumHeader();

        barcodeTest(header, ChecksumHeader.class, dto, DummyDTO.class);
    }

    @Test
    public void checksumHeaderCollectionsDtoBarcodeTest() {
        DummyDTO dto = DummyDTO.makeDummyDTO(false, true);
        ChecksumHeader header = new ChecksumHeader();

        barcodeTest(header, ChecksumHeader.class, dto, DummyDTO.class);
    }

    @Test
    public void checksumHeaderCollectionsCompositionDtoBarcodeTest() {
        DummyDTO dto = DummyDTO.makeDummyDTO(true, true);
        ChecksumHeader header = new ChecksumHeader();

        barcodeTest(header, ChecksumHeader.class, dto, DummyDTO.class);
    }

    @Test
    public void encryptedChecksumHeaderCollectionsCompositionDtoBarcodeTest() {
        DummyDTO dto = DummyDTO.makeDummyDTO(true, true);
        CryptoChecksumHeader header = new CryptoChecksumHeader();

        barcodeTest(header, CryptoChecksumHeader.class, dto, DummyDTO.class);
    }

    @Test
    public void wrapBytesTest() {
        // input shorter then the output
        String sStr = "short string", sStr1 = "short str1ng";

        byte[] rBytes = new JabBarcode.Hasher().wrapBytes(sStr.getBytes(StandardCharsets.UTF_8), 256);
        byte[] rBytes1 = new JabBarcode.Hasher().wrapBytes(sStr1.getBytes(StandardCharsets.UTF_8), 256);

        Assert.assertNotNull(rBytes);
        Assert.assertEquals(256, rBytes.length);
        Assert.assertNotEquals(rBytes, rBytes1);

        // input longer then the  output
        sStr = "some very long string to get bytes from for wrapping into shorter buffer";
        sStr1 = "some very 1ong string to get bytes from for wrapping into shorter buffer";

        rBytes = new JabBarcode.Hasher().wrapBytes(sStr.getBytes(StandardCharsets.UTF_8), 8);
        rBytes1 = new JabBarcode.Hasher().wrapBytes(sStr1.getBytes(StandardCharsets.UTF_8), 8);

        Assert.assertNotNull(rBytes);
        Assert.assertEquals(8, rBytes.length);
        Assert.assertNotEquals(rBytes, rBytes1);
    }

    private <H extends BarcodeHeader, P> P barcodeTest(H header, Class<H> hClass, P dto, Class<P> pClass) {
        JabBarcode jabBarcode = new JabBarcode();

        // generate barcode
        String barcode = null;
        try {
            barcode = jabBarcode.objectToBarcode(header, dto);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail();
        }

        Assert.assertNotNull(barcode);
        System.out.println("barcode: (" + barcode.length() + " bytes): \"" + barcode + "\"");
        Assert.assertTrue(barcode.startsWith(JabBarcode.PREFIX + JabBarcode.DELIMITER));

        // parse it back
        P res = null;
        try {
            res = jabBarcode.barcodeToObject(barcode, hClass, pClass);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail();
        }

        System.out.println(res);
        Assert.assertNotNull(res);
        Assert.assertEquals(dto, res);

        // alter the barcode!
        int length = barcode.length();
        int p0 = barcode.lastIndexOf("[");
        Assert.assertTrue(p0 > 0);
        int i = p0 + (int) (Math.random() * (length - p0));
        StringBuilder sb = new StringBuilder(barcode);
        char c = sb.charAt(i);
        c = c == 'A' ? 'B' : 'A';
        sb.setCharAt(i, c);
        String altBarcode = sb.toString();
        Assert.assertNotEquals(barcode, altBarcode);

        // try to decode it again (it should fail!)
        Object noRes = null;
        try {
            noRes = jabBarcode.barcodeToObject(altBarcode, hClass, pClass);
            // should not get here!
            Assert.fail();
        } catch (IOException e) {
            // expected exception
            System.out.println("Expected exception: " + e.getMessage());
        }
        Assert.assertNull(noRes);

        return res;
    }
}
