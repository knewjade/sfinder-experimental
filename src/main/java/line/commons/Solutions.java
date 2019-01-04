package line.commons;

import java.util.HashSet;
import java.util.Set;

public class Solutions {
    private final Set<Set<Integer>> solutions;

    public Solutions() {
        this.solutions = new HashSet<>();
    }

    public boolean contains(Candidate candidate) {
        return contains(candidate.getKeys());
    }

    private boolean contains(Set<Integer> keys) {
        return solutions.contains(keys);
    }

    public boolean add(Candidate candidate) {
        return add(candidate.getKeys());
    }

    public boolean add(Set<Integer> keys) {
        return solutions.add(keys);
    }

    public boolean partialContains(Candidate candidate, KeyOriginalPiece keyOriginalPiece) {
        return partialContains(candidate.getKeys(), keyOriginalPiece.getIndex());
    }

    public boolean partialContains(Set<Integer> candidateKeys, int currentKey) {
        Set<Integer> keys = new HashSet<>(candidateKeys);
        keys.add(currentKey);

        if (contains(keys)) {
            return true;
        }

        // currentKey以外をひとつ抜いた組み合わせでも solutions に含まれている
        for (int prevKey : candidateKeys) {
            keys.remove(prevKey);

            if (contains(keys)) {
                return true;
            }

            keys.add(prevKey);
        }

        return false;
    }
}
