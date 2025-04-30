package Server;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Represents a promise response to a prepare request in the Paxos consensus
 * protocol.
 * It contains information about the prepare request and any accepted proposal.
 */
public class PreparePromise {

  private long proposalId;
  private long previouslyAcceptedId;
  private Consumer<Map<String, String>> acceptedCommand;

  /**
   * Creates a PrepareResponse indicating both the prepare request ID
   * and a previously accepted proposal (if any).
   *
   * @param proposalId               the ID of the current prepare request
   * @param previouslyAcceptedId     the ID of the last accepted proposal, or -1
   *                                 if none
   * @param previouslyAcceptedAction the associated operation for the accepted
   *                                 proposal
   */
  public PreparePromise(long proposalId, long previouslyAcceptedId,
      Consumer<Map<String, String>> previouslyAcceptedAction) {
    this.proposalId = proposalId;
    this.previouslyAcceptedId = previouslyAcceptedId;
    this.acceptedCommand = previouslyAcceptedAction;
  }

  /**
   * Constructs a PrepareResponse when no prior accepted proposal exists.
   *
   * @param proposalId the ID of the current prepare attempt
   */
  public PreparePromise(long proposalId) {
    this.proposalId = proposalId;
    this.previouslyAcceptedId = -1;
  }

  /**
   * Indicates whether the proposal was already accepted by comparing IDs.
   *
   * @return true if the prepare ID matches the accepted one, false otherwise
   */
  public boolean isPromised() {
    return proposalId == previouslyAcceptedId;
  }

  /**
   * Gets the ID of the previously accepted proposal.
   *
   * @return the ID of the accepted proposal, or -1 if none
   */
  public Long getPreviousAcceptedId() {
    return previouslyAcceptedId;
  }

  /**
   * Gets the current prepare proposal ID.
   *
   * @return the prepare request ID
   */
  public long getProposalId() {
    return proposalId;
  }

  /**
   * Gets the operation associated with the previously accepted proposal.
   *
   * @return a Consumer representing the accepted command logic, or null if none
   */
  public Consumer<Map<String, String>> getAcceptedCommand() {
    return acceptedCommand;
  }

}
