package uk.gov.ons.census.fieldworkadapter.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.census.common.model.entity.ClusterLeader;
import uk.gov.ons.census.fieldworkadapter.model.repository.ClusterLeaderRepository;

@ExtendWith(MockitoExtension.class)
class ClusterLeaderStartupManagerTest {

  @Mock private ClusterLeaderRepository clusterLeaderRepository;

  private ClusterLeaderStartupManager underTest;

  @BeforeEach
  void setUp() throws Exception {
    underTest = new ClusterLeaderStartupManager(clusterLeaderRepository);
    ReflectionTestUtils.setField(underTest, "hostName", "test-host");
  }

  @Test
  void shouldDoNothingWhenLeaderAlreadyExists() {
    UUID leaderId = UUID.randomUUID();
    when(clusterLeaderRepository.existsById(leaderId)).thenReturn(true);

    underTest.doStartupChecksAndAttemptToElectLeaderIfRequired(leaderId);

    verify(clusterLeaderRepository).existsById(leaderId);
    verify(clusterLeaderRepository, never()).saveAndFlush(any(ClusterLeader.class));
  }

  @Test
  void shouldCreateLeaderRecordWhenLeaderMissing() {
    UUID leaderId = UUID.randomUUID();
    when(clusterLeaderRepository.existsById(leaderId)).thenReturn(false);

    underTest.doStartupChecksAndAttemptToElectLeaderIfRequired(leaderId);

    ArgumentCaptor<ClusterLeader> captor = ArgumentCaptor.forClass(ClusterLeader.class);
    verify(clusterLeaderRepository).saveAndFlush(captor.capture());

    ClusterLeader saved = captor.getValue();
    assertThat(saved.getId()).isEqualTo(leaderId);
    assertThat(saved.getHostName()).isEqualTo("test-host");
    assertThat(saved.getHostLastSeenAliveAt()).isNotNull();
  }
}
