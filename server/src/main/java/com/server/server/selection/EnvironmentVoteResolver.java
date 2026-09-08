// File Location: server/src/main/java/com/server/server/selection/EnvironmentVoteResolver.java
package com.server.server.selection;

import com.shared.shared.model.world.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/** Resolves the environment majority and randomly breaks a tie among tied selections. */
@Component
public class EnvironmentVoteResolver {
    private final Random random;

    public EnvironmentVoteResolver() {
        this(new Random());
    }

    EnvironmentVoteResolver(Random random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    public Environment resolve(Iterable<Environment> votes) {
        Map<Environment, Integer> counts = count(votes);
        int highest = counts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        if (highest == 0) {
            throw new IllegalArgumentException("At least one environment vote is required");
        }

        List<Environment> leaders = new ArrayList<>();
        for (Environment environment : Environment.values()) {
            if (counts.get(environment) == highest) leaders.add(environment);
        }
        return leaders.get(random.nextInt(leaders.size()));
    }

    public Map<Environment, Integer> count(Iterable<Environment> votes) {
        Map<Environment, Integer> counts = new EnumMap<>(Environment.class);
        for (Environment environment : Environment.values()) counts.put(environment, 0);
        if (votes == null) return counts;

        for (Environment vote : votes) {
            if (vote != null) counts.compute(vote, (key, value) -> value + 1);
        }
        return counts;
    }
}
