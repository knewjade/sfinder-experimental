package main.second;

import common.order.CountReverseOrderLookUpStartsWithEmpty;
import utils.RangeChecker;
import utils.bin.SolutionByteBinary;
import utils.frame.Frames;
import utils.frame.FramesComparator;
import utils.index.IndexParser;
import utils.pieces.PieceNumber;
import utils.pieces.PieceNumberConverter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class HoldEmpty implements BinaryOutput {
    private final SolutionByteBinary outputBinary;
    private final IndexParser indexParser;
    private final CountReverseOrderLookUpStartsWithEmpty lookUp;
    private final RangeChecker rangeChecker;
    private final List<PieceNumber> allPieceNumbers;
    private final FramesComparator comparator;

    HoldEmpty(SolutionByteBinary outputBinary, PieceNumberConverter converter, List<Integer> maxIndexes, IndexParser indexParser, FramesComparator comparator) {
        this.outputBinary = outputBinary;
        this.indexParser = indexParser;
        this.comparator = comparator;
        this.lookUp = new CountReverseOrderLookUpStartsWithEmpty(10, 11);
        this.rangeChecker = new RangeChecker(maxIndexes);
        this.allPieceNumbers = PieceNumberConverter.PPT_PIECES.stream()
                .map(converter::get)
                .collect(Collectors.toList());
    }

    @Override
    public void output(PieceNumberStep pieceNumberStep) {
        List<PieceNumber> beforeHoldList = Arrays.asList(pieceNumberStep.getNumbers());
        int baseStepCount = pieceNumberStep.getStepCount();

        assert 0 < baseStepCount;
        assert beforeHoldList.size() == 10;

        lookUp.parse(beforeHoldList).forEach(pair -> {
            List<PieceNumber> afterHoldWithNull = pair.getList();

            assert afterHoldWithNull.size() == 11;

            int holdIndex = afterHoldWithNull.indexOf(null);

            assert 0 <= holdIndex;

            PieceNumber[] piecesAfterHold = new PieceNumber[11];
            afterHoldWithNull.toArray(piecesAfterHold);

            int holdCount = pair.getHoldCount();
            byte step = calc(baseStepCount, holdCount);

            if (holdCount == 0) {
                // ホールドが必要ないケース
                for (PieceNumber holdPieceNumber : allPieceNumbers) {
                    piecesAfterHold[holdIndex] = holdPieceNumber;

                    if (rangeChecker.check(piecesAfterHold)) {
                        int offset = 0;  // empty
                        int index = indexParser.parse(piecesAfterHold) * 8 + offset;
                        outputBinary.putIfSatisfy(index, step, comparator::shouldUpdate);
                    }
                }
            } else {
                // ホールドが必要になるケース
                for (PieceNumber holdPieceNumber : allPieceNumbers) {
                    piecesAfterHold[holdIndex] = holdPieceNumber;

                    if (rangeChecker.check(piecesAfterHold)) {
                        int offset = holdPieceNumber.getNumber() + 1;
                        int index = indexParser.parse(piecesAfterHold) * 8 + offset;
                        outputBinary.putIfSatisfy(index, step, comparator::shouldUpdate);
                    }
                }
            }
        });
    }

    private byte calc(int baseStepCount, int holdCount) {
        return Frames.possible(baseStepCount, holdCount);
    }

    @Override
    public SolutionByteBinary get() {
        return outputBinary;
    }
}