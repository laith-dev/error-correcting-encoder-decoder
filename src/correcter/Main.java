package correcter;

import java.io.*;
import java.util.Random;
import java.util.Scanner;

public class Main {

    private static final String SEND_FILE = "send.txt";
    private static final String ENCODED_FILE = "encoded.txt";
    private static final String RECEIVED_FILE = "received.txt";
    private static final String DECODED_FILE = "decoded.txt";

    public static void main(String[] args) {
        System.out.print("Write a mode: ");
        Scanner sc = new Scanner(System.in);
        String mode = sc.next();
        switch (mode) {
            case "encode":
                encode();
                break;

            case "send":
                send();
                break;

            case "decode":
                decode();
                break;
        }
    }

    private static void encode() {
        byte[] sendAsBytes = new byte[0];
        try (InputStream fileInputStream = new FileInputStream(SEND_FILE)) {
            sendAsBytes = fileInputStream.readAllBytes();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /* Pad each byte with insignificant leading zeroes. */
        String[] bytes = new String[sendAsBytes.length];
        String currentByte;
        for (int i = 0; i < bytes.length; i++) {
            currentByte = Integer.toBinaryString(sendAsBytes[i]);
            int leadingZeroesNeeded = 8 - currentByte.length();

            bytes[i] = "".concat("0".repeat(leadingZeroesNeeded)).concat(currentByte);
        }

        String[] bytesWithParities = new String[bytes.length * 2];
        // Each byte makes two bytes with parities.
        int i = 0;
        for (String b : bytes) {
            bytesWithParities[i] = getByteWithParity(
                    b.charAt(0), b.charAt(1), b.charAt(2), b.charAt(3)
            );
            bytesWithParities[i + 1] = getByteWithParity(
                    b.charAt(4), b.charAt(5), b.charAt(6), b.charAt(7)
            );
            i += 2;
        }

        try (Writer fileWriter = new FileWriter(ENCODED_FILE, false)) {
            for (int j = 0; j < bytesWithParities.length; j++) {
                String toWrite = bytesWithParities[j] + ' ';
                // Remove the last trailing space.
                if (j + 1 == bytesWithParities.length) {
                    toWrite = toWrite.trim();
                }

                fileWriter.write(toWrite);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void send() {
        String[] bytes = new String[0];
        try (Scanner fileScanner = new Scanner(new File(ENCODED_FILE))) {
            bytes = fileScanner.nextLine().split(" ");
        } catch (IOException e) {
            e.printStackTrace();
        }

        simulateErrors(bytes);

        try (Writer fileWriter = new FileWriter(RECEIVED_FILE, false)) {
            for (int i = 0; i < bytes.length; i++) {
                String toWrite = bytes[i] + " ";
                if (i + 1 == bytes.length) {
                    toWrite = toWrite.trim();
                }

                fileWriter.write(toWrite);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void decode() {
        String[] bytes = new String[0];
        try (Scanner fileScanner = new Scanner(new File(RECEIVED_FILE))) {
            bytes = fileScanner.nextLine().split(" ");
        } catch (IOException e) {
            e.printStackTrace();
        }

        String[] decodedBytes = decodeBytes(bytes);

        try (Writer fileWriter = new FileWriter(DECODED_FILE, false)) {
            for (String aByte : decodedBytes) {
                fileWriter.write((char) Integer.parseInt(aByte, 2));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getByteWithParity(char first, char second, char third, char fourth) {
        int firstBit = Integer.parseInt(String.valueOf(first));
        int secondBit = Integer.parseInt(String.valueOf(second));
        int thirdBit = Integer.parseInt(String.valueOf(third));
        int fourthBit = Integer.parseInt(String.valueOf(fourth));

        // P is the position of parities. Each byte always ends with unusable zero.
        char[] byteWithParity = {'1', '2', first, '4', second, third, fourth, '0'};

        char p1 = (char) (((firstBit + secondBit + fourthBit) % 2) + '0');
        char p2 = (char) (((firstBit + thirdBit + fourthBit) % 2) + '0');
        char p4 = (char) (((secondBit + thirdBit + fourthBit) % 2) + '0');

        byteWithParity[0] = p1;
        byteWithParity[1] = p2;
        byteWithParity[3] = p4;

        return String.valueOf(byteWithParity);
    }

    private static void simulateErrors(String[] bytes) {
        Random random = new Random();

        char[] temp;
        int bitIndexToChange;
        for (int i = 0; i < bytes.length; i++) {
            bitIndexToChange = random.nextInt(8); // 0-7

            temp = bytes[i].toCharArray();
            temp[bitIndexToChange] = temp[bitIndexToChange] == '1' ? '0' : '1';

            bytes[i] = String.valueOf(temp);
        }
    }

    private static String[] decodeBytes(String[] bytes) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < bytes.length; i += 2) {
            stringBuilder.append(extractDataBytes(bytes[i]))
                    .append(extractDataBytes(bytes[i + 1]))
                    .append(' ');
        }

        return String.valueOf(stringBuilder).trim().split(" ");
    }

    private static String extractDataBytes(String aByte) {
        int p1 = Integer.parseInt(String.valueOf(aByte.charAt(0)));
        int p2 = Integer.parseInt(String.valueOf(aByte.charAt(1)));
        int d3 = Integer.parseInt(String.valueOf(aByte.charAt(2)));
        int p4 = Integer.parseInt(String.valueOf(aByte.charAt(3)));
        int d5 = Integer.parseInt(String.valueOf(aByte.charAt(4)));
        int d6 = Integer.parseInt(String.valueOf(aByte.charAt(5)));
        int d7 = Integer.parseInt(String.valueOf(aByte.charAt(6)));

        boolean p1Pass = p1 == (d3 + d5 + d7) % 2;
        boolean p2Pass = p2 == (d3 + d6 + d7) % 2;
        boolean p4Pass = p4 == (d5 + d6 + d7) % 2;

        /* If an error occurs in a data bit, it should be pointed out by two or
         * more parity bits. If only one parity bit has an error, then ignore it. */
        int errorBitIndex = -1;
        if (!p1Pass && !p2Pass && !p4Pass) {
            errorBitIndex = 6;
        } else if (!p1Pass && !p2Pass) {
            errorBitIndex = 2;
        } else if (!p1Pass && !p4Pass) {
            errorBitIndex = 4;
        } else if (!p2Pass && !p4Pass) {
            errorBitIndex = 5;
        }

        char[] chars = aByte.toCharArray();
        if (errorBitIndex != -1) {
            chars[errorBitIndex] = chars[errorBitIndex] == '1' ? '0' : '1';
        }

        return "" + chars[2] + chars[4] + chars[5] + chars[6];
    }

}