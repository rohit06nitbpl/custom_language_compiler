package cop5556fa17;



import java.awt.List;
import java.util.ArrayList;
import java.util.Arrays;

import cop5556fa17.Scanner.Kind;
import cop5556fa17.Scanner.Token;
import cop5556fa17.AST.ASTNode;
import cop5556fa17.AST.Declaration;
import cop5556fa17.AST.Declaration_Image;
import cop5556fa17.AST.Declaration_SourceSink;
import cop5556fa17.AST.Declaration_Variable;
import cop5556fa17.AST.Expression;
import cop5556fa17.AST.Expression_Binary;
import cop5556fa17.AST.Expression_BooleanLit;
import cop5556fa17.AST.Expression_Conditional;
import cop5556fa17.AST.Expression_FunctionAppWithExprArg;
import cop5556fa17.AST.Expression_FunctionAppWithIndexArg;
import cop5556fa17.AST.Expression_Ident;
import cop5556fa17.AST.Expression_IntLit;
import cop5556fa17.AST.Expression_PixelSelector;
import cop5556fa17.AST.Expression_PredefinedName;
import cop5556fa17.AST.Expression_Unary;
import cop5556fa17.AST.Index;
import cop5556fa17.AST.LHS;
import cop5556fa17.AST.Program;
import cop5556fa17.AST.Sink;
import cop5556fa17.AST.Sink_Ident;
import cop5556fa17.AST.Sink_SCREEN;
import cop5556fa17.AST.Source;
import cop5556fa17.AST.Source_CommandLineParam;
import cop5556fa17.AST.Source_Ident;
import cop5556fa17.AST.Source_StringLiteral;
import cop5556fa17.AST.Statement;
import cop5556fa17.AST.Statement_Assign;
import cop5556fa17.AST.Statement_In;
import cop5556fa17.AST.Statement_Out;
import cop5556fa17.Parser.SyntaxException;

import static cop5556fa17.Scanner.Kind.*;

public class Parser {

	@SuppressWarnings("serial")
	public class SyntaxException extends Exception {
		Token t;

		public SyntaxException(Token t, String message) {
			super(message);
			this.t = t;
		}

	}


	Scanner scanner;
	Token t;

	Parser(Scanner scanner) {
		this.scanner = scanner;
		t = scanner.nextToken();
	}

	/**
	 * Main method called by compiler to parser input.
	 * Checks for EOF
	 * @return 
	 * 
	 * @throws SyntaxException
	 */
	public Program parse() throws SyntaxException {
		Program p = program();
		matchEOF();
		return p;
	}
	

	/**
	 * Program ::=  IDENTIFIER   ( Declaration SEMI | Statement SEMI )*   
	 * 
	 * Program is start symbol of our grammar.
	 * @return 
	 * 
	 * @throws SyntaxException
	 */
	Program program() throws SyntaxException {
		Token firstToken = this.t;
		Token name = consume(IDENTIFIER,"@program");
		ArrayList<ASTNode> a_list = new ArrayList<ASTNode>();
		while(first_decl.contains(this.t.getKind()) || first_stmt.contains(this.t.getKind())) {
			if (first_decl.contains(this.t.getKind())) {
				a_list.add(decl());
				consume(SEMI,"@program");
			} else if(first_stmt.contains(this.t.getKind())) {
				a_list.add(stmt());
				consume(SEMI,"@program");
			} else {
				throw new SyntaxException(this.t,"@program");
			}
			
		}
		return new Program(firstToken,name,a_list);
	}
	
	
	private Declaration decl() throws SyntaxException {
		Token firstToken = this.t;
		if(first_var_decl.contains(this.t.getKind())) {
			Token _type = consume();
			Token _name = consume(IDENTIFIER,"@decl");
			Expression _e;
			if(this.t.isKind(OP_ASSIGN)) {
				consume();
				 _e = expression();
			} else {
				_e = null; // epsilon
			}
			return new Declaration_Variable(firstToken,_type,_name,_e);
		} else if(first_img_decl.contains(this.t.getKind())) {
			Expression _xSize,_ySize;
			consume();
			if (this.t.isKind(LSQUARE)) {
				consume();
				_xSize = expression();
				consume(COMMA,"@decl");
				_ySize = expression();
				consume(RSQUARE,"@decl");
			} else {
				// epsilon
				_xSize = null;
				_ySize = null;
			}
			Token _name = consume(IDENTIFIER,"@decl");
			Source _source;
			if (this.t.isKind(OP_LARROW)) {
				consume();
				_source = src();
			} else {
				_source = null; // epsilon
			}
			return new Declaration_Image(firstToken,_xSize, _ySize, _name, _source);
		} else if(first_src_sink_decl.contains(this.t.getKind())) {
			Token _type = consume();
			Token _name = consume(IDENTIFIER,"@decl");
			consume(OP_ASSIGN,"@decl");
			Source _source = src();
			return new Declaration_SourceSink(firstToken,_type,_name,_source);
		} else {
			throw new SyntaxException(this.t,"@decl");
		}
	}
	
