package org.maggus.myhealthnb.barcode.headers;

import org.maggus.myhealthnb.barcode.BarcodeConfig;
import org.maggus.myhealthnb.barcode.JabBarcode;

import java.io.IOException;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=true)
public class CryptoChecksumHeader extends ChecksumHeader {
    private Long keyId;

    @Override
    public String obfuscate(Object dto, String payload) {
        // shrink 'null's
        payload = super.obfuscate(dto, payload);

        // populate key id
        keyId = new JabBarcode.Hasher().hashString(BarcodeConfig.CRYPTO_KEY + BarcodeConfig.CRYPTO_SALT);

        // encrypt the payload
        String encrypted = new JabBarcode.Crypto().encryptString(payload, BarcodeConfig.CRYPTO_KEY);
        return "[" + encrypted + "]";
    }

    @Override
    public String deobfuscate(Object dto, String payload) throws IOException {
        // validate key id first
        long expectedKeyId = new JabBarcode.Hasher().hashString(BarcodeConfig.CRYPTO_KEY + BarcodeConfig.CRYPTO_SALT);
        if (expectedKeyId != keyId) {
            throw new IOException("Barcode security key mismatch; expected " + expectedKeyId + ", but got " + keyId);
        }

        //decrypt payload
        payload = payload.substring(1, payload.length() - 1);        // trim off "[", "]";
        String decrypted = new JabBarcode.Crypto().decryptString(payload, BarcodeConfig.CRYPTO_KEY);
        if (!decrypted.startsWith("[") || !decrypted.endsWith("]")) {
            throw new IOException("Unexpected decrypted barcode payload");
        }

        // inflate 'null's back
        decrypted = super.deobfuscate(dto, decrypted);

        return decrypted;
    }
}
