package line;

import common.datastore.Operation;
import common.datastore.Operations;
import common.datastore.PieceCounter;
import common.datastore.SimpleOperation;
import common.parser.OperationInterpreter;
import commons.Commons;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.Mino;
import core.mino.MinoFactory;
import core.mino.MinoShifter;
import core.mino.Piece;
import core.neighbor.OriginalPiece;
import core.neighbor.OriginalPieceFactory;
import core.srs.Rotate;
import entry.path.output.MyFile;
import lib.AsyncBufferedFileWriter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// ミノの個数ごとにファイルを振り分ける
// ライン消去にTが使われる解だけを抽出する
// すべての行にTがかかっている解だけを抽出する
// y=5が揃っている状態
// ミノが空中に浮いている可能性あり
public class Line2Main {
    public static void main(String[] args) throws IOException {
        {
            String fileName = "1line";
            System.out.println(fileName);
            run(fileName);
        }

        {
            String fileName = "12line";
            System.out.println(fileName);
            run(fileName);
        }
    }

    private static void run(String fileName) throws IOException {
        // Open
        int maxFile = 8;
        AsyncBufferedFileWriter[] writers = new AsyncBufferedFileWriter[maxFile];
        for (int index = 3; index < maxFile; index++) {
            String outputFileName = "output/" + fileName + "_" + index;
            writers[index] = new MyFile(outputFileName).newAsyncWriter();
        }

        // Grouping
        int maxHeight = 12;
        MinoFactory minoFactory = new MinoFactory();
        MinoShifter minoShifter = new MinoShifter();
        Line2Obj obj = new Line2Obj(minoFactory, minoShifter, maxHeight);

        AtomicInteger counter = new AtomicInteger();
        Files.lines(Paths.get("output/" + fileName)).parallel()
                .peek(line -> {
                    int count = counter.incrementAndGet();
                    if (count % 10000 == 0) System.out.println(count);
                })
                .filter(line -> line.contains("T"))
                .map(OperationInterpreter::parseToOperations)
                .flatMap(operations -> {
                    List<Operation> operationList = operations.getOperations().stream()
                            .map(it -> new SimpleOperation(it.getPiece(), it.getRotate(), it.getX(), it.getY() + 2))
                            .collect(Collectors.toList());
                    return obj.search(operationList);
                })
                .forEach(operations -> {
                    // 書き込み
                    int count = operations.getOperations().size();
                    String line = OperationInterpreter.parseToString(operations);
                    writers[count].writeAndNewLine(line);
                });

        // Close
        for (AsyncBufferedFileWriter writer : writers) {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        }
    }
}

