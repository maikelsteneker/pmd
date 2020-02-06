/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.cpd.token.internal;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;

import org.junit.Test;

import net.sourceforge.pmd.cpd.token.TokenFilter;
import net.sourceforge.pmd.lang.TokenManager;
import net.sourceforge.pmd.lang.ast.GenericToken;

import com.google.common.collect.ImmutableList;

public class BaseTokenFilterTest {

    class StringToken implements GenericToken {

        final private String text;

        StringToken(final String text) {
            this.text = text;
        }

        @Override
        public GenericToken getNext() {
            return null;
        }

        @Override
        public GenericToken getPreviousComment() {
            return null;
        }

        @Override
        public String getImage() {
            return text;
        }

        @Override
        public int getBeginLine() {
            return 0;
        }

        @Override
        public int getEndLine() {
            return 0;
        }

        @Override
        public int getBeginColumn() {
            return 0;
        }

        @Override
        public int getEndColumn() {
            return 0;
        }
    }

    @Test
    public void testRemainingTokensFunctionality() {
        final Iterable[] iterable = new Iterable[1];
        final TokenManager tokenManager = new TokenManager() {

            Iterator<String> iterator = ImmutableList.of("a", "b", "c").iterator();

            @Override
            public Object getNextToken() {
                if (iterator.hasNext()) {
                    return new StringToken(iterator.next());
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
                iterable[0] = remainingTokens;
            }
        };
        final GenericToken firstToken = tokenFilter.getNextToken();
        assertEquals("a", firstToken.getImage());
        final Iterator it1 = iterable[0].iterator();
        final Iterator it2 = iterable[0].iterator();
        StringToken firstValFirstIt = (StringToken) it1.next();
        StringToken firstValSecondIt = (StringToken) it2.next();
        assertEquals("b", firstValFirstIt.getImage());
        assertEquals("b", firstValSecondIt.getImage());
        StringToken secondValFirstIt = (StringToken) it1.next();
        StringToken secondValSecondIt = (StringToken) it2.next();
        assertEquals("c", secondValFirstIt.getImage());
        assertEquals("c", secondValSecondIt.getImage());
    }

}
