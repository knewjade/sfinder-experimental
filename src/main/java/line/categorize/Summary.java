package line.categorize;

import common.datastore.Operation;
import core.action.reachable.LockedReachable;
import core.field.Field;
import core.mino.MinoFactory;
import core.srs.MinoRotation;
import core.srs.Rotate;
import line.commons.FactoryPool;
import line.commons.LineCommons;
import line.commons.rotate.MinoRotationDetail;
import line.commons.spin.Spin;
import line.commons.spin.TSpins;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

class Summary {
    private final Map<Integer, AtomicInteger> eachFieldHeight = new HashMap<>();
    private final Map<Rotate, AtomicInteger> eachRotate = new HashMap<>();
    private final Map<Integer, AtomicInteger> eachX = new HashMap<>();
    private final Map<Integer, AtomicInteger> eachY = new HashMap<>();
    private final Map<Integer, AtomicInteger> eachLine = new HashMap<>();

    private final Map<TSpins, Map<Integer, AtomicInteger>> eachSpins = new HashMap<>();
    private final Map<TSpins, Map<Integer, AtomicInteger>> eachSpinsX = new HashMap<>();
    private final Map<TSpins, Map<Integer, AtomicInteger>> eachSpinsY = new HashMap<>();
    private final Map<TSpins, Map<Rotate, AtomicInteger>> eachSpinsRotate = new HashMap<>();

    private final FactoryPool pool;
    private final MinoFactory minoFactory;
    private final SpinSearcher spinSearcher;

    Summary(FactoryPool pool) {
        this.pool = pool;
        this.minoFactory = pool.getMinoFactory();

        MinoRotation minoRotation = pool.getMinoRotation();
        MinoRotationDetail minoRotationDetail = new MinoRotationDetail(minoRotation);
        this.spinSearcher = new SpinSearcher(minoFactory, minoRotationDetail);
    }

    boolean filter(Category category) {
        List<? extends Operation> operationList = category.getOperations().getOperations();
        Field field = LineCommons.toField(minoFactory, operationList, 24);
        Field freeze = field.freeze();

        Operation t = category.getOperation();
        freeze.remove(minoFactory.create(t.getPiece(), t.getRotate()), t.getX(), t.getY());

//        return category.getT().getRotate() == Rotate.Reverse && category.getT().getY() == 5;
//        LockedReachable lockedReachable = pool.createLockedReachable();
        List<Spin> spins = spinSearcher.getSpins(freeze, category.getOperation(), pool.createLockedReachable());
        Set<TSpins> sets = spins.stream().map(Spin::getSpin).collect(Collectors.toSet());
//        return category.getT().getRotate() == Rotate.Spawn
//                && spins.stream().anyMatch(spin -> spin.getSpin() == TSpins.Regular)
//                && category.getT().getY() == 2;

        return category.getClearedLine() == 2;
    }

    void add(Category category) {
        // Field Height
        int usingFieldHeight = category.getUsingFieldHeight();
        setUsingFieldHeight(usingFieldHeight);

        // Rotate, x-position, y-position
        setTSpin(category);

    }

    private void setUsingFieldHeight(int usingFieldHeight) {
        AtomicInteger counter = eachFieldHeight.computeIfAbsent(usingFieldHeight, (ignored) -> new AtomicInteger());
        counter.incrementAndGet();
    }

    private void setTSpin(Category category) {
        Operation operation = category.getT();

        List<? extends Operation> operationList = category.getOperations().getOperations();
        Field field = LineCommons.toField(minoFactory, operationList, 24);
        Field freeze = field.freeze();

        Operation t = category.getOperation();
        freeze.remove(minoFactory.create(t.getPiece(), t.getRotate()), t.getX(), t.getY());

        LockedReachable lockedReachable = pool.createLockedReachable();

        List<Spin> spins = spinSearcher.getSpins(freeze, category.getOperation(), lockedReachable);
        Set<TSpins> sets = spins.stream().map(Spin::getSpin).collect(Collectors.toSet());

        {
            AtomicInteger counter = eachRotate.computeIfAbsent(operation.getRotate(), (ignored) -> new AtomicInteger());
            counter.incrementAndGet();
        }

        {
            AtomicInteger counter = eachX.computeIfAbsent(operation.getX(), (ignored) -> new AtomicInteger());
            counter.incrementAndGet();
        }

        {
            AtomicInteger counter = eachY.computeIfAbsent(operation.getY(), (ignored) -> new AtomicInteger());
            counter.incrementAndGet();
        }

        {
            AtomicInteger counter = eachLine.computeIfAbsent(category.getClearedLine(), (ignored) -> new AtomicInteger());
            counter.incrementAndGet();
        }

        {
            for (TSpins set : sets) {
                Map<Integer, AtomicInteger> map = eachSpins.computeIfAbsent(set, (ignored) -> new HashMap<>());
                int clearedLine = category.getClearedLine();
                AtomicInteger counter = map.computeIfAbsent(clearedLine, (ignored) -> new AtomicInteger());
                counter.incrementAndGet();
            }
        }

        {
            for (TSpins set : sets) {
                Map<Integer, AtomicInteger> map = eachSpinsX.computeIfAbsent(set, (ignored) -> new HashMap<>());
                int x = category.getOperation().getX();
                AtomicInteger counter = map.computeIfAbsent(x, (ignored) -> new AtomicInteger());
                counter.incrementAndGet();
            }
        }

        {
            for (TSpins set : sets) {
                Map<Integer, AtomicInteger> map = eachSpinsY.computeIfAbsent(set, (ignored) -> new HashMap<>());
                int y = category.getOperation().getY();
                AtomicInteger counter = map.computeIfAbsent(y, (ignored) -> new AtomicInteger());
                counter.incrementAndGet();
            }
        }

        {
            for (TSpins set : sets) {
                Map<Rotate, AtomicInteger> map = eachSpinsRotate.computeIfAbsent(set, (ignored) -> new HashMap<>());
                Rotate rotate = category.getT().getRotate();
                AtomicInteger counter = map.computeIfAbsent(rotate, (ignored) -> new AtomicInteger());
                counter.incrementAndGet();
            }
        }
    }

