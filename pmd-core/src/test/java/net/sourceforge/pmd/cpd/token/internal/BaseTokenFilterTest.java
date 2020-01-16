package net.sourceforge.pmd.cpd.token.internal;

import com.google.common.collect.ImmutableList;
import net.sourceforge.pmd.cpd.token.TokenFilter;
import net.sourceforge.pmd.lang.TokenManager;
import net.sourceforge.pmd.lang.ast.GenericToken;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.*;

public class BaseTokenFilterTest {

    @Test
    public void testRemainingTokensFunctionality() {
        final TokenManager tokenManager = new TokenManager() {

            Iterator<String> iterator = ImmutableList.of("a", "b", "c").iterator();

            @Override
            public Object getNextToken() {
                if (iterator.hasNext()) {
                    return iterator.next();
                } else {
                    return null;
                }
            }

            @Override
            public void setFileName(final String fileName) {
            }
        };
        final TokenFilter tokenFilter = new BaseTokenFilter(tokenManager) {

            @Override
            protected boolean shouldStopProcessing(final GenericToken currentToken) {
                return currentToken == null;
            }

            @Override
            protected void analyzeTokens(final GenericToken currentToken, final Iterable remainingTokens) {
                final Iterator it1 = remainingTokens.iterator();
                final Iterator it2 = remainingTokens.iterator();
                Object firstValFirstIt = it1.next();
                Object firstValSecondIt = it2.next();
                assertEquals("a", firstValFirstIt);
                assertEquals("a", firstValSecondIt);
            }
        };
    }

}