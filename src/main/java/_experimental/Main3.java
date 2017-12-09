package _experimental;

import common.SyntaxException;
import common.datastore.blocks.LongPieces;
import common.pattern.LoadedPatternGenerator;
import helper.Patterns;

import java.util.Set;
import java.util.stream.Collectors;

public class Main3 {
    public static void main(String[] args) throws SyntaxException {
        for (int cycle = 1; cycle <= 8; cycle++) {
            LoadedPatternGenerator generator = new LoadedPatternGenerator(Patterns.hold(cycle));
            Set<LongPieces> collect = generator.blocksStream().parallel()
                    .map(pieces -> pieces.blockStream().limit(3L))
                    .map(LongPieces::new)
                    .collect(Collectors.toSet());

            int size = collect.size();
            System.out.println(size);
        }
    }
}
