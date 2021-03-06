/*
 * Copyright (C) 2013 salesforce.com, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.auraframework.impl.root.parser.handler.design;

import com.google.common.collect.ImmutableSet;
import org.auraframework.def.design.DesignLayoutDef;
import org.auraframework.def.design.DesignSectionDef;
import org.auraframework.impl.design.DesignLayoutDefImpl;
import org.auraframework.impl.root.parser.handler.BaseXMLElementHandler;
import org.auraframework.system.TextSource;
import org.auraframework.throwable.quickfix.QuickFixException;
import org.auraframework.util.AuraTextUtil;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.Set;

public class DesignLayoutDefHandler extends BaseXMLElementHandler {
    public static final String TAG = "design:layout";
    private static final String ATTRIBUTE_NAME = "name";
    private final Set<String> ALLOWED_ATTRIBUTES = ImmutableSet.of(ATTRIBUTE_NAME);
    private final boolean isInternalNamespace;
    private final DesignLayoutDefImpl.Builder builder = new DesignLayoutDefImpl.Builder();


    public DesignLayoutDefHandler(XMLStreamReader xmlReader,
                                  TextSource<?> source, boolean isInternalNamespace) {
        super(xmlReader, source);
        builder.setTagName(getTagName());
        this.isInternalNamespace = isInternalNamespace;
    }

    @Override
    protected void handleChildTag() throws XMLStreamException, QuickFixException {
        String tag = getTagName();
        if (DesignSectionDefHandler.TAG.equalsIgnoreCase(tag)) {
            DesignSectionDef section = new DesignSectionDefHandler(xmlReader, source, isInternalNamespace).createElement();
            builder.addSection(section);
        } else {
            throw new XMLStreamException(String.format("<%s> cannot contain tag %s", getHandledTag(), tag));
        }
    }

    @Override
    protected void readAttributes() throws QuickFixException {
        String name = getAttributeValue(ATTRIBUTE_NAME);
        if (name != null) {
            builder.setName(name);
        }

    }

    @Override
    protected void handleChildText() throws XMLStreamException, QuickFixException {
        String text = xmlReader.getText();
        if (!AuraTextUtil.isNullEmptyOrWhitespace(text)) {
            error("No literal text allowed in attribute design definition");
        }
    }

    @Override
    public Set<String> getAllowedAttributes() {
        return ALLOWED_ATTRIBUTES;
    }

    @Override
    public String getHandledTag() {
        return TAG;
    }

    public DesignLayoutDef createElement() throws QuickFixException, XMLStreamException {
        readElement();
        return builder.build();
    }
}
