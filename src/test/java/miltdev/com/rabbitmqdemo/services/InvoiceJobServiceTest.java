package miltdev.com.rabbitmqdemo.services;

import miltdev.com.rabbitmqdemo.entities.Invoice;
import miltdev.com.rabbitmqdemo.enums.InvoiceStatus;
import miltdev.com.rabbitmqdemo.enums.RetryType;
import miltdev.com.rabbitmqdemo.exceptions.RetryFailedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceJobServiceTest {

    private static final String LOCK_KEY = "invoice-job-initialization";
    private static final Duration LOCK_TTL = Duration.ofMinutes(30);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final Duration RETRY_INTERVAL = Duration.ofMinutes(5);

    @Mock
    private RabbitMQService rabbitMQService;
    @Mock
    private InvoiceService invoiceService;
    @Mock
    private RedisLockService redisLockService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private RetryService retryService;
    @Captor
    private ArgumentCaptor<List<Long>> invoiceIdsCaptor;
    @Captor
    private ArgumentCaptor<String> lockValueCaptor;
    @InjectMocks
    private InvoiceJobService invoiceJobService;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(
                rabbitMQService,
                invoiceService,
                redisLockService,
                objectMapper,
                retryService);
    }

    @Test
    void given_LockCannotBeAcquired_whenPrepareInvoiceJobs_then_DoesNotProcessInvoices() {
        // Arrange
        when(redisLockService.acquireLock(eq(LOCK_KEY), anyString(), eq(LOCK_TTL))).thenReturn(false);

        // Act
        invoiceJobService.prepareInvoiceJobs();

        // Assert
        verify(redisLockService).acquireLock(eq(LOCK_KEY), anyString(), eq(LOCK_TTL));
        verify(redisLockService, never()).releaseLock(anyString(), anyString());
        verifyNoInteractions(invoiceService, rabbitMQService, objectMapper);
    }

    @Test
    void given_PendingInvoicesAreEmpty_whenPrepareInvoiceJobs_then_ReleasesLockWithoutSendingMessage() {
        // Arrange
        mockSuccessfulLockAcquisition();
        when(invoiceService.getAllByPendingStatus()).thenReturn(Collections.emptyList());

        // Act
        invoiceJobService.prepareInvoiceJobs();

        // Assert
        verify(invoiceService).getAllByPendingStatus();
        verify(invoiceService, never()).updateInvoiceStatus(anyList(), any());
        verifyNoInteractions(rabbitMQService, objectMapper);
        verifyLockReleased();
    }

    @Test
    void given_PendingInvoicesAreNull_whenPrepareInvoiceJobs_then_ReleasesLockWithoutSendingMessage() {
        // Arrange
        mockSuccessfulLockAcquisition();
        when(invoiceService.getAllByPendingStatus()).thenReturn(null);

        // Act
        invoiceJobService.prepareInvoiceJobs();

        // Assert
        verify(invoiceService).getAllByPendingStatus();
        verify(invoiceService, never()).updateInvoiceStatus(anyList(), any());
        verifyNoInteractions(rabbitMQService, objectMapper);
        verifyLockReleased();
    }

    @Test
    void given_FetchingPendingInvoicesFails_whenPrepareInvoiceJobs_then_ReleasesLockAndPropagatesException() {
        // Arrange
        RuntimeException failure = new RuntimeException("database failure");
        mockSuccessfulLockAcquisition();
        when(invoiceService.getAllByPendingStatus()).thenThrow(failure);

        // Act + Assert
        assertThatThrownBy(() -> invoiceJobService.prepareInvoiceJobs())
                .isSameAs(failure);
        verifyNoInteractions(rabbitMQService, objectMapper);
        verifyLockReleased();
    }

    @Test
    void given_InvoiceCountIsSmallerThanBatchSize_whenPrepareInvoiceJobs_then_SendsOneBatchAndUpdatesStatus() {
        // Arrange
        List<Invoice> invoices = createInvoices(1, 10);
        List<Long> expectedInvoiceIds = createInvoiceIds(1, 10);
        mockSuccessfulLockAcquisition();
        mockObjectWriteToString();
        when(invoiceService.getAllByPendingStatus()).thenReturn(invoices);
        when(rabbitMQService.sendMessage(anyString())).thenReturn(true);

        // Act
        invoiceJobService.prepareInvoiceJobs();

        // Assert
        verify(rabbitMQService).sendMessage(expectedInvoiceIds.toString());
        verify(invoiceService).updateInvoiceStatus(invoiceIdsCaptor.capture(), eq(InvoiceStatus.PROCESSING));
        assertThat(invoiceIdsCaptor.getValue()).containsExactlyElementsOf(expectedInvoiceIds);
        verifyLockReleased();
    }

    @Test
    void given_InvoiceCountEqualsBatchSizeWhenPrepareInvoiceJobs_then_SendsOneFullBatch() {
        // Arrange
        List<Invoice> invoices = createInvoices(1, 100);
        List<Long> expectedInvoiceIds = createInvoiceIds(1, 100);
        mockSuccessfulLockAcquisition();
        mockObjectWriteToString();
        when(invoiceService.getAllByPendingStatus()).thenReturn(invoices);
        when(rabbitMQService.sendMessage(anyString())).thenReturn(true);

        // Act
        invoiceJobService.prepareInvoiceJobs();

        // Assert
        verify(rabbitMQService).sendMessage(expectedInvoiceIds.toString());
        verify(invoiceService).updateInvoiceStatus(invoiceIdsCaptor.capture(), eq(InvoiceStatus.PROCESSING));
        assertThat(invoiceIdsCaptor.getValue())
                .hasSize(100)
                .containsExactlyElementsOf(expectedInvoiceIds);
        verifyLockReleased();
    }

    @Test
    void given_InvoiceCountIsGreaterThanBatchSize_whenPrepareInvoiceJobs_then_SendsMultipleBatchesAndUpdatesEachBatch() {
        // Arrange
        List<Invoice> invoices = createInvoices(1, 250);
        mockSuccessfulLockAcquisition();
        mockObjectWriteToString();
        when(invoiceService.getAllByPendingStatus()).thenReturn(invoices);
        when(rabbitMQService.sendMessage(anyString())).thenReturn(true);

        // Act
        invoiceJobService.prepareInvoiceJobs();

        // Assert
        verify(rabbitMQService, times(3)).sendMessage(anyString());
        verify(invoiceService, times(3)).updateInvoiceStatus(invoiceIdsCaptor.capture(), eq(InvoiceStatus.PROCESSING));
        assertThat(invoiceIdsCaptor.getAllValues())
                .hasSize(3)
                .allSatisfy(batch -> assertThat(batch).hasSizeLessThanOrEqualTo(100));
        assertThat(invoiceIdsCaptor.getAllValues().get(0)).containsExactlyElementsOf(createInvoiceIds(1, 100));
        assertThat(invoiceIdsCaptor.getAllValues().get(1)).containsExactlyElementsOf(createInvoiceIds(101, 100));
        assertThat(invoiceIdsCaptor.getAllValues().get(2)).containsExactlyElementsOf(createInvoiceIds(201, 50));
        verifyLockReleased();
    }

    @Test
    void given_InitialSendFailsAndRetrySucceeds_whenPrepareInvoiceJobs_then_UpdatesStatusAfterRetry() {
        // Arrange
        List<Long> expectedInvoiceIds = createInvoiceIds(42, 1);
        mockSuccessfulLockAcquisition();
        mockObjectWriteToString();
        when(invoiceService.getAllByPendingStatus()).thenReturn(createInvoices(42, 1));
        when(rabbitMQService.sendMessage(anyString())).thenReturn(false, true);
        mockRetryServiceRunsAction();

        // Act
        invoiceJobService.prepareInvoiceJobs();

        // Assert
        verify(rabbitMQService, times(2)).sendMessage(expectedInvoiceIds.toString());
        verify(retryService).retry(
                eq(RetryType.MESSAGE_CONSUMER),
                eq(MAX_RETRY_ATTEMPTS),
                eq(RETRY_INTERVAL),
                any());
        verify(invoiceService).updateInvoiceStatus(invoiceIdsCaptor.capture(), eq(InvoiceStatus.PROCESSING));
        assertThat(invoiceIdsCaptor.getValue()).containsExactlyElementsOf(expectedInvoiceIds);
        verifyLockReleased();
    }

    @Test
    void given_InitialSendAndAllRetriesFail_whenPrepareInvoiceJobs_then_DoesNotUpdateStatusAndPropagatesRetryFailure() {
        // Arrange
        RetryFailedException failure = new RetryFailedException("message sending failed after retries");
        List<Invoice> invoices = createInvoices(7, 1);
        List<Long> expectedInvoiceIds = createInvoiceIds(7, 1);
        mockSuccessfulLockAcquisition();
        mockObjectWriteToString();
        when(invoiceService.getAllByPendingStatus()).thenReturn(invoices);
        when(rabbitMQService.sendMessage(anyString())).thenReturn(false);
        when(retryService.retry(eq(RetryType.MESSAGE_CONSUMER), eq(MAX_RETRY_ATTEMPTS), eq(RETRY_INTERVAL), any()))
                .thenThrow(failure);

        // Act + Assert
        assertThatThrownBy(() -> invoiceJobService.prepareInvoiceJobs())
                .isSameAs(failure);
        verify(rabbitMQService).sendMessage(expectedInvoiceIds.toString());
        verify(retryService).retry(
                eq(RetryType.MESSAGE_CONSUMER),
                eq(MAX_RETRY_ATTEMPTS),
                eq(RETRY_INTERVAL),
                any());
        verify(invoiceService, never()).updateInvoiceStatus(anyList(), any());
        verifyLockReleased();
    }

    @Test
    void given_SerializationFails_whenPrepareInvoiceJobs_then_DoesNotSendOrUpdateStatusAndReleasesLock() {
        // Arrange
        mockSuccessfulLockAcquisition();
        when(invoiceService.getAllByPendingStatus()).thenReturn(createInvoices(1, 1));
        when(objectMapper.writeValueAsString(any())).thenThrow(new TestJacksonException("serialization failure"));

        // Act
        invoiceJobService.prepareInvoiceJobs();

        // Assert
        verifyNoInteractions(rabbitMQService);
        verify(invoiceService, never()).updateInvoiceStatus(anyList(), any());
        verifyLockReleased();
    }

    @Test
    void given_StatusUpdateFails_whenPrepareInvoiceJobs_then_ReleasesLockAndPropagatesException() {
        // Arrange
        RuntimeException failure = new RuntimeException("status update failure");
        mockSuccessfulLockAcquisition();
        mockObjectWriteToString();
        when(invoiceService.getAllByPendingStatus()).thenReturn(createInvoices(1, 1));
        when(rabbitMQService.sendMessage(anyString())).thenReturn(true);
        doThrow(failure)
                .when(invoiceService).updateInvoiceStatus(anyList(), eq(InvoiceStatus.PROCESSING));

        // Act + Assert
        assertThatThrownBy(() -> invoiceJobService.prepareInvoiceJobs())
                .isSameAs(failure);
        verifyLockReleased();
    }

    private void mockSuccessfulLockAcquisition() {
        when(redisLockService.acquireLock(eq(LOCK_KEY), anyString(), eq(LOCK_TTL))).thenReturn(true);
    }

    private void mockObjectWriteToString() {
        when(objectMapper.writeValueAsString(any())).thenAnswer(invocation -> invocation.getArgument(0).toString());
    }

    private void mockRetryServiceRunsAction() {
        when(retryService.retry(eq(RetryType.MESSAGE_CONSUMER), eq(MAX_RETRY_ATTEMPTS), eq(RETRY_INTERVAL), any()))
                .thenAnswer(invocation -> {
                    Callable<Boolean> action = invocation.getArgument(3);
                    return action.call();
                });
    }

    private void verifyLockReleased() {
        verify(redisLockService).acquireLock(eq(LOCK_KEY), lockValueCaptor.capture(), eq(LOCK_TTL));
        assertThat(lockValueCaptor.getValue()).isNotBlank();
        verify(redisLockService).releaseLock(LOCK_KEY, lockValueCaptor.getValue());
    }

    private List<Invoice> createInvoices(long firstId, int count) {
        return LongStream.range(firstId, firstId + count)
                .mapToObj(this::createInvoice)
                .toList();
    }

    private List<Long> createInvoiceIds(long firstId, int count) {
        return LongStream.range(firstId, firstId + count)
                .boxed()
                .toList();
    }

    private Invoice createInvoice(long id) {
        Invoice invoice = new Invoice();
        invoice.setId(id);
        return invoice;
    }

    private static class TestJacksonException extends JacksonException {
        TestJacksonException(String message) {
            super(message);
        }
    }
}
