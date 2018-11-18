import common.buildup.BuildUp;
import common.datastore.FullOperationWithKey;
import common.datastore.MinoOperationWithKey;
import common.datastore.action.Action;
import common.parser.OperationWithKeyInterpreter;
import common.tetfu.common.ColorConverter;
import commons.Commons;
import concurrent.LockedReachableThreadLocal;
import core.action.candidate.RotateCandidate;
import core.action.reachable.LockedReachable;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.MinoFactory;
import core.mino.MinoShifter;
import core.mino.Piece;
import core.srs.MinoRotation;
import core.srs.Rotate;
import entry.path.output.OneFumenParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Spin3Main {
    public static void main(String[] args) throws IOException {
        int maxHeight = 7;
        MinoFactory minoFactory = new MinoFactory();
        MinoShifter minoShifter = new MinoShifter();
        MinoRotation minoRotation = new MinoRotation();
        ColorConverter colorConverter = new ColorConverter();
        RotateCandidate candidate = new RotateCandidate(minoFactory, minoShifter, minoRotation, maxHeight);
        LockedReachableThreadLocal reachableThreadLocal = new LockedReachableThreadLocal(minoFactory, minoShifter, minoRotation, maxHeight);

        Field initField = FieldFactory.createField(maxHeight);

        OneFumenParser fumenParser = new OneFumenParser(minoFactory, colorConverter);

        Files.lines(Paths.get("output/line3_m7"))
                .map(line -> {
                    List<MinoOperationWithKey> operations = OperationWithKeyInterpreter.parseToList(line, minoFactory);

                    List<MinoOperationWithKey> withoutT = operations.stream()
                            .filter(operation -> operation.getPiece() != Piece.T)
                            .collect(Collectors.toList());

                    Field field = initField.freeze(maxHeight);
                    for (MinoOperationWithKey operation : withoutT) {
                        Field piece = FieldFactory.createField(maxHeight);
                        piece.put(operation.getMino(), operation.getX(), operation.getY());
                        piece.insertWhiteLineWithKey(operation.getNeedDeletedKey());
                        field.merge(piece);
                    }

                    Optional<MinoOperationWithKey> piece = operations.stream()
                            .filter(operation -> operation.getPiece() == Piece.T)
                            .findAny();

                    Optional<List<MinoOperationWithKey>> result = piece.map(operation -> {
                        Set<Action> actions = candidate.search(field, Piece.T, maxHeight);

                        Rotate rotate = operation.getRotate();
                        int x = operation.getX();
                        int y = operation.getY();
                        boolean match = actions.stream().anyMatch(action -> {
                            return action.getRotate() == rotate && action.getX() == x && action.getY() == y;
                        });

                        if (!match) {
                            return null;
                        }

                        if (!Commons.isTSpin(field, x, y)) {
                            return null;
                        }

                        return operations;
                    });

                    int lowerY = field.getLowerY();
                    return result.map(operationWithKeys -> {
                        return operationWithKeys.stream()
                                .map(operation -> {
                                    int x = operation.getX();
                                    int y = operation.getY();
                                    long needDeletedKey = operation.getNeedDeletedKey();
                                    long usingKey = operation.getUsingKey();
                                    return (MinoOperationWithKey) new FullOperationWithKey(operation.getMino(), x, y - lowerY, needDeletedKey, usingKey);
                                })
                                .collect(Collectors.toList());
                    });
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(operationWithKeys -> {
                    LockedReachable reachable = reachableThreadLocal.get();
                    return BuildUp.cansBuild(initField, operationWithKeys, maxHeight, reachable);
                })
                .forEach(operationWithKeys -> {
                    String fumen = fumenParser.parse(operationWithKeys, initField, maxHeight);
                    System.out.println("http://fumen.zui.jp/?v115@" + fumen);
                });
    }


}
