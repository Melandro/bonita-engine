package org.bonitasoft.engine.transaction;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.Callable;

import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.bonitasoft.engine.events.EventService;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.junit.Test;


public class JTATransactionServiceImplTest {

    @Test
    public void beginTransaction() throws Exception {
        TechnicalLoggerService logger = mock(TechnicalLoggerService.class);
        TransactionManager txManager = mock(TransactionManager.class);
        EventService eventService = mock(EventService.class);

        when(txManager.getStatus()).thenReturn(Status.STATUS_NO_TRANSACTION);

        JTATransactionServiceImpl txService = new JTATransactionServiceImpl(logger, txManager, eventService);

        txService.begin();
        verify(txManager, times(1)).begin();
    }

    @Test(expected=STransactionCreationException.class)
    public void doNotSupportNestedTransaction() throws Exception {
        TechnicalLoggerService logger = mock(TechnicalLoggerService.class);
        TransactionManager txManager = mock(TransactionManager.class);
        EventService eventService = mock(EventService.class);

        when(txManager.getStatus()).thenReturn(Status.STATUS_ACTIVE);

        JTATransactionServiceImpl txService = new JTATransactionServiceImpl(logger, txManager, eventService);

        txService.begin();
    }

    @Test
    public void beginTransactionEventFailed() throws Exception {
        // We want to ensure that when an exception was thrown after the transaction's begin then
        // we close the open transaction to be in a consistent state.

        TechnicalLoggerService logger = mock(TechnicalLoggerService.class);
        TransactionManager txManager = mock(TransactionManager.class);
        EventService eventService = mock(EventService.class);

        when(txManager.getStatus()).thenReturn(Status.STATUS_NO_TRANSACTION);
        when(eventService.hasHandlers(TransactionService.TRANSACTION_ACTIVE_EVT, null)).thenThrow(new RuntimeException("Mocked"));

        JTATransactionServiceImpl txService = spy(new JTATransactionServiceImpl(logger, txManager, eventService));

        try {
            txService.begin();
            fail("The begin should have thrown an exception.");
        } catch (STransactionCreationException e) {
            verify(txManager, times(1)).rollback();
        }
    }

    @Test
    public void setRollbackOnly() throws Exception {
        TechnicalLoggerService logger = mock(TechnicalLoggerService.class);
        TransactionManager txManager = mock(TransactionManager.class);
        EventService eventService = mock(EventService.class);

        JTATransactionServiceImpl txService = spy(new JTATransactionServiceImpl(logger, txManager, eventService));

        txService.setRollbackOnly();
        verify(txManager).setRollbackOnly();
    }

    /**
     * The method call has to be executed between a transaction.
     * @throws Exception
     */
    @Test
    public void testExecuteInTransactionWithCommit() throws Exception {
        TechnicalLoggerService logger = mock(TechnicalLoggerService.class);
        TransactionManager txManager = mock(TransactionManager.class);
        EventService eventService = mock(EventService.class);

        when(txManager.getStatus()).thenReturn(Status.STATUS_NO_TRANSACTION);

        JTATransactionServiceImpl txService = spy(new JTATransactionServiceImpl(logger, txManager, eventService));
        Callable<Void> callable = mock(Callable.class);

        txService.executeInTransaction(callable);
        
        verify(txManager).begin();
        verify(callable).call();
        verify(txManager).commit();
    }

    /**
     * The method call has to be executed between a transaction.
     * @throws Exception
     */
    @Test
    public void testExecuteInTransactionWithRollback() throws Exception {
        TechnicalLoggerService logger = mock(TechnicalLoggerService.class);
        TransactionManager txManager = mock(TransactionManager.class);
        EventService eventService = mock(EventService.class);

        // First to allow to start the transaction, then to force to call rollback
        when(txManager.getStatus()).thenReturn(Status.STATUS_NO_TRANSACTION).thenReturn(Status.STATUS_MARKED_ROLLBACK);

        JTATransactionServiceImpl txService = spy(new JTATransactionServiceImpl(logger, txManager, eventService));
        
        Callable<Void> callable = mock(Callable.class);
        when(callable.call()).thenThrow(new Exception("Mocked exception"));

        try {
            txService.executeInTransaction(callable);
            fail("An exception should have been thrown.");
        } catch (Exception e) {
        }
        verify(txManager).begin();
        verify(callable).call();
        verify(txManager).setRollbackOnly();
        verify(txManager).rollback();
    }

}
