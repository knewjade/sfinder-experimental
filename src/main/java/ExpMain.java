import common.SyntaxException;
import mainv2.FirstBinaryMain;
import mainv2.SecondBinaryMain;
import verify.VerifyMain;

import java.io.IOException;
import java.util.Arrays;

public class ExpMain {
    public static void main(String[] args) throws IOException, SyntaxException {
//        FirstBinaryMain.main(args);
//        SecondBinaryMain.main(args);
//        VerifyMain.main(args);

        for (String prefix : Arrays.asList("SRS7BAG", "SRS")) {
            SecondBinaryMain.main(new String[]{prefix});
        }

//        FirstBinaryMain.main(args);
//        SecondBinaryMain.main(args);
    }
}
