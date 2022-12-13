package org.tl.nettyServer.media.util;

public class BinaryUtils {



    public static int getBinaryFromInt(int src, int srcBitsLength, int startIndex, int length) {
        int BINARY_ONE = 0x01;
        int pointer = BINARY_ONE;
        int bitTotalIndex = srcBitsLength - 1;

        int bunchOfOneLeftBorderIndex = bitTotalIndex;
        int bunchOfOneRightBorderIndex = bitTotalIndex;
        //we assume src and srcBitsLength is correct

        if (startIndex < 0 || startIndex > bitTotalIndex) {
            System.out.println("error startIndex");
        }

        if (length < 0 || length > srcBitsLength) {
            System.out.println("error length");
        }

        if (startIndex + length > srcBitsLength) {
            int maxStartIndex = srcBitsLength - length;
            int maxLength = srcBitsLength - startIndex;
            System.out.println(String.format("error maxStartIndex is %s or maxLength is %s", maxStartIndex, maxLength));
        }

        for (int i = 0; i < length - 1; i++) {
            pointer <<= 1;
            pointer += BINARY_ONE;
            bunchOfOneLeftBorderIndex--;
        }

        int needMoveBits = bunchOfOneLeftBorderIndex - startIndex;

        for (int i = 0; i < needMoveBits; i++) {
            pointer <<= 1;
            bunchOfOneLeftBorderIndex--;
            bunchOfOneRightBorderIndex--;
        }

        int res = (src & pointer) >> (bitTotalIndex - bunchOfOneRightBorderIndex);

        return res;
    }


    public static String printBinaryFromInt(int src, int srcBitsLength, int startIndex, int length) {
        int BINARY_ONE = 0x01;
        int pointer = BINARY_ONE;
        int bitTotalIndex = srcBitsLength - 1;

        int bunchOfOneLeftBorderIndex = bitTotalIndex;
        int bunchOfOneRightBorderIndex = bitTotalIndex;
        //we assume src and srcBitsLength is correct

        if (startIndex < 0 || startIndex > bitTotalIndex) {
            System.out.println("error startIndex");
        }

        if (length < 0 || length > srcBitsLength) {
            System.out.println("error length");
        }

        if (startIndex + length > srcBitsLength) {
            int maxStartIndex = srcBitsLength - length;
            int maxLength = srcBitsLength - startIndex;
            System.out.println(String.format("error maxStartIndex is %s or maxLength is %s", maxStartIndex, maxLength));
        }

        for (int i = 0; i < length - 1; i++) {
            pointer <<= 1;
            pointer += BINARY_ONE;
            bunchOfOneLeftBorderIndex--;
        }

        int needMoveBits = bunchOfOneLeftBorderIndex - startIndex;

        for (int i = 0; i < needMoveBits; i++) {
            pointer <<= 1;
            bunchOfOneLeftBorderIndex--;
            bunchOfOneRightBorderIndex--;
        }

        int res = (src & pointer) >> (bitTotalIndex - bunchOfOneRightBorderIndex);


        return printToBinaryFormatString(res, length);
    }


    static private String printToBinaryFormatString(int src, int length) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        while (true) {
            int b = src % 2;
            src /= 2;

            if (src == 0) {
                sb.append(b);
                count++;

                int needFill = length - count;
                for (int i = 0; i < needFill; i++) {
                    sb.append("  |  " + 0);
                }
                break;
            } else {
                sb.append(b + "  |  ");
                count++;
            }
        }
        sb.reverse();
        return sb.toString();
    }


    public static int bits2Int(int[] src) {
        return bits2Int(src, 0);
    }

    public static int bits2Int(int[] src, int zeroNum) {
        int res = 0;

        for (int i = 0; i < src.length; i++) {
            int step = src.length - 1 - i + zeroNum;
            res += (src[i] << step);
        }
        return res;
    }


    public static BinaryGetterByBit binaryGetterByBit(int bitNum) {
        return new BinaryGetterByBit(bitNum, true);
    }

    public static class BinaryGetterByBit {
        private static final int BINARY_ONE = 0x01;

        /**
         * when bitsNum is 8 true present pointer is  1000 0000  ,false is  0000 0001
         */
        private boolean startWithHigh = true;

        /**
         * how many bit src has
         */
        private final int bitsNum;

        /**
         * its like 1000 0000
         */
        private int pointer;

        /**
         * the index of pointer that current bit value is 1
         */
        private int currentBitIndex;

        /**
         * store the initial state of pointer to restore
         */
        private int restState;

        BinaryGetterByBit(int bitsNum, boolean startWithHigh) {
            this.startWithHigh = startWithHigh;
            this.bitsNum = bitsNum;
            this.init();
        }

        private void init() {
            this.pointer = BINARY_ONE;

            if (this.startWithHigh) {
                for (int i = 0; i < this.bitsNum - 1; i++) {
                    this.pointer <<= 1;
                }
                this.currentBitIndex = 0;
            } else {
                this.currentBitIndex = 7;
            }
            this.restState = this.pointer;
        }

        public byte getOneBitAndMovePointer(int src) {
            if (this.bitsNum < 0 || this.currentBitIndex > this.bitsNum - 1) {
                return -1;
            }

            int res = -2;
            if (this.startWithHigh) {
                if (currentBitIndex <= this.bitsNum - 1) {
                    //this.pointer >>= 1;
                    res = ((this.pointer >> this.currentBitIndex) & src) >> (bitsNum - 1 - this.currentBitIndex);
                    this.currentBitIndex++;
                }
            } else {
                if (currentBitIndex >= 0) {
                    //this.pointer <<= 1;
                    res = ((this.pointer >> (this.bitsNum - this.currentBitIndex)) & src) >> (bitsNum - 1 - this.currentBitIndex);
                    this.currentBitIndex--;
                }
            }
            return (byte) res;
        }

        public int getCurrentPointIndex() {
            return this.currentBitIndex;
        }

        public boolean setAtBitIndex(int bitIndex) {
            if (this.bitsNum < 0 || bitIndex > this.bitsNum - 1) {
                return false;
            }

            this.pointer = this.restState;


            this.currentBitIndex = bitIndex;
            return true;
        }

        public static void main(String[] args) {
            BinaryGetterByBit binaryGetterByBit = BinaryUtils.binaryGetterByBit(8);

            int res = -2;
            res = binaryGetterByBit.getOneBitAndMovePointer(0x13);
            res = binaryGetterByBit.getOneBitAndMovePointer(0x13);
            res = binaryGetterByBit.getOneBitAndMovePointer(0x13);
            res = binaryGetterByBit.getOneBitAndMovePointer(0x13);
            res = binaryGetterByBit.getOneBitAndMovePointer(0x13);
            res = binaryGetterByBit.getOneBitAndMovePointer(0x13);
            res = binaryGetterByBit.getOneBitAndMovePointer(0x13);
            res = binaryGetterByBit.getOneBitAndMovePointer(0x13);

            res = binaryGetterByBit.getOneBitAndMovePointer(0x13);


            binaryGetterByBit.setAtBitIndex(7);

            res = binaryGetterByBit.getOneBitAndMovePointer(0x13);
            String s = BinaryUtils.printBinaryFromInt(0x080, 8, 0, 8);
            int dot = BinaryUtils.getBinaryFromInt(0x080, 8, 0, 4);

            int i = bits2Int(new int[]{0, 1, 0, 0, 0, 0, 0, 0});

            int i1 = bits2Int(new int[]{0, 1, 0}, 5);
//

            byte[] as = new byte[2];

            as[0] = (byte) 0xff;
            as[1] = (byte) 255;
            System.out.println();
        }
    }
}
