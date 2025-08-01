package stroom.template.set.impl;

import stroom.docstore.api.DocumentSerialiser2;
import stroom.docstore.api.Serialiser2;
import stroom.docstore.api.Serialiser2Factory;
import stroom.template.set.shared.TemplateSetDoc;
import stroom.util.string.EncodingUtil;

import jakarta.inject.Inject;

import java.io.IOException;
import java.util.Map;

public class TemplateSetSerialiser implements DocumentSerialiser2<TemplateSetDoc> {

    private static final String TEXT = "txt";

    private final Serialiser2<TemplateSetDoc> delegate;

    @Inject
    public TemplateSetSerialiser(final Serialiser2Factory serialiser2Factory) {
        this.delegate = serialiser2Factory.createSerialiser(TemplateSetDoc.class);
    }

    @Override
    public TemplateSetDoc read(final Map<String, byte[]> data) throws IOException {
        final TemplateSetDoc document = delegate.read(data);
        document.setData(EncodingUtil.asString(data.get(TEXT)));
        return document;
    }

    @Override
    public Map<String, byte[]> write(final TemplateSetDoc document) throws IOException {
        final String text = document.getData();
        document.setData(null);

        final Map<String, byte[]> data = delegate.write(document);
        if (text != null) {
            data.put(TEXT, EncodingUtil.asBytes(text));
            document.setData(text);
        }

        return data;
    }
}
