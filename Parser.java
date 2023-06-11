// Parser.java
// Parser for language S

public class Parser {
    Token token;          // current token 
    Lexer lexer;
    String funId = "";

    public Parser(Lexer scan) { 
        lexer = scan;		  
        token = lexer.getToken(); // get the first token
    }
  
    private String match(Token t) {
        String value = token.value();
        if (token == t)
            token = lexer.getToken();
        else
            error(t);
        return value;
    }

    private void error(Token tok) {
        System.err.println("Syntax error: " + tok + " --> " + token);
        token=lexer.getToken();
    }
  
    private void error(String tok) {
        System.err.println("Syntax error: " + tok + " --> " + token);
        token=lexer.getToken();
    }
  
    public Command command() {
    // <command> ->  <decl> | <function> | <stmt>
	    if (isType()) {
	        Decl d = decl();
	        return d;
	    }
	    if (token == Token.FUN) {
	        Function f = function();
	        return f;
	    }
	    if (token != Token.EOF) {
	        Stmt s = stmt();
            return s;
	    }
	    return null;
    }

    private Decl decl() {
    // <decl>  -> <type> id [=<expr>]; | <type> id[<n>];
        Type t = type();
	    String id = match(Token.ID);
	    int n=0;
	    Decl d = null;
	    if(token==Token.LBRACKET) {
	    	match(Token.LBRACKET);
	    	n = (int) literal().value;
	    	match(Token.RBRACKET);
	    }
	    
	    if (n!=0)
	    	d = new Decl(id, t, n);
	    else if (token == Token.ASSIGN) {
	        match(Token.ASSIGN);
            Expr e = expr();
	        d = new Decl(id, t, e);
	    } else 
            d = new Decl(id, t);

	    match(Token.SEMICOLON);
	    return d;
    }

    private Decls decls () {
    // <decls> -> {<decl>}
        Decls ds = new Decls ();
	    while (isType()) {
	        Decl d = decl();
	        ds.add(d);
	    }
        return ds;             
    }


    private Function function() {
    // <function>  -> fun <type> id(<params>) <stmt> 
        match(Token.FUN);
	    Type t = type();
	    String str = match(Token.ID);
	    funId = str; 
	    Function f = new Function(str, t);
	    match(Token.LPAREN);
        if (token != Token.RPAREN)
            f.params = params();
	    match(Token.RPAREN);
	    Stmt s = stmt();		
	    f.stmt = s;
	    return f;
    }

    private Decls params() {
	    Decls params = new Decls();
	    Type t = type();
        String id = match(Token.ID);
	    params.add(new Decl(id, t));
        while (token == Token.COMMA) {
	        match(Token.COMMA);
	        t = type();
            id = match(Token.ID);
            params.add(new Decl(id, t));
        }
        return params;
    }
    
    private Type type () {
    // <type>  ->  int | bool | void | string 
        Type t = null;
        switch (token) {
	    case INT:
            t = Type.INT; break;
        case BOOL:
            t = Type.BOOL; break;
        case VOID:
            t = Type.VOID; break;
        case STRING:
            t = Type.STRING; break;
        default:
	        error("int | bool | void | string");
	    }
        match(token);
        return t;       
    }
  
    private Stmt stmt() {
    // <stmt> -> <block> | <assignment> | <ifStmt> | <whileStmt> | ...
        Stmt s = new Empty();
        switch (token) {
	    case SEMICOLON:
            match(token.SEMICOLON); return s;
        case LBRACE:			
	        match(Token.LBRACE);		
            s = stmts();
            match(Token.RBRACE);	
	        return s;
        case IF: 	// if statement 
            s = ifStmt(); return s;
        case WHILE:      // while statement 
            s = whileStmt(); return s;
        case ID:	// assignment
        	Identifier id = new Identifier(match(Token.ID));
        	if(token == Token.LPAREN) s = call(id);
        	else s = assignment(id);
        	return s;
	    case LET:	// let statement 
            s = letStmt(); return s;
	    case READ:	// read statement 
            s = readStmt(); return s;
	    case PRINT:	// print statement 
            s = printStmt(); return s;
	    case RETURN: // return statement 
            s = returnStmt(); return s;
        default:  
	        error("Illegal stmt"); return null; 
	    }
    }
  
