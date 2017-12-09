package _experimental;

import common.comparator.FieldComparator;
import common.datastore.action.Action;
import core.action.candidate.Candidate;
import core.field.Field;
import core.mino.MinoFactory;
import core.mino.Piece;
import searcher.common.validator.Validator;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class PatternTree implements IPatternTree {
    private final EnumMap<Piece, IPatternTree> map = new EnumMap<>(Piece.class);
    private final AtomicBoolean isPossible = new AtomicBoolean(false);

    public void build(List<Piece> pieces, Function<List<Piece>, IPatternTree> terminate) {
        build(pieces, 0, terminate);
    }

    @Override
    public void build(List<Piece> pieces, int depth, Function<List<Piece>, IPatternTree> terminate) {
        assert depth < pieces.size() : depth;
        Piece piece = pieces.get(depth);

        if (depth == pieces.size() - 1) {
            map.computeIfAbsent(piece, key -> terminate.apply(pieces));
        } else {
            IPatternTree tree = map.computeIfAbsent(piece, key -> new PatternTree());
            tree.build(pieces, depth + 1, terminate);
        }
    }

    public boolean get(List<Piece> pieces) {
        return get(pieces, 0);
    }

    @Override
    public boolean get(List<Piece> pieces, int depth) {
        assert depth < pieces.size() : depth;
        Piece piece = pieces.get(depth);

        assert map.containsKey(piece) : map;
        IPatternTree tree = map.get(piece);

        if (depth == pieces.size() - 1) {
            return tree.isPossible();
        } else {
            return tree.get(pieces, depth + 1);
        }
    }

    @Override
    public boolean isPossible() {
        return isPossible.get();
    }

    @Override
    public void success() {
        boolean oldValue = isPossible.getAndSet(true);
        if (!oldValue)
            for (IPatternTree tree : map.values())
                tree.success();
    }

    public boolean run(TreeVisitor visitor) {
        return run(visitor, 0);
    }

    // すべての探索が成功したときtrueを返す
    @Override
    public boolean run(TreeVisitor visitor, int depth) {
        boolean result = true;
        for (Map.Entry<Piece, IPatternTree> entry : map.entrySet()) {
            Piece piece = entry.getKey();
            visitor.visit(depth, piece);

            IPatternTree tree = entry.getValue();
            result &= tree.run(visitor, depth + 1);
        }

        if (result)
            this.success();

        return result;
    }
}

class CommonObj {
    private ThreadLocal<Candidate<? extends Action>> candidateThreadLocal;
    private MinoFactory minoFactory;
    private Validator validator;

    CommonObj(ThreadLocal<Candidate<? extends Action>> candidateThreadLocal, MinoFactory minoFactory, Validator validator) {
        this.candidateThreadLocal = candidateThreadLocal;
        this.minoFactory = minoFactory;
        this.validator = validator;
    }

    public Candidate<? extends Action> getCandidate() {
        return candidateThreadLocal.get();
    }

    public MinoFactory getMinoFactory() {
        return minoFactory;
    }

    public Validator getValidator() {
        return validator;
    }
}

class Obj implements Comparable<Obj> {
    private final Field field;
    private final int maxClearLine;
    private final Piece hold;

    public Obj(Field field, int maxClearLine, Piece hold) {
        this.field = field;
        this.maxClearLine = maxClearLine;
        this.hold = hold;
    }

    public int getPriority() {
        return Heuristic.c(field, maxClearLine);
    }

    public int getMaxClearLine() {
        return maxClearLine;
    }

    public Field getField() {
        return field;
    }

    public Piece getHold() {
        return hold;
    }

    @Override
    public boolean equals(Object o) {
        assert o != null;
        assert o instanceof Obj;
        Obj obj = (Obj) o;
        return hold == obj.hold && FieldComparator.compareField(this.field, obj.field) == 0;
    }

    @Override
    public int hashCode() {
        int number = hold != null ? hold.getNumber() : 7;
        return number * 31 + field.hashCode();
    }

    @Override
    public int compareTo(Obj o) {
        Piece hold1 = this.hold;
        Piece hold2 = o.hold;
        if (hold1 == hold2) {
            return FieldComparator.compareField(this.field, o.field);
        } else {
            int number1 = hold1 != null ? hold1.getNumber() : 7;
            int number2 = hold2 != null ? hold2.getNumber() : 7;
            return number1 - number2;
        }
    }
}