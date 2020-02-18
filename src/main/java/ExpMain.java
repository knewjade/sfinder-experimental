import common.SyntaxException;
import main.first.FirstBinaryMain;
import main.second.SecondBinaryMain;
import main.verify.VerifyMain;

import java.io.IOException;

public class ExpMain {
    public static void main(String[] args) throws IOException, SyntaxException {
        try {
//            FirstBinaryMain.main(args);
//            SecondBinaryMain.main(args);
            VerifyMain.main(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
