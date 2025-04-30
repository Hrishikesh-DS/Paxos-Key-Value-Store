package Server;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Encapsulates an accept-phase request containing a proposal ID
 * and an operation to apply to a shared key-value store.
 */
public class AcceptMessage {

  private long proposalId;

  private Consumer<Map<String, String>> operation;

  /**
   * Initializes an acceptance payload with a given proposal ID and associated
   * operation.
   *
   * @param acceptId  the unique identifier for this message
   * @param operation the consumer function to process the message's data
   */
  public AcceptMessage(long acceptId, Consumer<Map<String, String>> operation) {
    this.proposalId = acceptId;
    this.operation = operation;
  }

  /**
   * Retrieves the proposal ID linked with this acceptance message.
   *
   * @return the proposal's unique identifier
   */
  public long getProposalId() {
    return proposalId;
  }

  /**
   * Gets the operation that should be executed upon acceptance.
   *
   * @return the Consumer function tied to this message
   */
  public Consumer<Map<String, String>> getOperation() {
    return operation;
  }
}
