package org.tj.mouton;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

// https://medium.com/@MeshCollider/some-of-the-math-behind-bech32-addresses-cf03c7496285
// https://github.com/libra/lip/blob/master/lips/lip-5.md
// https://github.com/bitcoinj/bitcoinj/blob/master/core/src/main/java/org/bitcoinj/core/Bech32.java#L107
// https://github.com/libra/off-chain-reference/blob/master/src/offchainapi/bech32.py
public final class Mouton32 {
    private Mouton32(){}

    private static final String CODE_MAP = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";

    public static byte[] convertToBitsGroups(final byte[] data) {
        final int BECH32_VERSION = 1;
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(BECH32_VERSION);

        final int IN_BITS = 8;
        final int OUT_BITS = 5;
        int inputIndex = 0;
        int bitBuffer = 0;
        int bitBufferLen = 0;

        while (inputIndex < data.length || bitBufferLen > 0) {
            assert 0 <= bitBufferLen && bitBufferLen <= IN_BITS + OUT_BITS - 1;
            assert (bitBuffer << bitBufferLen) == 0;

            if (bitBufferLen < OUT_BITS) {
                if (inputIndex < data.length) {
                    bitBuffer |= (data[inputIndex] & 0xFF) << (32 - IN_BITS - bitBufferLen);
                    inputIndex++;
                    bitBufferLen += IN_BITS;
                } else
                {
                    bitBufferLen = OUT_BITS;
                }
            }
            assert bitBufferLen >= 5;

            baos.write(bitBuffer >>> (32 - OUT_BITS));
            bitBuffer <<= OUT_BITS;
            bitBufferLen -= OUT_BITS;
        }

        return baos.toByteArray();
    }

    public static String encode(final byte[] data) {
        final StringBuilder sb = new StringBuilder();

        for (byte b : data) {
            sb.append(CODE_MAP.charAt(b));
        }

        return sb.toString();
    }

    private static byte[] expandHRP(final String hrp) {
        final char[] s = hrp.toCharArray();
        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        for (char c : s) {
            result.write(c >>> 5);
        }
        result.write(0);
        for (char c : s) {
            result.write(c & 0x1F);
        }
        return result.toByteArray();
    }

    private static int polymod(final byte[] data) {
        final int[] generator = {0x3B6A57B2, 0x26508E6D, 0x1EA119FA, 0x3D4233DD, 0x2A1462B3};

        int result = 1;
        for (byte b : data) {
            assert 0 <= b && b < 32;
            int x = result >>> 25;
            result = ((result & ((1 << 25) - 1)) << 5) | (b & 0xff);
            for (int i = 0; i < generator.length; i++) {
                result ^= ((x >>> i) & 1) * generator[i];
            }
            assert (result >>> 30) == 0;
        }
        return result;
    }

    public static String checksum(final String hrp, final byte[] data) throws IOException {
        final int CHECKSUM_LENGTH = 6;

        final byte[] expandedHRP = expandHRP(hrp);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(expandedHRP);
        baos.write(data);
        baos.write(new byte[CHECKSUM_LENGTH]);
        final byte[] data4cs = baos.toByteArray();

        final int checksum = polymod(data4cs) ^ 1;
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CHECKSUM_LENGTH; i++) {
            int b = (checksum >>> ((CHECKSUM_LENGTH - 1 - i) * 5)) & 0x1F;
            sb.append(CODE_MAP.charAt(b));
        }

        return sb.toString();
    }

    public static String encode(final String hrp, final String hexAddress, final String hexSubAddress) throws IOException, DecoderException {
        final byte[] data = Hex.decodeHex(hexAddress + hexSubAddress);
        final byte[] bits = Mouton32.convertToBitsGroups(data);
        final String checksumStr = checksum(hrp, bits);
        return hrp + "1" + Mouton32.encode(bits) + checksumStr;
    }

    public static String[] decode(final String address) throws IOException {
        final String[] data = new String[4];

        data[0] = address.substring(0, 3);
        data[1] = address.substring(address.length() - 6);

        final String datum = address.substring(4, address.length() - 6);
        final byte[] res = new byte[datum.length()];

        for(int i = 0; i < datum.length(); i++) {
            final int index = CODE_MAP.indexOf(datum.charAt(i));
            assert index >= 0: "Index cannot be < 0";
            res[i] = (byte)index;
        }

        final String checksumStr = checksum(data[0], res);
        assert checksumStr.equals(data[1]): "Invalid checksum: " + data[1] + " != " + checksumStr;

        final byte[] output = new byte[(res.length-1) * 5 / 8];

        final int IN_BITS = 5;
        final int OUT_BITS = 8;
        int outputIndex = 0;
        int bitBuffer = 0;
        int bitBufferLen = 0;

        for (int i = 1; i < res.length; i++) {
            int b = res[i];
            assert 0 <= bitBufferLen && bitBufferLen <= IN_BITS * 2;
            assert (bitBuffer << bitBufferLen) == 0;

            bitBuffer |= b << (32 - IN_BITS - bitBufferLen);
            bitBufferLen += IN_BITS;

            if (bitBufferLen >= OUT_BITS) {
                output[outputIndex] = (byte)(bitBuffer >>> (32 - OUT_BITS));
                outputIndex++;
                bitBuffer <<= OUT_BITS;
                bitBufferLen -= OUT_BITS;
            }
        }

        final String adrSubAdrs = Hex.encodeHexString(output);
        data[2] = adrSubAdrs.substring(0, 32);
        data[3] = adrSubAdrs.substring(32, 32 + 16);

        return data;
    }
}
