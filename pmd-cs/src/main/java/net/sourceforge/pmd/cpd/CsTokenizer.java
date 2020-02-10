/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.cpd;

import java.util.Iterator;
import java.util.Properties;

import org.antlr.v4.runtime.CharStream;

import net.sourceforge.pmd.cpd.token.AntlrToken;
import net.sourceforge.pmd.cpd.token.AntlrTokenFilter;
import net.sourceforge.pmd.lang.antlr.AntlrTokenManager;
import net.sourceforge.pmd.lang.cs.antlr4.CSharpLexer;

/**
 * The C# tokenizer.
 */
public class CsTokenizer extends AntlrTokenizer {

    private boolean ignoreUsings = false;

    public void setProperties(Properties properties) {
        if (properties.containsKey(IGNORE_USINGS)) {
            ignoreUsings = Boolean.parseBoolean(properties.getProperty(IGNORE_USINGS, "false"));
        }
    }

    public void setIgnoreUsings(boolean ignoreUsings) {
        this.ignoreUsings = ignoreUsings;
    }

    @Override
    protected AntlrTokenManager getLexerForSource(final SourceCode sourceCode) {
        final CharStream charStream = AntlrTokenizer.getCharStreamFromSourceCode(sourceCode);
        return new AntlrTokenManager(new CSharpLexer(charStream), sourceCode.getFileName());
    }

    @Override
    protected AntlrTokenFilter getTokenFilter(final AntlrTokenManager tokenManager) {
        return new CsTokenFilter(tokenManager, ignoreUsings);
    }

    /**
     * The {@link CsTokenFilter} extends the {@link AntlrTokenFilter} to discard
     * C#-specific tokens.
     * <p>
     * By default, it enables annotation-based CPD suppression.
     * If the --ignoreUsings flag is provided, using directives are filtered out.
     * </p>
     */
    private static class CsTokenFilter extends AntlrTokenFilter {
        private enum UsingState {
            DEFAULT, // not within a using statement or directive
            KEYWORD, // just encountered the using keyword
            IDENTIFIER, // just encountered an identifier or var keyword
            DISCARDING // discarding the current using directive
        }

        private final boolean ignoreUsings;
        private UsingState usingState = UsingState.DEFAULT;
        private boolean discardingNL = false;

        CsTokenFilter(final AntlrTokenManager tokenManager, boolean ignoreUsings) {
            super(tokenManager);
            this.ignoreUsings = ignoreUsings;
        }

        @Override
        protected void analyzeToken(final AntlrToken currentToken) {
            skipNewLines(currentToken);
        }

        @Override
        protected void analyzeTokens(final AntlrToken currentToken, final Iterable<AntlrToken> remainingTokens) {
            final Iterator<AntlrToken> iterator = remainingTokens.iterator();
            AntlrToken token = currentToken;
            do {
                skipUsingDirectives(token);
                if (iterator.hasNext()) {
                    token = iterator.next();
                }
            } while (usingState != UsingState.DEFAULT && usingState != UsingState.DISCARDING && iterator.hasNext() && token.getType() != org.antlr.v4.runtime.Token.EOF);
            skipUsingDirectives(token);
        }

        private void skipUsingDirectives(final AntlrToken currentToken) {
            if (!ignoreUsings) {
                return;
            }
            final int type = currentToken.getType();
            if (usingState == UsingState.DEFAULT && type == CSharpLexer.USING) {
                usingState = UsingState.KEYWORD;
            } else if (usingState == UsingState.KEYWORD) {
                // The previous token was a using keyword.
                switch (type) {
                case CSharpLexer.STATIC:
                    // Definitely a using directive; start discarding.
                    // Example: using static System.Math;
                    usingState = UsingState.DISCARDING;
                    break;
                case CSharpLexer.VAR:
                    // Definitely a using statement; don't discard.
                    // Example: using var font1 = new Font("Arial", 10.0f);
                    usingState = UsingState.DEFAULT;
                    break;
                case CSharpLexer.OPEN_PARENS:
                    // Definitely a using statement; don't discard.
                    // Example: using (var font1 = new Font("Arial", 10.0f);
                    usingState = UsingState.DEFAULT;
                    break;
                case CSharpLexer.IDENTIFIER:
                    // This is either a type for a using statement or an alias for a using directive.
                    // Example (directive): using Project = PC.MyCompany.Project;
                    // Example (statement): using Font font1 = new Font("Arial", 10.0f);
                    usingState = UsingState.IDENTIFIER;
                    break;
                default:
                    break;
                }
            } else if (usingState == UsingState.IDENTIFIER) {
                // The previous token was an identifier.
                switch (type) {
                case CSharpLexer.ASSIGNMENT:
                    // Definitely a using directive; start discarding.
                    // Example: using Project = PC.MyCompany.Project;
                    usingState = UsingState.DISCARDING;
                    break;
                case CSharpLexer.IDENTIFIER:
                    // Definitely a using statement; don't discard.
                    // Example: using Font font1 = new Font("Arial", 10.0f);
                    usingState = UsingState.DEFAULT;
                    break;
                case CSharpLexer.DOT:
                    // This should be considered part of the same type; revert to previous state.
                    // Example (directive): using System.Text;
                    // Example (statement): using System.Drawing.Font font1 = new Font("Arial", 10.0f);
                    usingState = UsingState.KEYWORD;
                    break;
                case CSharpLexer.SEMICOLON:
                    // End of using directive; discard.
                    usingState = UsingState.DISCARDING;
                    break;
                default:
                    break;
                }
            } else if (usingState == UsingState.DISCARDING && type == CSharpLexer.SEMICOLON) {
                // End of using directive; stop discarding.
                usingState = UsingState.DEFAULT;
            }
        }

        private void skipNewLines(final AntlrToken currentToken) {
            discardingNL = currentToken.getType() == CSharpLexer.NL;
        }

        @Override
        protected boolean isLanguageSpecificDiscarding() {
            return usingState == UsingState.DISCARDING || discardingNL;
        }
    }
}
