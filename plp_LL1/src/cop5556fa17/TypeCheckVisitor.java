package cop5556fa17;

import cop5556fa17.Scanner.Kind;
import cop5556fa17.Scanner.Token;
import cop5556fa17.TypeUtils.Type;

import java.io.File;
import java.net.URL;
import java.util.HashMap;

import cop5556fa17.AST.ASTNode;
import cop5556fa17.AST.ASTVisitor;
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
import cop5556fa17.AST.Sink_Ident;
import cop5556fa17.AST.Sink_SCREEN;
import cop5556fa17.AST.Source_CommandLineParam;
import cop5556fa17.AST.Source_Ident;
import cop5556fa17.AST.Source_StringLiteral;
import cop5556fa17.AST.Statement_Assign;
import cop5556fa17.AST.Statement_In;
import cop5556fa17.AST.Statement_Out;

public class TypeCheckVisitor implements ASTVisitor {
	

	@SuppressWarnings("serial")
	public static class SemanticException extends Exception {
		Token t;

		public SemanticException(Token t, String message) {
			super("line " + t.line + " pos " + t.pos_in_line + ": "+  message);
			this.t = t;
		}

	}		
	
	@SuppressWarnings("serial")
    public static class SymbolTable {
    	private HashMap<String,Object> hm = new HashMap<String,Object>();;
    	
    	public Object lookup(String key) {
    		if (hm.containsKey(key)) {
    			return hm.get(key);
    		} else {
    			return null;
    		}
    	}
    	public void insert(String key, Object obj) {
    		hm.put(key, obj);
    	}
    	
    }
	
