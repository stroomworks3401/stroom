package stroom.template.set.impl;

import stroom.docstore.api.DocumentSerialiser2;
import stroom.docstore.api.Serialiser2;
import stroom.docstore.api.Serialiser2Factory;
import stroom.template.set.shared.TemplateSetDoc;

import jakarta.inject.Inject;

import java.io.IOException;
import java.util.Map;

public class TemplateSetSerialiser implements DocumentSerialiser2<TemplateSetDoc> {

    private final Serialiser2<TemplateSetDoc> delegate;

    @Inject
    TemplateSetSerialiser(final Serialiser2Factory serialiser2Factory) {
        delegate = serialiser2Factory.createSerialiser(TemplateSetDoc.class);
    }

    @Override
    public TemplateSetDoc read(final Map<String, byte[]> data) throws IOException {
        return delegate.read(data);
    }

    @Override
    public Map<String, byte[]> write(final TemplateSetDoc document) throws IOException {
        return delegate.write(document);
    }
}
