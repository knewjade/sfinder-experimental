package _experimental;

import core.mino.Piece;

import java.util.List;
import java.util.function.Function;

public interface IPatternTree {
    void build(List<Piece> pieces, int depth, Function<List<Piece>, IPatternTree> terminate);

    boolean get(List<Piece> pieces, int depth);

    boolean run(TreeVisitor visitor, int depth);

    void success();

    boolean isPossible();
}
