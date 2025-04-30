package Server;

import java.util.concurrent.atomic.AtomicBoolean;

import java.util.Random;

import Log.Logger;

/**
 * A runnable implementation that simulates the behavior of an acceptor in the
 * Paxos consensus protocol.
 * The acceptor thread periodically fails and restarts, simulating network or
 * process failures.
 * This class is solely for the purposes of demonstrating failure scenario in
 * Paxos Agreement.
 */
public class AcceptorSimulator implements Runnable {

  private final PaxosServerImpl server;
  private final AtomicBoolean running;
  private final Random random;
  private static final int MIN_FAILURE_INTERVAL = 5000; // Min time (in ms) before the thread fails
  private static final int MAX_FAILURE_INTERVAL = 10000; // Max time (in ms) before the thread fails
  private static final int MIN_RESTART_DELAY = 2000; // Min delay (in ms) before the thread restarts
  private static final int MAX_RESTART_DELAY = 5000; // Max delay (in ms) before the thread restarts

  private static Logger logger = new Logger(System.out);

  /**
   * Constructs an AcceptorRunnable with the specified Paxos server.
   *
   * @param server the Paxos server implementation associated with this acceptor
   */
  public AcceptorSimulator(PaxosServerImpl server) {
    this.server = server;
    this.running = new AtomicBoolean(true);
    this.random = new Random();
  }

  /**
   * Runs the acceptor thread, which simulates normal operation by sleeping for a
   * random interval,
   * then fails and restarts after a random delay. The thread will continue this
   * cycle while it is running.
   * It checks for the current number of acceptors that have failed before setting
   * the failed state to
   * false.
   */
  @Override
  public void run() {
    while (true) {
      if (running.get()) {
        try {
          // Check the number of failed threads
          synchronized (AcceptorStatusManager.class) {
            while (!AcceptorStatusManager.isSleepAllowed()) {
              // Wait if maximum number of acceptors are already sleeping
              AcceptorStatusManager.class.wait();
            }
            AcceptorStatusManager.updateSleepStatus(true);
          }

          // Simulate normal operation
          Thread.sleep(random.nextInt((MAX_FAILURE_INTERVAL - MIN_FAILURE_INTERVAL) + 1) + MIN_FAILURE_INTERVAL);

          // Fail the thread
          running.set(false);
          log("Acceptor thread failed, restarting...");

          // Restart after a random delay
          Thread.sleep(random.nextInt((MAX_RESTART_DELAY - MIN_RESTART_DELAY) + 1) + MIN_RESTART_DELAY);

          // Set state to not sleeping
          synchronized (AcceptorStatusManager.class) {
            AcceptorStatusManager.updateSleepStatus(false);
            // Notify other waiting threads
            AcceptorStatusManager.class.notifyAll();
          }

          running.set(true);
          log("Acceptor thread restarted");

        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Checks if the acceptor thread is currently running.
   *
   * @return true if the acceptor thread is running, false otherwise
   */
  public boolean isRunning() {
    return running.get();
  }

  /**
   * Logs a message using the server's logger.
   *
   * @param message the message to be logged
   */
  private void log(String message) {
    logger.log("Port:" + server.port + ":" + message);
  }
}
