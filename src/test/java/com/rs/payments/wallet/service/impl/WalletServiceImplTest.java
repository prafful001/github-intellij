package com.rs.payments.wallet.service.impl;

import com.rs.payments.wallet.exception.ResourceNotFoundException;
import com.rs.payments.wallet.model.Transaction;
import com.rs.payments.wallet.model.User;
import com.rs.payments.wallet.model.Wallet;
import com.rs.payments.wallet.repository.TransactionRepository;
import com.rs.payments.wallet.repository.UserRepository;
import com.rs.payments.wallet.repository.WalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    // ── createWalletForUser ──────────────────────────────────────────

    @Test
    @DisplayName("Should create wallet for existing user")
    void shouldCreateWalletForExistingUser() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        Wallet savedWallet = new Wallet();
        savedWallet.setBalance(BigDecimal.ZERO);
        savedWallet.setUser(user);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(walletRepository.existsByUser(user)).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenReturn(savedWallet);

        Wallet result = walletService.createWalletForUser(userId);

        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getBalance());
        verify(userRepository, times(1)).findById(userId);
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> walletService.createWalletForUser(userId));
        verify(userRepository, never()).save(any());
    }

    // ── deposit ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Should deposit amount into wallet")
    void shouldDepositAmountIntoWallet() {
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setBalance(BigDecimal.valueOf(100));

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(new Transaction());

        Wallet result = walletService.deposit(walletId, BigDecimal.valueOf(50));

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(150), result.getBalance());
        verify(walletRepository, times(1)).save(wallet);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw exception when wallet not found on deposit")
    void shouldThrowExceptionWhenWalletNotFoundOnDeposit() {
        UUID walletId = UUID.randomUUID();
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> walletService.deposit(walletId, BigDecimal.valueOf(50)));
    }

    // ── withdraw ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Should withdraw amount from wallet")
    void shouldWithdrawAmountFromWallet() {
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setBalance(BigDecimal.valueOf(100));

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(new Transaction());

        Wallet result = walletService.withdraw(walletId, BigDecimal.valueOf(50));

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(50), result.getBalance());
        verify(walletRepository, times(1)).save(wallet);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw exception when insufficient funds")
    void shouldThrowExceptionWhenInsufficientFunds() {
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setBalance(BigDecimal.valueOf(30));

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        assertThrows(IllegalArgumentException.class,
                () -> walletService.withdraw(walletId, BigDecimal.valueOf(50)));
        verify(walletRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when wallet not found on withdraw")
    void shouldThrowExceptionWhenWalletNotFoundOnWithdraw() {
        UUID walletId = UUID.randomUUID();
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> walletService.withdraw(walletId, BigDecimal.valueOf(50)));
    }

    // ── transfer ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Should transfer amount between wallets")
    void shouldTransferAmountBetweenWallets() {
        UUID fromWalletId = UUID.randomUUID();
        UUID toWalletId = UUID.randomUUID();

        Wallet fromWallet = new Wallet();
        fromWallet.setId(fromWalletId);
        fromWallet.setBalance(BigDecimal.valueOf(100));

        Wallet toWallet = new Wallet();
        toWallet.setId(toWalletId);
        toWallet.setBalance(BigDecimal.valueOf(50));

        when(walletRepository.findById(fromWalletId)).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findById(toWalletId)).thenReturn(Optional.of(toWallet));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(new Transaction());

        walletService.transfer(fromWalletId, toWalletId, BigDecimal.valueOf(50));

        assertEquals(BigDecimal.valueOf(50), fromWallet.getBalance());
        assertEquals(BigDecimal.valueOf(100), toWallet.getBalance());
        verify(walletRepository, times(2)).save(any(Wallet.class));
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw exception when insufficient funds on transfer")
    void shouldThrowExceptionWhenInsufficientFundsOnTransfer() {
        UUID fromWalletId = UUID.randomUUID();
        UUID toWalletId = UUID.randomUUID();

        Wallet fromWallet = new Wallet();
        fromWallet.setId(fromWalletId);
        fromWallet.setBalance(BigDecimal.valueOf(30));

        Wallet toWallet = new Wallet();
        toWallet.setId(toWalletId);
        toWallet.setBalance(BigDecimal.valueOf(50));

        when(walletRepository.findById(fromWalletId)).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findById(toWalletId)).thenReturn(Optional.of(toWallet));

        assertThrows(IllegalArgumentException.class,
                () -> walletService.transfer(fromWalletId, toWalletId, BigDecimal.valueOf(50)));
        verify(walletRepository, never()).save(any());
    }

    // ── getBalance ───────────────────────────────────────────────────

    @Test
    @DisplayName("Should return wallet balance")
    void shouldReturnWalletBalance() {
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setBalance(BigDecimal.valueOf(100));

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        BigDecimal balance = walletService.getBalance(walletId);

        assertEquals(BigDecimal.valueOf(100), balance);
    }

    @Test
    @DisplayName("Should throw exception when wallet not found on getBalance")
    void shouldThrowExceptionWhenWalletNotFoundOnGetBalance() {
        UUID walletId = UUID.randomUUID();
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> walletService.getBalance(walletId));
    }
}