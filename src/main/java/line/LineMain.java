package line;

import common.datastore.Operations;
import common.datastore.PieceCounter;
import common.parser.OperationInterpreter;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.Piece;
import core.neighbor.OriginalPiece;
import core.neighbor.OriginalPieceFactory;
import entry.path.output.MyFile;
import lib.AsyncBufferedFileWriter;

import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LineMain {
    private static final int LINE_Y = 3;

    public static void main(String[] args) {
        {
            int maxHeight = 7;
            Field lineField = FieldFactory.createField("" +
                    "__________" +
                    "__________" +
                    "__________" +
                    "XXXXXXXXXX" +
                    "__________" +
                    "__________" +
                    "__________"
            );
            String fileName = "1line";

            System.out.println(fileName);
            run(maxHeight, lineField, fileName);
        }

        {
            int maxHeight = 8;
            Field lineField = FieldFactory.createField("" +
                    "__________" +
                    "__________" +
                    "XXXXXXXXXX" +
                    "XXXXXXXXXX" +
                    "__________" +
                    "__________" +
                    "__________"
            );
            String fileName = "12line";

            System.out.println(fileName);
            run(maxHeight, lineField, fileName);
        }

        {
            int maxHeight = 9;
            Field lineField = FieldFactory.createField("" +
                    "__________" +
                    "XXXXXXXXXX" +
                    "__________" +
                    "XXXXXXXXXX" +
                    "__________" +
                    "__________" +
                    "__________"
            );
            String fileName = "13line";

//            System.out.println(fileName);
//            run(maxHeight, lineField, fileName);
        }
    }

    private static void run(int maxHeight, Field lineField, String fileName) {
        OriginalPieceFactory pieceFactory = new OriginalPieceFactory(maxHeight);
        Set<OriginalPiece> pieces = pieceFactory.create();

        HashMap<Long, EnumMap<Piece, List<OriginalPiece>>> maps = new HashMap<>();

        for (int dy = 0; dy < 3; dy++) {
            int y = LINE_Y + dy;
            for (int x = 0; x < 10; x++) {
                Field field = FieldFactory.createField(maxHeight);
                field.setBlock(x, y);

                EnumMap<Piece, List<OriginalPiece>> enumMap = new EnumMap<>(Piece.class);
                for (Piece piece : Piece.valueList()) {
                    List<OriginalPiece> piecesList = pieces.stream()
                            .filter(originalPiece -> originalPiece.getPiece() == piece)
                            .filter(originalPiece -> !field.canPut(originalPiece))
                            .collect(Collectors.toList());

                    enumMap.put(piece, piecesList);
                }

                maps.put(1L << (x + dy * 10), enumMap);
            }
        }

        Field field = FieldFactory.createField(maxHeight);

        LineObj firstLineObj = new LineObj(lineField, field, new PieceCounter(Piece.valueList()));

        LineRunner runner = new LineRunner(maps);

        MyFile file = new MyFile("output/" + fileName);
        try (AsyncBufferedFileWriter writer = file.newAsyncWriter()) {
            runner.run(firstLineObj, lineObj -> {
                Operations operations = new Operations(lineObj.pieceStream());
                String str = OperationInterpreter.parseToString(operations);
                writer.writeAndNewLine(str);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

interface ILineObj {
    int SLIDE_INDEX = 3 * 10;

    boolean isSolution();

    boolean isCandidate();

    long getMinBoard();

    ILineObj next(OriginalPiece piece);

    boolean canPut(OriginalPiece piece);

    Field getLeft();

    Field getField();

    Stream<OriginalPiece> pieceStream();

    PieceCounter getPieceCounter();
}

class LineObj implements ILineObj {
    private final Field left;
    private final Field field;
    private final PieceCounter pieceCounter;

    LineObj(Field left, Field field, PieceCounter pieceCounter) {
        assert left.getBoardCount() == 1;
        this.left = left;
        this.field = field;
        this.pieceCounter = pieceCounter;
    }

    @Override
    public boolean isSolution() {
        return left.isPerfect();
    }

    @Override
    public boolean isCandidate() {
        return pieceCounter.getCounter() != 0L;
    }

    @Override
    public long getMinBoard() {
        long board = left.getBoard(0);
        long min = board & (-board);
        return min >> SLIDE_INDEX;
    }

    @Override
    public ILineObj next(OriginalPiece piece) {
        return new RecursiveLineObj(this, piece);
    }

    @Override
    public boolean canPut(OriginalPiece piece) {
        return field.canPut(piece);
    }

    @Override
    public Field getLeft() {
        return left;
    }

    @Override
    public Field getField() {
        return field;
    }

    @Override
    public Stream<OriginalPiece> pieceStream() {
        return Stream.empty();
    }

    @Override
    public PieceCounter getPieceCounter() {
        return pieceCounter;
    }
}

class RecursiveLineObj implements ILineObj {
    private final ILineObj prev;
    private final Field left;
    private final Field field;
    private final PieceCounter pieceCounter;
    private final OriginalPiece piece;

    RecursiveLineObj(ILineObj prev, OriginalPiece piece) {
        this.prev = prev;

        Field freezeLeft = prev.getLeft().freeze();
        freezeLeft.remove(piece);

        Field freezeField = prev.getField().freeze();
        freezeField.put(piece);

        PieceCounter pieceCounter = prev.getPieceCounter();

        this.left = freezeLeft;
        this.field = freezeField;
        this.piece = piece;
        this.pieceCounter = pieceCounter.removeAndReturnNew(new PieceCounter(Stream.of(piece.getPiece())));
    }

    @Override
    public boolean isSolution() {
        return left.isPerfect();
    }

    @Override
    public boolean isCandidate() {
        return pieceCounter.getCounter() != 0L;
    }

    @Override
    public long getMinBoard() {
        long board = left.getBoard(0);
        long min = board & (-board);
        return min >> SLIDE_INDEX;
    }

    @Override
    public ILineObj next(OriginalPiece piece) {
        return new RecursiveLineObj(this, piece);
    }

    @Override
    public boolean canPut(OriginalPiece piece) {
        return field.canPut(piece);
    }

    @Override
    public Field getLeft() {
        return left;
    }

    @Override
    public Field getField() {
        return field;
    }

    @Override
    public Stream<OriginalPiece> pieceStream() {
        Stream<OriginalPiece> stream = prev.pieceStream();
        return Stream.concat(stream, Stream.of(piece));
    }

    @Override
    public PieceCounter getPieceCounter() {
        return pieceCounter;
    }
}

class LineRunner {
    private HashMap<Long, EnumMap<Piece, List<OriginalPiece>>> maps;

    LineRunner(HashMap<Long, EnumMap<Piece, List<OriginalPiece>>> maps) {
        this.maps = maps;
    }

    void run(ILineObj lineObj, Consumer<ILineObj> callback) {
        // noinspection ResultOfMethodCallIgnored
        this.run(Stream.of(lineObj), callback).count();
    }

    private Stream<ILineObj> run(Stream<ILineObj> stream, Consumer<ILineObj> callback) {
        return stream
                .flatMap(lineObj -> {
                    if (lineObj.isSolution()) {
                        callback.accept(lineObj);
                        return Stream.empty();
                    }

                    if (!lineObj.isCandidate()) {
                        return Stream.empty();
                    }

                    Stream<ILineObj> next = this.next(lineObj);
                    return this.run(next, callback);
                });
    }

    private Stream<ILineObj> next(ILineObj lineObj) {
        long minBoard = lineObj.getMinBoard();
        EnumMap<Piece, List<OriginalPiece>> enumMap = maps.get(minBoard);

        return lineObj.getPieceCounter().getBlockStream()
                .flatMap(piece -> {
                    List<OriginalPiece> pieces = enumMap.get(piece);
                    return pieces.stream()
                            .filter(lineObj::canPut)
                            .map(lineObj::next);
                });
    }
}