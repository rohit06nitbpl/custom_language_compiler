package cop5556fa17;

import cop5556fa17.Scanner.Kind;
import cop5556fa17.Scanner.Token;

public class TypeUtils {
	
	public static enum Type {
		INTEGER("I"),
		BOOLEAN("Z"),
		IMAGE(ImageSupport.ImageDesc),
		URL(""),
		FILE(""),
		SCREEN(""),
		NONE(null);
		
		private String jvmType;
		
		Type(String s) {
			jvmType = s;
		}
		
		public String getJvmType() {
			return jvmType;
		}
	}


	public static Type getType(Token token){
		switch (token.kind){
		case KW_int: {return Type.INTEGER;} 
		case KW_boolean: {return Type.BOOLEAN;} 
		case KW_image: {return Type.IMAGE;} 
		case KW_url: {return Type.URL;} 
		case KW_file: {return Type.FILE;}
			default :
				break; 
		}
		assert false;  //should not reach here
		return null;  
	}
	public static Type getType(Kind kind){
		switch (kind){
		case KW_int: {return Type.INTEGER;} 
		case KW_boolean: {return Type.BOOLEAN;} 
		case KW_image: {return Type.IMAGE;} 
		case KW_url: {return Type.URL;} 
		case KW_file: {return Type.FILE;}
			default :
				break; 
		}
		assert false;  //should not reach here
		return null;  
	}
}
