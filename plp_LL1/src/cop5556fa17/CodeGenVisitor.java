package cop5556fa17;

import java.util.ArrayList;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import cop5556fa17.TypeUtils.Type;
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
import cop5556fa17.AST.Source;
import cop5556fa17.AST.Source_CommandLineParam;
import cop5556fa17.AST.Source_Ident;
import cop5556fa17.AST.Source_StringLiteral;
import cop5556fa17.AST.Statement_In;
import cop5556fa17.AST.Statement_Out;
import cop5556fa17.AST.Statement_Assign;
//import cop5556fa17.image.ImageFrame;
//import cop5556fa17.image.ImageSupport;



public class CodeGenVisitor implements ASTVisitor, Opcodes {

	/**
	 * All methods and variable static.
	 */


	/**
	 * @param DEVEL
	 *            used as parameter to genPrint and genPrintTOS
	 * @param GRADE
	 *            used as parameter to genPrint and genPrintTOS
	 * @param sourceFileName
	 *            name of source file, may be null.
	 */
	public CodeGenVisitor(boolean DEVEL, boolean GRADE, String sourceFileName) {
		super();
		this.DEVEL = DEVEL;
		this.GRADE = GRADE;
		this.sourceFileName = sourceFileName;
	}

	ClassWriter cw;
	String className;
	String classDesc;
	String sourceFileName;

	MethodVisitor mv; // visitor of method currently under construction

	/** Indicates whether genPrint and genPrintTOS should generate code. */
	final boolean DEVEL;
	final boolean GRADE;
	


	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		className = program.name;  
		classDesc = "L" + className + ";";
		String sourceFileName = (String) arg;
		cw.visit(52, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object", null);
		cw.visitSource(sourceFileName, null);
		
		// create predefined static variables
		cw.visitField(ACC_STATIC, "DEF_X", "I", null, new Integer(256)).visitEnd();
		cw.visitField(ACC_STATIC, "DEF_Y", "I", null, new Integer(256)).visitEnd();
		cw.visitField(ACC_STATIC, "Z", "I", null, new Integer(16777215)).visitEnd();
		
		// create main method
		mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
		// initialize
		mv.visitCode();		
		//add label before first instruction
		Label mainStart = new Label();
		mv.visitLabel(mainStart);		
		// if GRADE, generates code to add string to log
		/*HW6 DIRECTIVE*///CodeGenUtils.genLog(GRADE, mv, "entering main");

		// visit decs and statements to add field to class
		//  and instructions to main method, respectivley
		ArrayList<ASTNode> decsAndStatements = program.decsAndStatements;
		for (ASTNode node : decsAndStatements) {
			node.visit(this, arg);
		}

		//generates code to add string to log
		/*HW6 DIRECTIVE*///CodeGenUtils.genLog(GRADE, mv, "leaving main");
		
		//adds the required (by the JVM) return statement to main
		mv.visitInsn(RETURN);
		
		//adds label at end of code
		Label mainEnd = new Label();
		mv.visitLabel(mainEnd);
		
		//handles parameters and local variables of main. Right now, only args
		mv.visitLocalVariable("args", "[Ljava/lang/String;", null, mainStart, mainEnd, 0);
		//CHK why prof said only args 
		mv.visitLocalVariable("x", "I", null, mainStart, mainEnd, 1);
		mv.visitLocalVariable("y", "I", null, mainStart, mainEnd, 2);
		mv.visitLocalVariable("X", "I", null, mainStart, mainEnd, 3);
		mv.visitLocalVariable("Y", "I", null, mainStart, mainEnd, 4);
		mv.visitLocalVariable("r", "I", null, mainStart, mainEnd, 5);
		mv.visitLocalVariable("a", "I", null, mainStart, mainEnd, 6);
		mv.visitLocalVariable("R", "I", null, mainStart, mainEnd, 7);
		mv.visitLocalVariable("A", "I", null, mainStart, mainEnd, 8);
		
		//Sets max stack size and number of local vars.
		//Because we use ClassWriter.COMPUTE_FRAMES as a parameter in the constructor,
		//asm will calculate this itself and the parameters are ignored.
		//If you have trouble with failures in this routine, it may be useful
		//to temporarily set the parameter in the ClassWriter constructor to 0.
		//The generated classfile will not be correct, but you will at least be
		//able to see what is in it.
		mv.visitMaxs(0, 0);
		
		//terminate construction of main method
		mv.visitEnd();
		
		//terminate class construction
		cw.visitEnd();

		//generate classfile as byte array and return
		return cw.toByteArray();
	}

