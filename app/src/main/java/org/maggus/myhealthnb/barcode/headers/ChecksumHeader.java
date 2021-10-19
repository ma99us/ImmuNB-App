package org.maggus.myhealthnb.barcode.headers;

import org.maggus.myhealthnb.barcode.JabBarcode;

import java.io.IOException;

import lombok.Data;

@Data
public class ChecksumHeader extends NoNullHeader {
    private Long checksum;

    @Override
    public void populate(Object dto, String payload) {
        //TODO: maybe use original DTO to get the checksum rather then payload barcode portion string?
        JabBarcode.Hasher hasher = new JabBarcode.Hasher();
        checksum = hasher.hashString(payload);
    }

    @Override
    public void validate(Object dto, String payload) throws IOException {
        //TODO: maybe use resulting DTO to get the checksum rather then payload barcode portion string?
        JabBarcode.Hasher hasher = new JabBarcode.Hasher();
        long hash = hasher.hashString(payload);
        if (checksum != hash) {
            throw new IOException("Barcode checksum mismatch; expected " + checksum + ", but got " + hash);
        }
    }
}
