package com.bankapp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.List;

public class BankAcidTests {
    private Bank bank;
    private static final int THREAD_COUNT = 10;
    private static final int OPERATIONS_PER_THREAD = 100;

    @BeforeEach
    void setUp() {
        bank = new Bank();
    }

    @Test
    @DisplayName("Atomicity: Transaction either fully completes or fully fails")
    void testAtomicity() throws InterruptedException {
        long accountNum = 1L;
        bank.createAccount(accountNum);
        bank.deposit(accountNum, 1000);

        try {
            // This should fail atomically
            bank.withdraw(accountNum, 2000);
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            // Balance should remain unchanged
            assertEquals(1000, bank.getBalance(accountNum));
        }
    }

    @Test
    @DisplayName("Consistency: Account balance never goes negative")
    void testConsistency() throws InterruptedException {
        long accountNum = 1L;
        bank.createAccount(accountNum);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        // Create many concurrent withdraw attempts
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        try {
                            bank.withdraw(accountNum, 100);
                        } catch (IllegalStateException e) {
                            // Expected when insufficient funds
                        }
                        assertTrue(bank.getBalance(accountNum) >= 0,
                                "Balance should never be negative");
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        assertTrue(bank.getBalance(accountNum) >= 0);
    }

    @Test
    @DisplayName("Isolation: Concurrent transactions don't interfere")
    void testIsolation() throws InterruptedException {
        long accountNum = 1L;
        bank.createAccount(accountNum);
        bank.deposit(accountNum, 1000);

        int threadCount = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Half threads deposit, half withdraw
        for (int i = 0; i < threadCount; i++) {
            final int amount = 10;
            if (i % 2 == 0) {
                executor.submit(() -> {
                    try {
                        bank.deposit(accountNum, amount);
                    } finally {
                        latch.countDown();
                    }
                });
            } else {
                executor.submit(() -> {
                    try {
                        try {
                            bank.withdraw(accountNum, amount);
                        } catch (IllegalStateException e) {
                            // Expected if insufficient funds
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Final balance should be initial (1000) + (50 deposits - 50 withdraws) * 10
        assertEquals(1000, bank.getBalance(accountNum));
    }

    @Test
    @DisplayName("Durability: Transactions persist after completion")
    void testDurability() throws InterruptedException {
        long accountNum = 1L;
        bank.createAccount(accountNum);

        // Perform series of transactions
        bank.deposit(accountNum, 500);
        assertEquals(500, bank.getBalance(accountNum));

        bank.withdraw(accountNum, 200);
        assertEquals(300, bank.getBalance(accountNum));

        // Simulate system stress with concurrent operations
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try {
                    bank.deposit(accountNum, 100);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Balance should be 300 + (10 * 100) = 1300
        assertEquals(1300, bank.getBalance(accountNum));

        // Verify balance persists after all operations
        Thread.sleep(1000); // Wait a bit
        assertEquals(1300, bank.getBalance(accountNum));
    }

    @Test
    @DisplayName("Complex ACID Test: Multiple accounts, concurrent transfers")
    void testComplexAcidScenario() throws InterruptedException {
        int numAccounts = 5;
        int initialBalance = 1000;
        List<Long> accounts = new ArrayList<>();

        // Create accounts with initial balance
        for (int i = 0; i < numAccounts; i++) {
            long accountNum = i + 1;
            accounts.add(accountNum);
            bank.createAccount(accountNum);
            bank.deposit(accountNum, initialBalance);
        }

        // Total initial money in bank
        long initialTotal = numAccounts * initialBalance;
        assertEquals(initialTotal, accounts.stream()
                .mapToLong(bank::getBalance)
                .sum());

        // Perform concurrent random transfers between accounts
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        // Random source and destination accounts
                        int sourceIndex = ThreadLocalRandom.current().nextInt(numAccounts);
                        int destIndex = (sourceIndex + 1) % numAccounts;
                        long amount = ThreadLocalRandom.current().nextLong(1, 100);

                        try {
                            // Attempt transfer
                            long sourceAcc = accounts.get(sourceIndex);
                            long destAcc = accounts.get(destIndex);
                            bank.withdraw(sourceAcc, amount);
                            bank.deposit(destAcc, amount);
                        } catch (IllegalStateException e) {
                            // Expected if insufficient funds
                        }

                        // Verify consistency after each operation
                        long currentTotal = accounts.stream()
                                .mapToLong(bank::getBalance)
                                .sum();
                        assertEquals(initialTotal, currentTotal,
                                "Total money in bank should remain constant");
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify final state
        long finalTotal = accounts.stream()
                .mapToLong(bank::getBalance)
                .sum();
        assertEquals(initialTotal, finalTotal);

        // Verify no negative balances
        for (Long account : accounts) {
            assertTrue(bank.getBalance(account) >= 0);
        }
    }
}