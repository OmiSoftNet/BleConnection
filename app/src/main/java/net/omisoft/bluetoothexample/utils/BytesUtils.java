package net.omisoft.bluetoothexample.utils;

public class BytesUtils {
    public static int toInt(byte one, byte two) {
        return ((one & 0xff) << 8) | (two & 0xff);
    }

    public static String byteArrayToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte aByte : bytes) {
            hex.append(String.format("%02x", aByte));
        }
        return hex.toString();
    }
}
