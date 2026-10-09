package com.predict.service;

import com.predict.RewardExchangeRequest;
import com.predict.RewardTransaction;
import com.predict.RewardWallet;
import com.predict.User;
import com.predict.enums.ExchangeStatus;
import com.predict.enums.RewardTransactionType;
import com.predict.repository.RewardExchangeRequestRepository;
import com.predict.repository.RewardTransactionRepository;
import com.predict.repository.RewardWalletRepository;
import com.predict.reward.RewardCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewardServiceTest {

    @Mock
    private RewardWalletRepository walletRepository;
    @Mock
    private RewardTransactionRepository transactionRepository;
    @Mock
    private RewardExchangeRequestRepository exchangeRepository;

    private RewardService rewardService;
    private User user;
    private User admin;
    private RewardWallet wallet;

    @BeforeEach
    void setUp() throws Exception {
        rewardService = new RewardService(walletRepository, transactionRepository, exchangeRepository);
        user = new User("유저", "direct", null);
        setField(user, "id", 7L);
        admin = new User("운영자", "direct", null);
        setField(admin, "id", 1L);
        wallet = new RewardWallet(7L);

        lenient().when(walletRepository.findById(7L)).thenReturn(Optional.of(wallet));
        lenient().when(transactionRepository.existsByIdempotencyKey(anyString())).thenReturn(false);
        lenient().when(transactionRepository.save(any(RewardTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(exchangeRepository.save(any(RewardExchangeRequest.class))).thenAnswer(inv -> {
            RewardExchangeRequest request = inv.getArgument(0);
            setField(request, "id", 15L);
            return request;
        });
    }

    private RewardExchangeRequest requested() throws Exception {
        RewardExchangeRequest request = new RewardExchangeRequest(user, RewardCatalog.require("CVS_3000"));
        setField(request, "id", 15L);
        when(exchangeRepository.findById(15L)).thenReturn(Optional.of(request));
        return request;
    }

    @Test
    void credit_isIdempotent() {
        when(transactionRepository.existsByIdempotencyKey("earn:submission:42")).thenReturn(true);

        RewardTransaction result = rewardService.credit(user, RewardTransactionType.EARN, 50, "earn:submission:42",
                null, null, "설문");

        assertThat(result).isNull();
        assertThat(wallet.getBalance()).isZero();
        verify(transactionRepository, never()).save(any(RewardTransaction.class));
    }

    @Test
    void credit_createsWalletOnFirstEarning() {
        User newcomer = new User("새 유저", "direct", null);
        try {
            setField(newcomer, "id", 9L);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        when(walletRepository.findById(9L)).thenReturn(Optional.empty());
        when(walletRepository.save(any(RewardWallet.class))).thenAnswer(inv -> inv.getArgument(0));

        RewardTransaction result = rewardService.credit(newcomer, RewardTransactionType.EARN, 10, "earn:submission:1",
                null, null, "출석 체크");

        assertThat(result.getBalanceAfter()).isEqualTo(10);
        verify(walletRepository).save(any(RewardWallet.class));
    }

    @Test
    void requestExchange_spendsPointsAndRecordsIt() {
        wallet.earn(4_000);

        RewardExchangeRequest request = rewardService.requestExchange(user, "CVS_3000");

        assertThat(request.getStatus()).isEqualTo(ExchangeStatus.REQUESTED);
        assertThat(request.getPoints()).isEqualTo(3_000);
        assertThat(wallet.getBalance()).isEqualTo(1_000);
        ArgumentCaptor<RewardTransaction> saved = ArgumentCaptor.forClass(RewardTransaction.class);
        verify(transactionRepository).save(saved.capture());
        assertThat(saved.getValue().getAmount()).isEqualTo(-3_000);
        assertThat(saved.getValue().getBalanceAfter()).isEqualTo(1_000);
        assertThat(saved.getValue().getIdempotencyKey()).isEqualTo("exchange:15");
    }

    @Test
    void requestExchange_withoutEnoughPoints_isRejected() {
        wallet.earn(1_250);

        assertThatThrownBy(() -> rewardService.requestExchange(user, "CVS_3000"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("포인트가 부족해요");
        assertThat(wallet.getBalance()).isEqualTo(1_250);
        verify(exchangeRepository, never()).save(any(RewardExchangeRequest.class));
    }

    @Test
    void requestExchange_unknownProduct_isBadRequest() {
        assertThatThrownBy(() -> rewardService.requestExchange(user, "NOPE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reject_refundsPoints() throws Exception {
        RewardExchangeRequest request = requested();

        rewardService.rejectExchange(admin, 15L, "본인인증 번호 불일치");

        assertThat(request.getStatus()).isEqualTo(ExchangeStatus.REJECTED);
        assertThat(request.getHandledBy()).isSameAs(admin);
        assertThat(wallet.getBalance()).isEqualTo(3_000);
        ArgumentCaptor<RewardTransaction> saved = ArgumentCaptor.forClass(RewardTransaction.class);
        verify(transactionRepository).save(saved.capture());
        assertThat(saved.getValue().getType()).isEqualTo(RewardTransactionType.REFUND);
        assertThat(saved.getValue().getIdempotencyKey()).isEqualTo("refund:15");
    }

    @Test
    void markSent_thenReject_isConflict() throws Exception {
        requested();
        rewardService.markSent(admin, 15L);

        assertThatThrownBy(() -> rewardService.rejectExchange(admin, 15L, "기타"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 처리된 교환 신청이에요");
        assertThat(wallet.getBalance()).isZero();
    }

    @Test
    void cancel_byOwner_refunds() throws Exception {
        RewardExchangeRequest request = requested();

        rewardService.cancelExchange(user, 15L);

        assertThat(request.getStatus()).isEqualTo(ExchangeStatus.CANCELED);
        assertThat(wallet.getBalance()).isEqualTo(3_000);
    }

    @Test
    void cancel_bySomeoneElse_isRejected() throws Exception {
        requested();

        assertThatThrownBy(() -> rewardService.cancelExchange(admin, 15L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("내 교환 신청만 취소할 수 있어요");
    }

    @Test
    void reject_withoutReason_isBadRequest() throws Exception {
        requested();

        assertThatThrownBy(() -> rewardService.rejectExchange(admin, 15L, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