	@Override
	public Object visitDeclaration_Variable(Declaration_Variable declaration_Variable, Object arg) throws Exception {
		cw.visitField(ACC_STATIC, declaration_Variable.name, declaration_Variable.getType().getJvmType(), null, null).visitEnd();
		declaration_Variable.visitChildren(this, arg);
		if (declaration_Variable.e != null) {
			mv.visitFieldInsn(PUTSTATIC, className, declaration_Variable.name, declaration_Variable.getType().getJvmType());
		}
		return null;
	}

	@Override
	public Object visitExpression_Binary(Expression_Binary expression_Binary, Object arg) throws Exception {
		
		expression_Binary.visitChildren(this, arg);
		Label is_exp_true = new Label();
		Label block_end = new Label();
		
		switch(expression_Binary.op) {
		case OP_PLUS:
		{
			mv.visitInsn(IADD);
			break;
		}
		case OP_MINUS:
		{
			mv.visitInsn(ISUB);
			break;
		}
		case OP_TIMES:
		{
			mv.visitInsn(IMUL);
			break;
		}
		case OP_DIV:
		{
			mv.visitInsn(IDIV);
			break;
		}
		case OP_AND:
		{
			mv.visitInsn(IAND);
			break;
		}
		case OP_OR:
		{
			mv.visitInsn(IOR);
			break;
		}
		case OP_MOD:
		{
			mv.visitInsn(IREM);
			break;
		}
		case OP_LT: 
		{
			mv.visitJumpInsn(IF_ICMPLT, is_exp_true);
			mv.visitLdcInsn(false);
			mv.visitJumpInsn(GOTO, block_end);
			mv.visitLabel(is_exp_true);
			mv.visitLdcInsn(true);
			mv.visitLabel(block_end);
			break;
		}
		case OP_LE: 
		{
			mv.visitJumpInsn(IF_ICMPLE, is_exp_true);
			mv.visitLdcInsn(false);
			mv.visitJumpInsn(GOTO, block_end);
			mv.visitLabel(is_exp_true);
			mv.visitLdcInsn(true);
			mv.visitLabel(block_end);
			break;
		}
		case OP_GT: 
		{
			mv.visitJumpInsn(IF_ICMPGT, is_exp_true);
			mv.visitLdcInsn(false);
			mv.visitJumpInsn(GOTO, block_end);
			mv.visitLabel(is_exp_true);
			mv.visitLdcInsn(true);
			mv.visitLabel(block_end);
			break;
		}
			
		case OP_GE: 
		{
			mv.visitJumpInsn(IF_ICMPGE, is_exp_true);
			mv.visitLdcInsn(false);
			mv.visitJumpInsn(GOTO, block_end);
			mv.visitLabel(is_exp_true);
			mv.visitLdcInsn(true);
			mv.visitLabel(block_end);
			break;
		}
		case OP_EQ: 
		{
			if (expression_Binary.getType() == Type.INTEGER || expression_Binary.getType() == Type.BOOLEAN) {
				mv.visitJumpInsn(IF_ICMPEQ, is_exp_true);
			} else {
				mv.visitJumpInsn(IF_ACMPEQ, is_exp_true);
			}
			mv.visitLdcInsn(false);
			mv.visitJumpInsn(GOTO, block_end);
			mv.visitLabel(is_exp_true);
			mv.visitLdcInsn(true);
			mv.visitLabel(block_end);
			break;
		}
		case OP_NEQ: 
		{
			if (expression_Binary.getType() == Type.INTEGER || expression_Binary.getType() == Type.BOOLEAN) {
				mv.visitJumpInsn(IF_ICMPNE, is_exp_true);
			} else {
				mv.visitJumpInsn(IF_ACMPNE, is_exp_true);
			}
			mv.visitLdcInsn(false);
			mv.visitJumpInsn(GOTO, block_end);
			mv.visitLabel(is_exp_true);
			mv.visitLdcInsn(true);
			mv.visitLabel(block_end);
			break;
		}
		default:
		{
			throw new UnsupportedOperationException();
		}
		}
			
		//CodeGenUtils.genLogTOS(GRADE, mv, expression_Binary.getType());
		return null;
	}

