import common.SyntaxException;
import main.SecondBinaryMain;
import verify.VerifyMain;

import java.io.IOException;
import java.util.Arrays;

public class ExpMain {
    public static void main(String[] args) throws IOException, SyntaxException {
//        FirstBinaryMain.main(args);
//        SecondBinaryMain.main(args);
        VerifyMain.main(args);

//        for (String prefix : Arrays.asList("SRS", "SRS7BAG")) {
//            SecondBinaryMain.main(new String[]{prefix});
//        }
    }
}
