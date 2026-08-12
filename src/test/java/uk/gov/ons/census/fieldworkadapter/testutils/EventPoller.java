package uk.gov.ons.census.fieldworkadapter.testutils;

import java.util.List;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.ons.census.common.model.entity.Event;
import uk.gov.ons.census.fieldworkadapter.model.repository.EventRepository;

@Component
@ActiveProfiles("test")
public class EventPoller {
  private final EventRepository eventRepository;

  public EventPoller(EventRepository eventRepository) {
    this.eventRepository = eventRepository;
  }

  @Retryable(
      retryFor = {EventsNotFoundException.class},
      maxAttempts = 10,
      backoff = @Backoff(delay = 2000),
      listeners = {"retryListener"})
  public List<Event> getEvents(int minExpectedEventCount) throws EventsNotFoundException {
    List<Event> events = eventRepository.findAll();

    if (events.size() < minExpectedEventCount) {
      throw new EventsNotFoundException(
          String.format(
              "Found: %d events, require at least: %d", events.size(), minExpectedEventCount));
    }

    return events;
  }
}
