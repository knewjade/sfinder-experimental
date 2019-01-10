package line.categorize;

import common.datastore.BlockField;
import common.datastore.MinoOperationWithKey;
import common.datastore.Operation;
import common.parser.OperationInterpreter;
import common.parser.OperationTransform;
import common.tetfu.Tetfu;
import common.tetfu.TetfuElement;
import common.tetfu.common.ColorConverter;
import common.tetfu.field.ColoredField;
import commons.Commons;
import core.field.Field;
import core.mino.MinoFactory;
import core.srs.MinoRotation;
import core.srs.Rotate;
import entry.path.output.MyFile;
import lib.AsyncBufferedFileWriter;
import line.commons.FactoryPool;
import line.commons.LineCommons;
import line.commons.rotate.MinoRotationDetail;
import line.commons.spin.Spin;
import line.commons.spin.TSpins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws IOException {
        int maxHeight = 24;
        FactoryPool factoryPool = new FactoryPool(maxHeight);
        MinoFactory minoFactory = factoryPool.getMinoFactory();

        List<Category> lists = Files.lines(Paths.get("output/last"))
                .map(OperationInterpreter::parseToOperations)
                .map(operations -> Category.create(operations, minoFactory, maxHeight))
                .collect(Collectors.toList());
        System.out.println(lists.size());

        Main main = new Main(factoryPool);

        main.spin(lists);
        main.num(lists);
        main.height(lists);
        main.rotate(lists);
        main.x(lists);
        main.y(lists);
    }

    private final MinoFactory minoFactory;
    private final ColorConverter colorConverter;
    private final FactoryPool factoryPool;
    private final SpinSearcher spinSearcher;

    private Main(FactoryPool factoryPool) {
        this.factoryPool = factoryPool;
        this.minoFactory = factoryPool.getMinoFactory();
        this.colorConverter = factoryPool.getColorConverter();

        MinoRotation minoRotation = factoryPool.getMinoRotation();
        MinoRotationDetail minoRotationDetail = new MinoRotationDetail(minoRotation);
        this.spinSearcher = new SpinSearcher(minoFactory, minoRotationDetail);
    }

    private void spin(List<Category> lists) {
        HashMap<TSpins, List<Category>> map = new HashMap<>();

        List<TSpins> keys = Arrays.asList(TSpins.Regular, TSpins.Mini);

        List<Category> regular = lists.stream().filter(o -> {
            Set<TSpins> spins = getSpins(o).stream().map(Spin::getSpin).collect(Collectors.toSet());
            return spins.contains(TSpins.Regular);
        }).collect(Collectors.toList());
        map.put(TSpins.Regular, regular);

        List<Category> mini = lists.stream().filter(o -> {
            Set<TSpins> spins = getSpins(o).stream().map(Spin::getSpin).collect(Collectors.toSet());
            return spins.contains(TSpins.Mini);
        }).collect(Collectors.toList());
        map.put(TSpins.Mini, mini);

        System.out.printf("=============== ========== ================================================================================%n");

        System.out.printf("%15s %10s %30s %n", "T-Spin type", "Sum", "");

        System.out.printf("=============== ========== ================================================================================%n");

        for (TSpins spin : keys) {
            List<Category> categories = map.get(spin);

            String descriptionEn = String.format("T-Spin %s in first bag (All %d patterns)", spin, categories.size());
            String descriptionJa = String.format("開幕7ミノでできるTスピンのうち、T-Spin %sの地形 (全%dパターン)", spin, categories.size());
            String fileName = "spin" + spin + ".html";
            toFile(categories, fileName, descriptionEn, descriptionJa, 100);

            String link = "https://s3-ap-northeast-1.amazonaws.com/sfinder/store/tspins/" + fileName;
            System.out.printf("%15s %10s %21s%n", spin, categories.size(), link);
        }

        System.out.printf("=============== ========== ================================================================================%n");
    }

    private void num(List<Category> lists) {
        Map<Integer, List<Category>> map = lists.stream().collect(Collectors.groupingBy(o -> o.getOperations().getOperations().size()));
        List<Integer> keys = new ArrayList<>(map.keySet());

        System.out.printf("======== ========== ============================== ================================================================================%n");

        System.out.printf("%8s %10s %30s %n", "Num", "Sum", "T-Spin type");

        System.out.printf("======== ========== ============================== ================================================================================%n");

        for (int num : keys) {
            List<Category> categories = map.get(num);

            long regular = categories.stream().filter(o -> {
                Set<TSpins> spins = getSpins(o).stream().map(Spin::getSpin).collect(Collectors.toSet());
                return spins.contains(TSpins.Regular);
            }).count();
            long mini = categories.stream().filter(o -> {
                Set<TSpins> spins = getSpins(o).stream().map(Spin::getSpin).collect(Collectors.toSet());
                return spins.contains(TSpins.Mini);
            }).count();

            String descriptionEn = String.format("T-Spins that num of pieces is %d in first bag (All %d patterns)", num, categories.size());
            String descriptionJa = String.format("開幕7ミノでできるTスピンのうち、使っているミノ数が%dの地形 (全%dパターン)", num, categories.size());
            String fileName = "num" + num + ".html";
            toFile(categories, fileName, descriptionEn, descriptionJa, 100);

            String link = "https://s3-ap-northeast-1.amazonaws.com/sfinder/store/tspins/" + fileName;
            System.out.printf("%8s %10s %30s %21s%n", num, categories.size(), "Regular:" + regular + ", Mini:" + mini, link);
        }

        System.out.printf("======== ========== ============================== ================================================================================%n");
    }

    private void height(List<Category> lists) {
        Map<Integer, List<Category>> map = lists.stream().collect(Collectors.groupingBy(Category::getUsingFieldHeight));
        List<Integer> keys = new ArrayList<>(map.keySet());

        System.out.printf("======== ========== ================================================================================%n");

        System.out.printf("%8s %8s %20s %n", "Height", "Sum", "T-Spin type");

        System.out.printf("======== ========== ================================================================================%n");

        for (int height : keys) {
            List<Category> categories = map.get(height);

            String descriptionEn = String.format("T-Spins that field height is %d in first bag (All %d patterns)", height, categories.size());
            String descriptionJa = String.format("開幕7ミノでできるTスピンのうち、高さ%dの地形 (全%dパターン)", height, categories.size());
            String fileName = "height" + height + ".html";
            toFile(categories, fileName, descriptionEn, descriptionJa, 100);

            String link = "https://s3-ap-northeast-1.amazonaws.com/sfinder/store/tspins/" + fileName;
            System.out.printf("%8s %10s %21s%n", height, categories.size(), link);
        }

        System.out.printf("======== ========== ================================================================================%n");
    }

    private void rotate(List<Category> lists) {
        Map<Rotate, List<Category>> map = lists.stream().collect(Collectors.groupingBy(o -> o.getT().getRotate()));
        List<Rotate> keys = Arrays.asList(Rotate.Reverse, Rotate.Left, Rotate.Right, Rotate.Spawn);

        System.out.printf("======== ========== ============================== ================================================================================%n");

        System.out.printf("%8s %8s %30s %n", "Height", "Sum", "T-Spin type");

        System.out.printf("======== ========== ============================== ================================================================================%n");

        for (Rotate rotate : keys) {
            List<Category> categories = map.get(rotate);

            long regular = categories.stream().filter(o -> {
                Set<TSpins> spins = getSpins(o).stream().map(Spin::getSpin).collect(Collectors.toSet());
                return spins.contains(TSpins.Regular);
            }).count();
            long mini = categories.stream().filter(o -> {
                Set<TSpins> spins = getSpins(o).stream().map(Spin::getSpin).collect(Collectors.toSet());
                return spins.contains(TSpins.Mini);
            }).count();

            String descriptionEn = String.format("T-Spins that rotate of T is %s in first bag (All %d patterns)", rotate, categories.size());
            String descriptionJa = String.format("開幕7ミノでできるTスピンのうち、Tの向きが%sの地形 (全%dパターン)", rotate, categories.size());
            String fileName = "rotate" + rotate + ".html";
            toFile(categories, fileName, descriptionEn, descriptionJa, 100);

            String link = "https://s3-ap-northeast-1.amazonaws.com/sfinder/store/tspins/" + fileName;
            System.out.printf("%8s %10s %30s %21s%n", rotate, categories.size(), "Regular:" + regular + ", Mini:" + mini, link);
        }

        System.out.printf("======== ========== ============================== ================================================================================%n");
    }

    private void x(List<Category> lists) {
        Map<Integer, List<Category>> map = lists.stream().collect(Collectors.groupingBy(o -> o.getT().getX()));
        List<Integer> keys = new ArrayList<>(map.keySet());

        System.out.printf("======== ========== ============================== ================================================================================%n");

        System.out.printf("%8s %8s %30s %n", "X", "Sum", "T-Spin type");

        System.out.printf("======== ========== ============================== ================================================================================%n");

        for (int x : keys) {
            List<Category> categories = map.get(x);

            long regular = categories.stream().filter(o -> {
                Set<TSpins> spins = getSpins(o).stream().map(Spin::getSpin).collect(Collectors.toSet());
                return spins.contains(TSpins.Regular);
            }).count();
            long mini = categories.stream().filter(o -> {
                Set<TSpins> spins = getSpins(o).stream().map(Spin::getSpin).collect(Collectors.toSet());
                return spins.contains(TSpins.Mini);
            }).count();

            String descriptionEn = String.format("T-Spins that x-index of T is %d in first bag (All %d patterns)", x, categories.size());
            String descriptionJa = String.format("開幕7ミノでできるTスピンのうち、Tの向きがx座標が%dの地形 (全%dパターン)", x, categories.size());
            String fileName = "x" + x + ".html";
            toFile(categories, fileName, descriptionEn, descriptionJa, 100);

            String link = "https://s3-ap-northeast-1.amazonaws.com/sfinder/store/tspins/" + fileName;
            System.out.printf("%8s %10s %30s %21s%n", x, categories.size(), "Regular:" + regular + ", Mini:" + mini, link);
        }

        System.out.printf("======== ========== ============================== ================================================================================%n");
    }

    private void y(List<Category> lists) {
        Map<Integer, List<Category>> map = lists.stream().collect(Collectors.groupingBy(o -> o.getT().getY()));
        List<Integer> keys = new ArrayList<>(map.keySet());

        System.out.printf("======== ========== ============================== ================================================================================%n");

        System.out.printf("%8s %8s %30s %n", "Y", "Sum", "T-Spin type");

        System.out.printf("======== ========== ============================== ================================================================================%n");

        for (int y : keys) {
            List<Category> categories = map.get(y);

            long regular = categories.stream().filter(o -> {
                Set<TSpins> spins = getSpins(o).stream().map(Spin::getSpin).collect(Collectors.toSet());
                return spins.contains(TSpins.Regular);
            }).count();
            long mini = categories.stream().filter(o -> {
                Set<TSpins> spins = getSpins(o).stream().map(Spin::getSpin).collect(Collectors.toSet());
                return spins.contains(TSpins.Mini);
            }).count();

            String descriptionEn = String.format("T-Spins that y-index of T is %d in first bag (All %d patterns)", y, categories.size());
            String descriptionJa = String.format("開幕7ミノでできるTスピンのうち、Tの向きがy座標が%dの地形 (全%dパターン)", y, categories.size());
            String fileName = "y" + y + ".html";
            toFile(categories, fileName, descriptionEn, descriptionJa, 100);

            String link = "https://s3-ap-northeast-1.amazonaws.com/sfinder/store/tspins/" + fileName;
            System.out.printf("%8s %10s %30s %21s%n", y, categories.size(), "Regular:" + regular + ", Mini:" + mini, link);
        }

        System.out.printf("======== ========== ============================== ================================================================================%n");
    }

    private List<Spin> getSpins(Category category) {
        List<? extends Operation> operationList = category.getOperations().getOperations();
        Field field = LineCommons.toField(minoFactory, operationList, 24);
        Field freeze = field.freeze();

        Operation t = category.getOperation();
        freeze.remove(minoFactory.create(t.getPiece(), t.getRotate()), t.getX(), t.getY());

        return spinSearcher.getSpins(freeze, category.getOperation(), factoryPool.createLockedReachable());
    }

    private void toFile(List<Category> categories, String fileName, String descriptionEn, String descriptionJa, int max) {
        MyFile file = new MyFile("output/" + fileName);

        try (AsyncBufferedFileWriter writer = file.newAsyncWriter()) {
            // Header
            writer.writeAndNewLine("<!DOCTYPE html>");
            writer.writeAndNewLine("<html>");
            writer.writeAndNewLine("<head><meta charset=\"UTF-8\"><style>body { margin-left: 30px; }</style></head>");
            writer.writeAndNewLine("<body>");
            writer.writeAndNewLine("<div>" + descriptionEn + "</div>");
            writer.writeAndNewLine("<div>" + descriptionJa + "</div>");
            writer.writeAndNewLine("<div><ol>");

            int maxHeight = 24;
            int size = categories.size();
            for (int count = 0; count < Math.ceil((double) size / max); count++) {
                int minIndex = count * max;
                int maxIndex = (count + 1) * max;
                if (size < maxIndex) {
                    maxIndex = size;
                }

                List<Category> sub = categories.subList(minIndex, maxIndex);
                List<TetfuElement> elements = sub.stream()
                        .map(category -> {
                            List<MinoOperationWithKey> keys = LineCommons.toOperationWithKeys(minoFactory, category.getOperations().getOperations());
                            BlockField blockField = OperationTransform.parseToBlockField(keys, minoFactory, maxHeight);
                            ColoredField coloredField = Commons.toColoredField(blockField, colorConverter);
                            return new TetfuElement(coloredField, "");
                        })
                        .collect(Collectors.toList());

                Tetfu tetfu = new Tetfu(minoFactory, colorConverter);
                String encode = tetfu.encode(elements);

                String line = String.format("<li><a href='%s' target='_blank'>%d-%d</a></li>", LineCommons.toURL(encode), minIndex + 1, maxIndex);
                writer.writeAndNewLine(line);
            }

            writer.writeAndNewLine("</ol></div>");
            writer.writeAndNewLine("</body></html>");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