class Line2Obj {
    @NotNull
    private static HashMap<Long, HashMap<Long, EnumMap<Piece, List<OriginalPiece>>>> getOriginalPieceMap(MinoShifter minoShifter, int maxHeight) {
        OriginalPieceFactory pieceFactory = new OriginalPieceFactory(maxHeight);
        Set<OriginalPiece> pieces = pieceFactory.create();

        HashMap<Long, HashMap<Long, EnumMap<Piece, List<OriginalPiece>>>> maps = new HashMap<>();

        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 10; x++) {
                Field field = FieldFactory.createMiddleField();
                field.setBlock(x, y);

                EnumMap<Piece, List<OriginalPiece>> enumMap = new EnumMap<>(Piece.class);
                for (Piece piece : Piece.valueList()) {
                    Set<Rotate> uniqueRotates = minoShifter.getUniqueRotates(piece);

                    List<OriginalPiece> piecesList = pieces.stream()
                            .filter(originalPiece -> originalPiece.getPiece() == piece)
                            .filter(originalPiece -> uniqueRotates.contains(originalPiece.getRotate()))
                            .filter(originalPiece -> !field.canPut(originalPiece))
                            .collect(Collectors.toList());

                    enumMap.put(piece, piecesList);
                }

                HashMap<Long, EnumMap<Piece, List<OriginalPiece>>> secondsMap = maps.computeIfAbsent(field.getBoard(0), (ignore) -> new HashMap<>());
                secondsMap.put(field.getBoard(1), enumMap);
            }
        }
        return maps;
    }

    @NotNull
    private static HashMap<Integer, List<Field>> getMaskFields(int maxHeight) {
        HashMap<Integer, List<Field>> maps = new HashMap<>();

        List<Integer> diff = Arrays.asList(-1, 1);

        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 10; x++) {
                ArrayList<Field> fields = new ArrayList<>();

                Field maskField = FieldFactory.createField(maxHeight);
                for (int dx : diff) {
                    int cx = x + dx;
                    if (cx < 0 || 10 <= cx) {
                        continue;
                    }

                    for (int dy : diff) {
                        int cy = y + dy;
                        if (cy < 0 || maxHeight <= cy) {
                            continue;
                        }

                        maskField.setBlock(cx, cy);
                    }
                }

                for (int dx : diff) {
                    int cx = x + dx;
                    if (cx < 0 || 10 <= cx) {
                        continue;
                    }

                    for (int dy : diff) {
                        int cy = y + dy;
                        if (cy < 0 || maxHeight <= cy) {
                            continue;
                        }

                        maskField.removeBlock(cx, cy);
                        fields.add(maskField.freeze());
                        maskField.setBlock(cx, cy);
                    }
                }

                maps.put(x + y * 10, fields);
            }
        }

        return maps;
    }

    private static final PieceCounter ALL_PIECE_WITHOUT_T = new PieceCounter(
            Piece.valueList().stream().filter(piece -> piece != Piece.T)
    );

    private final int maxHeight;
    private final MinoFactory minoFactory;
    private final HashMap<Long, HashMap<Long, EnumMap<Piece, List<OriginalPiece>>>> pieceMap;
    private final HashMap<Integer, List<Field>> maskFields;

    Line2Obj(MinoFactory minoFactory, MinoShifter minoShifter, int maxHeight) {
        this.maxHeight = maxHeight;
        this.minoFactory = minoFactory;
        this.pieceMap = getOriginalPieceMap(minoShifter, maxHeight);
        this.maskFields = getMaskFields(maxHeight);
    }

    Stream<Operations> search(List<Operation> operationList) {
        Field fieldWithoutT = toFieldWithoutT(operationList);

        // Tがない地形でライン消去が発生するとき
        int clearLine = fieldWithoutT.freeze().clearLine();
        if (0 < clearLine) {
            return Stream.empty();
        }

        // Tミノを取得
        // 必ずTが含まれていること
        Optional<? extends Operation> optional = operationList.stream()
                .filter(operation -> operation.getPiece() == Piece.T)
                .findFirst();
        assert optional.isPresent();
        Operation operationT = optional.get();

        // Tミノが入れば、Tスピンになる
        if (Commons.isTSpin(fieldWithoutT, operationT.getX(), operationT.getY())) {
            // 解
            return Stream.of(new Operations(operationList));
        }

        // 使っていないミノを置いてみてTスピンができないか探索
        return localSearch(operationList, operationT, fieldWithoutT);
    }

    private Field toFieldWithoutT(List<? extends Operation> operationList) {
        Field field = FieldFactory.createField(maxHeight);
        for (Operation operation : operationList) {
            if (operation.getPiece() == Piece.T) {
                continue;
            }

            Mino mino = minoFactory.create(operation.getPiece(), operation.getRotate());
            field.put(mino, operation.getX(), operation.getY());
        }
        return field;
    }

    private Stream<Operations> localSearch(List<Operation> operationList, Operation operationT, Field fieldWithoutT) {
        PieceCounter usingPieceCounter = new PieceCounter(
                operationList.stream().map(Operation::getPiece).filter(piece -> piece != Piece.T)
        );
        PieceCounter restPieceCounter = ALL_PIECE_WITHOUT_T.removeAndReturnNew(usingPieceCounter);

        if (restPieceCounter.getCounter() == 0L) {
            return Stream.empty();
        }

        int index = operationT.getX() + operationT.getY() * 10;
        return maskFields.get(index).stream()
                .flatMap(maskField -> {
                    Field needBlock = maskField.freeze();
                    needBlock.reduce(fieldWithoutT);

                    assert !needBlock.isPerfect();

                    Field field = LineCommons.toField(minoFactory, operationList, maxHeight);
                    return this.next(operationList, restPieceCounter, field, needBlock);
                });
    }

    private Stream<Operations> next(List<Operation> operationList, PieceCounter restPieceCounter, Field field, Field needBlock) {
        if (needBlock.isPerfect()) {
            return Stream.of(new Operations(operationList));
        }

        if (restPieceCounter.getCounter() == 0L) {
            return Stream.empty();
        }

        EnumMap<Piece, List<OriginalPiece>> map = getMap(needBlock);

        return restPieceCounter.getBlockStream()
                .flatMap(piece -> {
                    PieceCounter nextRestPieceCounter = restPieceCounter.removeAndReturnNew(new PieceCounter(Stream.of(piece)));
                    List<OriginalPiece> originalPieces = map.get(piece);
                    return originalPieces.stream()
                            .filter(originalPiece -> field.canMerge(originalPiece.getMinoField()))
                            .flatMap(originalPiece -> {
                                Field freeze = field.freeze();
                                freeze.put(originalPiece);

                                Field freezeNeedBlock = needBlock.freeze();
                                freezeNeedBlock.remove(originalPiece);

                                ArrayList<Operation> operations = new ArrayList<>(operationList);
                                operations.add(originalPiece);
                                return this.next(operations, nextRestPieceCounter, freeze, freezeNeedBlock);
                            });
                });
    }

    private EnumMap<Piece, List<OriginalPiece>> getMap(Field needBlock) {
        long lowBoard = needBlock.getBoard(0);

        if (lowBoard != 0L) {
            long bit = lowBoard & (-lowBoard);
            return this.pieceMap.get(bit).get(0L);
        }

        long highBoard = needBlock.getBoard(1);
        long bit = highBoard & (-highBoard);
        return this.pieceMap.get(0L).get(bit);
    }
}