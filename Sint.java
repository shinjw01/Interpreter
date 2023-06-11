// Sint.java
// Interpreter for S
import java.util.Iterator;
import java.util.Scanner;

public class Sint {
    static Scanner sc = new Scanner(System.in);
    static State state = new State();

    State Eval(Command c, State state) { 
	if (c instanceof Decl) {
	    Decls decls = new Decls();
	    decls.add((Decl) c);
	    return allocate(decls, state);
	}

	if (c instanceof Function) {
	    Function f = (Function) c; 
	    state.push(f.id, new Value(f)); 
	    return state;
	}

	if (c instanceof Stmt)
	    return Eval((Stmt) c, state); 
		
	    throw new IllegalArgumentException("no command");
    }
  
    State Eval(Stmt s, State state) {
        if (s instanceof Empty) 
	        return Eval((Empty)s, state);
        if (s instanceof Assignment)  
	        return Eval((Assignment)s, state);
        if (s instanceof If)  
	        return Eval((If)s, state);
        if (s instanceof While)  
	        return Eval((While)s, state);
        if (s instanceof Stmts)  
	        return Eval((Stmts)s, state);
	    if (s instanceof Let)  
	        return Eval((Let)s, state);
	    if (s instanceof Read)  
	        return Eval((Read)s, state);
	    if (s instanceof Print)  
	        return Eval((Print)s, state);
        if (s instanceof Call) {
        	//Call c = (Call) s;
        	//Value v = state.get(c.fid);
        	//if(v.funValue().type == Type.VOID)
        		return Eval((Call)s, state);
        	//else
        		//return V((Call)s, state);	
        }
	    if (s instanceof Return) 
	        return Eval((Return)s, state);
        throw new IllegalArgumentException("no statement");
    }

    // call without return value
    State Eval(Call c, State state) {
	//
	// evaluate call without return value
    	 Value v = state.get(c.fid);  		// find function
         Function f = v.funValue();
         State s = newFrame(state, c, f); // create new frame on the stack
         s = Eval(f.stmt, s); 					// interpret the call
         s = deleteFrame(s, c, f); 			// delete the frame on the stack
         return s;
    }

    // value-returning call 
    Value V (Call c, State state) { 
	    Value v = state.get(c.fid);  		// find function
        Function f = v.funValue();
        State s = newFrame(state, c, f); // create new frame on the stack
        s = Eval(f.stmt, s); 					// interpret the call
	    v = s.pop().val;   						// remove the return value
        s = deleteFrame(s, c, f); 			// delete the frame on the stack
    	return v;
    }

    State Eval(Return r, State state) {
        Value v = V(r.expr, state);
        return state.push(r.fid, v);
    }

    State newFrame (State state, Call c, Function f) {
        if (c.args.size() == 0) 
            return state;
	// evaluate arguments
	// activate a new stack frame in the stack 
        Value val[] = new Value[f.params.size()];
        int i = 0;
        for(Expr e: c.args)
        	val[i++] = V(e,state);
        
        allocate(f.params, state);
        //인자값을 매개변수에 전달.................
        for(i=0;i<f.params.size();i++)
        	state.set(f.params.get(i).id,val[i]);
        
        return state;
    }

    State deleteFrame (State state, Call c, Function f) {
	//
	// free a stack frame from the stack
	//
    	free(f.params,state);
    	
    	return state;
    }

    State Eval(Empty s, State state) {
        return state;
    }
  
    State Eval(Assignment a, State state) {
    	//add array assignment
    	if(a.ar == null) {
    		Value v = V(a.expr, state);
    		return state.set(a.id, v);
	    }
    	else {
    		Value v = V(a.expr, state);
    		Identifier i = a.ar.id;
    		Value n = V(a.ar.expr,state);
        	(state.get(i).arrValue())[n.intValue()] = v;
    		return state;
    	}
    }

    State Eval(Read r, State state) {
    	Value v = state.get(r.id);
        if (v.type == Type.INT) {
	        int i = sc.nextInt();
	        state.set(r.id, new Value(i));
	    } 

	    if (v.type == Type.BOOL) {
	        boolean b = sc.nextBoolean();	
            state.set(r.id, new Value(b));
	    }

	//
	// input string 
	    if(v.type == Type.STRING) {
	    	String s = sc.next();
	    	state.set(r.id, new Value(s));
	    }
	
	    return state;
    }

