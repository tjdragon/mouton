package org.tj.mouton;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.Test;

import java.io.IOException;

// https://github.com/libra/lip/blob/master/lips/lip-5.md
public class Bech32Test {
  @Test
  public void bech32_1_Test() throws IOException, DecoderException {
    final String hrp = "lbr";
    final String address = "f72589b71ff4f8d139674a3f7369c69b";
    final String subAddress = "cf64428bdeb62af2";
    final byte[] data = Hex.decodeHex(address + subAddress);
    final byte[] bits = Mouton32.convertToBitsGroups(data);
    final String checksumStr = Mouton32.checksum(hrp, bits);

    final String eb32 = hrp + "1" + Mouton32.encode(bits) + checksumStr;
    assert eb32.equals("lbr1p7ujcndcl7nudzwt8fglhx6wxn08kgs5tm6mz4usw5p72t"): checksumStr;
    assert eb32.equals(Mouton32.encode(hrp, address, subAddress));
  }

  @Test
  public void bech32_2_Test() throws IOException, DecoderException {
    final String hrp = "lbr";
    final String address = "f72589b71ff4f8d139674a3f7369c69b";
    final String subAddress = "0000000000000000";
    final byte[] data = Hex.decodeHex(address + subAddress);
    final byte[] bits = Mouton32.convertToBitsGroups(data);
    final String checksumStr = Mouton32.checksum(hrp, bits);

    final String eb32 = hrp + "1" + Mouton32.encode(bits) + checksumStr;
    assert eb32.equals("lbr1p7ujcndcl7nudzwt8fglhx6wxnvqqqqqqqqqqqqqflf8ma"): checksumStr;
    assert eb32.equals(Mouton32.encode(hrp, address, subAddress));
  }

  @Test
  public void bech32_3_Test() throws IOException {
    final String[] data = Mouton32.decode("lbr1p7ujcndcl7nudzwt8fglhx6wxn08kgs5tm6mz4usw5p72t");

    assert "lbr".equals(data[0]): "Wrong network: lbr != " + data[0];
    assert "w5p72t".equals(data[1]): "Wrong checksum: flf8ma != " + data[1];
    assert "f72589b71ff4f8d139674a3f7369c69b".equals(data[2]): "Wrong address: f72589b71ff4f8d139674a3f7369c69b != " + data[2];
    assert "cf64428bdeb62af2".equals(data[3]): "Wrong sub-address: cf64428bdeb62af2 != " + data[3];
  }

  @Test
  public void bech32_4_Test() throws IOException {
    final String[] data = Mouton32.decode("lbr1p7ujcndcl7nudzwt8fglhx6wxnvqqqqqqqqqqqqqflf8ma");

    assert "lbr".equals(data[0]): "Wrong network: lbr != " + data[0];
    assert "flf8ma".equals(data[1]): "Wrong checksum: flf8ma != " + data[1];
    assert "f72589b71ff4f8d139674a3f7369c69b".equals(data[2]): "Wrong address: f72589b71ff4f8d139674a3f7369c69b != " + data[2];
    assert "0000000000000000".equals(data[3]): "Wrong sub-address: 0000000000000000 != " + data[3];
  }
}
