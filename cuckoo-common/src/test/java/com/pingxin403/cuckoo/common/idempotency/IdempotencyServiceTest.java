package com.pingxin403.cuckoo.common.idempotency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * IdempotencyService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private IdempotencyService idempotencyService;

    @Test
    void isDuplicate_shouldReturnTrue_whenEventAlreadyProcessed() {
        when(processedEventRepository.existsByEventId("event-123")).thenReturn(true);

        assertTrue(idempotencyService.isDuplicate("event-123"));
    }

    @Test
    void isDuplicate_shouldReturnFalse_whenEventNotProcessed() {
        when(processedEventRepository.existsByEventId("event-456")).thenReturn(false);

        assertFalse(idempotencyService.isDuplicate("event-456"));
    }

    @Test
    void markProcessed_shouldSaveEvent_whenNotDuplicate() {
        when(processedEventRepository.existsByEventId("event-789")).thenReturn(false);

        idempotencyService.markProcessed("event-789");

        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void markProcessed_shouldNotSave_whenAlreadyProcessed() {
        when(processedEventRepository.existsByEventId("event-dup")).thenReturn(true);

        idempotencyService.markProcessed("event-dup");

        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
    }
}
