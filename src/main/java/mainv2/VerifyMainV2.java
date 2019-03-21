package mainv2;

import bin.*;
import bin.index.IndexParser;
import bin.pieces.PieceNumber;
import bin.pieces.PieceNumberConverter;
import common.SyntaxException;
import common.buildup.BuildUp;
import common.datastore.MinoOperationWithKey;
import common.datastore.Operation;
import common.datastore.Pair;
import common.datastore.PieceCounter;
import common.datastore.blocks.Pieces;
import common.parser.StringEnumTransform;
import common.pattern.LoadedPatternGenerator;
import common.tetfu.common.ColorConverter;
import concurrent.HarddropReachableThreadLocal;
import core.action.reachable.HarddropReachable;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.MinoFactory;
import core.mino.MinoShifter;
import core.mino.Piece;
import core.neighbor.SimpleOriginalPiece;
import core.srs.MinoRotation;
import core.srs.Rotate;
import entry.path.output.OneFumenParser;
import lib.Randoms;
import main.BinaryLoader;
import main.IndexPiecePair;
import main.IndexPiecePairs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VerifyMainV2 {
    private static final String CSV_DIRECTORY = "resources/csv/";
    private static final String BIN_DIRECTORY = "resources/mov/SRS7BAG/";

    private static final List<List<Integer>> maxIndexesHoldEmpty = Arrays.asList(
            Arrays.asList(7, 4),
            Arrays.asList(4, 7),
            Arrays.asList(1, 7, 3),
            Arrays.asList(5, 6),
            Arrays.asList(2, 7, 2),
            Arrays.asList(6, 5),
            Arrays.asList(3, 7, 1)
    );

    private static final List<List<Integer>> maxIndexesUsingHold = Arrays.asList(
            Arrays.asList(1, 6, 4),
            Arrays.asList(1, 3, 7),
            Arrays.asList(1, 7, 3),
            Arrays.asList(1, 4, 6),
            Arrays.asList(1, 1, 7, 2),
            Arrays.asList(1, 5, 5),
            Arrays.asList(1, 2, 7, 1)
    );

    public static void main(String[] args) throws IOException, SyntaxException {
        verifyAuto();
//        verifyManual();
    }

    private static void verifyManual() throws IOException {
        VerifyMainV2 main = createMain();

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.println("# Input '<pc-number> <sequence>' or 'exit'");

                if (!scanner.hasNextLine()) {
                    return;
                }

                String line = scanner.nextLine();
                if ("exit".equals(line)) {
                    return;
                }

                String[] split = line.split(" ");
                if (split.length != 2) {
                    System.err.println("ERROR: Num of arguments should be 2");
                    continue;
                }

                try {
                    main.run(split[0], split[1]);
                } catch (Exception e) {
                    System.err.println("ERROR: " + e.getMessage());
                }
            }
        }

//        run("1", "[]SZJLTOI SZJL");  // no solution
//        run("1", "[]JSZTLOI JTIS");  // empty hold
//        run("1", "[]JSZTLOI JTSI");  // exist soluiton