    State Eval(Print p, State state) {
	    System.out.println(V(p.expr, state));
        return state; 
    }
  
    State Eval(Stmts ss, State state) {
        for (Stmt s : ss.stmts) {
            state = Eval(s, state);
        }
        return state;
    }
  
    State Eval(If c, State state) {
        if (V(c.expr, state).boolValue( ))
            return Eval(c.stmt1, state);
        else
            return Eval(c.stmt2, state);
    }
 
    State Eval(While l, State state) {
        if (V(l.expr, state).boolValue( ))
            return Eval(l, Eval(l.stmt, state));
        else 
	        return state;
    }

    State Eval(Let l, State state) {
        State s = allocate(l.decls, state);
        s = Eval(l.stmts,s);
	    return free(l.decls, s);
    }

    State allocate(Decls ds, State state) {
        // add entries for declared variables on the state
        //
    	if (ds != null)
    	{
    		Iterator<Decl> it = ds.iterator();
        	while(it.hasNext()) {
        		Decl d = it.next();
        		if(d.expr != null && d.arraysize == 0)
        			state.push(d.id, (Value)d.expr);
        		else if(d.expr == null && d.arraysize == 0) {
        			if(d.type == Type.INT) state.push(d.id, new Value(0));
        			if(d.type == Type.BOOL) state.push(d.id, new Value(false));
        			if(d.type == Type.STRING) state.push(d.id, new Value(""));
        		}
        		else state.push(d.id, new Value(new Value[d.arraysize]));
        	}
        	return state;
    	}

        return null;
    }

    State free (Decls ds, State state) {
        // free the entries for declared variables from the state
        //
    	if (ds != null) {
    		Iterator<Decl> it = ds.iterator();
        	while(it.hasNext()) {
        		state.pop();
        		it.next();
        	}
        	return state;
    	}
        return null;
    }

    Value binaryOperation(Operator op, Value v1, Value v2) {
        check(!v1.undef && !v2.undef,"reference to undef value");
	    switch (op.val) {
	    case "+":
            return new Value(v1.intValue() + v2.intValue());
        case "-": 
            return new Value(v1.intValue() - v2.intValue());
        case "*": 
            return new Value(v1.intValue() * v2.intValue());
        case "/": 
            return new Value(v1.intValue() / v2.intValue());
	//
	// relational operations 
        case "<":
        	if(v1.type == Type.INT && v2.type == Type.INT) {
        		return new Value(v1.intValue() < v2.intValue());
        	}
        	if(v1.type == Type.STRING && v2.type == Type.STRING) {
        		int num = v1.toString().compareTo(v2.toString());
        		if(num < 0)
        			return new Value(true);
        		else
        			return new Value(false);
        	}
        	else
        		throw new IllegalArgumentException("wrong operator type");
        case "<=":
        	if(v1.type == Type.INT && v2.type == Type.INT) {
        		return new Value(v1.intValue() <= v2.intValue());
        	}
        	if(v1.type == Type.STRING && v2.type == Type.STRING) {
        		int num = v1.toString().compareTo(v2.toString());
        		if(num <= 0)
        			return new Value(true);
        		else
        			return new Value(false);
        	}
        	else
        		throw new IllegalArgumentException("wrong operator type");
        case ">":
        	if(v1.type == Type.INT && v2.type == Type.INT) {
        		return new Value(v1.intValue() > v2.intValue());
        	}
        	if(v1.type == Type.STRING && v2.type == Type.STRING) {
        		int num = v1.toString().compareTo(v2.toString());
        		if(num > 0)
        			return new Value(true);
        		else
        			return new Value(false);
        	}
        	else
        		throw new IllegalArgumentException("wrong operator type");
        case ">=":
        	if(v1.type == Type.INT && v2.type == Type.INT) {
        		return new Value(v1.intValue() >= v2.intValue());
        	}
        	if(v1.type == Type.STRING && v2.type == Type.STRING) {
        		int num = v1.toString().compareTo(v2.toString());
        		if(num >= 0)
        			return new Value(true);
        		else
        			return new Value(false);
        	}
        	else
        		throw new IllegalArgumentException("wrong operator type");
        case "==":
        	if(v1.type == Type.INT && v2.type == Type.INT) {
        		return new Value(v1.intValue() == v2.intValue());
        	}
        	if(v1.type == Type.STRING && v2.type == Type.STRING) {
        		int num = v1.toString().compareTo(v2.toString());
        		if(num == 0)
        			return new Value(true);
        		else
        			return new Value(false);
        	}
        	else
        		throw new IllegalArgumentException("wrong operator type");
        case "!=":
        	if(v1.type == Type.INT && v2.type == Type.INT) {
        		return new Value(v1.intValue() != v2.intValue());
        	}
        	if(v1.type == Type.STRING && v2.type == Type.STRING) {
        		int num = v1.toString().compareTo(v2.toString());
        		if(num != 0)
        			return new Value(true);
        		else
        			return new Value(false);
        	}
        	else
        		throw new IllegalArgumentException("wrong operator type");

    //
	// logical operations
        case "&":
        	return new Value(v1.boolValue() && v2.boolValue());
        case "|":
        	return new Value(v1.boolValue() || v2.boolValue());

	    default:
	        throw new IllegalArgumentException("no operation");
	    }
    } 
    