	private Statement stmt() throws SyntaxException {
		Token firstToken = this.t;
		Token _name = consume(IDENTIFIER,"@stmt");
		if (this.t.isKind(LSQUARE) || this.t.isKind(OP_ASSIGN)) {
			Index _index;
			if (this.t.isKind(LSQUARE)) {
				consume();
				_index = lhs_sel();
				consume(RSQUARE,"@stml");
			} else {
				_index = null; //epsilon
			}
			LHS _lhs = new LHS(firstToken,_name,_index);
			consume(OP_ASSIGN,"@stmt");
			Expression _e = expression();
			return new Statement_Assign(firstToken,_lhs,_e);
		} else if(this.t.isKind(OP_RARROW) || this.t.isKind(OP_LARROW)) {
			if (this.t.isKind(OP_RARROW)) {
				consume();
				Sink _sink = sink();
				return new Statement_Out(firstToken, _name, _sink);
			} else if (this.t.isKind(OP_LARROW)) {
				consume();
				Source _source = src();
				return new Statement_In(firstToken, _name, _source);
			} else {
				throw new SyntaxException(this.t,"@stmt");
			}
			
		} else {
			throw new SyntaxException(this.t,"@stmt");
		}
	}
	
	private Index lhs_sel() throws SyntaxException {
		Index _i = null;
		consume(LSQUARE,"@lhs_sel");
		if(this.t.isKind(KW_x) || this.t.isKind(KW_r)) {
			if(this.t.isKind(KW_x)) {
				Token e0_firstToken = consume();
				Expression_PredefinedName e0 = new Expression_PredefinedName(e0_firstToken,KW_x);
				consume(COMMA,"@lhs_sel");
				Token e1_firstToken = consume(KW_y,"@lhs_sel");
				Expression_PredefinedName e1 = new Expression_PredefinedName(e1_firstToken,KW_y);
				_i = new Index(e0_firstToken,e0,e1);
			} else if(this.t.isKind(KW_r)) {
				Token e0_firstToken = consume();
				Expression_PredefinedName e0 = new Expression_PredefinedName(e0_firstToken,KW_r);
				consume(COMMA,"@lhs_sel");
				Token e1_firstToken = consume(KW_a,"@lhs_sel");
				Expression_PredefinedName e1 = new Expression_PredefinedName(e1_firstToken,KW_A);
				_i = new Index(e0_firstToken,e0,e1);
			} else {
				throw new SyntaxException(this.t,"@lhs_sel");
			}
		}
		consume(RSQUARE,"@program");
		return _i;
	}

	private Source src() throws SyntaxException {
		Token firstToken = this.t;
		if(this.t.isKind(STRING_LITERAL)) {
			consume();
			return new Source_StringLiteral(firstToken,firstToken.getText());
		} else if (this.t.isKind(OP_AT)) {
			consume();
			Expression _paramNum = expression();
			return new Source_CommandLineParam(firstToken, _paramNum);
		} else if (this.t.isKind(IDENTIFIER)) {
			consume();
			return new Source_Ident(firstToken, firstToken);
		} else {
			throw new SyntaxException(this.t,"@src");
		}
	}
	
