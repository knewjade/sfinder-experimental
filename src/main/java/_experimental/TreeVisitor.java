package _experimental;

import core.mino.Piece;

public interface TreeVisitor {
    void visit(int depth, Piece piece);

    boolean execute(int depth);
}
