package org.codice.ddf.admin.sources.fields;

import java.util.ArrayList;
import java.util.List;

import org.codice.ddf.admin.api.action.Message;
import org.codice.ddf.admin.common.fields.base.scalar.StringField;

public class SourceName extends StringField {

    public static final String DEFAULT_FIELD_NAME = "sourceName";

    public static final String FIELD_TYPE_NAME = "SourceName";

    public static final String DESCRIPTION =
            "A unique human readable string value for representing a source.";

    public SourceName() {
        this(DEFAULT_FIELD_NAME);
    }

    public SourceName(String fieldName) {
        super(fieldName, FIELD_TYPE_NAME, DESCRIPTION);
    }

    @Override
    public List<Message> validate() {
        List<Message> messages = new ArrayList();

        return messages;
    }
}
