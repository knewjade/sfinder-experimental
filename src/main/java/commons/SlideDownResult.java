package commons;

import common.datastore.MinoOperationWithKey;

import java.util.List;

public class SlideDownResult {
    private final List<MinoOperationWithKey> operationWithKeys;
    private final int slideDownY;

    SlideDownResult(List<MinoOperationWithKey> operationWithKeys, int slideDownY) {
        this.operationWithKeys = operationWithKeys;
        this.slideDownY = slideDownY;
    }

    public List<MinoOperationWithKey> getOperationWithKeys() {
        return operationWithKeys;
    }
}
