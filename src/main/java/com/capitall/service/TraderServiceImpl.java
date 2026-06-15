package com.capitall.service;

import com.capitall.dto.CreateTraderRequest;
import com.capitall.dto.TraderDto;
import com.capitall.exception.EmailAlreadyExistsException;
import com.capitall.exception.ResourceNotFoundException;
import com.capitall.model.Trader;
import com.capitall.repository.TraderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TraderServiceImpl implements TraderService {

    private final TraderRepository traderRepository;

    public TraderServiceImpl(TraderRepository traderRepository) {
        this.traderRepository = traderRepository;
    }

    @Override
    @Transactional
    public TraderDto createTrader(CreateTraderRequest request) {
        if (traderRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email " + request.email() + " is already registered");
        }

        Trader trader = Trader.builder()
                .name(request.name())
                .email(request.email())
                .build();

        Trader savedTrader = traderRepository.save(trader);
        return mapToDto(savedTrader);
    }

    @Override
    public TraderDto getTraderById(Long id) {
        return traderRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Trader not found with id: " + id));
    }

    @Override
    public List<TraderDto> getAllTraders() {
        return traderRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteTrader(Long id) {
        if (!traderRepository.existsById(id)) {
            throw new ResourceNotFoundException("Trader not found with id: " + id);
        }
        traderRepository.deleteById(id);
    }

    private TraderDto mapToDto(Trader trader) {
        return new TraderDto(
                trader.getId(),
                trader.getName(),
                trader.getEmail(),
                trader.getCreatedAt()
        );
    }
}
