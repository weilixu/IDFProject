package main.java.util;

public class VisualUtil {
    /**
     * Blue: #0074D9
     * Green: #2ECC40
     * Red: #FF4136
     */
    public static String makeColor(double val, double min, double max) {
        double mid = (min+max)/2;
        if(val==mid){
            return "0x2ECC40";
        }

        int rCode, gCode, bCode;
        if(mid>val){
            // blue to green
            double rDis = 45;    //00 - 2E
            double gDis = 88;    //74 - CC
            double bDis = 153;   //D9 - 40
            double ratio = (val - min) / (mid - min);

            rCode = (int)Math.round(ratio*rDis);
            gCode = (int)Math.round(ratio*gDis)+116;  //0x74
            bCode = 217-(int)Math.round(ratio*bDis);  //0xD9
        }else {
            //green to red
            double rDis = 209;  //2E - FF
            double gDis = 139;  //CC - 41
            double bDis = 10;   //40 - 36
            double ratio = (val - mid) / (max - mid);

            rCode = (int)Math.round(ratio*rDis)+46;   //0x2E
            gCode = 204-(int)Math.round(ratio*gDis);  //0xCC
            bCode = 64-(int)Math.round(ratio*bDis);   //0x40
        }

        String hexR = Integer.toHexString(rCode).toUpperCase();
        String hexG = Integer.toHexString(gCode).toUpperCase();
        String hexB = Integer.toHexString(bCode).toUpperCase();

        if (hexR.length() == 1) {
            hexR = "0" + hexR;
        }
        if (hexG.length() == 1) {
            hexG = "0" + hexG;
        }
        if (hexB.length() == 1) {
            hexB = "0" + hexB;
        }

        return "0x" + hexR + hexG + hexB;

        /*int range = 0xFF;
        int colorCode = (int) ((val - min) / (max - min) * range);

        String hexR = Integer.toHexString(colorCode).toUpperCase();
        String hexG = Integer.toHexString(0xFF - colorCode).toUpperCase();

        if (hexR.length() == 1) {
            hexR = "0" + hexR;
        }
        if (hexG.length() == 1) {
            hexG = "0" + hexG;
        }
        return "0x" + hexR + hexG + "00";*/
    }

    /**
     * Blue: #0074D9
     * Green: #2ECC40
     * Red: #FF4136
     */
    public static String makeColorReverse(double val, double min, double max) {
        double mid = (min + max) / 2;
        if (val == mid) {
            return "0x2ECC40";
        }

        int rCode, gCode, bCode;
        if (mid > val) {
            // red to green
            double rDis = 209;    //FF - 2E
            double gDis = 139;    //41 - CC
            double bDis = 10;     //36 - 40
            double ratio = (val - min) / (mid - min);

            rCode = 255 - (int) Math.round(ratio * rDis);  //0xFF
            gCode = (int) Math.round(ratio * gDis) + 65;   //0x41
            bCode = (int) Math.round(ratio * bDis) + 54;   //0x36
        } else {
            //green to blue
            double rDis = 46;    //2E - 00
            double gDis = 88;    //CC - 74
            double bDis = 153;   //40 - D9
            double ratio = (val - mid) / (max - mid);

            rCode = (int) Math.round(ratio * rDis) + 46;   //0x2E
            gCode = 204 - (int) Math.round(ratio * gDis);  //0xCC
            bCode = (int) Math.round(ratio * bDis) + 64;   //0x40
        }

        String hexR = Integer.toHexString(rCode).toUpperCase();
        String hexG = Integer.toHexString(gCode).toUpperCase();
        String hexB = Integer.toHexString(bCode).toUpperCase();

        if (hexR.length() == 1) {
            hexR = "0" + hexR;
        }
        if (hexG.length() == 1) {
            hexG = "0" + hexG;
        }
        if (hexB.length() == 1) {
            hexB = "0" + hexB;
        }

        return "0x" + hexR + hexG + hexB;
    }

    public static void main(String[] args){
        System.out.println(makeColor(0d, 0d, 100d));
        System.out.println(makeColor(100d/9d, 0d, 100d));
        System.out.println(makeColor(200d/9d, 0d, 100d));
        System.out.println(makeColor(300d/9d, 0d, 100d));
        System.out.println(makeColor(400d/9d, 0d, 100d));
        System.out.println(makeColor(500d/9d, 0d, 100d));
        System.out.println(makeColor(600d/9d, 0d, 100d));
        System.out.println(makeColor(700d/9d, 0d, 100d));
        System.out.println(makeColor(800d/9d, 0d, 100d));
        System.out.println(makeColor(100d, 0d, 100d));
    }
}
