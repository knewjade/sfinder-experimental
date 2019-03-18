package verify;

import bin.RangeChecker;
import bin.SolutionBinary;
import bin.index.IndexParser;
import bin.pieces.PieceNumber;
import bin.pieces.PieceNumberConverter;
import common.SyntaxException;
import common.buildup.BuildUp;
import common.datastore.MinoOperationWithKey;
import common.datastore.Operation;
import common.datastore.PieceCounter;
import common.datastore.blocks.LongPieces;
import common.datastore.blocks.Pieces;
import common.order.ForwardOrderLookUp;
import common.parser.StringEnumTransform;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VerifyMain {
    private static final String CSV_DIRECTORY = "resources/csv/";
    private static final String BIN_DIRECTORY = "resources/bin/";

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
        VerifyMain main = createMain();

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
        VerifyMain main = createMain();

        Randoms randoms = new Randoms();

        for (boolean isFirstHoldEmpty : Arrays.asList(true, false)) {
            for (int cycle = 1; cycle <= 7; cycle++) {
                List<Integer> maxIndexes = (isFirstHoldEmpty ? maxIndexesHoldEmpty : maxIndexesUsingHold).get(cycle - 1);
                String patterns = maxIndexes.stream().map(it -> "*p" + it).collect(Collectors.joining(","));
                System.out.println(patterns);

                LoadedPatternGenerator generator = new LoadedPatternGenerator(patterns);
                List<Pieces> candidates = generator.blocksStream().filter(it -> randoms.nextBoolean(0.0005)).collect(Collectors.toList());
                System.out.println(candidates.size());
                Collections.shuffle(candidates);

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

    private static VerifyMain createMain() throws IOException {
        System.out.println("Loading...");

        MinoFactory minoFactory = new MinoFactory();
        int fieldHeight = 4;
        Path indexPath = Paths.get(CSV_DIRECTORY + "index.csv");
        Map<Integer, IndexPiecePair> indexes = IndexPiecePairs.create(indexPath, minoFactory, fieldHeight);

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

                        return new WithTetris(operations, iPiece);
                    });
                })
                .collect(Collectors.toList());

        System.out.println();

        return new VerifyMain(minoFactory, fieldHeight, withTetrisList);
    }

    private final int fieldHeight;
    private final List<WithTetris> withTetrisList;
    private final HarddropReachableThreadLocal harddropReachableThreadLocal;
    private final ForwardOrderLookUp lookUp;
    private final OneFumenParser fumenParser;

    private VerifyMain(MinoFactory minoFactory, int fieldHeight, List<WithTetris> withTetrisList) {
        this.fieldHeight = fieldHeight;
        this.withTetrisList = withTetrisList;
        this.harddropReachableThreadLocal = new HarddropReachableThreadLocal(fieldHeight);
        this.lookUp = new ForwardOrderLookUp(10, 11);
        this.fumenParser = new OneFumenParser(minoFactory, new ColorConverter());
    }

    private void run(String cycleString, String sequence) throws IOException {
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

        boolean isFirstHoldEmpty = hold == null;
        List<List<Integer>> maxIndexesList = isFirstHoldEmpty ? maxIndexesHoldEmpty : maxIndexesUsingHold;
        List<Integer> maxIndexes = maxIndexesList.get(cycle - 1);

        PieceNumberConverter converter = PieceNumberConverter.createDefaultConverter();
        PieceNumber[] numbers = initAllPieces.stream().map(converter::get).toArray(PieceNumber[]::new);

        RangeChecker rangeChecker = new RangeChecker(maxIndexes);
        if (!rangeChecker.check(numbers)) {
            throw new IllegalArgumentException("The sequence is out of PC cycle range");
        }

        IndexParser indexParser = new IndexParser(maxIndexes);
        int index = indexParser.parse(numbers);

        String inputName = getFileName(maxIndexes, isFirstHoldEmpty);
        SolutionBinary binary = BinaryLoader.load(inputName);
        byte solution = binary.at(index);

        System.out.println("PC number: " + cycle);
        System.out.println("Sequence: " + initAllPieces);
        System.out.println("Hold: " + hold);
        System.out.println();

        System.out.println("# from binary");
        System.out.println("File: " + inputName);
        System.out.println("Index: " + index);
        System.out.println("Solution: 0b" + toBinary(solution) + " [" + toString(converter, solution) + "]");
        System.out.println();

        VerifyData data = verify(hold, initAllPieces);

        System.out.println("# from index csv");

        System.out.println("hold empty: " + (data.isHoldEmpty != null));
        System.out.println("hold pieces: " + data.holdCandidates);

        if (data.isHoldEmpty != null || !data.sampleFumens.isEmpty()) {
            System.out.println("Sample: ");
        }
        if (data.isHoldEmpty != null) {
            System.out.println(data.isHoldEmpty);
        }
        for (String fumen : data.sampleFumens) {
            System.out.println(fumen);
        }

        // Verify
        if (solution == (byte) -2) {
            // no solution
            if (data.isHoldEmpty == null && data.holdCandidates.isEmpty()) {
                System.out.println("*** Verified ***");
            } else {
                throw new IllegalStateException("Not verified: byte is -2, but solution is found");
            }
        } else if (solution == (byte) -1) {
            // empty
            if (data.isHoldEmpty != null) {
                System.out.println("*** Verified ***");
            } else {
                throw new IllegalStateException("Not verified: byte is -1, but solution without hold is not found");
            }
        } else {
            // any piece
            byte expected = 0;
            for (Piece piece : data.holdCandidates) {
                expected |= converter.get(piece).getBitByte();
            }
            if (solution == expected) {
                System.out.println("*** Verified ***");
            } else {
                throw new IllegalStateException("Not verified: byte or solution to verify is wrong");
            }
        }

        System.out.println();
    }

    private VerifyData verify(Piece hold, List<Piece> initPieces) {
        Field initField = FieldFactory.createField(fieldHeight);

        LongPieces noHold9Sequence = null;
        if (hold == null) {
            // 入力にホールドなし
            if (initPieces.get(9) == Piece.I) {
                noHold9Sequence = new LongPieces(initPieces.subList(0, 9));
            }
        }

        Optional<String> isHoldEmpty = Optional.empty();
        if (noHold9Sequence != null) {
            List<Piece> noHold9Pieces = noHold9Sequence.getPieces();

            // ホールドなしで組める可能性がある
            PieceCounter noHold9PieceCounter = new PieceCounter(noHold9Pieces.stream());

            // ホールドなしでパフェできる
            Optional<WithTetris> optional = withTetrisList.parallelStream()
                    .filter(it -> noHold9PieceCounter.equals(it.get9PieceCounter()))
                    .filter(it -> {
                        HarddropReachable reachable = harddropReachableThreadLocal.get();
                        return BuildUp.existsValidByOrder(initField, it.get9Operations().stream(), noHold9Pieces, fieldHeight, reachable);
                    })
                    .findAny();

            isHoldEmpty = optional.map(withTetris -> {
                String fumen = fumenParser.parse(withTetris.getAllOperations(), initField, fieldHeight, "");
                return "Empty: http://harddrop.com/fumen/?v115@" + fumen;
            });
        }

        List<LongPieces> collect = lookUp.parse(initPieces).map(LongPieces::new).collect(Collectors.toList());

        if (hold == null) {
            // 入力にホールドなし
            LongPieces noHold10Sequence = new LongPieces(initPieces.subList(0, 10));

            // ホールドせずにおくミノ順（同じ並びのもの）をひとつだけ取り除く
            // ホールドした結果、同じ並びになるものがあるかもしれないため、ひとつだけ取り除く
            int firstNoHoldIndex = collect.indexOf(noHold10Sequence);
            assert 0 <= firstNoHoldIndex;
            collect.remove(firstNoHoldIndex);
        }

        HashSet<LongPieces> piecesWithHold = collect.stream()
                .filter(it -> {
                    // 10番目が必ずIミノになる形のみ抽出
                    return it.getPieces().get(9) == Piece.I;
                })
                .collect(Collectors.toCollection(HashSet::new));

        Map<Piece, String> pieceFlag = new ConcurrentHashMap<>();

        PieceCounter initPieceCounter = new PieceCounter(initPieces.stream());
        PieceCounter singlePieceCounter = PieceCounter.getSinglePieceCounter(Piece.I);
        piecesWithHold.parallelStream().forEach(it2 -> {
            List<Piece> pieces1 = it2.getPieces().subList(0, 9);
            assert pieces1.size() == 9;
            PieceCounter current9PieceCounter = new PieceCounter(pieces1);
            Optional<WithTetris> exists = withTetrisList.stream()
                    .filter(it -> current9PieceCounter.equals(it.get9PieceCounter()))
                    .filter(it -> {
                        HarddropReachable reachable = harddropReachableThreadLocal.get();
                        return BuildUp.existsValidByOrder(initField, it.get9Operations().stream(), pieces1, fieldHeight, reachable);
                    })
                    .findAny();

            exists.ifPresent(withTetris -> {
                PieceCounter x = initPieceCounter.removeAndReturnNew(current9PieceCounter);
                assert x.getBlocks().size() == 2;
                assert x.containsAll(singlePieceCounter);
                PieceCounter last = x.removeAndReturnNew(singlePieceCounter);
                List<Piece> blocks = last.getBlocks();
                assert blocks.size() == 1;

                String fumen = fumenParser.parse(withTetris.getAllOperations(), initField, fieldHeight, "");

                pieceFlag.put(blocks.get(0), "http://harddrop.com/fumen/?v115@" + fumen);
            });
        });

        List<String> samples = pieceFlag.entrySet().stream()
                .map(entry -> (
                        entry.getKey() + ": " + entry.getValue()
                ))
                .collect(Collectors.toList());

        return new VerifyData(isHoldEmpty.orElse(null), pieceFlag.keySet(), samples);
    }

    private String toBinary(byte value) {
        StringBuilder str = new StringBuilder();
        for (int index = 7; 0 <= index; index--) {
            str.append((value >> index) & 1L);
        }
        return str.toString();
    }

    private String toString(PieceNumberConverter converter, byte value) {
        if (value == (byte) -2) {
            return "no solution";
        }

        if (value == (byte) -1) {
            return "empty hold";
        }

        assert 0 < value : value;

        StringBuilder str = new StringBuilder();
        for (Piece piece : PieceNumberConverter.PPT_PIECES) {
            if ((value & converter.get(piece).getBitByte()) != 0) {
                str.append(piece.getName());
            }
        }
        return str.toString();
    }

    private String getFileName(List<Integer> maxIndexes, boolean isFirstHoldEmpty) {
        String inputName;
        if (isFirstHoldEmpty) {
            String prefix = maxIndexes.stream().map(Object::toString).collect(Collectors.joining());
            inputName = BIN_DIRECTORY + prefix + ".bin";
        } else {
            String prefix = maxIndexes.stream().map(Object::toString).collect(Collectors.joining());
            inputName = BIN_DIRECTORY + "h" + prefix + ".bin";
        }
        return inputName;
    }
}

class WithTetris {
    private final List<MinoOperationWithKey> allOperations;
    private final List<MinoOperationWithKey> operations9;
    private final IndexPiecePair last;
    private final PieceCounter pieceCounter9;

    WithTetris(List<IndexPiecePair> operations, IndexPiecePair last) {
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
}

class VerifyData {
    final String isHoldEmpty;
    final Set<Piece> holdCandidates;
    final List<String> sampleFumens;

    VerifyData(String isHoldEmpty, Set<Piece> holdCandidates, List<String> sampleFumens) {
        this.isHoldEmpty = isHoldEmpty;
        this.holdCandidates = holdCandidates;
        this.sampleFumens = sampleFumens;
    }
}