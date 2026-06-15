package com.capitall.service;

import com.capitall.dto.CreateTraderRequest;
import com.capitall.dto.TraderDto;
import com.capitall.exception.EmailAlreadyExistsException;
import com.capitall.exception.ResourceNotFoundException;
import com.capitall.model.Trader;
import com.capitall.repository.TraderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TraderServiceImplTest {

    @Mock
    private TraderRepository traderRepository;

    private TraderService traderService;

    @BeforeEach
    void setUp() {
        traderService = new TraderServiceImpl(traderRepository);
    }

    @Test
    void createTrader_ShouldSaveAndReturnTrader_WhenEmailIsUnique() {
        CreateTraderRequest request = new CreateTraderRequest("John Doe", "john.doe@example.com");
        Trader savedTrader = Trader.builder()
                .id(1L)
                .name("John Doe")
                .email("john.doe@example.com")
                .build();

        when(traderRepository.existsByEmail(request.email())).thenReturn(false);
        when(traderRepository.save(any(Trader.class))).thenReturn(savedTrader);

        TraderDto result = traderService.createTrader(request);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("John Doe");
        assertThat(result.email()).isEqualTo("john.doe@example.com");
        verify(traderRepository).existsByEmail(request.email());
        verify(traderRepository).save(any(Trader.class));
    }

    @Test
    void createTrader_ShouldThrowException_WhenEmailAlreadyExists() {
        CreateTraderRequest request = new CreateTraderRequest("John Doe", "john.doe@example.com");
        when(traderRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> traderService.createTrader(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("already registered");

        verify(traderRepository).existsByEmail(request.email());
        verify(traderRepository, never()).save(any(Trader.class));
    }

    @Test
    void getTraderById_ShouldReturnTrader_WhenTraderExists() {
        Long traderId = 1L;
        Trader trader = Trader.builder()
                .id(traderId)
                .name("John Doe")
                .email("john.doe@example.com")
                .build();

        when(traderRepository.findById(traderId)).thenReturn(Optional.of(trader));

        TraderDto result = traderService.getTraderById(traderId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(traderId);
        verify(traderRepository).findById(traderId);
    }

    @Test
    void getTraderById_ShouldThrowException_WhenTraderDoesNotExist() {
        Long traderId = 1L;
        when(traderRepository.findById(traderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> traderService.getTraderById(traderId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Trader not found");

        verify(traderRepository).findById(traderId);
    }
}
