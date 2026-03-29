package com.rs.payments.wallet.controller;

import com.rs.payments.wallet.dto.CreateWalletRequest;
import com.rs.payments.wallet.dto.DepositRequest;
import com.rs.payments.wallet.dto.WithdrawRequest;
import com.rs.payments.wallet.model.Wallet;
import com.rs.payments.wallet.service.WalletService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private WalletController walletController;

    @Test
    @DisplayName("Should create wallet")
    void shouldCreateWallet() {
        UUID userId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserId(userId);

        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setBalance(BigDecimal.ZERO);

        when(walletService.createWalletForUser(userId)).thenReturn(wallet);

        ResponseEntity<Wallet> response = walletController.createWallet(request);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(wallet, response.getBody());
        verify(walletService, times(1)).createWalletForUser(userId);
    }

    @Test
    @DisplayName("Should deposit into wallet")
    void shouldDepositIntoWallet() {
        UUID walletId = UUID.randomUUID();
        DepositRequest request = new DepositRequest(BigDecimal.valueOf(100));

        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setBalance(BigDecimal.valueOf(100));

        when(walletService.deposit(walletId, BigDecimal.valueOf(100))).thenReturn(wallet);

        ResponseEntity<Wallet> response = walletController.deposit(walletId, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(wallet, response.getBody());
        verify(walletService, times(1)).deposit(walletId, BigDecimal.valueOf(100));
    }

    @Test
    @DisplayName("Should withdraw from wallet")
    void shouldWithdrawFromWallet() {
        UUID walletId = UUID.randomUUID();
        WithdrawRequest request = new WithdrawRequest(BigDecimal.valueOf(50));

        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setBalance(BigDecimal.valueOf(50));

        when(walletService.withdraw(walletId, BigDecimal.valueOf(50))).thenReturn(wallet);

        ResponseEntity<Wallet> response = walletController.withdraw(walletId, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(wallet, response.getBody());
        verify(walletService, times(1)).withdraw(walletId, BigDecimal.valueOf(50));
    }

    @Test
    @DisplayName("Should get wallet balance")
    void shouldGetWalletBalance() {
        UUID walletId = UUID.randomUUID();

        when(walletService.getBalance(walletId)).thenReturn(BigDecimal.valueOf(100));

        ResponseEntity<BigDecimal> response = walletController.getBalance(walletId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(BigDecimal.valueOf(100), response.getBody());
        verify(walletService, times(1)).getBalance(walletId);
    }
}