    Value unaryOperation(Operator op, Value v) {
        check( !v.undef, "reference to undef value");
	    switch (op.val) {
        case "!": 
            return new Value(!v.boolValue( ));
        case "-": 
            return new Value(-v.intValue( ));
        default:
            throw new IllegalArgumentException("no operation: " + op.val); 
        }
    } 

    static void check(boolean test, String msg) {
        if (test) return;
        System.err.println(msg);
    }

    Value V(Expr e, State state) {
        if (e instanceof Value) 
            return (Value) e;

        if (e instanceof Identifier) {
	        Identifier v = (Identifier) e;
            return (Value)(state.get(v));
	    }

        if (e instanceof Binary) {
            Binary b = (Binary) e;
            Value v1 = V(b.expr1, state);
            Value v2 = V(b.expr2, state);
            return binaryOperation (b.op, v1, v2); 
        }

        if (e instanceof Unary) {
            Unary u = (Unary) e;
            Value v = V(u.expr, state);
            return unaryOperation(u.op, v); 
        }

        if (e instanceof Call) 
    	    return V((Call)e, state); 
        if (e instanceof Array) {
        	Identifier i = ((Array) e).id;
        	Value n = V(((Array)e).expr,state);
        	return (Value)((state.get(i).arrValue())[n.intValue()]);
        }
        
        throw new IllegalArgumentException("no operation");
    }

    public static void main(String args[]) {
	    if (args.length == 0) {
	        Sint sint = new Sint(); Lexer.interactive = true;
            System.out.println("Language S Interpreter 1.0");
            System.out.print(">> ");
	        Parser parser  = new Parser(new Lexer());

	        do { // Program = Command*
	            if (parser.token == Token.EOF)
		        parser.token = parser.lexer.getToken();
	       
	            Command command=null;
                try {
	                command = parser.command();
                    //command.type = TypeChecker.Check(command); 
                } catch (Exception e) {
                    System.out.println(e);
		            System.out.print(">> ");
                    continue;
                }

	            if (command.type != Type.ERROR) {
                    System.out.println("\nInterpreting..." );
                    try {
                        state = sint.Eval(command, state);
                    } catch (Exception e) {
                         System.err.println(e);  
                    }
                }
		    System.out.print(">> ");
	        } while (true);
	    }
        else {
	        System.out.println("Begin parsing... " + args[0]);
	        Command command = null;
	        Parser parser  = new Parser(new Lexer(args[0]));
	        Sint sint = new Sint();

	        do {	// Program = Command*
	            if (parser.token == Token.EOF)
                    break;

                try {
		            command = parser.command();
                    // command.type = TypeChecker.Check(command);    
                } catch (Exception e) {
                    System.out.println(e);
                    continue;
                }

	            if (command.type!=Type.ERROR) {
                    System.out.println("\nInterpreting..." + args[0]);
                    try {
                        state = sint.Eval(command, state);
                    } catch (Exception e) {
                        System.err.println(e);  
                    }
                }
	        } while (command != null);
        }        
    }
}