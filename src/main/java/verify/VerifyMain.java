package verify;

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
    private static final List<List<Integer>> maxIndexesHoldEmpty = Arrays.asList(
            Arrays.asList(7, 4),
            Arrays.asList(6, 5),
            Arrays.asList(5, 6),
            Arrays.asList(4, 7),
            Arrays.asList(3, 7, 1),
            Arrays.asList(2, 7, 2),
            Arrays.asList(1, 7, 3)
    );

    private static final List<List<Integer>> maxIndexesUsingHold = Arrays.asList(
            Arrays.asList(1, 7, 3),
            Arrays.asList(1, 6, 4),
            Arrays.asList(1, 5, 5),
            Arrays.asList(1, 4, 6),
            Arrays.asList(1, 3, 7),
            Arrays.asList(1, 2, 7, 1),
            Arrays.asList(1, 1, 7, 2)
    );

    public static void main(String[] args) throws IOException, SyntaxException {
        System.out.println("Loading...");

        MinoFactory minoFactory = new MinoFactory();
        int fieldHeight = 4;
        Path indexPath = Paths.get("resources/index.csv");
        Map<Integer, IndexPiecePair> indexes = IndexPiecePairs.create(indexPath, minoFactory, fieldHeight);

        List<WithTetris> withTetrisList = Files.lines(Paths.get("resources/tetris_indexed_solutions_SRS7BAG.csv")).parallel()
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

//        run("1", "[]SZJLTOI SZJL");  // no solution
//        run("1", "[]JSZTLOI JTIS");  // empty hold
//        run("1", "[]JSZTLOI JTSI");  // exist soluiton

        System.out.println();

        VerifyMain main = new VerifyMain(minoFactory, fieldHeight, withTetrisList);

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

                for (Pieces pieces : candidates.subList(0, 100)) {
                    String pieceString = pieces.blockStream().map(Piece::getName).collect(Collectors.joining());
                    String sequence;
                    if (isFirstHoldEmpty) {
                        sequence = "[]" + pieceString;
                    } else {
                        sequence = "[" + pieceString.charAt(0) + "]" + pieceString.substring(1);
                    }
                    System.out.println(sequence);
                    main.run(Integer.toString(cycle), sequence);
                }
            }
        }

