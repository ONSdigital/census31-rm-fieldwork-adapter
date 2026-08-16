package uk.gov.ons.census.fieldworkadapter.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.census.common.model.entity.ClusterLeader;
import uk.gov.ons.census.fieldworkadapter.model.repository.ClusterLeaderRepository;

@ExtendWith(MockitoExtension.class)
class ClusterLeaderManagerTest {

  @Mock private ClusterLeaderRepository clusterLeaderRepository;
  @Mock private ClusterLeaderStartupManager clusterLeaderStartupManager;

  private ClusterLeaderManager underTest;

  @BeforeEach
  void setUp() throws Exception {
    underTest = new ClusterLeaderManager(clusterLeaderRepository, clusterLeaderStartupManager);
    ReflectionTestUtils.setField(underTest, "hostName", "host-a");
    ReflectionTestUtils.setField(underTest, "leaderDeathTimeout", 30);
  }

  @Test
  void shouldReturnFalseWhenLeaderElectionDeadHeatOccurs() {
    doThrow(new DataIntegrityViolationException("duplicate key"))
        .when(clusterLeaderStartupManager)
        .doStartupChecksAndAttemptToElectLeaderIfRequired(any());

    boolean isLeader = underTest.isThisHostClusterLeader();

    assertThat(isLeader).isFalse();
    verify(clusterLeaderRepository, never()).getClusterLeaderAndLockById(any());
  }

  @Test
  void shouldReturnFalseWhenLeaderRowCannotBeLocked() {
    when(clusterLeaderRepository.getClusterLeaderAndLockById(any())).thenReturn(Optional.empty());

    boolean isLeader = underTest.isThisHostClusterLeader();

    assertThat(isLeader).isFalse();
  }

  @Test
  void shouldReturnTrueWhenThisHostIsAlreadyLeader() {
    ClusterLeader clusterLeader = new ClusterLeader();
    clusterLeader.setHostName("host-a");
    clusterLeader.setHostLastSeenAliveAt(OffsetDateTime.now());
    when(clusterLeaderRepository.getClusterLeaderAndLockById(any()))
        .thenReturn(Optional.of(clusterLeader));

    boolean isLeader = underTest.isThisHostClusterLeader();

    assertThat(isLeader).isTrue();
    verify(clusterLeaderRepository, never()).saveAndFlush(any(ClusterLeader.class));
  }

  @Test
  void shouldTakeOverLeadershipWhenCurrentLeaderIsDead() {
    ClusterLeader clusterLeader = new ClusterLeader();
    clusterLeader.setHostName("host-b");
    clusterLeader.setHostLastSeenAliveAt(OffsetDateTime.now().minusMinutes(5));
    when(clusterLeaderRepository.getClusterLeaderAndLockById(any()))
        .thenReturn(Optional.of(clusterLeader));

    boolean isLeader = underTest.isThisHostClusterLeader();

    assertThat(isLeader).isTrue();
    ArgumentCaptor<ClusterLeader> captor = ArgumentCaptor.forClass(ClusterLeader.class);
    verify(clusterLeaderRepository).saveAndFlush(captor.capture());
    assertThat(captor.getValue().getHostName()).isEqualTo("host-a");
    assertThat(captor.getValue().getHostLastSeenAliveAt()).isNotNull();
  }

  @Test
  void shouldReturnFalseWhenAnotherHostIsAliveAndLeader() {
    ClusterLeader clusterLeader = new ClusterLeader();
    clusterLeader.setHostName("host-b");
    clusterLeader.setHostLastSeenAliveAt(OffsetDateTime.now());
    when(clusterLeaderRepository.getClusterLeaderAndLockById(any()))
        .thenReturn(Optional.of(clusterLeader));

    boolean isLeader = underTest.isThisHostClusterLeader();

    assertThat(isLeader).isFalse();
    verify(clusterLeaderRepository, never()).saveAndFlush(any(ClusterLeader.class));
  }

  @Test
  void shouldNotUpdateKeepAliveWhenLeaderRowCannotBeLocked() {
    when(clusterLeaderRepository.getClusterLeaderAndLockById(any())).thenReturn(Optional.empty());

    underTest.leaderKeepAlive();

    verify(clusterLeaderRepository, never()).saveAndFlush(any(ClusterLeader.class));
  }

  @Test
  void shouldUpdateKeepAliveWhenThisHostIsLeader() {
    ClusterLeader clusterLeader = new ClusterLeader();
    clusterLeader.setHostName("host-a");
    clusterLeader.setHostLastSeenAliveAt(OffsetDateTime.now().minusMinutes(1));
    when(clusterLeaderRepository.getClusterLeaderAndLockById(any()))
        .thenReturn(Optional.of(clusterLeader));

    underTest.leaderKeepAlive();

    verify(clusterLeaderRepository).saveAndFlush(clusterLeader);
  }

  @Test
  void shouldNotUpdateKeepAliveWhenAnotherHostIsLeader() {
    ClusterLeader clusterLeader = new ClusterLeader();
    clusterLeader.setHostName("host-z");
    clusterLeader.setHostLastSeenAliveAt(OffsetDateTime.now());
    when(clusterLeaderRepository.getClusterLeaderAndLockById(any()))
        .thenReturn(Optional.of(clusterLeader));

    underTest.leaderKeepAlive();

    verify(clusterLeaderRepository, never()).saveAndFlush(any(ClusterLeader.class));
  }
}