	private Sink sink() throws SyntaxException {
		Token firstToken = this.t;
		if (this.t.isKind(IDENTIFIER)) {
			return new Sink_Ident(firstToken,consume());
		} else if(this.t.isKind(KW_SCREEN)) {
			consume();
			return new Sink_SCREEN(firstToken);
		} else {
			throw new SyntaxException(this.t,"@sink");
		}
	}


	/**
	 * Expression ::=  OrExpression  OP_Q  Expression OP_COLON Expression    | OrExpression
	 * 
	 * Our test cases may invoke this routine directly to support incremental development.
	 * 
	 * @throws SyntaxException
	 */
	Expression expression() throws SyntaxException {
		Token firstToken = this.t;
		Expression _expr = null;
		Expression _or_expr = or_expr();
		if (this.t.isKind(OP_Q)) {
			consume();
			Expression _true_expr = expression();
			consume(OP_COLON,"@expression");
			Expression _false_expr = expression();
			_expr = new Expression_Conditional(firstToken, _or_expr, _true_expr, _false_expr);
		} else {
			_expr = _or_expr; // epsilon
		}
		return _expr;
	}


	private Expression or_expr() throws SyntaxException {
		Token firstToken = this.t;
		Expression _e0 = null;
		Expression _e1 = null;
		_e0 = and_expr();
		while(this.t.isKind(OP_OR)) {
			Token _op = consume();
			_e1 = and_expr();
			_e0 = new Expression_Binary(firstToken,_e0,_op,_e1);
		}
		return _e0;
	}

	private Expression and_expr() throws SyntaxException {
		Token firstToken = this.t;
		Expression _e0 = null;
		Expression _e1 = null;
		_e0= eq_expr();
		while(this.t.isKind(OP_AND)) {
			Token _op = consume();
			_e1 = eq_expr();
			_e0 = new Expression_Binary(firstToken,_e0,_op,_e1);
		}
		return _e0;
	}

	private Expression eq_expr() throws SyntaxException {
		Token firstToken = this.t;
		Expression _e0 = null;
		Expression _e1 = null;
		_e0 = rel_expr();
		while(this.t.isKind(OP_EQ) || this.t.isKind(OP_NEQ)) {
			Token _op = consume();
			_e1 = rel_expr();
			_e0 = new Expression_Binary(firstToken,_e0,_op,_e1);
		}
		return _e0;
	}

	private Expression rel_expr() throws SyntaxException {
		Token firstToken = this.t;
		Expression _e0 = null;
		Expression _e1 = null;
		_e0 = add_expr();
		while(this.t.isKind(OP_LT) || this.t.isKind(OP_GT) || this.t.isKind(OP_LE) || this.t.isKind(OP_GE)) {
			Token _op = consume();
			_e1 = add_expr();
			_e0 = new Expression_Binary(firstToken,_e0,_op,_e1);
		}
		return _e0;
	}

	private Expression add_expr() throws SyntaxException {
		Token firstToken = this.t;
		Expression _e0 = null;
		Expression _e1 = null;
		_e0 = mult_expr();
		while(this.t.isKind(OP_PLUS) || this.t.isKind(OP_MINUS)) {
			Token _op = consume();
			_e1 = mult_expr();
			_e0 = new Expression_Binary(firstToken,_e0,_op,_e1);
		}
		return _e0;
	}

	private Expression mult_expr() throws SyntaxException {
		Token firstToken = this.t;
		Expression _e0 = null;
		Expression _e1 = null;
		_e0 = unary_expr();
		while(this.t.isKind(OP_TIMES) || this.t.isKind(OP_DIV) || this.t.isKind(OP_MOD)) {
			Token _op = consume();
			_e1 = unary_expr();
			_e0 = new Expression_Binary(firstToken,_e0,_op,_e1);
		}
		return _e0;
	}

	private Expression unary_expr() throws SyntaxException {
		Token firstToken = this.t;
		if(this.t.isKind(OP_PLUS)) {
			Token _op = consume();
			Expression _e = unary_expr();
			return new Expression_Unary(firstToken,_op, _e);
		} else if (this.t.isKind(OP_MINUS)) {
			Token _op = consume();
			Expression _e = unary_expr();
			return new Expression_Unary(firstToken,_op, _e);
		} else if (first_unary_expr_not_plus_minus.contains(this.t.getKind())) {
			return unary_expr_not_plus_minus();
		} else {
			throw new SyntaxException(this.t,"@unary_expr");
		}
		
	}

