package line.step5;

import common.datastore.Operation;
import common.datastore.PieceCounter;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.Mino;
import core.mino.MinoFactory;
import core.mino.Piece;
import core.neighbor.OriginalPiece;
import core.srs.MinoRotation;
import core.srs.Rotate;
import line.commons.FactoryPool;
import line.commons.KeyOriginalPiece;
import line.commons.LineCommons;

import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

class Roofs {
    private final MinoFactory minoFactory;
    private final MinoRotation minoRotation;
    private final int maxHeight;
    private final List<OriginalPiece> allPieces;
    private final EnumMap<Piece, PieceCounter> allPieceCounters;

    Roofs(FactoryPool factoryPool) {
        this.minoFactory = factoryPool.getMinoFactory();
        this.minoRotation = factoryPool.getMinoRotation();
        this.maxHeight = factoryPool.getMaxHeight();
        this.allPieces = factoryPool.createUniqueOriginalPieces();
        this.allPieceCounters = LineCommons.getAllPieceCounters();
    }

    List<KeyOriginalPiece> getPieces(Operation operation, Field notAllowedBlock, PieceCounter pieceCounter) {
        Field influenceBlocks = getInfluenceBlocks(operation);
        AtomicInteger counter = new AtomicInteger();
        return allPieces.stream()
                .filter(originalPiece -> pieceCounter.containsAll(allPieceCounters.get(originalPiece.getPiece())))
                .filter(originalPiece -> !influenceBlocks.canPut(originalPiece))
                .filter(notAllowedBlock::canPut)
                .map(originalPiece -> new KeyOriginalPiece(originalPiece, counter.getAndIncrement()))
                .collect(Collectors.toList());
    }

    private Field getInfluenceBlocks(Operation operation) {
        Piece piece = operation.getPiece();
        Rotate currentRotate = operation.getRotate();
        int currentX = operation.getX();
        int currentY = operation.getY();

        Field influenceBlocks = FieldFactory.createField(maxHeight);

        // 左回転する前のミノ
        {
            Rotate beforeRotate = currentRotate.getRightRotate();
            Mino beforeMino = minoFactory.create(piece, beforeRotate);
            int[][] patterns = minoRotation.getLeftPatternsFrom(beforeMino);
            for (int[] pattern : patterns) {
                influenceBlocks.put(beforeMino, currentX - pattern[0], currentY - pattern[1]);
            }
        }

        // 右回転する前のミノ
        {
            Rotate beforeRotate = currentRotate.getLeftRotate();
            Mino beforeMino = minoFactory.create(piece, beforeRotate);
            int[][] patterns = minoRotation.getRightPatternsFrom(beforeMino);
            for (int[] pattern : patterns) {
                influenceBlocks.put(beforeMino, currentX - pattern[0], currentY - pattern[1]);
            }
        }

        Mino currentMino = minoFactory.create(piece, currentRotate);
        influenceBlocks.remove(currentMino, currentX, currentY);

        return influenceBlocks;
    }
}
