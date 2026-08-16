package uk.gov.ons.census.fieldworkadapter.schedule;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessageToSendPollerTest {

  @Mock private MessageToSendProcessor messageToSendProcessor;

  @InjectMocks private MessageToSendPoller underTest;

  @Test
  void shouldContinuePollingWhileWorkRemains() {
    when(messageToSendProcessor.isThereWorkToDo()).thenReturn(true, true, false);

    underTest.processQueuedMessages();

    verify(messageToSendProcessor, times(3)).processChunk();
    verify(messageToSendProcessor, times(3)).isThereWorkToDo();
  }
}
