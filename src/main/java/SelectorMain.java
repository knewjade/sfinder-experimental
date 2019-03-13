import common.SyntaxException;
import common.buildup.BuildUp;
import common.datastore.MinoOperationWithKey;
import common.datastore.blocks.Pieces;
import common.pattern.LoadedPatternGenerator;
import common.tetfu.common.ColorConverter;
import concurrent.HarddropReachableThreadLocal;
import core.action.reachable.HarddropReachable;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.MinoFactory;
import core.mino.Piece;
import core.neighbor.SimpleOriginalPiece;
import core.srs.Rotate;
import entry.path.output.MyFile;
import entry.path.output.OneFumenParser;
import lib.AsyncBufferedFileWriter;
import output.HTMLBuilder;
import output.HTMLColumn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Select solutions from sequence
public class SelectorMain {
    public static void main(String[] args) throws IOException, SyntaxException {
        if (args.length == 0) {
            throw new IllegalArgumentException("Specify sequence at 1st argument");
        }

        int fieldHeight = 4;
        MinoFactory minoFactory = new MinoFactory();

        Path indexPath = Paths.get("output/index.csv");
        Map<Integer, IndexPiecePair> indexes = IndexPiecePairs.create(indexPath, minoFactory, fieldHeight);
        System.out.println(indexes.size());

        String piecesString = "JTOLSZTIJI";
        LoadedPatternGenerator generator = new LoadedPatternGenerator(piecesString);

        int numOfPieces = generator.getDepth();
        if (numOfPieces != 10) {
            throw new IllegalArgumentException("Length of sequence should be 10: num=" + numOfPieces);
        }

        Optional<Pieces> optional = generator.blocksStream().findFirst();
        if (!optional.isPresent()) {
            throw new IllegalArgumentException("Should specify piece sequence");
        }

        Pieces pieces = optional.get();
        List<Piece> piecesList = pieces.getPieces();
        if (piecesList.get(piecesList.size() - 1) != Piece.I) {
            throw new IllegalArgumentException("Last piece in sequence must be I");
        }

        Field initField = FieldFactory.createField(fieldHeight);
        HarddropReachableThreadLocal harddropReachableThreadLocal = new HarddropReachableThreadLocal(fieldHeight);

        List<ResultWithTetris> results = Files.lines(Paths.get("output/tetris_indexed_solutions_SRS7BAG.csv")).parallel()
                .map(line -> (
                        Arrays.stream(line.split(","))
                                .map(Integer::parseInt)
                                .map(indexes::get)
                                .collect(Collectors.toList())
                ))
                .flatMap(pairs -> {
                    List<IndexPiecePair> allIList = pairs.stream()
                            .filter(it -> {
                                SimpleOriginalPiece originalPiece = it.getSimpleOriginalPiece();
                                return originalPiece.getPiece() == Piece.I && originalPiece.getRotate() == Rotate.Left;
                            })
                            .collect(Collectors.toList());
                    assert 0 < allIList.size();

                    return allIList.stream().map(iPiece -> {
                        List<IndexPiecePair> operations = pairs.stream()
                                .filter(it -> !iPiece.equals(it))
                                .collect(Collectors.toList());

                        HarddropReachable reachable = harddropReachableThreadLocal.get();
                        Stream<SimpleOriginalPiece> stream = operations.stream().map(IndexPiecePair::getSimpleOriginalPiece);
                        List<Piece> subPieceList = piecesList.subList(0, piecesList.size() - 1);
                        assert subPieceList.size() == 9;

                        if (!BuildUp.existsValidByOrder(initField, stream, subPieceList, fieldHeight, reachable)) {
                            return null;
                        }

                        return new ResultWithTetris(operations, iPiece);
                    });
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        System.out.println(results.size());

        results.sort(Comparator.comparingInt(ResultWithTetris::calcCost));

        HTMLBuilder<Column> htmlBuilder = new HTMLBuilder<>("tetris solutions");
        htmlBuilder.addHeader(piecesString);

        ColorConverter colorConverter = new ColorConverter();
        OneFumenParser fumenParser = new OneFumenParser(minoFactory, colorConverter);

        Column column = new Column(piecesString);

        for (ResultWithTetris result : results) {
            List<MinoOperationWithKey> operations = result.toAllOperations();
            String fumen = fumenParser.parse(operations, initField, fieldHeight, "");
            String title = result.getTitle();
            String aLink = String.format("<div><a href='http://fumen.zui.jp/?v115@%s' target='_blank'>%s</a></div>", fumen, title);
            htmlBuilder.addColumn(column, aLink);
        }

        MyFile file = new MyFile("output/tetris.html");
        try (AsyncBufferedFileWriter writer = file.newAsyncWriter()) {
            List<String> lines = htmlBuilder.toList(Collections.singletonList(column), false);
            for (String line : lines) {
                writer.writeAndNewLine(line);
            }
            writer.flush();
        }
    }
}

class Column implements HTMLColumn {
    private final String pieces;

    Column(String pieces) {
        this.pieces = pieces;
    }

    @Override
    public String getTitle() {
        return pieces;
    }

    @Override
    public String getId() {
        return pieces.toLowerCase();
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.empty();
    }
}


class ResultWithTetris {
    private final IndexPiecePair last;
    private final List<MinoOperationWithKey> allOperations;
    private final int moveX;
    private final int rotate1;
    private final int rotate2;

    ResultWithTetris(List<IndexPiecePair> operations, IndexPiecePair last) {
        this.last = last;

        List<MinoOperationWithKey> allOperations = Stream.concat(operations.stream(), Stream.of(last))
                .map(IndexPiecePair::getSimpleOriginalPiece)
                .collect(Collectors.toList());
        this.allOperations = allOperations;

        int moveX = 0;
        for (MinoOperationWithKey operation : allOperations) {
            moveX += Math.abs(operation.getX() - 4);
        }
        this.moveX = moveX;

        int rotate1 = 0;
        int rotate2 = 0;
        for (MinoOperationWithKey operation : allOperations) {
            switch (operation.getRotate()) {
                case Left:
                case Right:
                    rotate1 += 1;
                    break;
                case Reverse:
                    rotate2 += 1;
                    break;
            }
        }
        this.rotate1 = rotate1;
        this.rotate2 = rotate2;
    }

    List<MinoOperationWithKey> toAllOperations() {
        return allOperations;
    }

    int calcCost() {
        return moveX;
    }

    String getTitle() {
        SimpleOriginalPiece piece = last.getSimpleOriginalPiece();
        return String.format("%s (x=%d) [moveX=%d, rotate1=%d, rotate2=%d]", piece.getRotate(), piece.getX(), moveX, rotate1, rotate2);
    }
}