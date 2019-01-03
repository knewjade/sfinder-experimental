package line.commons;

import commons.RotateReachableThreadLocal;
import concurrent.LockedReachableThreadLocal;
import core.action.reachable.LockedReachable;
import core.action.reachable.RotateReachable;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.MinoFactory;
import core.mino.MinoShifter;
import core.mino.Piece;
import core.neighbor.OriginalPiece;
import core.neighbor.OriginalPieceFactory;
import core.srs.MinoRotation;
import core.srs.Rotate;

import java.util.*;
import java.util.stream.Collectors;

public class FactoryPool {
    private final int maxHeight;
    private final MinoFactory minoFactory;
    private final MinoShifter minoShifter;
    private final MinoRotation minoRotation;
    private final LockedReachableThreadLocal lockedReachableThreadLocal;
    private final RotateReachableThreadLocal rotateReachableThreadLocal;

    public FactoryPool(int maxHeight) {
        this.maxHeight = maxHeight;
        this.minoFactory = new MinoFactory();
        this.minoShifter = new MinoShifter();
        this.minoRotation = new MinoRotation();
        this.lockedReachableThreadLocal = new LockedReachableThreadLocal(minoFactory, minoShifter, minoRotation, maxHeight);
        this.rotateReachableThreadLocal = new RotateReachableThreadLocal(minoFactory, minoShifter, minoRotation, maxHeight);
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public MinoFactory getMinoFactory() {
        return minoFactory;
    }

    public MinoShifter getMinoShifter() {
        return minoShifter;
    }

    public RotateReachable createRotateReachable() {
        return rotateReachableThreadLocal.get();
    }

    public LockedReachable createLockedReachable() {
        return lockedReachableThreadLocal.get();
    }

    // 指定したブロックを埋めるすべてのミノを取得する
    // Long=埋めたい1ブロック -> Piece=置くミノの種類 -> List=指定したブロックを埋めるミノ一覧
    // フィールドの高さは6まで
    public HashMap<Long, EnumMap<Piece, List<OriginalPiece>>> getBlockMaskMapBoard1(int lineY) {
        HashMap<Long, EnumMap<Piece, List<OriginalPiece>>> maps = new HashMap<>();

        List<OriginalPiece> uniqueOriginalPieces = createUniqueOriginalPieces();

        for (int dy = 0; dy < 3; dy++) {
            int y = lineY + dy;
            for (int x = 0; x < 10; x++) {
                Field field = FieldFactory.createField(maxHeight);
                field.setBlock(x, y);

                EnumMap<Piece, List<OriginalPiece>> enumMap = new EnumMap<>(Piece.class);
                for (Piece piece : Piece.valueList()) {
                    List<OriginalPiece> piecesList = uniqueOriginalPieces.stream()
                            .filter(originalPiece -> originalPiece.getPiece() == piece)
                            .filter(originalPiece -> !field.canPut(originalPiece))
                            .collect(Collectors.toList());

                    enumMap.put(piece, piecesList);
                }

                maps.put(1L << (x + dy * 10), enumMap);
            }
        }

        return maps;
    }

    public List<OriginalPiece> createUniqueOriginalPieces() {
        OriginalPieceFactory pieceFactory = new OriginalPieceFactory(maxHeight);
        return pieceFactory.create().stream()
                .filter(originalPiece -> {
                    Piece piece = originalPiece.getPiece();
                    Set<Rotate> uniqueRotates = minoShifter.getUniqueRotates(piece);
                    return uniqueRotates.contains(originalPiece.getRotate());
                })
                .collect(Collectors.toList());
    }

    // 指定したブロックを埋めるすべてのミノを取得する
    // Long=埋めたい1ブロック -> Piece=置くミノの種類 -> List=指定したブロックを埋めるミノ一覧
    // フィールドの高さは12まで
    public HashMap<Long, HashMap<Long, EnumMap<Piece, List<OriginalPiece>>>> getBlockMaskMapBoard2() {
        HashMap<Long, HashMap<Long, EnumMap<Piece, List<OriginalPiece>>>> maps = new HashMap<>();

        List<OriginalPiece> uniqueOriginalPieces = createUniqueOriginalPieces();

        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 10; x++) {
                Field field = FieldFactory.createMiddleField();
                field.setBlock(x, y);

                EnumMap<Piece, List<OriginalPiece>> enumMap = new EnumMap<>(Piece.class);
                for (Piece piece : Piece.valueList()) {
                    List<OriginalPiece> piecesList = uniqueOriginalPieces.stream()
                            .filter(originalPiece -> originalPiece.getPiece() == piece)
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

    // Tスピンとして判定されるのに必要なブロックを取得
    public HashMap<Integer, List<Field>> getTSpinMaskFields(int maxHeight) {
        HashMap<Integer, List<Field>> maps = new HashMap<>();

        List<Integer> diff = Arrays.asList(-1, 1);

        for (int y = 0; y < maxHeight; y++) {
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

    public MinoRotation getMinoRotation() {
        return minoRotation;
    }
}
