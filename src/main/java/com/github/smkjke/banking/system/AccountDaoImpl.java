package com.github.smkjke.banking.system;

import java.sql.*;
import java.util.Arrays;

public class AccountDaoImpl implements AccountDao {

    private static final String URL = "jdbc:h2:/tmp/task_card/";
    // number is supposed to be unique while h2 doesn't support unique constraint on blob cblob
    private static final String CREATE_NEW_TABLE = "CREATE TABLE IF not EXISTS card (" +
            "ownerId INTEGER," +
            "number TEXT," +
            "pin TEXT," +
            "balance INTEGER DEFAULT 0)";
    private static final String LOCK_BY_CARD_SQL = "SELECT number FROM card where number = ? FOR UPDATE";
    private String fileName = "";

    public AccountDaoImpl(String fileName) {
        this.fileName = fileName;
        createIfNotExists();
    }

    /**
     * @return the Connection object
     */

    private Connection connect() {
        try {
            return DriverManager.
                    getConnection(URL + fileName);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }


    void createIfNotExists() {
        Statement stmt = null;
        try {
            Connection conn = this.connect();
            stmt = conn.createStatement();
            stmt.execute(CREATE_NEW_TABLE);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Account get(String number) {
        try (Connection conn = this.connect();
             PreparedStatement statement = conn.prepareStatement("select * from card " +
                     "where number = ? ")) {
            statement.setString(1, number);

            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                return new Account(rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getInt(4));
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void create(Account account) {
        try {
            Connection conn = this.connect();
            PreparedStatement statement = conn.prepareStatement(
                    "INSERT into card (ownerId, number, pin) values (?, ?, ?)");
            statement.setInt(1, account.getId());
            statement.setString(2, account.getCardNumber());
            statement.setString(3, account.getPinCode());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Locks cards according to their natural order to avoid possible deadlock.
     *
     * @param connection with autocommit mode disabled
     * @param cards      array of cards to lock
     * @throws SQLException
     */
    private void orderedLockByCard(Connection connection, String[] cards) throws SQLException {
        if (connection.getAutoCommit()) {
            throw new IllegalStateException("Connection is in autocommit mode.");
        }

        Arrays.sort(cards);
        final PreparedStatement lockStatement = connection.prepareStatement(LOCK_BY_CARD_SQL);
        for (int i = 0; i < cards.length; i++) {
            final String card = cards[0];
            lockStatement.setString(1, card);
            ResultSet lockRs = lockStatement.executeQuery();
            if (!lockRs.next()) {
                throw new IllegalStateException("Lock failed. Card " + card + " doesn't exists.");
            }
        }
    }

    public void transfer(Account sender, Account receiver, int amount, long waitBeforeStartMs, boolean fail) {
        Connection connection = null;

        try {
            if (waitBeforeStartMs > 0) {
                Thread.sleep(waitBeforeStartMs);
            }

            connection = this.connect();
            connection.setAutoCommit(false);

            // to avoid deadlock
            orderedLockByCard(connection, new String[]{sender.getCardNumber(), receiver.getCardNumber()});

            if (waitBeforeStartMs == 0) {
                // for testing purposes
                Thread.sleep(100);
            }

            final PreparedStatement debitStatement = connection.prepareStatement(
                    "UPDATE card SET balance = balance - ? WHERE number = ? and balance >= ?"
            );
            debitStatement.setInt(1, amount);
            debitStatement.setString(2, sender.getCardNumber());
            debitStatement.setInt(3, amount);

            if (debitStatement.executeUpdate() != 1) {
                throw new IllegalStateException("Not enough money, card " + sender.getCardNumber());
            }

            if (fail) {
                // let's assume here is thrown some kind of exception
                throw new IllegalStateException();
            }

            final PreparedStatement creditStatement = connection.prepareStatement(
                    "UPDATE card SET balance = balance + ? WHERE number = ?"
            );
            creditStatement.setInt(1, amount);
            creditStatement.setString(2, receiver.getCardNumber());

            creditStatement.executeUpdate();

            connection.commit();
            connection.setAutoCommit(true);

        } catch (Exception e) {
            e.printStackTrace();
            try {
                connection.rollback();
            } catch (SQLException e1) {
            }
        } finally {

            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e1) {
                }
            }
        }
    }


    @Override
    public void delete(int accountId) {
        try (Connection conn = this.connect();
             PreparedStatement statement = conn.prepareStatement("delete from card where ownerId = ?")) {

            statement.setInt(1, accountId);
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(int accountId, int amount) {

        try (Connection conn = this.connect();
             PreparedStatement statement = conn.prepareStatement(
                     "update card set balance = balance + ? where ownerId = ?")) {

            statement.setInt(1, amount);
            statement.setInt(2, accountId);
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void dropTable() {
        try (Connection conn = this.connect();
             PreparedStatement statement = conn.prepareStatement("DROP TABLE card")) {
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

