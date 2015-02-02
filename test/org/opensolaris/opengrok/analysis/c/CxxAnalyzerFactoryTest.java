/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * See LICENSE.txt included in this distribution for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */

/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 */
package org.opensolaris.opengrok.analysis.c;

import java.io.File;
import java.io.IOException;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import java.io.InputStream;
import java.io.StringWriter;
import org.apache.lucene.document.Field;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.opensolaris.opengrok.analysis.AnalyzerGuru.string_ft_nstored_nanalyzed_norms;
import org.opensolaris.opengrok.analysis.Ctags;
import org.opensolaris.opengrok.analysis.FileAnalyzer;
import org.opensolaris.opengrok.analysis.Scopes;
import org.opensolaris.opengrok.analysis.Scopes.Scope;
import org.opensolaris.opengrok.analysis.StreamSource;
import org.opensolaris.opengrok.configuration.RuntimeEnvironment;
import org.opensolaris.opengrok.search.QueryBuilder;

/**
 *
 * @author kotal
 */
public class CxxAnalyzerFactoryTest {
    
    FileAnalyzer analyzer;
    private String ctagsProperty = "org.opensolaris.opengrok.analysis.Ctags";
    private static Ctags ctags;
    
    private static String PATH = "org/opensolaris/opengrok/analysis/c/sample.cxx";
    
    public CxxAnalyzerFactoryTest() {
        CxxAnalyzerFactory analFact = new CxxAnalyzerFactory();
        this.analyzer = analFact.getAnalyzer();
        RuntimeEnvironment env = RuntimeEnvironment.getInstance();
        env.setCtags(System.getProperty(ctagsProperty, "ctags"));
        if (env.validateExuberantCtags()) {
            this.analyzer.setCtags(new Ctags());
        }
    }
    
    private static StreamSource getStreamSource(final String fname) {
        return new StreamSource() {
            @Override
            public InputStream getStream() throws IOException {
                return getClass().getClassLoader().getResourceAsStream(fname);
            }
        };
    }
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        ctags = new Ctags();
        ctags.setBinary(RuntimeEnvironment.getInstance().getCtags());
    }

    @AfterClass
    public static void tearDownClass() throws Exception {        
        ctags.close();
        ctags = null;
    }

    /**
     * Test of writeXref method, of class CAnalyzerFactory.
     */
    @Test
    public void testScopeAnalyzer() throws Exception {        
        Document doc = new Document();
        doc.add(new Field(QueryBuilder.FULLPATH, new File("test/" + PATH).getAbsolutePath(),
            string_ft_nstored_nanalyzed_norms));
        StringWriter xrefOut = new StringWriter();
        analyzer.setCtags(ctags);
        analyzer.setScopesEnabled(true);
        analyzer.analyze(doc, getStreamSource(PATH), xrefOut);
        
        IndexableField scopesField = doc.getField(QueryBuilder.SCOPES);
        assertNotNull(scopesField);
        Scopes scopes = Scopes.deserialize(scopesField.binaryValue().bytes);
        Scope globalScope = scopes.getScope(-1);
        assertEquals(9, scopes.size());
        
        for (int i=0; i<50; ++i) {
            if (i >= 11 && i <= 15) {
                assertEquals("SomeClass", scopes.getScope(i).name);
                assertEquals("class:SomeClass", scopes.getScope(i).scope);
            } else if (i >= 17 && i <= 20) {                
                assertEquals("~SomeClass", scopes.getScope(i).name);
                assertEquals("class:SomeClass", scopes.getScope(i).scope);
            } else if (i >= 22 && i <= 25) {
                assertEquals("MemberFunc", scopes.getScope(i).name);
                assertEquals("class:SomeClass", scopes.getScope(i).scope);
            } else if (i >= 27 && i <= 29) {
                assertEquals("operator ++", scopes.getScope(i).name);
                assertEquals("class:SomeClass", scopes.getScope(i).scope);
            } else if (i >= 32 && i <= 34) {
                assertEquals("TemplateMember", scopes.getScope(i).name);
                assertEquals("class:SomeClass", scopes.getScope(i).scope);
            } else if (i >= 44 && i <= 46) {
                assertEquals("SomeFunc", scopes.getScope(i).name);
                assertEquals("class:ns1::NamespacedClass", scopes.getScope(i).scope);
            } else if (i >= 51 && i <= 54) {
                assertEquals("foo", scopes.getScope(i).name);
                assertNull(scopes.getScope(i).scope);
            } else if (i >= 59 && i <= 73) {
                assertEquals("bar", scopes.getScope(i).name);
                assertNull(scopes.getScope(i).scope);
            } else if (i >= 76 && i <= 87) {
                assertEquals("main", scopes.getScope(i).name);
                assertNull(scopes.getScope(i).scope);
            } else {
                assertEquals(scopes.getScope(i), globalScope);
                assertNull(scopes.getScope(i).scope);
            }
        }
    }
    
}