	SymbolTable symbolTable = new SymbolTable();
	
	
	/**
	 * The program name is only used for naming the class.  It does not rule out
	 * variables with the same name.  It is returned for convenience.
	 * 
	 * @throws Exception 
	 */
	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		for (ASTNode node: program.decsAndStatements) {
			node.visit(this, arg);
		}
		return program.name;
	}

	@Override
	public Object visitDeclaration_Variable(
			Declaration_Variable declaration_Variable, Object arg)
			throws Exception {
		
		Token firstToken = 	declaration_Variable.firstToken;
		String key = declaration_Variable.name;
		declaration_Variable.visitChildren(this, arg);
		
		if(symbolTable.lookup(key) == null) {
			symbolTable.insert(key, declaration_Variable);
			declaration_Variable.setType(TypeUtils.getType(declaration_Variable.type));
			if(declaration_Variable.e != null) {
				if(declaration_Variable.e.getType() == declaration_Variable.getType()) {
					
				} else {
					throw new SemanticException(firstToken, "@visitDeclaration_Variable, decl.type != expr.type for "+key);
				}
			}
		} else {
			throw new SemanticException(firstToken, "@visitDeclaration_Variable, symbolTable already has "+key);
		}
		//throw new UnsupportedOperationException();
		return null;
	}

	@Override
	public Object visitExpression_Binary(Expression_Binary expression_Binary,
			Object arg) throws Exception {
		
		Token firstToken = 	expression_Binary.firstToken;
		expression_Binary.visitChildren(this, arg);
		
		Kind op = expression_Binary.op;
		Type type;
		
		if (op == Kind.OP_EQ || op == Kind.OP_NEQ) {
			type = Type.BOOLEAN;
		} else if ((op == Kind.OP_GE || op == Kind.OP_GT || op == Kind.OP_LE || op == Kind.OP_LT) && expression_Binary.e0.getType() == Type.INTEGER) {
			type = Type.BOOLEAN;
		} else if ((op == Kind.OP_AND || op == Kind.OP_OR) && (expression_Binary.e0.getType() == Type.INTEGER || expression_Binary.e0.getType() == Type.BOOLEAN )) {
			type = expression_Binary.e0.getType();
		} else if ((op == Kind.OP_DIV || op == Kind.OP_MINUS || op == Kind.OP_MOD || op == Kind.OP_PLUS || op == Kind.OP_POWER || op == Kind.OP_TIMES) && expression_Binary.e0.getType() == Type.INTEGER) {
			type = Type.INTEGER;
		}
		else {
			type = Type.NONE;
		}
		
		if (expression_Binary.e0.getType() == expression_Binary.e1.getType()) {
			if (type != Type.NONE) {
				expression_Binary.setType(type);
			} else {
				throw new SemanticException(firstToken, "@visitExpression_Binary, can not assign type to expression_Binary object");
			}
			
		} else {
			throw new SemanticException(firstToken, "@visitExpression_Binary, sub expressions,  e0.type != e1.type");
		}
		//throw new UnsupportedOperationException();
		return null;
	}

	@Override
	public Object visitExpression_Unary(Expression_Unary expression_Unary,
			Object arg) throws Exception {
		Token firstToken = 	expression_Unary.firstToken;
		expression_Unary.visitChildren(this, arg);
		
		Kind op = expression_Unary.op;
		Type type;
		
		if (op == Kind.OP_EXCL) {
			if (expression_Unary.e.getType() == Type.BOOLEAN || expression_Unary.e.getType() == Type.INTEGER) {
				type = expression_Unary.e.getType();
			} else {
				throw new SemanticException(firstToken, "@visitExpression_Unary, sub expression e.type is not one of {BOOLEAN, INTEGER}");
			}
		} else if (op == Kind.OP_PLUS || op == Kind.OP_MINUS) {
			if (expression_Unary.e.getType() == Type.INTEGER) {
				type = expression_Unary.e.getType();
			} else {
				throw new SemanticException(firstToken, "@visitExpression_Unary, sub expression e.type is not INTEGER");
			}
		} else {
			type = Type.NONE;
		}
		
		if (type != Type.NONE) {
			expression_Unary.setType(type);
		} else {
			throw new SemanticException(firstToken, "@visitExpression_Unary, could not assign type to expression_Unary");
		}
		
		//throw new UnsupportedOperationException();
		return null;
	}

	@Override
	public Object visitIndex(Index index, Object arg) throws Exception {
		Token firstToken = 	index.firstToken;
		index.visitChildren(this, arg);
		
		Expression e0 = index.e0;
		Expression e1 = index.e1;
		
		if (e0.getType() == Type.INTEGER && e1.getType() == Type.INTEGER) {
			boolean isCartesian = !(e0.firstToken.kind == Kind.KW_r && e1.firstToken.kind == Kind.KW_a);
			index.setCartesian(isCartesian);
		} else {
			throw new SemanticException(firstToken, "@visitIndex, sub expressions are not of type INTEGER");
		}
		
		//throw new UnsupportedOperationException();
		return null;
	}

	@Override
	public Object visitExpression_PixelSelector(
			Expression_PixelSelector expression_PixelSelector, Object arg)
			throws Exception {
		
		Token firstToken = 	expression_PixelSelector.firstToken;
		String key = expression_PixelSelector.name;
		expression_PixelSelector.visitChildren(this, arg);
		
		ASTNode astNode_name = (ASTNode) symbolTable.lookup(key);
		Type type;
		
		if (astNode_name != null) {
			if (astNode_name.getType() == Type.IMAGE) {
				type = Type.INTEGER;
			} else if (expression_PixelSelector.index == null) {
				type = astNode_name.getType();
			} else {
				type = Type.NONE;
			}
			
			if (type != Type.NONE) {
				expression_PixelSelector.setType(type);
			} else {
				throw new SemanticException(firstToken, "@visitExpression_PixelSelector, Could not assign type to expression_PixelSelector");
			}
		} else {
			throw new SemanticException(firstToken, "@expression_PixelSelector, Unexpected Error: No object with name "+key);
		}
		
		//throw new UnsupportedOperationException();
		return null;
	}

	@Override
	public Object visitExpression_Conditional(
			Expression_Conditional expression_Conditional, Object arg)
			throws Exception {
				
		Token firstToken = 	expression_Conditional.firstToken;
		expression_Conditional.visitChildren(this, arg);
		
		expression_Conditional.setType(expression_Conditional.trueExpression.getType());
		
		if (expression_Conditional.condition.getType() == Type.BOOLEAN && (expression_Conditional.trueExpression.getType() == expression_Conditional.falseExpression.getType())) {
			
		} else {
			throw new SemanticException(firstToken, "@visitExpression_Conditional, either expr_cond is not boolean or expr_true.type != expr_false.type");
		}
		//throw new UnsupportedOperationException();
		return null;
	}

	@Override
	public Object visitDeclaration_Image(Declaration_Image declaration_Image,
			Object arg) throws Exception {
		
		Token firstToken = 	declaration_Image.firstToken;
		String key = declaration_Image.name;
		declaration_Image.visitChildren(this, arg);
		
		if(symbolTable.lookup(key) == null) {
			symbolTable.insert(key, declaration_Image);
			declaration_Image.setType(Type.IMAGE);
			if (declaration_Image.xSize != null ) {
				if (declaration_Image.ySize != null && declaration_Image.xSize.getType() == Type.INTEGER && declaration_Image.ySize.getType() == Type.INTEGER) {
					
				}
				else {
					throw new SemanticException(firstToken, "@visitDeclaration_Image, one of xSize and ySize expr is null or they are not type INTEGER "+key);
				}
			}
		} else {
			throw new SemanticException(firstToken, "@visitDeclaration_Image, symbolTable already has "+key);
		}
		//throw new UnsupportedOperationException();
		return null;
	}

	@Override
	public Object visitSource_StringLiteral(
			Source_StringLiteral source_StringLiteral, Object arg)
			throws Exception {
		Token firstToken = 	source_StringLiteral.firstToken;
		String fileOrUrl = source_StringLiteral.fileOrUrl;
		
		boolean isUrl = false;
		boolean isFile = false;
		
		try {
			URL u = new URL(fileOrUrl);
			u.toURI();
			isUrl = true;
		} catch (Exception e) {
			// OK NOT URL 
		}
		if (!isUrl) {
			try {
				File file = new File(fileOrUrl);
				isFile = true;
			} catch (Exception e) {
				// OK NOT FILE
			}
			
		}
		
		if (isUrl) {
			source_StringLiteral.setType(Type.URL);
		} else if (isFile) {
			source_StringLiteral.setType(Type.FILE);
		} else {
			// not sure whether this needs be dealt here or at later stage
		}
		
		//throw new UnsupportedOperationException();
		return null;
	}

	@Override
	public Object visitSource_CommandLineParam(
			Source_CommandLineParam source_CommandLineParam, Object arg)
			throws Exception {
		Token firstToken = 	source_CommandLineParam.firstToken;
		//String key = source_Ident.name;
		source_CommandLineParam.visitChildren(this, arg);
		//TODO check for null ?? expression can not null for paramNum
		source_CommandLineParam.setType(Type.NONE);
		if (source_CommandLineParam.paramNum.getType() == Type.INTEGER) {
			
		} else {
			throw new SemanticException(firstToken, "@visitSource_CommandLineParam, source_CommandLineParam.type != INTEGER");
		}
		
		//throw new UnsupportedOperationException();
		return null;
	}

	@Override
	public Object visitSource_Ident(Source_Ident source_Ident, Object arg)
			throws Exception {
		
		Token firstToken = 	source_Ident.firstToken;
		String key = source_Ident.name;
		//source_Ident.visitChildren(this, arg);
		
		ASTNode astNode = (ASTNode) symbolTable.lookup(key);
		
		if (astNode != null) {
			if (astNode.getType() == Type.FILE || astNode.getType() == Type.URL) {
				source_Ident.setType(astNode.getType());
			} else {
				throw new SemanticException(firstToken, "@visitSource_Ident, Source_Ident.type != {FILE, URL}");
			}
		} else {
			throw new SemanticException(firstToken, "@visitSource_Ident, Unexpected Error: No object with name "+key);
		}
		
		//throw new UnsupportedOperationException();
		return null;
	}

	@Override
	public Object visitDeclaration_SourceSink(
			Declaration_SourceSink declaration_SourceSink, Object arg)
			throws Exception {
		
		Token firstToken = 	declaration_SourceSink.firstToken;
		String key = declaration_SourceSink.name;
		declaration_SourceSink.visitChildren(this, arg);
		
		if(symbolTable.lookup(key) == null) {
			symbolTable.insert(key, declaration_SourceSink);
			declaration_SourceSink.setType(TypeUtils.getType(declaration_SourceSink.type));
			if (declaration_SourceSink.source != null) {
				if ((declaration_SourceSink.source.getType() == declaration_SourceSink.getType()) || declaration_SourceSink.source.getType() == Type.NONE) {
					
				} else {
					throw new SemanticException(firstToken, "@visitDeclaration_SourceSink, RHS source/sink type do not match LHS/decl source sink type "+key);
				}
			}
		} else {
			throw new SemanticException(firstToken, "@visitDeclaration_SourceSink, symbolTable already has "+key);
		}
		//throw new UnsupportedOperationException(); 
		return null;
	}

	@Override
	public Object visitExpression_IntLit(Expression_IntLit expression_IntLit,
			Object arg) throws Exception {
		
		expression_IntLit.setType(Type.INTEGER);
		//throw new UnsupportedOperationException();
		return null;
	}

	@Override
	public Object visitExpression_FunctionAppWithExprArg(
			Expression_FunctionAppWithExprArg expression_FunctionAppWithExprArg,
			Object arg) throws Exception {
		
		Token firstToken = 	expression_FunctionAppWithExprArg.firstToken;
		expression_FunctionAppWithExprArg.visitChildren(this, arg);
		
		if (expression_FunctionAppWithExprArg.arg != null && expression_FunctionAppWithExprArg.arg.getType() == Type.INTEGER) {
			expression_FunctionAppWithExprArg.setType(Type.INTEGER);
		} else {
			throw new SemanticException(firstToken, "@visitExpression_FunctionAppWithExprArg, arg_expr.type != INTEGER");
		}
		//throw new UnsupportedOperationException();
		return null;
		
	}

	@Override
	public Object visitExpression_FunctionAppWithIndexArg(
			Expression_FunctionAppWithIndexArg expression_FunctionAppWithIndexArg,
			Object arg) throws Exception {
		expression_FunctionAppWithIndexArg.setType(Type.INTEGER);
		//throw new UnsupportedOperationException();
		return null;
	}

	@Override
	public Object visitExpression_PredefinedName(
			Expression_PredefinedName expression_PredefinedName, Object arg)
			throws Exception {
		expression_PredefinedName.setType(Type.INTEGER);
		//throw new UnsupportedOperationException();
		return null;
		
	}

	@Override
	public Object visitStatement_Out(Statement_Out statement_Out, Object arg)
			throws Exception {
		
		Token firstToken = 	statement_Out.firstToken;
		String key = statement_Out.name;
		statement_Out.visitChildren(this, arg);
		
		Declaration dec = (Declaration) symbolTable.lookup(key);
		statement_Out.setDec(dec);
		
		if (dec != null) {
			boolean cond_1 = (dec.getType() == Type.INTEGER || dec.getType() == Type.BOOLEAN) && statement_Out.sink.getType() == Type.SCREEN;
			boolean cond_2 =  dec.getType()== Type.IMAGE && (statement_Out.sink.getType() == Type.FILE || statement_Out.sink.getType() == Type.SCREEN);
			if ( cond_1 || cond_2) {
				
			} else {
				throw new SemanticException(firstToken, "@visitStatement_Out, both cond_1 and cond_2 are false in typechecking");
			}
		} else {
			throw new SemanticException(firstToken, "@visitStatement_Out, symbolTable does not have dec named "+key);
		}
		//throw new UnsupportedOperationException();
		return null;
	}

	@Override
	public Object visitStatement_In(Statement_In statement_In, Object arg)
			throws Exception {
		Token firstToken = 	statement_In.firstToken;
		statement_In.visitChildren(this, arg);
		
		Declaration dec = (Declaration) symbolTable.lookup(statement_In.name); // is this cast safe ?
		
		if (dec != null /* removed in HW5  && dec.getType() == statement_In.source.getType() */) {
			statement_In.setDec(dec);
		} else {
			throw new SemanticException(firstToken, "@Statement_In, dec.type != source.type");
		}
		//throw new UnsupportedOperationException();
		return null;
	}

	@Override
	public Object visitStatement_Assign(Statement_Assign statement_Assign,
			Object arg) throws Exception {
		
		Token firstToken = 	statement_Assign.firstToken;
		statement_Assign.visitChildren(this, arg);
		
		if (statement_Assign.lhs.getType() == statement_Assign.e.getType() || (statement_Assign.lhs.getType() == Type.IMAGE && statement_Assign.e.getType() == Type.INTEGER)) {
			statement_Assign.setCartesian(statement_Assign.lhs.isCartesian());
			
		} else {
			throw new SemanticException(firstToken, "@visitStatement_Assign, lhs.type != expr.type");
		}
		//throw new UnsupportedOperationException(); 
		return null;
	}

	@Override
	public Object visitLHS(LHS lhs, Object arg) throws Exception {
		
		Token firstToken = 	lhs.firstToken;
		String key = lhs.name;
		lhs.visitChildren(this, arg);
		
		Declaration dec = (Declaration) symbolTable.lookup(key);
		
		if (dec != null) {
			lhs.setType(dec.getType());
		} else {
			throw new SemanticException(firstToken, "@visitLHS, Unexpected Error: No object with name "+key);
		}
		
		if (lhs.index != null) {
			lhs.setCartesian(lhs.index.isCartesian());
		} 
		/*else {
			throw new SemanticException(firstToken, "@visitLHS, Unexpected Error: No index object");
		}*/
		
		//throw new UnsupportedOperationException();
		return null;
	}

	@Override
	public Object visitSink_SCREEN(Sink_SCREEN sink_SCREEN, Object arg)
			throws Exception {
		sink_SCREEN.setType(Type.SCREEN);
		//throw new UnsupportedOperationException();
		return null;
	}

	@Override
	public Object visitSink_Ident(Sink_Ident sink_Ident, Object arg)
			throws Exception {
				
		Token firstToken = 	sink_Ident.firstToken;
		String key = sink_Ident.name;
		//sink_Ident.visitChildren(this, arg);
		
		ASTNode astNode = (ASTNode) symbolTable.lookup(key);
		
		if (astNode != null) {
            if (astNode.getType() == Type.FILE) {
            	sink_Ident.setType(Type.FILE);
			} else {
				throw new SemanticException(firstToken, "@visitSink_Ident, sink_Ident.type != FILE");
			}
		} else {
			throw new SemanticException(firstToken, "@visitSink_Ident, Unexpected Error: No object with name "+key);
		}
		
		//throw new UnsupportedOperationException();
		return null;
	}

	@Override
	public Object visitExpression_BooleanLit(
			Expression_BooleanLit expression_BooleanLit, Object arg)
			throws Exception {
		expression_BooleanLit.setType(Type.BOOLEAN);
		//throw new UnsupportedOperationException();
		return null;
	}

	@Override
	public Object visitExpression_Ident(Expression_Ident expression_Ident,
			Object arg) throws Exception {
		
		Token firstToken = 	expression_Ident.firstToken;
		String key = expression_Ident.name;
		//expression_Ident.visitChildren(this, arg);
		
		ASTNode astNode = (ASTNode) symbolTable.lookup(key);
		
		if (astNode != null) {
			expression_Ident.setType(astNode.getType());
		} else {
			throw new SemanticException(firstToken, "@visitExpression_Ident, Unexpected Error: No object with name "+key);
		}
		
		//throw new UnsupportedOperationException();
		return null;
	}

}