	private Expression unary_expr_not_plus_minus() throws SyntaxException {
		Token firstToken = this.t;
		if (this.t.isKind(OP_EXCL)) {
			Token _op = consume();
			Expression _e = unary_expr();
			return new Expression_Unary(firstToken,_op, _e);
		} else if(first_primary.contains(this.t.getKind())) {
			return primary();
		} else if(first_ident_pixel_sel_expr.contains(this.t.getKind())) {
			return ident_pixel_sel_expr();
		} else if(rem_unary_expr_not_plus_minus.contains(this.t.getKind())) {
			consume();
			return new Expression_PredefinedName(firstToken,firstToken.getKind());
		} else {
			throw new SyntaxException(this.t,"@unary_expr_not_plus_minus");
		}
	}

	private Expression ident_pixel_sel_expr() throws SyntaxException {
		Token firstToken = this.t;
		Token _name = consume(IDENTIFIER, "@ident_pixel_sel_expr");
		Index _index = null;
		if (this.t.isKind(LSQUARE)) {
			consume();
			_index = sel();
			consume(RSQUARE, "@ident_pixel_sel_expr");
		} else {
			//epsilon
			return new Expression_Ident(firstToken, _name);
		}
		return new Expression_PixelSelector(firstToken, _name, _index);
	}

	private Index sel() throws SyntaxException {
		Token firstToken = this.t;
		Expression _e0 = expression();
		consume(COMMA, "@sel");
		Expression _e1 = expression();
		return new Index(firstToken,_e0,_e1);
	}

	private Expression primary() throws SyntaxException {
		Token firstToken = this.t;
		if (this.t.isKind(INTEGER_LITERAL)) {
			consume();
			return new Expression_IntLit(firstToken, firstToken.intVal());
		} else if (this.t.isKind(LPAREN)) {
			consume();
			Expression _e = expression();
			consume(RPAREN, "@primary");
			return _e;
		} else if (first_func_app.contains(this.t.getKind())) {
			return func_app();
		} else if (this.t.isKind(BOOLEAN_LITERAL)) {
			consume();
			return new Expression_BooleanLit(firstToken,Boolean.parseBoolean(firstToken.getText())); 
		} else {
			throw new SyntaxException(this.t,"@primary");
		}
		
	}

	private Expression func_app() throws SyntaxException {
		Token firstToken = this.t;
		Token func_name = func_name();
		if (this.t.isKind(LPAREN)) {
			consume();
			Expression _args = expression();
			consume(RPAREN, "@func_app");
			return new Expression_FunctionAppWithExprArg(firstToken,func_name.getKind(),_args);
		} else if(this.t.isKind(LSQUARE)) {
			consume();
			Index _args = sel();
			consume(RSQUARE, "@func_app");
			return new Expression_FunctionAppWithIndexArg(firstToken,func_name.getKind(),_args);
		} else {
			throw new SyntaxException(this.t,"@func_app");
		}
	}

	private Token func_name() throws SyntaxException {
		// TODO Auto-generated method stub
		if (first_func_name.contains(this.t.getKind())) {
			return consume();
		} else {
			throw new SyntaxException(this.t,"@func_name");
		}
		
	}

	/**
	 * Only for check at end of program. Does not "consume" EOF so no attempt to get
	 * nonexistent next Token.
	 * 
	 * @return
	 * @throws SyntaxException
	 */
	private Token matchEOF() throws SyntaxException {
		if (t.kind == EOF) {
			return t;
		}
		String message =  "Expected EOL at " + t.line + ":" + t.pos_in_line;
		throw new SyntaxException(t, message);
	}
	
	private Token consume() {
		Token retValue = this.t;
		this.t = this.scanner.nextToken();
		return retValue;
	}
	
	
	private Token consume(Kind kind,String msg) throws SyntaxException {
		// TODO Auto-generated method stub
		if(this.t.isKind(kind)) {
			return consume();
		} else {
			throw new SyntaxException(this.t,msg);
		}
	}