//        main.run("1", "[I]JSZTLOI JST");  // no solution
//        main.run("1", "[O]JSZTLOI JST");  // no solution
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
            throw new IllegalArgumentException("1st argument (pc cycle) should be integer (1 <= value <= 7)");
        }

        if (cycle < 1 || 7 < cycle) {
            throw new IllegalArgumentException("1st argument (pc cycle) should be 1 <= value <= 7: " + cycleString);
        }

        if (!sequence.startsWith("[")) {
            throw new IllegalArgumentException("2nd argument (Sequence) should start with '['");
        }

        if (!sequence.contains("]")) {
            throw new IllegalArgumentException("2nd argument (Sequence) should contain ']'");
        }

        Piece hold;
        List<Piece> pieces;
        if (sequence.startsWith("[]")) {
            hold = null;
            pieces = Stream.of(sequence.substring(2).split(""))
                    .map(StringEnumTransform::toPiece)
                    .collect(Collectors.toList());

            if (pieces.size() != 11) {
                throw new IllegalArgumentException("Length of sequence should be 11: " + pieces.size());
            }
        } else if (sequence.charAt(2) == ']') {
            hold = StringEnumTransform.toPiece(sequence.charAt(1));
            pieces = Stream.concat(
                    Stream.of(hold),
                    Stream.of(sequence.substring(3).split("")).map(StringEnumTransform::toPiece)
            ).collect(Collectors.toList());

            if (pieces.size() != 11) {
                throw new IllegalArgumentException("Length of sequence excluding hold piece should be 10: " + pieces.size());
            }
        } else {
            throw new IllegalArgumentException("Illegal sequence: " + sequence);
        }

        run(cycle, hold, pieces);
    }

    private void run(int cycle, Piece hold, List<Piece> initPieces) throws IOException {
        assert initPieces.size() == 11;

        boolean isFirstHoldEmpty = hold == null;
        List<List<Integer>> maxIndexesList = isFirstHoldEmpty ? maxIndexesHoldEmpty : maxIndexesUsingHold;
        List<Integer> maxIndexes = maxIndexesList.get(cycle - 1);
        String inputName = getFileName(maxIndexes, isFirstHoldEmpty);

        SolutionBinary binary = BinaryLoader.load(inputName);

        IndexParser indexParser = new IndexParser(maxIndexes);
        PieceNumberConverter converter = PieceNumberConverter.createDefaultConverter();
        PieceNumber[] numbers = initPieces.stream().map(converter::get).toArray(PieceNumber[]::new);

        int index = indexParser.parse(numbers);

        byte solution = binary.at(index);

        System.out.println("PC cycle number: " + cycle);
        System.out.println("Sequence: " + initPieces);
        System.out.println("Hold: " + hold);
        System.out.println();

        System.out.println("# from binary");
        System.out.println("File: " + inputName);
        System.out.println("Index: " + index);
        System.out.println("Solution: 0b" + toBinary(solution) + " [" + toString(converter, solution) + "]");
        System.out.println();

        VerifyData data = verify(hold, initPieces);

        System.out.println("# from index csv");

        System.out.println("hold empty: " + data.isHoldEmpty);
        System.out.println("hold pieces: " + data.holdCandidates);

        if (!data.sampleFumens.isEmpty()) {
            System.out.println("Sample: ");
        }
        for (String fumen : data.sampleFumens) {
            System.out.println(fumen);
        }

        // Verify
        if (solution == (byte) -2) {
            if (!data.isHoldEmpty && data.holdCandidates.isEmpty()) {
                System.out.println("*** Verified ***");
            } else {
                throw new IllegalStateException("Not verified: byte is -2, but solution is found");
            }
        } else if (solution == (byte) -1) {
            if (data.isHoldEmpty) {
                System.out.println("*** Verified ***");
            } else {
                throw new IllegalStateException("Not verified: byte is -1, but solution without hold is not found");
            }
        } else {
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

        boolean isHoldEmpty = false;
        if (noHold9Sequence != null) {
            List<Piece> noHold9Pieces = noHold9Sequence.getPieces();

            // ホールドなしで組める可能性がある
            PieceCounter noHold9PieceCounter = new PieceCounter(noHold9Pieces.stream());

            // ホールドなしでパフェできる
            isHoldEmpty = withTetrisList.parallelStream()
                    .filter(it -> noHold9PieceCounter.equals(it.get9PieceCounter()))
                    .anyMatch(it -> {
                        HarddropReachable reachable = harddropReachableThreadLocal.get();
                        return BuildUp.existsValidByOrder(initField, it.get9Operations().stream(), noHold9Pieces, fieldHeight, reachable);
                    });
        }

        LongPieces noHold10Sequence;
        if (hold != null) {
            // 入力にホールドあり
            noHold10Sequence = new LongPieces(initPieces.subList(1, 11));
        } else {
            // 入力にホールドなし
            noHold10Sequence = new LongPieces(initPieces.subList(0, 10));
        }

        // ホールドと同じ並びのものをひとつだけ取り除く
        // ホールドした結果、同じ並びになるものがあるかもしれないため、取り除くのはひとつだけにする
        List<LongPieces> collect = lookUp.parse(initPieces).map(LongPieces::new).collect(Collectors.toList());
        int firstNoHoldIndex = collect.indexOf(noHold10Sequence);
        assert 0 <= firstNoHoldIndex;
        collect.remove(firstNoHoldIndex);

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

        return new VerifyData(isHoldEmpty, pieceFlag.keySet(), samples);
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
            inputName = "resources/third/" + prefix + ".bin";
        } else {
            String prefix = maxIndexes.stream().map(Object::toString).collect(Collectors.joining());
            inputName = "resources/third/h" + prefix + ".bin";
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
    final boolean isHoldEmpty;
    final Set<Piece> holdCandidates;
    final List<String> sampleFumens;

    public VerifyData(boolean isHoldEmpty, Set<Piece> holdCandidates, List<String> sampleFumens) {
        this.isHoldEmpty = isHoldEmpty;
        this.holdCandidates = holdCandidates;
        this.sampleFumens = sampleFumens;
    }
}