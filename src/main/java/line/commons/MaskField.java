package line.commons;

import core.field.Field;

public class MaskField {
    private final Field need;
    private final Field notAllowed;

    public MaskField(Field need, Field notAllowed) {
        this.need = need;
        this.notAllowed = notAllowed;
    }

    public Field getNeed() {
        return need;
    }

    public Field getNotAllowed() {
        return notAllowed;
    }
}