	@Override
	public Object visitExpression_Unary(Expression_Unary expression_Unary, Object arg) throws Exception {
		expression_Unary.visitChildren(this, arg);
		switch (expression_Unary.op) {
		case OP_PLUS:
		{
			break;
		}
		case OP_MINUS:
		{
			mv.visitInsn(INEG);
			break;
		}
		case OP_EXCL:
		{
			if (expression_Unary.e.getType() == Type.BOOLEAN) {
				Label is_exp_true = new Label();
				Label block_end = new Label();
				mv.visitJumpInsn(IFEQ, is_exp_true);
				mv.visitLdcInsn(false);
				mv.visitJumpInsn(GOTO, block_end);
				mv.visitLabel(is_exp_true);
				mv.visitLdcInsn(true);
				mv.visitLabel(block_end);
				break;
				
				
			} else if (expression_Unary.e.getType() == Type.INTEGER) {
				mv.visitLdcInsn(new Integer(Integer.MAX_VALUE));
				mv.visitInsn(IXOR);
			}
			break;
			// ONLY TWO CASE IN TYPE CHECKING
		}
		default:
		{
			throw new UnsupportedOperationException();
		}
		}
		
		//CodeGenUtils.genLogTOS(GRADE, mv, expression_Unary.getType());
		return null;
	}

	// generate code to leave the two values on the stack
	@Override
	public Object visitIndex(Index index, Object arg) throws Exception {
		index.visitChildren(this, arg);
		/*if (!index.isCartesian()) {
			mv.visitInsn(Opcodes.DUP2);
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className, "cart_x", RuntimeFunctions.cart_xSig,false);
			mv.visitInsn(Opcodes.DUP_X2);
			mv.visitInsn(Opcodes.POP);
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className, "cart_y", RuntimeFunctions.cart_ySig,false);
		}*/
		return null;
	}

