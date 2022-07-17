package nghviet.hgw.utility;

public class Processing {
    public static int unsignedByte(byte value) {
        if(value < 0) return (int) value + 256;
        return (int)value;
    }

    public static String convert(byte[] value) {
        int intValue = 0;
        return String.valueOf(Float.intBitsToFloat( (value[0]&0xFF) ^ (value[1]&0xFF)<<8 ^ (value[2]&0xFF)<<16 ^ (value[3]&0xFF)<<24));
    }

    public static String convertShort(byte[] edt) {
        int intValue = 0;
        return String.valueOf( (edt[0]&0xFF) ^ (edt[1]&0xFF)<<8 );
    }
}

