package com.github.smkjke.banking.system;

import java.util.concurrent.atomic.AtomicInteger;

public class BankSystem {

    private AtomicInteger bankID = new AtomicInteger(0);
    private final AccountDao accountDao;

    public BankSystem(String fileName) {
        this(new AccountDaoImpl(fileName));
    }

    BankSystem(AccountDao dao) {
        this.accountDao = dao;
    }


    public int getBankID() {
        return bankID.get();
    }


    public Account getAccount(String cardNum) {
        return accountDao.get(cardNum);
    }

    public boolean tryToLogInAccount(final String cardNum, final String pinNum) {
        Account account = accountDao.get(cardNum);
        return account != null && account.getCardNumber().equals(cardNum) && account.getPinCode().equals(pinNum);
    }

    public void tryToLogOutAccount() {
        System.out.println("You have successfully logged out!");
    }

    public void addCardToDB(Account account) {
        accountDao.create(account);
        bankID.incrementAndGet();
    }


    public void addIncome(Account account, int addSum) {
        accountDao.update(account.getId(), addSum);
    }

    public int getCurrentCardBalance(String cardNum) {
        Account account = accountDao.get(cardNum);
        return account.getBalance();
    }

    // remove after fixing
    public void doTransfer(String senderCardNum, String receiverCardNum, int sum) {
        doTransfer(senderCardNum, receiverCardNum, sum, 0, false);
    }

    public void doTransfer(String senderCardNum, String receiverCardNum, int sum, long waitBeforeStartMs, boolean fail) {
        accountDao.transfer(accountDao.get(senderCardNum), accountDao.get(receiverCardNum), sum, waitBeforeStartMs, fail);
    }


    public boolean isCardExistsAndValid(String receiverCard) {
        if (accountDao.get(receiverCard) != null) {
            if (receiverCard.length() == 16 && LuhnAlgo.isValid(receiverCard)) {
                return true;
            } else {
                System.out.println("Probably you made mistake in card number. Please try again!");
                return false;
            }
        }
        System.out.println("Such a card does not exist.");
        return false;
    }


    public void closeAccount(int id) {
        accountDao.delete(id);
        System.out.println("The account " + id + " has been deleted!");
    }

}