//        main.run("1", "[I]JSZTLOI JST");  // no solution
//        main.run("1", "[O]JSZTLOI JST");  // no solution
    }

    private static void verifyAuto() throws IOException, SyntaxException {
        VerifyMainV2 main = createMain();

        Randoms randoms = new Randoms();

        for (boolean isFirstHoldEmpty : Arrays.asList(true, false)) {
            for (int cycle = 1; cycle <= 7; cycle++) {
                List<Integer> maxIndexes = (isFirstHoldEmpty ? maxIndexesHoldEmpty : maxIndexesUsingHold).get(cycle - 1);
                String patterns = maxIndexes.stream().map(it -> "*p" + it).collect(Collectors.joining(","));
                System.out.println(patterns);

                // 候補のミノのからランダムに選択
                LoadedPatternGenerator generator = new LoadedPatternGenerator(patterns);
                List<Pieces> candidates = generator.blocksStream().filter(it -> randoms.nextBoolean(0.0005)).collect(Collectors.toList());
                System.out.println(candidates.size());
                Collections.shuffle(candidates);

                // 実行
                for (Pieces pieces : candidates.subList(0, 200)) {
                    String pieceString = pieces.blockStream().map(Piece::getName).collect(Collectors.joining());
                    String sequence;
                    if (isFirstHoldEmpty) {
                        sequence = "[]" + pieceString;
                    } else {
                        sequence = "[" + pieceString.charAt(0) + "]" + pieceString.substring(1);
                    }

                    System.out.println("### " + sequence + " ###");
                    main.run(Integer.toString(cycle), sequence);
                }
            }
        }
    }

    private static VerifyMainV2 createMain() throws IOException {
        System.out.println("Loading...");

        MinoFactory minoFactory = new MinoFactory();
        int fieldHeight = 4;
        Path indexPath = Paths.get(CSV_DIRECTORY + "index.csv");
        Map<Integer, IndexPiecePair> indexes = IndexPiecePairs.create(indexPath, minoFactory, fieldHeight);

        MinoRotation minoRotation = new MinoRotation();
        MinoShifter minoShifter = new MinoShifter();
        Movement movement = new Movement(minoFactory, minoRotation, minoShifter);

        List<WithTetris> withTetrisList = Files.lines(Paths.get(CSV_DIRECTORY + "tetris_indexed_solutions_SRS7BAG.csv")).parallel()
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

                        return new WithTetris(operations, iPiece, movement);
                    });
                })
                .collect(Collectors.toList());

        System.out.println();

        return new VerifyMainV2(minoFactory, fieldHeight, withTetrisList);
    }

    private final int fieldHeight;
    private final List<WithTetris> withTetrisList;
    private final HarddropReachableThreadLocal harddropReachableThreadLocal;
    private final OneFumenParser fumenParser;
    private final MovementComparator comparator;

    private VerifyMainV2(MinoFactory minoFactory, int fieldHeight, List<WithTetris> withTetrisList) {
        this.fieldHeight = fieldHeight;
        this.withTetrisList = withTetrisList;
        this.harddropReachableThreadLocal = new HarddropReachableThreadLocal(fieldHeight);
        this.fumenParser = new OneFumenParser(minoFactory, new ColorConverter());
        this.comparator = new MovementComparator();
    }

    private void run(String cycleString, String sequence) throws IOException {
        // 初期化 & 入力値チェック
        sequence = sequence.replace(" ", "");

        int cycle;
        try {
            cycle = Integer.valueOf(cycleString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("1st argument (pc number) should be integer (1 <= value <= 7)");
        }

        if (cycle < 1 || 7 < cycle) {
            throw new IllegalArgumentException("1st argument (pc number) should be 1 <= value <= 7: " + cycleString);
        }

        if (!sequence.startsWith("[")) {
            throw new IllegalArgumentException("2nd argument (Sequence) should start with '['");
        }

        if (!sequence.contains("]")) {
            throw new IllegalArgumentException("2nd argument (Sequence) should contain ']'");
        }

        Piece hold;
        List<Piece> allPieces;
        if (sequence.startsWith("[]")) {
            hold = null;
            allPieces = Stream.of(sequence.substring(2).split(""))
                    .map(StringEnumTransform::toPiece)
                    .collect(Collectors.toList());

            if (allPieces.size() != 11) {
                throw new IllegalArgumentException("Length of sequence should be 11: " + allPieces.size());
            }
        } else if (sequence.charAt(2) == ']') {
            hold = StringEnumTransform.toPiece(sequence.charAt(1));
            allPieces = Stream.concat(
                    Stream.of(hold),
                    Stream.of(sequence.substring(3).split("")).map(StringEnumTransform::toPiece)
            ).collect(Collectors.toList());

            if (allPieces.size() != 11) {
                throw new IllegalArgumentException("Length of sequence excluding hold piece should be 10: " + allPieces.size());
            }
        } else {
            throw new IllegalArgumentException("Illegal sequence: " + sequence);
        }

        run(cycle, hold, allPieces);
    }

    private void run(int cycle, Piece hold, List<Piece> initAllPieces) throws IOException {
        assert initAllPieces.size() == 11;

        // 初期化
        boolean isFirstHoldEmpty = hold == null;
        List<List<Integer>> maxIndexesList = isFirstHoldEmpty ? maxIndexesHoldEmpty : maxIndexesUsingHold;
        List<Integer> maxIndexes = maxIndexesList.get(cycle - 1);

        PieceNumberConverter converter = PieceNumberConverter.createDefaultConverter();
        PieceNumber[] numbers = initAllPieces.stream().map(converter::get).toArray(PieceNumber[]::new);

        // 指定PCサイクルの範囲内かチェック
        RangeChecker rangeChecker = new RangeChecker(maxIndexes);
        if (!rangeChecker.check(numbers)) {
            throw new IllegalArgumentException("The sequence is out of PC cycle range");
        }

        // インデックスの計算
        IndexParser indexParser = new IndexParser(maxIndexes);
        int index = indexParser.parse(numbers);

        // バイナリをロード
        String inputName = getFileName(maxIndexes, isFirstHoldEmpty);
        SolutionShortBinary binary = BinaryLoader.loadShortBinary(inputName);

        System.out.println("PC number: " + cycle);
        System.out.println("Sequence: " + initAllPieces);
        System.out.println("Hold: " + hold);
        System.out.println();

        System.out.println("# from binary");
        System.out.println("File: " + inputName);
        System.out.println("Index: " + index);

        System.out.println("Solutions:");
        for (int pieceNumber = 0; pieceNumber < 8; pieceNumber++) {
            System.out.print("  ");
            System.out.print(pieceNumber != 0 ? converter.get(pieceNumber - 1).getPiece().getName() : "Empty");
            System.out.print(" -> ");

            short solution = binary.at(index * 8 + pieceNumber);
            System.out.println("0b" + toBinary(solution) + " [" + toString(solution) + "]");
        }

        // csvから探索
        Stream.Builder<Order> builder = Stream.builder();
        if (isFirstHoldEmpty) {
            g(builder, initAllPieces, null, 0, new LinkedList<>(), 0);
        } else {
            g(builder, initAllPieces.subList(1, initAllPieces.size()), initAllPieces.get(0), 0, new LinkedList<>(), 0);
        }

        Map<Integer, FinalResult> results = new HashMap<>();
        builder.build().sequential()
                .filter(order -> order.getPieces().get(9) == Piece.I)
                .forEach(order -> {
                    int holdCount = order.getHoldCount();

                    List<Piece> pieces1 = order.getPieces().subList(0, 9);
                    assert pieces1.size() == 9;

                    PieceCounter current9PieceCounter = new PieceCounter(pieces1);

                    Optional<WithTetris> best = withTetrisList.stream()
                            .filter(it -> current9PieceCounter.equals(it.get9PieceCounter()))
                            .filter(it -> {
                                Field initField = FieldFactory.createField(fieldHeight);
                                HarddropReachable reachable = harddropReachableThreadLocal.get();
                                return BuildUp.existsValidByOrder(initField, it.get9Operations().stream(), pieces1, fieldHeight, reachable);
                            })
                            .min((a, b) -> {
                                short step1 = a.step(holdCount);
                                short step2 = b.step(holdCount);
                                return comparator.compare(step1, step2);
                            });

                    best.ifPresent(withTetris -> {
                        Piece hold2 = order.getHold();
                        short newStep = withTetris.step(order.getHoldCount());
                        int holdNumber = hold2 != null ? converter.get(hold2).getNumber() + 1 : 0;

                        FinalResult pair = results.get(holdNumber);
                        if (pair == null || comparator.shouldUpdate(pair.getStep(), newStep)) {
                            Field initField = FieldFactory.createField(fieldHeight);
                            String fumen = fumenParser.parse(withTetris.getAllOperations(), initField, fieldHeight, "");

                            results.put(holdNumber, new FinalResult(order, newStep, "http://harddrop.com/fumen/?v115@" + fumen));
                        }
                    });
                });

        System.out.println();

        System.out.println("# from index csv");
        for (int pieceNumber = 0; pieceNumber < 8; pieceNumber++) {
            System.out.print("  ");
            System.out.print(pieceNumber != 0 ? converter.get(pieceNumber - 1).getPiece().getName() : "Empty");
            System.out.print(" -> ");

            if (results.containsKey(pieceNumber)) {
                FinalResult result = results.get(pieceNumber);
                System.out.print(toBinary(result.getStep()));
                System.out.print(" / ");
                System.out.print(result.getFumen());
                System.out.println();
            } else {
                System.out.println("no solution");
            }
        }

        // Verify
        for (int pieceNumber = 0; pieceNumber < 8; pieceNumber++) {
            short binSolution = binary.at(index * 8 + pieceNumber);

            short csvSolution;
            if (results.containsKey(pieceNumber)) {
                FinalResult result = results.get(pieceNumber);
                csvSolution = result.getStep();
            } else {
                csvSolution = Movements.impossible();
            }

            if (binSolution != csvSolution) {
                throw new IllegalStateException("Not verified: solutions are not same");
            }
        }

        System.out.println();
    }

    private void g(Stream.Builder<Order> builder, List<Piece> pieces, Piece hold, int depth, LinkedList<Piece> fixed, int holdCount) {
        if (fixed.size() == 10) {
            // 確定
            builder.accept(new Order(new ArrayList<>(fixed), hold, holdCount));
            return;
        }

        // そのままピースをおく
        Piece piece = pieces.get(depth);
        fixed.addLast(piece);
        g(builder, pieces, hold, depth + 1, fixed, holdCount);
        fixed.removeLast();

        // ホールドする
        if (hold == null) {
            // 空のときは2つめをおく
            Piece nextPiece = pieces.get(depth + 1);
            fixed.addLast(nextPiece);
            g(builder, pieces, piece, depth + 2, fixed, holdCount + 1);
            fixed.removeLast();
        } else {
            // 交換しておく
            fixed.addLast(hold);
            g(builder, pieces, piece, depth + 1, fixed, holdCount + 1);
            fixed.removeLast();
        }
    }

    private String toBinary(short value) {
        StringBuilder str = new StringBuilder();
        for (int index = 15; 0 <= index; index--) {
            str.append((value >> index) & 1L);
        }
        return str.toString();
    }

    private String toString(short value) {
        if (value == (short) 0) {
            return "no solution";
        }

        int move = (value & 0b111111_00000000) >> 8;
        int rotate = (value & 0b11110000) >> 4;
        int hold = value & 0b1111;

        return String.format("move=%d, rotate=%d, hold=%d", move, rotate, hold);
    }

    private String getFileName(List<Integer> maxIndexes, boolean isFirstHoldEmpty) {
        String inputName;
        if (isFirstHoldEmpty) {
            String prefix = maxIndexes.stream().map(Object::toString).collect(Collectors.joining());
            inputName = BIN_DIRECTORY + prefix + "_mov.bin";
        } else {
            String prefix = maxIndexes.stream().map(Object::toString).collect(Collectors.joining());
            inputName = BIN_DIRECTORY + "h" + prefix + "_mov.bin";
        }
        return inputName;
    }
}

