package com.github.smkjke.banking.system;

import java.util.Random;

public class Account {

    private String cardNumber;
    private String pinCode;
    private int balance;
    private int id;

    public Account(int id) {
        this.id = id;
        this.generateCard();
        this.balance = 0;
    }

    public Account(int id, String number, String pinCode, int balance) {
        this.id = id;
        this.cardNumber = number;
        this.pinCode = pinCode;
        this.balance = balance;
    }

    /**
     * Create card number with
     * IIN 400000, random customer account number
     * and Luhn algorithm checksum
     */

    public void generateCard() {
        Random random = new Random();
        String randomCard = "400000" + ((long) Math.floor(Math.random() * 9_000_000_00) + 1_000_000_00);

        this.cardNumber = randomCard + String.valueOf(LuhnAlgo.getChecksum(randomCard));
        this.pinCode = String.format("%04d", random.nextInt(10000));
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getPinCode() {
        return pinCode;
    }

    public void setPinCode(String pinCode) {
        this.pinCode = pinCode;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

}
