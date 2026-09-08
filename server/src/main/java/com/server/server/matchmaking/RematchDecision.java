// File Location: server/src/main/java/com/server/server/matchmaking/RematchDecision.java
package com.server.server.matchmaking;

import com.shared.shared.network.MatchState;

/** Atomic result of recording one room member's rematch vote. */
public final class RematchDecision {
    private final boolean accepted;
    private final boolean started;
    private final String error;
    private final int votes;
    private final int requiredVotes;
    private final MatchState state;

    private RematchDecision(boolean accepted, boolean started, String error,
                            int votes, int requiredVotes, MatchState state) {
        this.accepted = accepted;
        this.started = started;
        this.error = error;
        this.votes = votes;
        this.requiredVotes = requiredVotes;
        this.state = state;
    }

    static RematchDecision rejected(String error, int votes, int requiredVotes) {
        return new RematchDecision(false, false, error, votes, requiredVotes, null);
    }

    static RematchDecision waiting(int votes, int requiredVotes) {
        return new RematchDecision(true, false, null, votes, requiredVotes, null);
    }

    static RematchDecision started(int votes, MatchState state) {
        return new RematchDecision(true, true, null, votes, votes, state);
    }

    public boolean isAccepted() { return accepted; }
    public boolean isStarted() { return started; }
    public String getError() { return error; }
    public int getVotes() { return votes; }
    public int getRequiredVotes() { return requiredVotes; }
    public MatchState getState() { return state; }
}
