package com.github.smkjke.banking.system;

public interface AccountDao {

    Account get(String cardNum);

    void create(Account account);

    void update(int accountId, int amount);

    void transfer(Account sender, Account receiver, int amount, long waitBeforeStartMs, boolean fail);

    void delete(int accountId);

}
