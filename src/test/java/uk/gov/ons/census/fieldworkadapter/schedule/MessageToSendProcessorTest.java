package uk.gov.ons.census.fieldworkadapter.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.census.common.model.entity.MessageToSend;
import uk.gov.ons.census.fieldworkadapter.model.repository.MessageToSendRepository;

@ExtendWith(MockitoExtension.class)
class MessageToSendProcessorTest {

  @Mock private MessageToSendRepository messageToSendRepository;
  @Mock private MessageToSendSender messageToSendSender;

  @InjectMocks private MessageToSendProcessor underTest;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(underTest, "chunkSize", 2);
  }

  @Test
  void shouldSendAndDeleteOnlySuccessfullyPublishedMessages() {
    MessageToSend first = new MessageToSend();
    first.setMessageBody("first");
    first.setDestinationTopic("topic");

    MessageToSend second = new MessageToSend();
    second.setMessageBody("second");
    second.setDestinationTopic("topic");

    when(messageToSendRepository.findMessagesToSend(2)).thenReturn(Stream.of(first, second));
    doAnswer(
            (Answer<Void>)
                invocation -> {
                  MessageToSend sent = invocation.getArgument(0);
                  if (sent == second) {
                    throw new RuntimeException("cannot publish");
                  }
                  return null;
                })
        .when(messageToSendSender)
        .sendMessage(any(MessageToSend.class));

    underTest.processChunk();

    verify(messageToSendSender).sendMessage(first);
    verify(messageToSendSender).sendMessage(second);

    ArgumentCaptor<List<MessageToSend>> captor = ArgumentCaptor.forClass(List.class);
    verify(messageToSendRepository).deleteAllInBatch(captor.capture());
    assertThat(captor.getValue()).containsExactly(first);
  }

  @Test
  void shouldReportWorkToDoWhenQueueHasMessages() {
    when(messageToSendRepository.count()).thenReturn(1L);

    assertThat(underTest.isThereWorkToDo()).isTrue();
  }

  @Test
  void shouldReportNoWorkToDoWhenQueueEmpty() {
    when(messageToSendRepository.count()).thenReturn(0L);

    assertThat(underTest.isThereWorkToDo()).isFalse();
    verify(messageToSendRepository, times(1)).count();
  }
}