	final ArrayList<Scanner.Kind> first_var_decl = new ArrayList<>(Arrays.asList(KW_int, KW_boolean));
	final ArrayList<Scanner.Kind> first_img_decl = new ArrayList<>(Arrays.asList(KW_image));
	final ArrayList<Scanner.Kind> first_src_sink_decl = new ArrayList<>(Arrays.asList(KW_url, KW_file));
	final ArrayList<Scanner.Kind> first_decl = new ArrayList<>(Arrays.asList(KW_int, KW_boolean,KW_image,KW_url, KW_file));
	final ArrayList<Scanner.Kind> first_stmt = new ArrayList<>(Arrays.asList(IDENTIFIER));
	final ArrayList<Scanner.Kind> first_src = new ArrayList<>(Arrays.asList(STRING_LITERAL , OP_AT , IDENTIFIER));
	final ArrayList<Scanner.Kind> first_sink = new ArrayList<>(Arrays.asList(IDENTIFIER , KW_SCREEN));
	final ArrayList<Scanner.Kind> first_func_name = new ArrayList<>(Arrays.asList(KW_sin , KW_cos , KW_atan , KW_abs , KW_cart_x , KW_cart_y , KW_polar_a , KW_polar_r));
	final ArrayList<Scanner.Kind> first_func_app = new ArrayList<>(Arrays.asList(KW_sin , KW_cos , KW_atan , KW_abs , KW_cart_x , KW_cart_y , KW_polar_a , KW_polar_r));
	final ArrayList<Scanner.Kind> first_primary = new ArrayList<>(Arrays.asList(INTEGER_LITERAL,  LPAREN, KW_sin , KW_cos , KW_atan , KW_abs , KW_cart_x , KW_cart_y , KW_polar_a , KW_polar_r , BOOLEAN_LITERAL));
	final ArrayList<Scanner.Kind> first_ident_pixel_sel_expr = new ArrayList<>(Arrays.asList(IDENTIFIER));
	final ArrayList<Scanner.Kind> first_unary_expr_not_plus_minus = new ArrayList<>(Arrays.asList(OP_EXCL ,  INTEGER_LITERAL,  LPAREN, KW_sin , KW_cos , KW_atan , KW_abs , KW_cart_x , KW_cart_y , KW_polar_a , KW_polar_r , BOOLEAN_LITERAL, IDENTIFIER, KW_x , KW_y , KW_r , KW_a , KW_X , KW_Y, KW_Z , KW_A , KW_R , KW_DEF_X , KW_DEF_Y));
	final ArrayList<Scanner.Kind> first_lhs_sel = new ArrayList<>(Arrays.asList(LSQUARE));
	final ArrayList<Scanner.Kind> first_unary_expr = new ArrayList<>(Arrays.asList(OP_PLUS ,  OP_MINUS, OP_EXCL ,  INTEGER_LITERAL,  LPAREN, KW_sin , KW_cos , KW_atan , KW_abs , KW_cart_x , KW_cart_y , KW_polar_a , KW_polar_r , BOOLEAN_LITERAL, IDENTIFIER, KW_x , KW_y , KW_r , KW_a , KW_X , KW_Y, KW_Z , KW_A , KW_R , KW_DEF_X , KW_DEF_Y));
	final ArrayList<Scanner.Kind> first_mult_expr = new ArrayList<>(Arrays.asList(OP_PLUS ,  OP_MINUS, OP_EXCL ,  INTEGER_LITERAL,  LPAREN, KW_sin , KW_cos , KW_atan , KW_abs , KW_cart_x , KW_cart_y , KW_polar_a , KW_polar_r , BOOLEAN_LITERAL, IDENTIFIER, KW_x , KW_y , KW_r , KW_a , KW_X , KW_Y, KW_Z , KW_A , KW_R , KW_DEF_X , KW_DEF_Y));
	final ArrayList<Scanner.Kind> first_add_expr = new ArrayList<>(Arrays.asList(OP_PLUS ,  OP_MINUS, OP_EXCL ,  INTEGER_LITERAL,  LPAREN, KW_sin , KW_cos , KW_atan , KW_abs , KW_cart_x , KW_cart_y , KW_polar_a , KW_polar_r , BOOLEAN_LITERAL, IDENTIFIER, KW_x , KW_y , KW_r , KW_a , KW_X , KW_Y, KW_Z , KW_A , KW_R , KW_DEF_X , KW_DEF_Y));
	final ArrayList<Scanner.Kind> first_rel_expr = new ArrayList<>(Arrays.asList(OP_PLUS ,  OP_MINUS, OP_EXCL ,  INTEGER_LITERAL,  LPAREN, KW_sin , KW_cos , KW_atan , KW_abs , KW_cart_x , KW_cart_y , KW_polar_a , KW_polar_r , BOOLEAN_LITERAL, IDENTIFIER, KW_x , KW_y , KW_r , KW_a , KW_X , KW_Y, KW_Z , KW_A , KW_R , KW_DEF_X , KW_DEF_Y));
	final ArrayList<Scanner.Kind> first_eq_expr = new ArrayList<>(Arrays.asList(OP_PLUS ,  OP_MINUS, OP_EXCL ,  INTEGER_LITERAL,  LPAREN, KW_sin , KW_cos , KW_atan , KW_abs , KW_cart_x , KW_cart_y , KW_polar_a , KW_polar_r , BOOLEAN_LITERAL, IDENTIFIER, KW_x , KW_y , KW_r , KW_a , KW_X , KW_Y, KW_Z , KW_A , KW_R , KW_DEF_X , KW_DEF_Y));
	final ArrayList<Scanner.Kind> first_and_expr = new ArrayList<>(Arrays.asList(OP_PLUS ,  OP_MINUS, OP_EXCL ,  INTEGER_LITERAL,  LPAREN, KW_sin , KW_cos , KW_atan , KW_abs , KW_cart_x , KW_cart_y , KW_polar_a , KW_polar_r , BOOLEAN_LITERAL, IDENTIFIER, KW_x , KW_y , KW_r , KW_a , KW_X , KW_Y, KW_Z , KW_A , KW_R , KW_DEF_X , KW_DEF_Y));
	final ArrayList<Scanner.Kind> first_or_expr = new ArrayList<>(Arrays.asList(OP_PLUS ,  OP_MINUS, OP_EXCL ,  INTEGER_LITERAL,  LPAREN, KW_sin , KW_cos , KW_atan , KW_abs , KW_cart_x , KW_cart_y , KW_polar_a , KW_polar_r , BOOLEAN_LITERAL, IDENTIFIER, KW_x , KW_y , KW_r , KW_a , KW_X , KW_Y, KW_Z , KW_A , KW_R , KW_DEF_X , KW_DEF_Y));
	final ArrayList<Scanner.Kind> first_expr = new ArrayList<>(Arrays.asList(OP_PLUS ,  OP_MINUS, OP_EXCL ,  INTEGER_LITERAL,  LPAREN, KW_sin , KW_cos , KW_atan , KW_abs , KW_cart_x , KW_cart_y , KW_polar_a , KW_polar_r , BOOLEAN_LITERAL, IDENTIFIER, KW_x , KW_y , KW_r , KW_a , KW_X , KW_Y, KW_Z , KW_A , KW_R , KW_DEF_X , KW_DEF_Y));
	final ArrayList<Scanner.Kind> first_sel = new ArrayList<>(Arrays.asList(OP_PLUS ,  OP_MINUS, OP_EXCL ,  INTEGER_LITERAL,  LPAREN, KW_sin , KW_cos , KW_atan , KW_abs , KW_cart_x , KW_cart_y , KW_polar_a , KW_polar_r , BOOLEAN_LITERAL, IDENTIFIER, KW_x , KW_y , KW_r , KW_a , KW_X , KW_Y, KW_Z , KW_A , KW_R , KW_DEF_X , KW_DEF_Y));
	final ArrayList<Scanner.Kind> rem_unary_expr_not_plus_minus = new ArrayList<>(Arrays.asList(KW_x , KW_y , KW_r , KW_a , KW_X , KW_Y, KW_Z , KW_A , KW_R , KW_DEF_X , KW_DEF_Y));
	
}
