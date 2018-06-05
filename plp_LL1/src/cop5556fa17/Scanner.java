/* *
 * Scanner for the class project in COP5556 Programming Language Principles 
 * at the University of Florida, Fall 2017.
 * 
 * This software is solely for the educational benefit of students 
 * enrolled in the course during the Fall 2017 semester.  
 * 
 * This software, and any software derived from it,  may not be shared with others or posted to public web sites,
 * either during the course or afterwards.
 * 
 *  @Beverly A. Sanders, 2017
  */

package cop5556fa17;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Scanner {
	
	@SuppressWarnings("serial")
	public static class LexicalException extends Exception {
		
		int pos;

		public LexicalException(String message, int pos) {
			super(message);
			this.pos = pos;
		}
		
		public int getPos() { return pos; }

	}
	
	public static enum State {
		START,
		IN_LTCR,
		IN_FSLASH,
		IN_COMMENT,
		IN_EQUAL,
		IN_LESSTHAN,
		IN_GTRTHAN,
		IN_NEG,
		IN_STAR,
		IN_MINUS,
		IN_DIGIT,
		IN_STRING,
		IN_IDENT,
		EOS
		
	}

	public static enum Kind {
		IDENTIFIER, INTEGER_LITERAL, BOOLEAN_LITERAL, STRING_LITERAL, 
		KW_x/* x */, KW_X/* X */, KW_y/* y */, KW_Y/* Y */, KW_r/* r */, KW_R/* R */, KW_a/* a */, 
		KW_A/* A */, KW_Z/* Z */, KW_DEF_X/* DEF_X */, KW_DEF_Y/* DEF_Y */, KW_SCREEN/* SCREEN */, 
		KW_cart_x/* cart_x */, KW_cart_y/* cart_y */, KW_polar_a/* polar_a */, KW_polar_r/* polar_r */, 
		KW_abs/* abs */, KW_sin/* sin */, KW_cos/* cos */, KW_atan/* atan */, KW_log/* log */, 
		KW_image/* image */,  KW_int/* int */, 
		KW_boolean/* boolean */, KW_url/* url */, KW_file/* file */, OP_ASSIGN/* = */, OP_GT/* > */, OP_LT/* < */, 
		OP_EXCL/* ! */, OP_Q/* ? */, OP_COLON/* : */, OP_EQ/* == */, OP_NEQ/* != */, OP_GE/* >= */, OP_LE/* <= */, 
		OP_AND/* & */, OP_OR/* | */, OP_PLUS/* + */, OP_MINUS/* - */, OP_TIMES/* * */, OP_DIV/* / */, OP_MOD/* % */, 
		OP_POWER/* ** */, OP_AT/* @ */, OP_RARROW/* -> */, OP_LARROW/* <- */, LPAREN/* ( */, RPAREN/* ) */, 
		LSQUARE/* [ */, RSQUARE/* ] */, SEMI/* ; */, COMMA/* , */, EOF;
	}

	/** Class to represent Tokens. 
	 * 
	 * This is defined as a (non-static) inner class
	 * which means that each Token instance is associated with a specific 
	 * Scanner instance.  We use this when some token methods access the
	 * chars array in the associated Scanner.
	 * 
	 * 
	 * @author Beverly Sanders
	 *
	 */
	public class Token {
		public final Kind kind;
		public final int pos;
		public final int length;
		public final int line;
		public final int pos_in_line;

		public Token(Kind kind, int pos, int length, int line, int pos_in_line) {
			super();
			this.kind = kind;
			this.pos = pos;
			this.length = length;
			this.line = line;
			this.pos_in_line = pos_in_line;
		}

		public String getText() {
			if (kind == Kind.STRING_LITERAL) {
				return chars2String(chars, pos, length);
			}
			else return String.copyValueOf(chars, pos, length);
		}

		/**
		 * To get the text of a StringLiteral, we need to remove the
		 * enclosing " characters and convert escaped characters to
		 * the represented character.  For example the two characters \ t
		 * in the char array should be converted to a single tab character in
		 * the returned String
		 * 
		 * @param chars
		 * @param pos
		 * @param length
		 * @return
		 */
		private String chars2String(char[] chars, int pos, int length) {
			StringBuilder sb = new StringBuilder();
			for (int i = pos + 1; i < pos + length - 1; ++i) {// omit initial and final "
				char ch = chars[i];
				if (ch == '\\') { // handle escape
					i++;
					ch = chars[i];
					switch (ch) {
					case 'b':
						sb.append('\b');
						break;
					case 't':
						sb.append('\t');
						break;
					case 'f':
						sb.append('\f');
						break;
					case 'r':
						sb.append('\r'); //for completeness, line termination chars not allowed in String literals
						break;
					case 'n':
						sb.append('\n'); //for completeness, line termination chars not allowed in String literals
						break;
					case '\"':
						sb.append('\"');
						break;
					case '\'':
						sb.append('\'');
						break;
					case '\\':
						sb.append('\\');
						break;
					default:
						assert false;
						break;
					}
				} else {
					sb.append(ch);
				}
			}
			return sb.toString();
		}

		/**
		 * precondition:  This Token is an INTEGER_LITERAL
		 * 
		 * @returns the integer value represented by the token
		 */
		public int intVal() {
			assert kind == Kind.INTEGER_LITERAL;
			return Integer.valueOf(String.copyValueOf(chars, pos, length));
		}

		public String toString() {
			return "[" + kind + "," + String.copyValueOf(chars, pos, length)  + "," + pos + "," + length + "," + line + ","
					+ pos_in_line + "]";
		}

		/** 
		 * Since we overrode equals, we need to override hashCode.
		 * https://docs.oracle.com/javase/8/docs/api/java/lang/Object.html#equals-java.lang.Object-
		 * 
		 * Both the equals and hashCode method were generated by eclipse
		 * 
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((kind == null) ? 0 : kind.hashCode());
			result = prime * result + length;
			result = prime * result + line;
			result = prime * result + pos;
			result = prime * result + pos_in_line;
			return result;
		}

		/**
		 * Override equals method to return true if other object
		 * is the same class and all fields are equal.
		 * 
		 * Overriding this creates an obligation to override hashCode.
		 * 
		 * Both hashCode and equals were generated by eclipse.
		 * 
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Token other = (Token) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (kind != other.kind)
				return false;
			if (length != other.length)
				return false;
			if (line != other.line)
				return false;
			if (pos != other.pos)
				return false;
			if (pos_in_line != other.pos_in_line)
				return false;
			return true;
		}

		/**
		 * used in equals to get the Scanner object this Token is 
		 * associated with.
		 * @return
		 */
		private Scanner getOuterType() {
			return Scanner.this;
		}

		public boolean isKind(Kind kind) {
			// TODO Auto-generated method stub
			return this.kind == kind;
			
		}

		public Kind getKind() {
			// TODO Auto-generated method stub
			return this.kind;
		}
		
		

	}

	/** 
	 * Extra character added to the end of the input characters to simplify the
	 * Scanner.  
	 */
	static final char EOFchar = 0;
	
	/**
	 * The list of tokens created by the scan method.
	 */
	final ArrayList<Token> tokens;
	
	/**
	 * An array of characters representing the input.  These are the characters
	 * from the input string plus and additional EOFchar at the end.
	 */
	final char[] chars;  



	
	/**
	 * position of the next token to be returned by a call to nextToken
	 */
	private int nextTokenPos = 0;

	Scanner(String inputString) {
		int numChars = inputString.length();
		this.chars = Arrays.copyOf(inputString.toCharArray(), numChars + 1); // input string terminated with null char
		chars[numChars] = EOFchar;
		tokens = new ArrayList<Token>();
	}


	/**
	 * Method to scan the input and create a list of Tokens.
	 * 
	 * If an error is encountered during scanning, throw a LexicalException.
	 * 
	 * @return
	 * @throws LexicalException
	 */
	public Scanner scan() throws LexicalException {
		/* TODO  Replace this with a correct and complete implementation!!! */
		int pos = 0; 
		int line = 1;
		int posInLine = 1;
		int startPos = 0;
		
		initializeHashMap();
		
		State state = State.START;
		    
		while (pos < chars.length) 
		{        
			char ch = chars[pos];        
			switch (state) {            
			case START: {
				startPos = pos;
				switch (ch) {
				case '\n': //LF
				{
					pos++;
					line++;
					posInLine = 1;
					
				} break;
				case '\r' : //CR
				{
					pos++;
					state = State.IN_LTCR;
				} break;
				case ' ' : //SP
				{
					pos++;
					posInLine++;
				} break;
				case '\t' : //HT
				{
					pos++;
					posInLine++;
					
				} break;
				case '\f': //FF
				{
					pos++;
					posInLine++;
				} break;
				case '/': // Forward Slash
				{
					pos++;
					state = State.IN_FSLASH;
				} break;
				case '(':
				{
					tokens.add(new Token(Kind.LPAREN, pos, 1, line, posInLine));
					pos++;
					posInLine++;
				} break;
				case ')':
				{
					tokens.add(new Token(Kind.RPAREN, pos, 1, line, posInLine));
					pos++;
					posInLine++;
				} break;
				case '[':
				{
					tokens.add(new Token(Kind.LSQUARE, pos, 1, line, posInLine));
					pos++;
					posInLine++;
				} break;
				case ']':
				{
					tokens.add(new Token(Kind.RSQUARE, pos, 1, line, posInLine));
					pos++;
					posInLine++;
				} break;
				case ';':
				{
					tokens.add(new Token(Kind.SEMI, pos, 1, line, posInLine));
					pos++;
					posInLine++;
				} break;
				case ',':
				{
					tokens.add(new Token(Kind.COMMA, pos, 1, line, posInLine));
					pos++;
					posInLine++;
				} break;
				case '=':
				{
					pos++;
					state = State.IN_EQUAL;
				} break;
				case '>':
				{
					pos++;
					state = State.IN_GTRTHAN;
				} break;
				case '<':
				{
					pos++;
					state = State.IN_LESSTHAN;
				} break;
				case '!':
				{
					pos++;
					state = State.IN_NEG;
				} break;
				case '?':
				{
					tokens.add(new Token(Kind.OP_Q, pos, 1, line, posInLine));
					pos++;
					posInLine++;
				} break;
				case ':':
				{
					tokens.add(new Token(Kind.OP_COLON, pos, 1, line, posInLine));
					pos++;
					posInLine++;
				} break;
				case '&':
				{
					tokens.add(new Token(Kind.OP_AND, pos, 1, line, posInLine));
					pos++;
					posInLine++;
				} break;
				case '|':
				{
					tokens.add(new Token(Kind.OP_OR, pos, 1, line, posInLine));
					pos++;
					posInLine++;
				} break;
				case '+':
				{
					tokens.add(new Token(Kind.OP_PLUS, pos, 1, line, posInLine));
					pos++;
					posInLine++;
				} break;
				case '-':
				{
					pos++;
					state = State.IN_MINUS;
				} break;
				case '*':
				{
					pos++;
					state = State.IN_STAR;
				} break;
				case '%':
				{
					tokens.add(new Token(Kind.OP_MOD, pos, 1, line, posInLine));
					pos++;
					posInLine++;
				} break;
				case '@':
				{
					tokens.add(new Token(Kind.OP_AT, pos, 1, line, posInLine));
					pos++;
					posInLine++;
				} break;
				case '0':
				{
					tokens.add(new Token(Kind.INTEGER_LITERAL, pos, 1, line, posInLine));
					pos++;
					posInLine++;
				} break;
				case '\"':
				{
					pos++;
					state = State.IN_STRING;
				} break;
				case EOFchar: // \0 or ASCII 0
				{
					tokens.add(new Token(Kind.EOF, pos, 0, line, posInLine)); // why length 0 by prof
					pos = chars.length;
				} break;
				default :
				{
					if (isDigit(ch)) {
						pos++;
						state = State.IN_DIGIT;
					} else if (isIdentifierStart(ch)) {
						pos++;
						state = State.IN_IDENT;
					} else {
						throw new LexicalException("Invalid Character occured at "+pos, pos);
					}
				}
				}
			} break;            
			case IN_LTCR: {
				switch (ch) {
				case '\n':
				{
					pos++;
					line++;
					posInLine = 1;
					state = State.START;
				}break;
				default:
				{
					line++;
					posInLine = 1;
					state = State.START;					
				}
			    }
				
			} break;            
			case IN_FSLASH: {
				switch (ch) {
				case '/':
				{
					pos++;
					//posInLine = posInLine + 2;
					state = State.IN_COMMENT;
				} break;
				default:
				{
					tokens.add(new Token(Kind.OP_DIV,startPos,1,line,posInLine));
					posInLine++;
					state = State.START;
				}
				}
			} break;            
			case IN_COMMENT: {
				while (ch != EOFchar && ch != '\r' && ch != '\n')
				{
					pos++;
					ch = chars[pos];
				}
				state = State.START;
			} break;
			case IN_EQUAL: {
				switch (ch) {
				case '=':
				{
					tokens.add(new Token(Kind.OP_EQ,startPos,2,line,posInLine));
					pos++;
					posInLine = posInLine + 2;
					state = State.START;
				} break;
				default:
				{
					tokens.add(new Token(Kind.OP_ASSIGN,startPos,1,line,posInLine));
					posInLine++;
					state = State.START;
				}
				}
			} break;
			case IN_GTRTHAN: {
				switch (ch) {
				case '=':
				{
					tokens.add(new Token(Kind.OP_GE,startPos,2,line,posInLine));
					pos++;
					posInLine = posInLine + 2;
					state = State.START;
				} break;
				default:
				{
					tokens.add(new Token(Kind.OP_GT,startPos,1,line,posInLine));
					posInLine++;
					state = State.START;
				}
				}
			} break;
			case IN_LESSTHAN:{
				switch (ch) {
				case '=':
				{
					tokens.add(new Token(Kind.OP_LE,startPos,2,line,posInLine));
					pos++;
					posInLine = posInLine + 2;
					state = State.START;
				} break;
				case '-':
				{
					tokens.add(new Token(Kind.OP_LARROW,startPos,2,line,posInLine));
					pos++;
					posInLine = posInLine + 2;
					state = State.START;
				} break;
				default:
				{
					tokens.add(new Token(Kind.OP_LT,startPos,1,line,posInLine));
					posInLine++;
					state = State.START;
				}
				}
			} break;
			case IN_NEG: {
				switch (ch) {
				case '=' :
				{
					tokens.add(new Token(Kind.OP_NEQ,startPos,2,line,posInLine));
					pos++;
					posInLine = posInLine + 2;
					state = State.START;
				} break;
				default:
				{
					tokens.add(new Token(Kind.OP_EXCL,startPos,1,line,posInLine));
					posInLine++;
					state = State.START;
				}
				}
			} break;
			case IN_MINUS: {
				switch (ch) {
				case '>':
				{
					tokens.add(new Token(Kind.OP_RARROW,startPos,2,line,posInLine));
					pos++;
					posInLine = posInLine + 2;
					state = State.START;
				} break;
				default:
				{
					tokens.add(new Token(Kind.OP_MINUS,startPos,1,line,posInLine));
					posInLine++;
					state = State.START;
				}
				}
			} break;
			case IN_STAR: {
				switch (ch) {
				case '*':
				{
					tokens.add(new Token(Kind.OP_POWER,startPos,2,line,posInLine));
					pos++;
					posInLine = posInLine + 2;
					state = State.START;
				} break;
				default:
				{
					tokens.add(new Token(Kind.OP_TIMES,startPos,1,line,posInLine));
					posInLine++;
					state = State.START;
				}
				}
			} break;
			case IN_DIGIT: {
				while(isDigit(ch)) {
					pos++;
					ch = chars[pos];
				}
				String s = new String(chars, startPos, pos-startPos);
				try {
					int i = Integer.parseInt(s);
					tokens.add(new Token(Kind.INTEGER_LITERAL,startPos,pos-startPos,line,posInLine));
					posInLine = posInLine + pos-startPos;
					state = State.START;
				} catch (NumberFormatException nfe) {
					throw new LexicalException("Number Format Exception at "+startPos, startPos);
				} // do i need to use finally block to set state back to start
			} break;
			case IN_STRING: {
				while (ch != '\r' && ch != '\n' && ch != '\"' && ch != EOFchar) {
					if (ch == '\\') {
						pos++;
						ch = chars[pos];
						if (ch == 'b' || ch == 't' || ch == 'n' || ch == 'f' || ch == 'r' ||
								ch == '\"' || ch == '\'' || ch == '\\') {
							
						} else {
							throw new LexicalException("Not an Escape Sequence at "+pos, pos);
						}
					}
					pos++;
					ch = chars[pos];
				}
				switch (ch) {
				case '\r':
				{
					throw new LexicalException("Line Terminator should not be at "+pos, pos);
				} //break;
				case '\n':
				{
					throw new LexicalException("Line Terminator should not be at "+pos, pos);
				} //break;
				case '\"':
				{
					pos++;
					tokens.add(new Token(Kind.STRING_LITERAL,startPos,pos-startPos,line,posInLine));
					posInLine = posInLine + pos-startPos;
					state = State.START;
				} break;
				case EOFchar:
				{
					throw new LexicalException("String Literal not terminated at "+pos, pos);
				} //break;
				}
			} break;
			case IN_IDENT: {
				while(isIdentifierStart(ch)||isDigit(ch)) {
					pos++;
					ch = chars[pos];
				}
				String s = new String(chars, startPos, pos-startPos);
				if (hm.containsKey(s)) {
					tokens.add(new Token(hm.get(s),startPos,pos-startPos,line,posInLine));
				} else {
					tokens.add(new Token(Kind.IDENTIFIER,startPos,pos-startPos,line,posInLine));
				}
				posInLine = posInLine + pos-startPos;
				state = State.START;
			} break;
			default:        
			}// switch(state)    
		} // while    
		return this;
	}


	/**
	 * Returns true if the internal interator has more Tokens
	 * 
	 * @return
	 */
	public boolean hasTokens() {
		return nextTokenPos < tokens.size();
	}

	/**
	 * Returns the next Token and updates the internal iterator so that
	 * the next call to nextToken will return the next token in the list.
	 * 
	 * It is the callers responsibility to ensure that there is another Token.
	 * 
	 * Precondition:  hasTokens()
	 * @return
	 */
	public Token nextToken() {
		return tokens.get(nextTokenPos++);
	}
	
	/**
	 * Returns the next Token, but does not update the internal iterator.
	 * This means that the next call to nextToken or peek will return the
	 * same Token as returned by this methods.
	 * 
	 * It is the callers responsibility to ensure that there is another Token.
	 * 
	 * Precondition:  hasTokens()
	 * 
	 * @return next Token.
	 */
	public Token peek() {
		return tokens.get(nextTokenPos);
	}
	
	
	/**
	 * Resets the internal iterator so that the next call to peek or nextToken
	 * will return the first Token.
	 */
	public void reset() {
		nextTokenPos = 0;
	}

	/**
	 * Returns a String representation of the list of Tokens 
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Tokens:\n");
		for (int i = 0; i < tokens.size(); i++) {
			sb.append(tokens.get(i)).append('\n');
		}
		return sb.toString();
	}
	
	private boolean isDigit(char ch) {
		if (ch == '0' || ch == '1' || ch == '2' || ch == '3' || ch == '4' || ch == '5' ||
				ch == '6' || ch == '7' || ch == '8' || ch == '9') {
			return true;
		} else {
			return false;
		}
	}
	private boolean isIdentifierStart(char ch) {
		if (ch >= 65 && ch <= 90 || ch >= 97 && ch <= 122 || ch == 36 || ch == 95) {
			return true;
		} else {
			return false;
		}
	}
	private void initializeHashMap() {
		hm = new HashMap<String,Scanner.Kind>();
		//keywords
		hm.put("x", Kind.KW_x);
		hm.put("X", Kind.KW_X);
		hm.put("y", Kind.KW_y);
		hm.put("Y", Kind.KW_Y);
		hm.put("r", Kind.KW_r);
		hm.put("R", Kind.KW_R);
		hm.put("a", Kind.KW_a);
		hm.put("A", Kind.KW_A);
		hm.put("Z", Kind.KW_Z);
		hm.put("DEF_X", Kind.KW_DEF_X);
		hm.put("DEF_Y", Kind.KW_DEF_Y);
		hm.put("SCREEN", Kind.KW_SCREEN);
		hm.put("cart_x", Kind.KW_cart_x);
		hm.put("cart_y", Kind.KW_cart_y);
		hm.put("polar_a", Kind.KW_polar_a);
		hm.put("polar_r", Kind.KW_polar_r);
		hm.put("abs", Kind.KW_abs);
		hm.put("sin", Kind.KW_sin);
		hm.put("cos", Kind.KW_cos);
		hm.put("atan", Kind.KW_atan);
		hm.put("log", Kind.KW_log);
		hm.put("image", Kind.KW_image);
		hm.put("int", Kind.KW_int);
		hm.put("boolean", Kind.KW_boolean);
		hm.put("url", Kind.KW_url);
		hm.put("file", Kind.KW_file);
		//boolean literals
		hm.put("true", Kind.BOOLEAN_LITERAL);
		hm.put("false", Kind.BOOLEAN_LITERAL);
	}
	private HashMap<String,Scanner.Kind> hm;

}
