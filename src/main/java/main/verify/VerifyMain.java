package main.verify;

import common.SyntaxException;
import common.buildup.BuildUp;
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
import utils.Movement;
import utils.RangeChecker;
import utils.frame.Frames;
import utils.frame.FramesComparator;
import utils.index.IndexParser;
import utils.index.IndexPiecePair;
import utils.index.IndexPiecePairs;
import utils.pieces.PieceNumber;
import utils.pieces.PieceNumberConverter;
import utils.step.Step;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VerifyMain {
    private static final String CSV_DIRECTORY = "csv/";
    private static final String BIN_DIRECTORY = "bin/";

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
//        verifyAuto();
        verifyManual();
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

                if ("".equals(line)) {
                    continue;
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

    private static String toPath(String path) {
        String jarPath = System.getProperty("java.class.path");
        String dirPath = jarPath.substring(0, jarPath.lastIndexOf(File.separator) + 1);
        return dirPath + "/" + path;
    }

    private static VerifyMain createMain() throws IOException {
        System.out.println("Loading...");


        MinoFactory minoFactory = new MinoFactory();
        int fieldHeight = 4;
        Path indexPath = Paths.get(toPath(CSV_DIRECTORY + "index.csv"));
        Map<Integer, IndexPiecePair> indexes = IndexPiecePairs.create(indexPath, minoFactory, fieldHeight);

        MinoRotation minoRotation = MinoRotation.create();
        MinoShifter minoShifter = new MinoShifter();
        Movement movement = new Movement(minoFactory, minoRotation, minoShifter);
        Step step = Step.create(movement);

        List<WithTetris> withTetrisList = Files.lines(Paths.get(toPath(CSV_DIRECTORY + "tetris_indexed_solutions_SRS7BAG.csv"))).parallel()
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

                        return new WithTetris(operations, iPiece, step);
                    });
                })
                .collect(Collectors.toList());

        System.out.println();

        return new VerifyMain(minoFactory, fieldHeight, withTetrisList);
    }

    private final int fieldHeight;
    private final List<WithTetris> withTetrisList;
    private final HarddropReachableThreadLocal harddropReachableThreadLocal;
    private final OneFumenParser fumenParser;
    private final FramesComparator comparator;

    private VerifyMain(MinoFactory minoFactory, int fieldHeight, List<WithTetris> withTetrisList) {
        this.fieldHeight = fieldHeight;
        this.withTetrisList = withTetrisList;
        this.harddropReachableThreadLocal = new HarddropReachableThreadLocal(fieldHeight);
        this.fumenParser = new OneFumenParser(minoFactory, new ColorConverter());
        this.comparator = new FramesComparator();
    }

    private void run(String cycleString, String sequence) throws IOException {
        // 初期化 & 入力値チェック
        sequence = sequence.replace(" ", "");

        int cycle;
        try {
            cycle = Integer.parseInt(cycleString);
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
        System.out.println(inputName);

        System.out.println("PC number: " + cycle);
        System.out.println("Sequence: " + initAllPieces);
        System.out.println("Hold: " + hold);
        System.out.println();

        System.out.println("# from binary");
        System.out.println("File: " + inputName);
        System.out.println("Index: " + index);

        System.out.println("Solutions:");
        {
            try (DataInputStream dataInStream = new DataInputStream(new BufferedInputStream(new FileInputStream(inputName)))) {
                dataInStream.skipBytes(index * 8);
                for (int pieceNumber = 0; pieceNumber < 8; pieceNumber++) {
                    System.out.print("  ");
                    System.out.print(pieceNumber != 0 ? converter.get(pieceNumber - 1).getPiece().getName() : "Empty");
                    System.out.print(" -> ");

                    // exists
                    {
                        byte value = dataInStream.readByte();
                        System.out.printf("0b%s ", toBinary(value));

                        int frameCount = Frames.getFrameCount(value);
                        System.out.printf("frame=%d", frameCount);
                    }

                    System.out.println();
                }
                dataInStream.close();
            }
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
                                byte step1 = a.frame(holdCount);
                                byte step2 = b.frame(holdCount);
                                return comparator.compare(step1, step2);
                            });

                    best.ifPresent(withTetris -> {
                        Piece hold2 = order.getHold();
                        byte newFrame = withTetris.frame(order.getHoldCount());
                        int holdNumber = hold2 != null ? converter.get(hold2).getNumber() + 1 : 0;

                        FinalResult pair = results.get(holdNumber);
                        if (pair == null || comparator.shouldUpdate(pair.getFrame(), newFrame)) {
                            Field initField = FieldFactory.createField(fieldHeight);
                            String fumen = fumenParser.parse(withTetris.getAllOperations(), initField, fieldHeight, "");

                            results.put(holdNumber, new FinalResult(order, newFrame, "http://harddrop.com/fumen/?v115@" + fumen));
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
                System.out.print(toBinary(result.getFrame()));
                System.out.print(" / ");
                System.out.print(result.getFumen());
                System.out.println();
            } else {
                System.out.println("no solution");
            }
        }

//        // Verify
//        for (int pieceNumber = 0; pieceNumber < 8; pieceNumber++) {
//            short binSolution = binary.at(index * 8 + pieceNumber);
//
//            short csvSolution;
//            if (results.containsKey(pieceNumber)) {
//                FinalResult result = results.get(pieceNumber);
//                csvSolution = result.getStep();
//            } else {
//                csvSolution = Movements.impossible();
//            }
//
//            if (binSolution != csvSolution) {
//                throw new IllegalStateException("Not verified: solutions are not same");
//            }
//        }

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
            str.append((value >> index) & 1);
        }
        return str.toString();
    }

    private String toBinary(byte value) {
        StringBuilder str = new StringBuilder();
        for (int index = 7; 0 <= index; index--) {
            str.append((value >> index) & 1);
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
            inputName = toPath(BIN_DIRECTORY + prefix + "_mov.bin");
        } else {
            String prefix = maxIndexes.stream().map(Object::toString).collect(Collectors.joining());
            inputName = toPath(BIN_DIRECTORY + "h" + prefix + "_mov.bin");
        }
        return inputName;
    }
}





