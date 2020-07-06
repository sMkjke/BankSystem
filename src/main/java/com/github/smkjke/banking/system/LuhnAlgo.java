package com.github.smkjke.banking.system;

/**
 * Luhn Algoritm is a simple checksum formula used to validate a variety of identification numbers,
 * such as credit card numbers, IMEI numbers...
 **/

public class LuhnAlgo {

    public static int getChecksum(final String mainNumber) {
        int sum = sum(mainNumber);
        return sum % 10 == 0 ? 0 : 10 - sum % 10;
    }

    /**
     * Checks cardNumber for LuhnAlgo validation
     *
     * @param cardNumber
     */
    public static boolean isValid(final String cardNumber) {
        return sum(cardNumber) % 10 == 0;
    }

    private static int sum(String number) {
        int sum = 0;
        for (int i = 0; i < number.length(); i++) {
            int num = Integer.parseInt(String.valueOf(number.charAt(i)));
            if (i % 2 == 0) {
                num *= 2;
            }
            if (num > 9) {
                num -= 9;
            }
            sum += num;
        }
        return sum;
    }
}