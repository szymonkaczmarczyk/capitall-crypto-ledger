package com.capitall.service;

import com.capitall.dto.CreateTraderRequest;
import com.capitall.dto.TraderDto;
import java.util.List;

public interface TraderService {
    TraderDto createTrader(CreateTraderRequest request);
    TraderDto getTraderById(Long id);
    List<TraderDto> getAllTraders();
    void deleteTrader(Long id);
}
