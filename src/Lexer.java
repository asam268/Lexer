/*
 * Author: Asa Marshall, Chukwufunayan Ojiagbaje, James Bozhkov
 * Class: CS 4308 W01
 * School: Kennesaw State University
 * Professor: Dr. Jose Garrido
 * Date: February 16, 2020
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Lexer {
    private int line;
    private int pos;
    private int position;
    private char chr;
    private String s;

    Map<String, TokenType> keywords = new HashMap<>();

    /**
     * Sub-class Token contains information on each token including token type, value, line, and position. Token is also
     * used to print output about each token.
     */
    static class Token {
        public TokenType tokentype;
        public String value;
        public int line;
        public int pos;

        /**
         * Constructor for Token
         * @param token token type
         * @param value value of token
         * @param line  line # in input
         * @param pos   position on current line
         */
        Token(TokenType token, String value, int line, int pos) {
            this.tokentype = token; this.value = value; this.line = line; this.pos = pos;
        }

        @Override
        public String toString() {
            String result = String.format("%5d  %5d %-15s", this.line, this.pos, this.tokentype);
            if (this.tokentype == TokenType.String) {
                result += String.format(" \"%s\"", value);
            } else {
                result += String.format(" %s", value);
            }
            return result;
        }
    }

    /**
     * Defines constants for different token types
     */
    enum TokenType {
        End_of_input, Op_exponent, Op_multiply,  Op_divide, Op_mod, Op_add, Op_subtract,
        Op_negate, Op_not, Op_less, Op_lessequal, Op_greater, Op_greaterequal,
        Op_equal, Op_notequal, Op_assign, Op_and, Op_or, Keyword_if, Keyword_then, Keyword_endif,
        Keyword_else, Keyword_do, Keyword_while, Keyword_endwhile, Keyword_for, Keyword_endfor, Keyword_print,
        Keyword_putc, Keyword_endfun, LeftParen, RightParen, LeftBracket, RightBracket,
        Semicolon, Comma, Identifier, Integer, String
    }

    /**
     * Prints error message for unrecognized characters.
     * @param line  line #
     * @param pos   position in line
     * @param msg   description of error
     */
    static void error(int line, int pos, String msg) {
        if (line > 0 && pos > 0) {
            System.out.printf("%s in line %d, pos %d\n", msg, line, pos);
        } else {
            System.out.println(msg);
        }
        System.exit(1);
    }

    /**
     * Constructor for Lexer.
     * @param source    String containing code from input file
     */
    Lexer(String source) {
        this.line = 1;
        this.pos = 0;
        this.position = 0;
        this.s = source;
        this.chr = this.s.charAt(0);
        this.keywords.put("if", TokenType.Keyword_if);
        this.keywords.put("then", TokenType.Keyword_then);
        this.keywords.put("endif", TokenType.Keyword_endif);
        this.keywords.put("else", TokenType.Keyword_else);
        this.keywords.put("print", TokenType.Keyword_print);
        this.keywords.put("putc", TokenType.Keyword_putc);
        this.keywords.put("do", TokenType.Keyword_do);
        this.keywords.put("while", TokenType.Keyword_while);
        this.keywords.put("endwhile", TokenType.Keyword_endwhile);
        this.keywords.put("for", TokenType.Keyword_for);
        this.keywords.put("endfor", TokenType.Keyword_endfor);
        this.keywords.put("endfun", TokenType.Keyword_endfun);

    }

    /**
     * Analyzes tokens with ambiguous behavior that is dependent on a trailing character.
     * @param expect    expected next character
     * @param ifyes     token type if next character is equal to expect
     * @param ifno      token type if next character is not equal to expect
     * @param line      line #
     * @param pos       position in line
     * @return          Token with token type determined by trailing character
     */
    Token follow(char expect, TokenType ifyes, TokenType ifno, int line, int pos) {
        if (getNextChar() == expect) {
            getNextChar();
            return new Token(ifyes, "", line, pos);
        }
        if (ifno == TokenType.End_of_input) {
            error(line, pos, String.format("follow: unrecognized character: (%d) '%c'", (int)this.chr, this.chr));
        }
        return new Token(ifno, "", line, pos);
    }

    /**
     * Handles behavior for single quotes
     * @param line  line #
     * @param pos   position in line
     * @return      Integer Token or error if syntax is incorrect
     */
    Token char_lit(int line, int pos) {
        char c = getNextChar(); // skip opening quote
        int n = (int)c;
        if (c == '\'') {
            error(line, pos, "empty character constant");
        } else if (c == '\\') {
            c = getNextChar();
            if (c == 'n') {
                n = 10;
            } else if (c == '\\') {
                n = '\\';
            } else {
                error(line, pos, String.format("unknown escape sequence \\%c", c));
            }
        }
        if (getNextChar() != '\'') {
            error(line, pos, "multi-character constant");
        }
        getNextChar();
        return new Token(TokenType.Integer, "" + n, line, pos);
    }

    /**
     * Handles behavior for double quotes
     * @param start the beginning of the String literal
     * @param line  line #
     * @param pos   position in line
     * @return      String Token or error if syntax is incorrect
     */
    Token string_lit(char start, int line, int pos) {
        String result = "";
        while (getNextChar() != start) {
            if (this.chr == '\u0000') {
                error(line, pos, "EOF while scanning string literal");
            }
            if (this.chr == '\n') {
                error(line, pos, "EOL while scanning string literal");
            }
            result += this.chr;
        }
        getNextChar();
        return new Token(TokenType.String, result, line, pos);
    }

    /**
     * Analyzes the '/' character to see if it is a divide operator or comment.
     * @param line  line #
     * @param pos   position in line
     * @return      Divide operator Token or getToken() if '/' indicates comment behavior
     */
    Token div_or_comment(int line, int pos) {
        if (getNextChar() != '*') {
            return new Token(TokenType.Op_divide, "", line, pos);
        }
        getNextChar();
        while (true) {
            if (this.chr == '\u0000') {
                error(line, pos, "EOF in comment");
            } else if (this.chr == '*') {
                if (getNextChar() == '/') {
                    getNextChar();
                    return getToken();
                }
            }
            else {
                getNextChar();
            }
        }
    }

    /**
     * Determines the difference between identifiers and integers.
     * @param line  line #
     * @param pos   position in line
     * @return      Integer Token or Identifier Token
     */
    Token identifier_or_integer(int line, int pos) {
        boolean is_number = true;
        String text = "";

        while (Character.isAlphabetic(this.chr) || Character.isDigit(this.chr) || this.chr == '_' || this.chr == '#') {
            text += this.chr;
            if (!Character.isDigit(this.chr)) {
                is_number = false;
            }
            getNextChar();
        }

        if (text.equals("")) {
            error(line, pos, String.format("identifier_or_integer unrecognized character: (%d) %c", (int)this.chr, this.chr));
        }

        if (Character.isDigit(text.charAt(0))) {
            if (!is_number) {
                error(line, pos, String.format("invalid number: %s", text));
            }
            return new Token(TokenType.Integer, text, line, pos);
        }

        if (this.keywords.containsKey(text)) {
            return new Token(this.keywords.get(text), "", line, pos);
        }
        return new Token(TokenType.Identifier, text, line, pos);
    }

    /**
     * Calls appropriate methods to determine token type based on the value of the token using a switch.
     * @return  Token
     */
    Token getToken() {
        int line, pos;
        while (Character.isWhitespace(this.chr)) {
            getNextChar();
        }
        line = this.line;
        pos = this.pos;

        switch (this.chr) {
            case '\u0000': return new Token(TokenType.End_of_input, "", this.line, this.pos);
            case '/': return div_or_comment(line, pos);
            case '\'': return char_lit(line, pos);
            case '<': return follow('=', TokenType.Op_lessequal, TokenType.Op_less, line, pos);
            case '>': return follow('=', TokenType.Op_greaterequal, TokenType.Op_greater, line, pos);
            case ':': return follow('=', TokenType.Op_assign, TokenType.End_of_input, line, pos);
            case '=': getNextChar(); return new Token(TokenType.Op_equal, "=", line, pos);
            case '!': return follow('=', TokenType.Op_notequal, TokenType.Op_not, line, pos);
            case '&': return follow('&', TokenType.Op_and, TokenType.End_of_input, line, pos);
            case '|': return follow('|', TokenType.Op_or, TokenType.End_of_input, line, pos);
            case '"': return string_lit(this.chr, line, pos);
            case '(': getNextChar(); return new Token(TokenType.LeftParen, "(", line, pos);
            case ')': getNextChar(); return new Token(TokenType.RightParen, ")", line, pos);
            case '[': getNextChar(); return new Token(TokenType.LeftBracket, "[", line, pos);
            case ']': getNextChar(); return new Token(TokenType.RightBracket, "]", line, pos);
            case '^': getNextChar(); return new Token(TokenType.Op_exponent, "^", line, pos);
            case '+': getNextChar(); return new Token(TokenType.Op_add, "+", line, pos);
            case '-': getNextChar(); return new Token(TokenType.Op_subtract, "-", line, pos);
            case '*': getNextChar(); return new Token(TokenType.Op_multiply, "*", line, pos);
            case '%': getNextChar(); return new Token(TokenType.Op_mod, "%", line, pos);
            case ';': getNextChar(); return new Token(TokenType.Semicolon, ";", line, pos);
            case ',': getNextChar(); return new Token(TokenType.Comma, ",", line, pos);

            default: return identifier_or_integer(line, pos);
        }
    }

    /**
     * Gets the next character in the source code
     * @return  the next character in the source code
     */
    char getNextChar() {
        this.pos++;
        this.position++;
        if (this.position >= this.s.length()) {
            this.chr = '\u0000';
            return this.chr;
        }
        this.chr = this.s.charAt(this.position);
        if (this.chr == '\n') {
            this.line++;
            this.pos = 0;
        }
        return this.chr;
    }

    /**
     * Outputs information for each token
     */
    void printTokens(String path) throws IOException{
        Token t;
//        while ((t = getToken()).tokentype != TokenType.End_of_input) {
//            System.out.println(t);
//        }
//        System.out.println(t);
        FileWriter fw = new FileWriter(path);
        while ((t = getToken()).tokentype != TokenType.End_of_input) {
            fw.write(t.toString());
            fw.write(System.lineSeparator());
        }
        fw.write(t.toString());
        fw.close();
    }

    /**
     * Drives code.
     * @param args  [0] Path to input file
     *              [1] Path to output file
     */
    public static void main(String[] args) {
            try {
                File f = new File(args[0]);
                Scanner s = new Scanner(f);
                String source = " ";
                while (s.hasNext()) {
                    source += s.nextLine() + "\n";
                }
                Lexer l = new Lexer(source);
                l.printTokens(args[1]);
                s.close();
            } catch(FileNotFoundException e) {
                error(-1, -1, "Exception: " + e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
            }
    }
}