    private Stmts stmts () {
    // <block> -> {<stmt>}
        Stmts ss = new Stmts();
	    while((token != Token.RBRACE) && (token != Token.END))
	        ss.stmts.add(stmt()); 
        return ss;
    }

    private Let letStmt () {
    // <letStmt> -> let <decls> in <block> end
	    match(Token.LET);	
        Decls ds = decls();
	    match(Token.IN);
        Stmts ss = stmts();
        match(Token.END);	
        match(Token.SEMICOLON);
        return new Let(ds, null, ss);
    }

    private Read readStmt() {
    // <readStmt> -> read id;
    // parse read statement
    	match(Token.READ);
    	Identifier id = new Identifier(match(Token.ID));
    	match(Token.SEMICOLON);
    	return new Read(id);
    }

    private Print printStmt() {
    // <printStmt> -> print <expr>;
    // parse print statement
    	match(Token.PRINT);
    	Expr e = expr();
    	match(Token.SEMICOLON);
    	return new Print(e);
    }

    private Return returnStmt() {
        // <returnStmt> -> return <expr>; 
            match(Token.RETURN);
            Expr e = expr();
            match(Token.SEMICOLON);
            return new Return(funId, e);
        }

    private Stmt assignment(Identifier id) {
    // <assignment> -> id = <expr>;   | id[<expr>] = <expr>;
        Expr n = null;
        Array a = null;
        
	    if(token==Token.LBRACKET) {
	    	match(Token.LBRACKET);
	    	n = expr();
	    	match(Token.RBRACKET);
	    	a = new Array(id, n);
	    }
        
        match(Token.ASSIGN);
        Expr e = expr();
        match(Token.SEMICOLON);
        
        if(a != null)
        	return new Assignment(a,e);
        else
        	return new Assignment(id, e);
    }

    private Call call(Identifier id) {
    // <call> -> id(<expr>{,<expr>});
    //
    // parse function call
    	match(Token.LPAREN);
    	Call c = new Call(id, arguments());
    	match(Token.RPAREN);
    	match(Token.SEMICOLON);
    	return c;
    }

    private If ifStmt () {
    // <ifStmt> -> if (<expr>) then <stmt> [else <stmt>]
        match(Token.IF);
	    match(Token.LPAREN);
        Expr e = expr();
	    match(Token.RPAREN);
        match(Token.THEN);
        Stmt s1 = stmt();
        Stmt s2 = new Empty();
        if (token == Token.ELSE){
            match(Token.ELSE); 
            s2 = stmt();
        }
        return new If(e, s1, s2);
    }

    private While whileStmt () {
    // <whileStmt> -> while (<expr>) <stmt>
    // parse while statement
    	match(Token.WHILE);
    	match(Token.LPAREN);
    	Expr e = expr();
    	match(Token.RPAREN);
    	Stmt s = stmt();
        return new While(e,s);
    }

    private Expr expr () {
    // <expr> -> <bexp> {& <bexp> | '|'<bexp>} | !<expr> | true | false
        switch (token) {
	    case NOT:
	        Operator op = new Operator(match(token));
	        Expr e = expr();
            return new Unary(op, e);
        case TRUE:
            match(Token.TRUE);
            return new Value(true);
        case FALSE:
            match(Token.FALSE);
            return new Value(false);
        }

        Expr e = bexp();
	//
	// parse logical operations
        while (token == Token.AND || token == Token.OR) {
            Operator op = new Operator(match(token));
            Expr b = bexp();
            e = new Binary(op, e, b);
        }
        return e;
    }