	@Override
	public Object visitExpression_PixelSelector(Expression_PixelSelector expression_PixelSelector, Object arg)
			throws Exception {
		
		mv.visitFieldInsn(GETSTATIC, className, expression_PixelSelector.name, ImageSupport.ImageDesc);
		//CHK INDEX in this object should never be null however mam checked for null in typechecking
		//CHK so not checking for null
		expression_PixelSelector.index.visit(this, arg);
		
		if (!expression_PixelSelector.index.isCartesian()) {
			mv.visitInsn(Opcodes.DUP2);
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className, "cart_x", RuntimeFunctions.cart_xSig,false);
			mv.visitInsn(Opcodes.DUP_X2);
			mv.visitInsn(Opcodes.POP);
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className, "cart_y", RuntimeFunctions.cart_ySig,false);
	    }
		mv.visitMethodInsn(INVOKESTATIC, ImageSupport.className, "getPixel", ImageSupport.getPixelSig,false);
		
		return null;
	}

	@Override
	public Object visitExpression_Conditional(Expression_Conditional expression_Conditional, Object arg)
			throws Exception {
		
		expression_Conditional.condition.visit(this, arg);
		Label is_exp_true = new Label();
		Label block_end = new Label();
		
		mv.visitLdcInsn(true);
		
		mv.visitJumpInsn(IF_ICMPEQ, is_exp_true);
		expression_Conditional.falseExpression.visit(this, arg);
		mv.visitJumpInsn(GOTO, block_end);
		mv.visitLabel(is_exp_true);
		expression_Conditional.trueExpression.visit(this, arg);
		mv.visitLabel(block_end);
		
		//CodeGenUtils.genLogTOS(GRADE, mv, expression_Conditional.trueExpression.getType());
		return null;
	}


	@Override
	public Object visitDeclaration_Image(Declaration_Image declaration_Image, Object arg) throws Exception {
		cw.visitField(ACC_STATIC, declaration_Image.name, ImageSupport.ImageDesc, null,null).visitEnd();
		if (declaration_Image.source != null) {
			declaration_Image.source.visit(this, arg);
			if(declaration_Image.xSize != null) {
				declaration_Image.xSize.visit(this, arg);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
				//CHK if xSize!=null then ySize must not be null by type checking constraints
				//CHK not checking for null
				declaration_Image.ySize.visit(this, arg);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
				
			} else {
				mv.visitInsn(ACONST_NULL);//xSize
				mv.visitInsn(ACONST_NULL);//ySize
			}
			mv.visitMethodInsn(INVOKESTATIC, ImageSupport.className, "readImage", ImageSupport.readImageSig,false);
		} else {
			if(declaration_Image.xSize != null) {
				declaration_Image.xSize.visit(this, arg);
				//mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
				//CHK if xSize!=null then ySize must not be null by type checking constraints
				//CHK not checking for null
				declaration_Image.ySize.visit(this, arg);
				//mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
				
			} else {
				mv.visitFieldInsn(GETSTATIC, className, "DEF_X", "I");
				mv.visitFieldInsn(GETSTATIC, className, "DEF_Y", "I");
			}
			mv.visitMethodInsn(INVOKESTATIC, ImageSupport.className, "makeImage", ImageSupport.makeImageSig,false);
		}
		mv.visitFieldInsn(PUTSTATIC, className, declaration_Image.name, ImageSupport.ImageDesc);
		return null;
	}
	
  
	@Override
	public Object visitSource_StringLiteral(Source_StringLiteral source_StringLiteral, Object arg) throws Exception {
		mv.visitLdcInsn(source_StringLiteral.fileOrUrl);
		return null;
	}


	@Override
	public Object visitSource_CommandLineParam(Source_CommandLineParam source_CommandLineParam, Object arg)
			throws Exception {
		mv.visitVarInsn(ALOAD, 0);
		source_CommandLineParam.visitChildren(this, arg);
		//CHK no need to check for null, cause type checking ensures that
		mv.visitInsn(AALOAD);
		return null;
	}

	
	@Override
	public Object visitSource_Ident(Source_Ident source_Ident, Object arg) throws Exception {
		mv.visitFieldInsn(GETSTATIC, className, source_Ident.name, "Ljava/lang/String;");
		return null;
	}


	@Override
	public Object visitDeclaration_SourceSink(Declaration_SourceSink declaration_SourceSink, Object arg)
			throws Exception {
		cw.visitField(ACC_STATIC, declaration_SourceSink.name, "Ljava/lang/String;", null,null).visitEnd();
		//CHK source can not be null , parsing will return object
		//CHK not checking for null
		declaration_SourceSink.source.visit(this, arg);
		mv.visitFieldInsn(PUTSTATIC, className, declaration_SourceSink.name, ImageSupport.StringDesc);
		return null;
	}
	

	@Override
	public Object visitExpression_IntLit(Expression_IntLit expression_IntLit, Object arg) throws Exception {

		mv.visitLdcInsn(new Integer(expression_IntLit.value));
		//CodeGenUtils.genLogTOS(GRADE, mv, Type.INTEGER);
		return null;
	}

	@Override
	public Object visitExpression_FunctionAppWithExprArg(
			Expression_FunctionAppWithExprArg expression_FunctionAppWithExprArg, Object arg) throws Exception {
		
		expression_FunctionAppWithExprArg.arg.visit(this, null);
		switch (expression_FunctionAppWithExprArg.function) {
		case KW_abs: 
		{
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className, "abs", RuntimeFunctions.absSig, false);
			break;
		}
		case KW_log: 
		{
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className, "log", RuntimeFunctions.logSig, false);
			break;
		}
		default: 
		{
			throw new UnsupportedOperationException();
		}
		}
		return null;
	}

	@Override
	public Object visitExpression_FunctionAppWithIndexArg(
			Expression_FunctionAppWithIndexArg expression_FunctionAppWithIndexArg, Object arg) throws Exception {
		
		//CHK no need to check arg for null
		expression_FunctionAppWithIndexArg.arg.visit(this, arg);
		
		switch(expression_FunctionAppWithIndexArg.function) {
		case KW_cart_x : 
		{
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className, "cart_x", RuntimeFunctions.cart_xSig,false);
			break;
		}
		case KW_cart_y : 
		{
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className, "cart_y", RuntimeFunctions.cart_ySig,false);
			break;
		}
		case KW_polar_r : 
		{
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className, "polar_r", RuntimeFunctions.polar_rSig,false);
			break;
		}
		case KW_polar_a :
		{
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className, "polar_a", RuntimeFunctions.polar_aSig,false);
			break;
		}
		default : 
		{
			throw new UnsupportedOperationException();
		}
		}
		return null;
	}

	@Override
	public Object visitExpression_PredefinedName(Expression_PredefinedName expression_PredefinedName, Object arg)
			throws Exception {
		switch (expression_PredefinedName.kind) {
		case KW_x : 
		{			
			mv.visitVarInsn(ILOAD, 1);	
			break;
		}
		case KW_y : 
		{
			mv.visitVarInsn(ILOAD, 2);
			break;
		}
		case KW_X :
		{
			mv.visitVarInsn(ILOAD, 3);
			break;
		}
		case KW_Y : 
		{
			mv.visitVarInsn(ILOAD, 4);
			break;
		}
		case KW_r : 
		{
			mv.visitVarInsn(ILOAD, 1);
			mv.visitVarInsn(ILOAD, 2);			
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className, "polar_r", RuntimeFunctions.polar_rSig,false);
			break;
			
		}
		case KW_a : 
		{
			mv.visitVarInsn(ILOAD, 1);
			mv.visitVarInsn(ILOAD, 2);
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className, "polar_a", RuntimeFunctions.polar_aSig,false);
			break;
		}
		
		case KW_R : 
		{
			mv.visitVarInsn(ILOAD, 7);
			break;
		}
		case KW_A :
		{
			mv.visitVarInsn(ILOAD, 8);
			break;
		}
		case KW_Z : {
			mv.visitFieldInsn(GETSTATIC, className, "Z", "I");
			break;
		}
		case KW_DEF_X : 
		{
			mv.visitFieldInsn(GETSTATIC, className, "DEF_X", "I");
			break;
		}
		case KW_DEF_Y : 
		{
			mv.visitFieldInsn(GETSTATIC, className, "DEF_Y", "I");
			break;
		}
		default : 
		{
			throw new UnsupportedOperationException();
		}		
		}
		
		return null;
	}

	/** For Integers and booleans, the only "sink"is the screen, so generate code to print to console.
	 * For Images, load the Image onto the stack and visit the Sink which will generate the code to handle the image.
	 */
	@Override
	public Object visitStatement_Out(Statement_Out statement_Out, Object arg) throws Exception {
		// TODO in HW5:  only INTEGER and BOOLEAN
		// TODO HW6 remaining cases
		//statement_Out.visitChildren(this, arg); call this for image only
		//CHK sink object will never be null 
		//CHK not checking for null
		if (statement_Out.getDec().getType() == Type.INTEGER) {
			mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
			mv.visitFieldInsn(GETSTATIC, className, statement_Out.name, "I");
			CodeGenUtils.genLogTOS(GRADE, mv, Type.INTEGER);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "print", "(I)V", false);
		} else if (statement_Out.getDec().getType() == Type.BOOLEAN) {
			mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
			mv.visitFieldInsn(GETSTATIC, className, statement_Out.name, "Z");
			CodeGenUtils.genLogTOS(GRADE, mv, Type.BOOLEAN);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Z)V", false);
		} else if (statement_Out.getDec().getType() == Type.IMAGE) {
			mv.visitFieldInsn(GETSTATIC, className, statement_Out.name, ImageSupport.ImageDesc);
			mv.visitMethodInsn(INVOKESTATIC, "cop5556fa17/RuntimeLog", "globalLogAddImage", "("+ImageSupport.ImageDesc + ")V", false);
			mv.visitFieldInsn(GETSTATIC, className, statement_Out.name, ImageSupport.ImageDesc);
			statement_Out.sink.visit(this, arg);
		}
		return null;
	}

	/**
	 * Visit source to load rhs, which will be a String, onto the stack
	 * 
	 *  In HW5, you only need to handle INTEGER and BOOLEAN
	 *  Use java.lang.Integer.parseInt or java.lang.Boolean.parseBoolean 
	 *  to convert String to actual type. 
	 *  
	 *  TODO HW6 remaining types
	 */
	@Override
	public Object visitStatement_In(Statement_In statement_In, Object arg) throws Exception {
		// TODO /*HW6*/
		statement_In.visitChildren(this, arg);
		//CHK source can not be null
		//CHK we checked for Statement_In.type == source.type as well
		if (statement_In.getDec().getType() == Type.INTEGER) {
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "parseInt", "(Ljava/lang/String;)I", false);
			mv.visitFieldInsn(PUTSTATIC, className, statement_In.name, statement_In.getDec().getType().getJvmType());
		} else if (statement_In.getDec().getType() == Type.BOOLEAN) {
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "parseBoolean", "(Ljava/lang/String;)Z", false);
			mv.visitFieldInsn(PUTSTATIC, className, statement_In.name, statement_In.getDec().getType().getJvmType());
		} else if (statement_In.getDec().getType() == Type.IMAGE){
			Declaration_Image img = (Declaration_Image) statement_In.getDec();
			if(img.xSize != null) {
				img.xSize.visit(this, arg);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
				//CHK ySize now must be non null
				img.ySize.visit(this, arg);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
			}else {
				mv.visitInsn(ACONST_NULL);
				mv.visitInsn(ACONST_NULL);
			}
			mv.visitMethodInsn(INVOKESTATIC, ImageSupport.className, "readImage", ImageSupport.readImageSig,false);
			mv.visitFieldInsn(PUTSTATIC, className, statement_In.name, ImageSupport.ImageDesc);
			
					
		} else {
			throw new UnsupportedOperationException();
		}
			
		return null;
	}

	
	/**
	 * In HW5, only handle INTEGER and BOOLEAN types.
	 */
	@Override
	public Object visitStatement_Assign(Statement_Assign statement_Assign, Object arg) throws Exception {
		
		//CHK IDENTIFIER ( LSQUARE LhsSelector RSQUARE | ε ) OP_ASSIGN Expression
		//CHK index can be null , identifier and expression has to be there, 
		if (statement_Assign.lhs.getType() == Type.INTEGER || statement_Assign.lhs.getType() == Type.BOOLEAN) {
			statement_Assign.e.visit(this, arg);
			mv.visitFieldInsn(PUTSTATIC, className, statement_Assign.lhs.name, statement_Assign.lhs.getType().getJvmType());
		} else if (statement_Assign.lhs.getType() == Type.IMAGE) {
			if (statement_Assign.lhs.index != null) {
				//Initialize X and Y local variable
				mv.visitFieldInsn(GETSTATIC, className, statement_Assign.lhs.name, ImageSupport.ImageDesc);
				mv.visitMethodInsn(INVOKESTATIC, ImageSupport.className, "getX", ImageSupport.getXSig,false);
				mv.visitVarInsn(ISTORE, 3);
				mv.visitFieldInsn(GETSTATIC, className, statement_Assign.lhs.name, ImageSupport.ImageDesc);
				mv.visitMethodInsn(INVOKESTATIC, ImageSupport.className, "getY", ImageSupport.getYSig,false);
				mv.visitVarInsn(ISTORE, 4);
				
				Label outer_loop_start = new Label();
				Label outer_loop_end = new Label();
				Label inner_loop_start = new Label();
				Label inner_loop_end = new Label();
				
				//set x to zero
				mv.visitInsn(ICONST_0);
				mv.visitVarInsn(ISTORE, 1);
				
				
				mv.visitLabel(outer_loop_start);
				mv.visitVarInsn(ILOAD, 1);
				mv.visitVarInsn(ILOAD, 3);
				mv.visitJumpInsn(IF_ICMPGE, outer_loop_end);
				
				//set y to zero
				mv.visitInsn(ICONST_0);
				mv.visitVarInsn(ISTORE, 2);
				
				mv.visitLabel(inner_loop_start);
				mv.visitVarInsn(ILOAD, 2);
				mv.visitVarInsn(ILOAD, 4);
				mv.visitJumpInsn(IF_ICMPGE, inner_loop_end);
				
			
				statement_Assign.e.visit(this, arg); //pixel value on stack
				mv.visitFieldInsn(GETSTATIC, className, statement_Assign.lhs.name, ImageSupport.ImageDesc); //putting image ref on stack
				mv.visitVarInsn(ILOAD, 1); //x on stack
				mv.visitVarInsn(ILOAD, 2); //y on stack
				mv.visitMethodInsn(INVOKESTATIC, ImageSupport.className, "setPixel", ImageSupport.setPixelSig,false); //executing static command
				
				
				mv.visitIincInsn(2, 1); //increment y in local variable
				mv.visitJumpInsn(GOTO, inner_loop_start);
				mv.visitLabel(inner_loop_end);
				mv.visitIincInsn(1, 1); //increment x in local variable
				mv.visitJumpInsn(GOTO, outer_loop_start);
				mv.visitLabel(outer_loop_end);

			} else {
				//direct assignment of one image to another
			}
		} else {
			throw new UnsupportedOperationException();
		}
		
		return null;
	}

	/**
	 * In HW5, only handle INTEGER and BOOLEAN types.
	 */
	@Override
	public Object visitLHS(LHS lhs, Object arg) throws Exception {
		
		return null;
	}
	

	@Override
	public Object visitSink_SCREEN(Sink_SCREEN sink_SCREEN, Object arg) throws Exception {
		//CHK only for image
		mv.visitMethodInsn(INVOKESTATIC, ImageSupport.className, "makeFrame", ImageSupport.makeFrameSig,false);
		mv.visitInsn(POP);
		return null;
	}

	@Override
	public Object visitSink_Ident(Sink_Ident sink_Ident, Object arg) throws Exception {
		//CHK only for image
		mv.visitFieldInsn(GETSTATIC, className, sink_Ident.name, ImageSupport.StringDesc);
		mv.visitMethodInsn(INVOKESTATIC, ImageSupport.className, "write", ImageSupport.writeSig,false);
		return null;
	}

	@Override
	public Object visitExpression_BooleanLit(Expression_BooleanLit expression_BooleanLit, Object arg) throws Exception {
		mv.visitLdcInsn(new Boolean(expression_BooleanLit.value));
		//CodeGenUtils.genLogTOS(GRADE, mv, Type.BOOLEAN);
		return null;
	}

	@Override
	public Object visitExpression_Ident(Expression_Ident expression_Ident,
			Object arg) throws Exception {
		mv.visitFieldInsn(GETSTATIC, className, expression_Ident.name, expression_Ident.getType().getJvmType());
		//CodeGenUtils.genLogTOS(GRADE, mv, expression_Ident.getType());
		return null;
	}

}
