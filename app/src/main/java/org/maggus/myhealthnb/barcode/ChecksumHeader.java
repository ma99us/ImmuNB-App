package org.maggus.myhealthnb.barcode;

import java.io.IOException;

import lombok.Data;

@Data
public class ChecksumHeader implements JabBarcodeHeader{
    private Long checksum;

    @Override
    public void populate(Object dto, String payload) {
        //TODO: maybe use original DTO to get the checksum rather then payload barcode portion string?
        checksum = JabBarcode.hashString(payload);
    }

    @Override
    public void validate(Object dto, String payload) throws IOException {
        //TODO: maybe use resulting DTO to get the checksum rather then payload barcode portion string?
        long hash = JabBarcode.hashString(payload);
        if (checksum != hash) {
            throw new IOException("Barcode checksum mismatch; expected " + checksum + ", but got " + hash);
        }
    }

    @Override
    public String obfuscate(Object dto, String payload) {
        return payload; // do nothing
    }

    @Override
    public String deobfuscate(Object dto, String payload) {
        return payload; // do nothing
    }
}