    private Expr bexp() {
        // <bexp> -> <aexp> [ (< | <= | > | >= | == | !=) <aexp> ]
        Expr e = aexp();
	//
	// parse relational operations
        if(token == Token.EQUAL || token == Token.LTEQ || token == Token.LT || token == Token.GTEQ || token == Token.GT|| token == Token.NOTEQ) {
        	Operator op = new Operator(match(token));
        	Expr a = aexp();
        	e = new Binary(op,e,a);
        }
        return e;
    }
  
    private Expr aexp () {
        // <aexp> -> <term> { + <term> | - <term> }
        Expr e = term();
        while (token == Token.PLUS || token == Token.MINUS) {
            Operator op = new Operator(match(token));
            Expr t = term();
            e = new Binary(op, e, t);
        }
        return e;
    }
  
    private Expr term () {
        // <term> -> <factor> { * <factor> | / <factor>}
        Expr t = factor();
        while (token == Token.MULTIPLY || token == Token.DIVIDE) {
            Operator op = new Operator(match(token));
            Expr f = factor();
            t = new Binary(op, t, f);
        }
        return t;
    }
  
    private Expr factor() {
        // <factor> -> [-](id | <call> | id[<expr>] | literal | '('<aexp> ')')
        Operator op = null;
        if (token == Token.MINUS) 
            op = new Operator(match(Token.MINUS));

        Expr e = null;
        switch(token) {
        case ID:
            Identifier v = new Identifier(match(Token.ID));
            if (token == Token.LPAREN) {  // function call
                match(Token.LPAREN); 
                Call c = new Call(v,arguments());
                match(Token.RPAREN);
                e = c;
            } 
            else if(token == Token.LBRACKET) {
            	match(Token.LBRACKET);
    	    	Expr n = expr();
    	    	match(Token.RBRACKET);
    	    	Array a = new Array(v, n);
    	    	e = a;
            }
            else e = v;
            break;
        case NUMBER: case STRLITERAL: 
            e = literal();
            break; 
        case LPAREN: 
            match(Token.LPAREN); 
            e = aexp();       
            match(Token.RPAREN);
            break; 
        default: 
            error("Identifier | Literal"); 
        }

        if (op != null)
            return new Unary(op, e);
        else return e;
    }
    
    private Exprs arguments() {
        // arguments -> [ <expr> {, <expr> } ]
            Exprs es = new Exprs();
            while (token != Token.RPAREN) {
                es.add(expr());
                if (token == Token.COMMA)
                    match(Token.COMMA);
                else if (token != Token.RPAREN)
                    error("Exprs");
            }  
            return es;  
        }

    private Value literal( ) {
        String s = null;
        switch (token) {
        case NUMBER:
            s = match(Token.NUMBER);
            return new Value(Integer.parseInt(s));
        case STRLITERAL:
            s = match(Token.STRLITERAL);
            return new Value(s);
        }
        throw new IllegalArgumentException( "no literal");
    }
 
    private boolean isType( ) {
        switch(token) {
        case INT: case BOOL: case STRING:
            return true;
        default: 
            return false;
        }
    }
    
    public static void main(String args[]) {
	    Parser parser;
	    if (args.length == 0) {
            System.out.println("Begin parsing... ");
	        System.out.print(">> ");
	        Lexer.interactive = true;
	        parser  = new Parser(new Lexer());
	        do {
	            if (parser.token == Token.EOF) {
		            parser.token = parser.lexer.getToken();
		        }

		        Command command = null;
                try {
                    command = parser.command();
                } catch (Exception e) {
                    System.err.println(e);
                }
		        System.out.print("\n>> ");
	        } while(true);
	    }
    	else {
	        System.out.println("Begin parsing... " + args[0]);
	        parser  = new Parser(new Lexer(args[0]));
	        Command command = null;
	        do {
                try {
		            command = parser.command();
                } catch (Exception e) {
                    System.err.println(e);
                }
	        } while (command != null);
	    }
    } //main
} // Parser