class WithTetris {
    private final List<MinoOperationWithKey> allOperations;
    private final List<MinoOperationWithKey> operations9;
    private final IndexPiecePair last;
    private final PieceCounter pieceCounter9;
    private final short step;

    WithTetris(List<IndexPiecePair> operations, IndexPiecePair last, Movement movement) {
        this.last = last;

        List<MinoOperationWithKey> operations9 = operations.stream()
                .map(IndexPiecePair::getSimpleOriginalPiece)
                .collect(Collectors.toList());
        this.operations9 = operations9;

        List<MinoOperationWithKey> allOperations = Stream.concat(operations.stream(), Stream.of(last))
                .map(IndexPiecePair::getSimpleOriginalPiece)
                .collect(Collectors.toList());
        this.allOperations = allOperations;

        this.pieceCounter9 = new PieceCounter(operations9.stream().map(Operation::getPiece));

        this.step = Movements.calcMinStep(movement, allOperations);
    }

    PieceCounter get9PieceCounter() {
        return pieceCounter9;
    }

    List<MinoOperationWithKey> getAllOperations() {
        return allOperations;
    }

    List<MinoOperationWithKey> get9Operations() {
        return operations9;
    }

    short step(int holdCount) {
        return Movements.possible(step, holdCount);
    }
}

class VerifyData {
    private final Map<Integer, Pair<Order, Short>> results;

    VerifyData(Map<Integer, Pair<Order, Short>> results) {
        this.results = results;
    }
}

class Order {
    private final List<Piece> pieces;
    private final Piece hold;
    private final int holdCount;

    Order(List<Piece> pieces, Piece hold, int holdCount) {
        this.pieces = pieces;
        this.hold = hold;
        this.holdCount = holdCount;
    }

    public List<Piece> getPieces() {
        return pieces;
    }

    public Piece getHold() {
        return hold;
    }

    public int getHoldCount() {
        return holdCount;
    }
}

class FinalResult {
    private final short step;
    private final String fumen;

    FinalResult(Order order, short step, String fumen) {
        this.step = step;
        this.fumen = fumen;
    }

    short getStep() {
        return step;
    }

    String getFumen() {
        return fumen;
    }
}