    void show() {
        System.out.println("# Field height");
        for (Map.Entry<Integer, AtomicInteger> entry : eachFieldHeight.entrySet()) {
            System.out.println("| " + entry.getKey() + " | " + entry.getValue() + " |  |");
        }

        System.out.println();

        System.out.println("# Rotate");
        for (Map.Entry<Rotate, AtomicInteger> entry : eachRotate.entrySet()) {
            Rotate rotate = entry.getKey();
            AtomicInteger sum = entry.getValue();

            Map<Rotate, AtomicInteger> map1 = eachSpinsRotate.getOrDefault(TSpins.Regular, new HashMap<>());
            AtomicInteger regular = map1.getOrDefault(rotate, new AtomicInteger(0));

            Map<Rotate, AtomicInteger> map2 = eachSpinsRotate.getOrDefault(TSpins.Mini, new HashMap<>());
            AtomicInteger mini = map2.getOrDefault(rotate, new AtomicInteger(0));

            System.out.printf("| %s | %d (Regular: %d, Mini: %d) |%n", rotate, sum.intValue(), regular.intValue(), mini.intValue());
        }

        System.out.println();

        System.out.println("# X");
        for (Map.Entry<Integer, AtomicInteger> entry : eachX.entrySet()) {
            int x = entry.getKey();
            AtomicInteger sum = entry.getValue();

            Map<Integer, AtomicInteger> map1 = eachSpinsX.getOrDefault(TSpins.Regular, new HashMap<>());
            AtomicInteger regular = map1.getOrDefault(x, new AtomicInteger(0));

            Map<Integer, AtomicInteger> map2 = eachSpinsX.getOrDefault(TSpins.Mini, new HashMap<>());
            AtomicInteger mini = map2.getOrDefault(x, new AtomicInteger(0));

            System.out.printf("| %d | %d (Regular: %d, Mini: %d) |%n", x, sum.intValue(), regular.intValue(), mini.intValue());
        }

        System.out.println();

        System.out.println("# Y");
        for (Map.Entry<Integer, AtomicInteger> entry : eachY.entrySet()) {
            int y = entry.getKey();
            AtomicInteger sum = entry.getValue();

            Map<Integer, AtomicInteger> map1 = eachSpinsY.getOrDefault(TSpins.Regular, new HashMap<>());
            AtomicInteger regular = map1.getOrDefault(y, new AtomicInteger(0));

            Map<Integer, AtomicInteger> map2 = eachSpinsY.getOrDefault(TSpins.Mini, new HashMap<>());
            AtomicInteger mini = map2.getOrDefault(y, new AtomicInteger(0));

            System.out.printf("| %d | %d (Regular: %d, Mini: %d) |%n", y, sum.intValue(), regular.intValue(), mini.intValue());
        }

        System.out.println();

        System.out.println("# Clear Line");
        for (Map.Entry<Integer, AtomicInteger> entry : eachLine.entrySet()) {
            System.out.println("| " + entry.getKey() + " | " + entry.getValue() + " |  |");
        }

        System.out.println();

        System.out.println("# Clear Line -> T-Spin");
        for (Map.Entry<TSpins, Map<Integer, AtomicInteger>> entry1 : eachSpins.entrySet()) {
            TSpins key1 = entry1.getKey();
            Map<Integer, AtomicInteger> map1 = entry1.getValue();
            System.out.print("| " + key1 + " | ");
            for (int clearLine = 1; clearLine <= 2; clearLine++) {
                AtomicInteger counter = map1.getOrDefault(clearLine, new AtomicInteger(0));
                System.out.print(counter + " | ");
            }
        }
    }
}