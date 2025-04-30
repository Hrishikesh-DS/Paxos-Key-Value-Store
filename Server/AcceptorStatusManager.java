package Server;

/**
 * Manages the state of acceptor threads in the Paxos protocol.
 * It tracks the number of acceptor threads that are currently sleeping
 * and ensures that no more than a specified maximum number of acceptors sleep
 * at the same time.
 * This class is solely for the purposes of demonstrating failure.
 */
public class AcceptorStatusManager {

  private static int sleepingAcceptors = 0;
  private static final int MAX_SLEEPING_ACCEPTERS = 2;

  /**
   * Determines whether another acceptor thread is allowed to go to sleep.
   * Only permitted if the current sleeping count is below the defined threshold.
   *
   * @return true if sleeping is allowed, false otherwise
   */
  public synchronized static boolean isSleepAllowed() {
    return sleepingAcceptors < MAX_SLEEPING_ACCEPTERS;
  }

  /**
   * Updates the state of an acceptor — either marking it as asleep or awake.
   * Increases or decreases the count of sleeping acceptors accordingly.
   *
   * @param shouldSleep true to mark the acceptor as sleeping, false to wake it up
   */
  public synchronized static void updateSleepStatus(boolean shouldSleep) {
    if (shouldSleep) {
      sleepingAcceptors++;
    } else {
      sleepingAcceptors--;
    }
  }

}
