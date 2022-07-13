import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.*;
import javax.swing.JFileChooser;

/**
 * The parser and interpreter. The top level parse function, a main method for
 * testing, and several utility methods are provided. You need to implement
 * parseProgram and all the rest of the parser.
 */
public class Parser {

	/**
	 * Top level parse method, called by the World
	 */
	static RobotProgramNode parseFile(File code) {
		Scanner scan = null;
		try {
			scan = new Scanner(code);

			// the only time tokens can be next to each other is
			// when one of them is one of (){},;
			scan.useDelimiter("\\s+|(?=[{}(),;])|(?<=[{}(),;])");

			RobotProgramNode n = parseProgram(scan); // You need to implement this!!!

			scan.close();
			return n;
		} catch (FileNotFoundException e) {
			System.out.println("Robot program source file not found");
		} catch (ParserFailureException e) {
			System.out.println("Parser error:");
			System.out.println(e.getMessage());
			scan.close();
		}
		return null;
	}

	/** For testing the parser without requiring the world */

	public static void main(String[] args) {
		if (args.length > 0) {
			for (String arg : args) {
				File f = new File(arg);
				if (f.exists()) {
					System.out.println("Parsing '" + f + "'");
					RobotProgramNode prog = parseFile(f);
					System.out.println("Parsing completed ");
					if (prog != null) {
						System.out.println("================\nProgram:");
						System.out.println(prog);
					}
					System.out.println("=================");
				} else {
					System.out.println("Can't find file '" + f + "'");
				}
			}
		} else {
			while (true) {
				JFileChooser chooser = new JFileChooser(".");// System.getProperty("user.dir"));
				int res = chooser.showOpenDialog(null);
				if (res != JFileChooser.APPROVE_OPTION) {
					break;
				}
				RobotProgramNode prog = parseFile(chooser.getSelectedFile());
				System.out.println("Parsing completed");
				if (prog != null) {
					System.out.println("Program: \n" + prog);
				}
				System.out.println("=================");
			}
		}
		System.out.println("Done");
	}

	// Useful Patterns

	static Pattern NUMPAT = Pattern.compile("-?(0|[1-9][0-9]*)"); 
	static Pattern OPENPAREN = Pattern.compile("\\(");
	static Pattern CLOSEPAREN = Pattern.compile("\\)");
	static Pattern OPENBRACE = Pattern.compile("\\{");
	static Pattern CLOSEBRACE = Pattern.compile("\\}");

	// Act patterns 
	static Pattern MOVEPAT = Pattern.compile("move");
	static Pattern TURNLPAT = Pattern.compile("turnL"); 
	static Pattern TURNRPAT = Pattern.compile("turnR");
	static Pattern TAKEFUELPAT = Pattern.compile("takeFuel");
	static Pattern WAITPAT = Pattern.compile("wait");
	static Pattern SHIELDONPAT = Pattern.compile("shieldOn");
	static Pattern SHIELDOFFPAT = Pattern.compile("shieldOff");
	static Pattern TURNAROUNDPAT = Pattern.compile("turnAround");

	// Condidtion/loop patterns
	static Pattern LOOPPAT = Pattern.compile("loop");
	static Pattern WHILEPAT = Pattern.compile("while");
	static Pattern IFPAT = Pattern.compile("if");

	// Op patterns
	static Pattern ADDPAT = Pattern.compile("add");
	static Pattern SUBPAT = Pattern.compile("sub");
	static Pattern MULPAT = Pattern.compile("mul");
	static Pattern DIVPAT = Pattern.compile("div");

	// Or patterns 
	static Pattern ACTPAT = Pattern.compile("move|turnL|turnR|takeFuel|wait|shieldOn|shieldOff|turnAround");
	static Pattern SENPAT = Pattern.compile("fuelLeft|oppLR|oppFB|numBarrels|barrelLR|barrelFB|wallDist");
	static Pattern RELOPPAT = Pattern.compile("lt|gt|eq");
	static Pattern OPPAT = Pattern.compile("add|sub|mul|div");
	static Pattern CONDPAT = Pattern.compile("and|or|not");

	/**
	 * See assignment handout for the grammar.
	 */
	static RobotProgramNode parseProgram(Scanner s) {
		// THE PARSER GOES HERE
		if(!s.hasNext()){fail("Empty expr", s);}

		ProgNode prog = new ProgNode(new ArrayList<RobotProgramNode>());

		while(s.hasNext()){
			prog.addChild(parseStmt(s));
		}

		return prog;
	}

	static RobotProgramNode parseStmt(Scanner s){
		if(!s.hasNext()){fail("Empty expr", s);}
		RobotProgramNode child = null;

		if(s.hasNext(ACTPAT)){
			child = parseAct(s); 
		}else if(s.hasNext(LOOPPAT)){
			child = parseLoop(s); 
		}else if(s.hasNext(IFPAT)){
			child = parseIf(s);
		}else if(s.hasNext(WHILEPAT)){
			child = parseWhile(s);
		}

		return child;
	}

	static RobotProgramNode parseAct(Scanner s){
		if(!s.hasNext()){fail("Empty expr", s);}
		RobotProgramNode child = null; 

		if(s.hasNext(MOVEPAT)){
			s.next();
			if(s.hasNext(OPENPAREN)){
				String str = require(OPENPAREN, "No open parenthesis.", s);
				RobotSensorNode num = parseExp(s);
				str = require(CLOSEPAREN, "No closing parenthesis.", s);
				child = new MoveNode(num);
			}else{
				child = new MoveNode(); 
			}
		}else if(s.hasNext(TURNLPAT)){
			s.next();
			child = new TurnLNode();
		}else if(s.hasNext(TURNRPAT)){
			s.next();
			child = new TurnRNode();
		}else if(s.hasNext(TAKEFUELPAT)){
			s.next();
			child = new TakeFuelNode();
		}else if(s.hasNext(WAITPAT)){
			s.next();
			if(s.hasNext(OPENPAREN)){
				String str = require(OPENPAREN, "No open parenthesis.", s);
				RobotSensorNode num = parseExp(s);
				str = require(CLOSEPAREN, "No closing parenthesis.", s);
				child = new WaitNode(num);
			}else{
				child = new WaitNode(); 
			}
		}else if(s.hasNext(SHIELDONPAT)){
			s.next();
			child = new shieldOnNode();
		}else if(s.hasNext(SHIELDOFFPAT)){
			s.next();
			child = new shieldOffNode();
		}else if(s.hasNext(TURNAROUNDPAT)){
			s.next();
			child = new turnAroundNode();
		}

		// Make sure that there is a semicolon on the end
		String str = require(";", " Error: no semicolon.", s);
		return child; 
	}

	static RobotSensorNode parseExp(Scanner s){
		if(!s.hasNext()){fail("Empty expr", s);}
		RobotSensorNode child = null;
		if(s.hasNext(NUMPAT)){ // It is a number
			int num = requireInt(NUMPAT, "Not an integer", s);
			child = new numNode(num);
		}else if(s.hasNext(SENPAT)){ // It is a sensor
			child = parseSen(s);
		}else if(s.hasNext(OPPAT)){ // It is an opperator
			child = parseOp(s);
		}
		return child; 
	}

	static RobotProgramNode parseLoop(Scanner s){
		if(!s.hasNext()){fail("Empty expr", s);}
		String str = require(LOOPPAT, "Not 'loop'", s);

		RobotProgramNode child = null; 

		if(str.equals("loop")){
			child = parseBlock(s);
		}

		return child; 
	}

	static RobotProgramNode parseBlock(Scanner s){
		if(!s.hasNext()){fail("Empty expr", s);}

		BlockNode child = null; 
		
		String str = require(OPENBRACE, "No open brace.", s);

		if(str.equals("{")){
			child = new BlockNode();
		}

		while(s.hasNext()){
			if(!s.hasNext("}")){
				RobotProgramNode blockChild = parseStmt(s);
				child.addNode(blockChild);
			}else{
				break;
			}
		}	
		
		String str2 = require(CLOSEBRACE, "No close brack.", s);

		return child; 
	}

	static RobotProgramNode parseIf(Scanner s){
		if(!s.hasNext()){fail("Empty expr", s);}
		
		ifNode nodeIf = new ifNode(); 
		RobotConditionNode cond = null; 
		BlockNode block = null; 

		// require 'if' and then '('
		String str = require(IFPAT, "Not 'if", s);

		str = require(OPENPAREN, "No open parenthesis.", s);

		cond = parseCond(s); 

		// require a ')' at the end of the condition
		str = require(CLOSEPAREN, "No close parenthesis.", s); 

		block = (BlockNode) parseBlock(s);

		// if there is an 'else', set the 
		if(s.hasNext("else")){
			str = require("else", "No 'else'.", s); 
			nodeIf.setElse(true); 
			nodeIf.setElseBlockNode(parseBlock(s));
		}

		nodeIf.setCondNode(cond);
		nodeIf.setBlockNode(block);
				
		return nodeIf;
	}

	static RobotProgramNode parseWhile(Scanner s){
		if(!s.hasNext()){fail("Empty expr", s);}

		whileNode nodeWhile = new whileNode();
		RobotConditionNode cond = null; 
		BlockNode block = null; 

		// require 'while' and then '('
		String str = require(WHILEPAT, "Not 'while", s);

		str = require(OPENPAREN, "No open parenthesis.", s);

		cond = parseCond(s); 

		// require a ')' at the end of the condition
		str = require(CLOSEPAREN, "No close parenthesis.", s); 
		
		block = (BlockNode) parseBlock(s);

		nodeWhile.setCondNode(cond);
		nodeWhile.setBlockNode(block);

		return nodeWhile;
	}

	static RobotConditionNode parseCond(Scanner s){
		if(!s.hasNext()){fail("Empty expr", s);}
		condNode cond = new condNode();
		RobotConditionNode relop = null;
		RobotSensorNode expOne = null;
		RobotSensorNode expTwo = null; 

		if(s.hasNext(RELOPPAT)){ // if it is ==, < or >
			relop = parseRelop(s);	
			String str = require(OPENPAREN, "No open parenthesis.", s);
			expOne = parseExp(s);
			str = require(",", "No comma COND.", s);
			expTwo = parseExp(s);
			str = require(CLOSEPAREN, "No close parenthesis.", s);

			cond.setRelop(relop);
			cond.setExpOne(expOne);
			cond.setExpTwo(expTwo);
		}else if(s.hasNext(CONDPAT)){ // if it is and, or, not... 
			return parseCondOp(s);
		}

		return cond;
	}

	static RobotConditionNode parseRelop(Scanner s){
		if(!s.hasNext()){fail("Empty expr", s);}
		RobotConditionNode child = null; 

		if(s.hasNext("lt")){
			child = new ltNode();
		}else if(s.hasNext("gt")){
			child = new gtNode();
		}else if(s.hasNext("eq")){
			child = new eqNode();
		}

		s.next();
		return child;
	}

	static RobotConditionNode parseCondOp(Scanner s){
		if(!s.hasNext()){fail("Empty expr", s);}
		RobotConditionNode child = null; 

		if(s.hasNext("and")){
			s.next(); 
			String str = require(OPENPAREN, "No open parenthesis", s);
			RobotConditionNode condOne = parseCond(s);
			str = require(",", "No comma", s);
			RobotConditionNode condTwo = parseCond(s);
			str = require(CLOSEPAREN, "No closing parenthesis", s);
			child = new andNode(condOne, condTwo); 
		}else if(s.hasNext("or")){
			s.next(); 
			String str = require(OPENPAREN, "No open parenthesis", s);
			RobotConditionNode condOne = parseCond(s);
			str = require(",", "No comma", s);
			RobotConditionNode condTwo = parseCond(s);
			str = require(CLOSEPAREN, "No closing parenthesis", s);
			child = new orNode(condOne, condTwo); 
		}else if(s.hasNext("not")){
			s.next(); 
			String str = require(OPENPAREN, "No open parenthesis", s);
			RobotConditionNode cond = parseCond(s);
			str = require(CLOSEPAREN, "No closing parenthesis", s);
			child = new notNode(cond); 
		}
		return child;
	}

	static RobotSensorNode parseSen(Scanner s){
		if(!s.hasNext()){fail("Empty expr", s);}
		RobotSensorNode child = null; 
		String str = require(SENPAT, "Entered invalid sensor", s);

		if(str.equals("fuelLeft")){
			child = new fuelLeftNode();
		}else if(str.equals("oppLR")){
			child = new oppLRNode();
		}else if(str.equals("oppFB")){
			child = new oppFBNode();
		}else if(str.equals("numBarrels")){
			child = new numBarrelsNode();
		}else if(str.equals("barrelLR")){
			child = new barrelLRNode();
		}else if(str.equals("barrelFB")){
			child = new barrelFBNode();
		}else if(str.equals("wallDist")){
			child = new wallDistNode(); 
		}

		return child;
	}

	static RobotSensorNode parseOp(Scanner s){
		if(!s.hasNext()){fail("Empty expr", s);}
		RobotSensorNode child = null; 

		if(s.hasNext(ADDPAT)){
			s.next();
			String str = require(OPENPAREN, "No open parenthesis.", s);
			RobotSensorNode condOne = parseExp(s);
			str = require(",", "No comma", s);
			RobotSensorNode condTwo = parseExp(s);
			str = require(CLOSEPAREN, "No close parenthesis.", s);
			child = new addNode(condOne,condTwo);
		}else if(s.hasNext(SUBPAT)){
			s.next();
			String str = require(OPENPAREN, "No open parenthesis.", s);
			RobotSensorNode condOne = parseExp(s);
			str = require(",", "No comma at sub", s);
			RobotSensorNode condTwo = parseExp(s);
			str = require(CLOSEPAREN, "No close parenthesis.", s);
			child = new subNode(condOne,condTwo);
		}else if(s.hasNext(MULPAT)){
			s.next();
			String str = require(OPENPAREN, "No open parenthesis.", s);
			RobotSensorNode condOne = parseExp(s);
			str = require(",", "No comma", s);
			RobotSensorNode condTwo = parseExp(s);
			str = require(CLOSEPAREN, "No close parenthesis.", s);
			child = new mulNode(condOne,condTwo);
		}else if(s.hasNext(DIVPAT)){
			s.next();
			String str = require(OPENPAREN, "No open parenthesis.", s);
			RobotSensorNode condOne = parseExp(s);
			str = require(",", "No comma", s);
			RobotSensorNode condTwo = parseExp(s);
			str = require(CLOSEPAREN, "No close parenthesis.", s);
			child = new divNode(condOne,condTwo);
		}
		return child; 
	}

	
	// utility methods for the parser

	/**
	 * Report a failure in the parser.
	 */
	static void fail(String message, Scanner s) {
		String msg = message + "\n   @ ...";
		for (int i = 0; i < 5 && s.hasNext(); i++) {
			msg += " " + s.next();
		}
		throw new ParserFailureException(msg + "...");
	}

	/**
	 * Requires that the next token matches a pattern if it matches, it consumes
	 * and returns the token, if not, it throws an exception with an error
	 * message
	 */
	static String require(String p, String message, Scanner s) {
		if (s.hasNext(p)) {
			return s.next();
		}
		fail(message, s);
		return null;
	}

	static String require(Pattern p, String message, Scanner s) {
		if (s.hasNext(p)) {
			return s.next();
		}
		fail(message, s);
		return null;
	}

	/**
	 * Requires that the next token matches a pattern (which should only match a
	 * number) if it matches, it consumes and returns the token as an integer if
	 * not, it throws an exception with an error message
	 */
	static int requireInt(String p, String message, Scanner s) {
		if (s.hasNext(p) && s.hasNextInt()) {
			return s.nextInt();
		}
		fail(message, s);
		return -1;
	}

	static int requireInt(Pattern p, String message, Scanner s) {
		if (s.hasNext(p) && s.hasNextInt()) {
			return s.nextInt();
		}
		fail(message, s);
		return -1;
	}

	/**
	 * Checks whether the next token in the scanner matches the specified
	 * pattern, if so, consumes the token and return true. Otherwise returns
	 * false without consuming anything.
	 */
	static boolean checkFor(String p, Scanner s) {
		if (s.hasNext(p)) {
			s.next();
			return true;
		} else {
			return false;
		}
	}

	static boolean checkFor(Pattern p, Scanner s) {
		if (s.hasNext(p)) {
			s.next();
			return true;
		} else {
			return false;
		}
	}

}

// You could add the node classes here, as long as they are not declared public (or private) -------------------------------------------------


class ProgNode implements RobotProgramNode{
	ArrayList<RobotProgramNode> children; 

    public ProgNode(ArrayList<RobotProgramNode> ch){
        this.children = ch; 
    }

	public void addChild(RobotProgramNode n){
		this.children.add(n);
	}

    public void execute(Robot robot) {
		for(RobotProgramNode ch: children){
			ch.execute(robot);
		}
    }

	public ArrayList<RobotProgramNode> getChildren(){
		return this.children; 
	}

    public String toString(){
		String s = "";
		for(RobotProgramNode ch: children){
			s = s + ch.toString() + " ";
		}

        return s;
    }
}

class StmtNode implements RobotProgramNode{
    RobotProgramNode child; 

    public StmtNode(RobotProgramNode ch){
        this.child = ch; 
    }
    
    public void execute(Robot robot) {
        child.execute(robot);
    }

    public String toString(){
        if(child instanceof ActNode){
            return child.toString() + ";"; 
        }
        return child.toString(); 
    }
}

//--  ACT CLASSES  ---------------------------------------------------------------------------------------------------------------------
class ActNode implements RobotProgramNode{
    RobotProgramNode child; 

    public ActNode(RobotProgramNode ch){
        this.child = ch;
    }

    public void execute(Robot robot) {
        child.execute(robot);
    }

    public String toString(){
        return child.toString(); 
    }
}

class LoopNode implements RobotProgramNode{
    BlockNode block;
    public LoopNode(BlockNode bn){this.block = bn; }
    public String toString() {return "loop" + this.block.toString();}
    public void execute(Robot robot) {this.block.execute(robot);}
}

class MoveNode implements RobotProgramNode{
	RobotSensorNode num = null; 
	public MoveNode(){}
	public MoveNode(RobotSensorNode n){this.num = n;}

    public void execute(Robot robot) {
		// move the robot multiple times
		if(num != null){
			for(int i = 0; i < num.evaluate(robot); i++){
				robot.move();
			}
		}else{robot.move();}
	}

	public void setNum(numNode i){this.num = i;}
    public String toString() {
		if(num != null){
			return "move(" + num.toString() + ")"; 
		}
		return "move";
	}
}

class TurnLNode implements RobotProgramNode{
	public TurnLNode(){}
    public void execute(Robot robot) {robot.turnLeft();}
    public String toString() {return "turnL";}
}

class TurnRNode implements RobotProgramNode{
	public TurnRNode(){}
    public void execute(Robot robot) {robot.turnRight();}
    public String toString() {return "turnR";}
}

class TakeFuelNode implements RobotProgramNode{
	public TakeFuelNode(){}
    public void execute(Robot robot) {robot.takeFuel();}  
    public String toString() {return "takeFuel";}
}

class WaitNode implements RobotProgramNode{
	RobotSensorNode num = null;

	public WaitNode(){}
	public WaitNode(RobotSensorNode n){this.num = n;}

    public void execute(Robot robot) {
		if(num != null){
			for(int i = 0; i < num.evaluate(robot); i++){
				robot.idleWait();
			}
		}
		robot.idleWait();
	}

	public void setNum(numNode n){this.num = n;}
    public String toString() {
		if(num != null){
			return "wait(" + num.toString() + ")"; 
		}
		return "wait";
	}
}

class turnAroundNode implements RobotProgramNode{
	public turnAroundNode(){}
    public void execute(Robot robot) {robot.turnAround();}
    public String toString() {return "turnAround";}
}

class shieldOnNode implements RobotProgramNode{
	public shieldOnNode(){}
    public void execute(Robot robot) {robot.setShield(true);}
    public String toString() {return "shieldOn";}
}

class shieldOffNode implements RobotProgramNode{
	public shieldOffNode(){}
    public void execute(Robot robot) {robot.setShield(false);}
    public String toString() {return "shieldOff";}
}

//--  CONDITIONAL CLASSES  ---------------------------------------------------------------------------------------------------------------------
class BlockNode implements RobotProgramNode{
    ArrayList<RobotProgramNode> children;

	public BlockNode(){
		children = new ArrayList<RobotProgramNode>(); 
	}

    public void addNode(RobotProgramNode ch){
        children.add(ch);
    }

    public void execute(Robot robot) {
		// for core, needs to be in a while(true){} loop
		for(RobotProgramNode n: children){
			n.execute(robot); 
		}
    }

    public String toString() {
        String s = "{";
        for(RobotProgramNode n: children){ 
		s = s + n.toString() + " ";}
        return s + "}"; 
    }
}

class whileNode implements RobotProgramNode{
	RobotConditionNode cond = null; 
	RobotProgramNode block = null;

	public whileNode(){}
	public void setCondNode(RobotConditionNode cn){this.cond = cn; }
	public void setBlockNode(RobotProgramNode rpn){this.block = rpn;}

	public void execute(Robot robot){
		System.out.println("start while");
		while(cond.evaluate(robot)){
			System.out.println("in while");
			block.execute(robot);
		}
	}

	public String toString() {
		String whileToString = "while(" + cond.toString() + "){" + block.toString() + "}";
		return whileToString;
	}
}

class ifNode implements RobotProgramNode{
	RobotConditionNode cond = null; 
	RobotProgramNode block = null;
	RobotProgramNode elseBlock = null; 
	boolean hasElse = false; 
	
	public ifNode(){}
	public void setCondNode(RobotConditionNode cn){this.cond = cn; }
	public void setBlockNode(RobotProgramNode rpn){this.block = rpn;}
	public void setElseBlockNode(RobotProgramNode rpn){this.elseBlock = rpn;}
	public void setElse(boolean b){this.hasElse = b;}

    public void execute(Robot robot){
		if(cond.evaluate(robot)){
			block.execute(robot); 
		}else if(hasElse){
			elseBlock.execute(robot);
		}
	}

    public String toString() {
		if(hasElse){
			return "if(" + cond.toString() + "){" + block.toString() + "}" + "else{" + elseBlock.toString() + "}";
		}
		return "if(" + cond.toString() + "){" + block.toString() + "}"; 
	}
}

class condNode implements RobotConditionNode{
	RobotConditionNode relop = null;
	RobotSensorNode expOne = null; 
	RobotSensorNode expTwo = null; 

	public condNode(){}

	public void setRelop(RobotConditionNode r){this.relop = r;}
	public void setExpOne(RobotSensorNode r){this.expOne = r;}
	public void setExpTwo(RobotSensorNode n){this.expTwo = n;}

	public boolean evaluate(Robot robot){
		if(relop.toString().equals("eq")){
			return (expOne.evaluate(robot) == expTwo.evaluate(robot)); 
		}
		if(relop.toString().equals("lt")){
			return (expOne.evaluate(robot) < expTwo.evaluate(robot)); 
		}
		if(relop.toString().equals("gt")){
			return (expOne.evaluate(robot) > expTwo.evaluate(robot));
		}
		return true; 
	}
    public String toString() {
		if(relop.toString().equals("eq")){
			return expOne.toString() + " == " + expTwo.toString();
		}
		if(relop.toString().equals("lt")){
			return expOne.toString() + " < " + expTwo.toString();
		}
		if(relop.toString().equals("gt")){
			return expOne.toString() + " > " + expTwo.toString();
		}
		return null;
	}
}

//--  COMPARISONS CLASSES  ---------------------------------------------------------------------------------------------------------------------
class relopNode implements RobotConditionNode{
	public relopNode(){}
    public boolean evaluate(Robot robot) {
		return true;
	}
    public String toString() {return null;}
}

// Less than conditional
class ltNode implements RobotConditionNode{
	public ltNode(){}
    public boolean evaluate(Robot robot) {
		return true; 
	}
    public String toString() {return "lt";}
}

// Greater than conditional
class gtNode implements RobotConditionNode{
	public gtNode(){}
    public boolean evaluate(Robot robot) {
		return true;
	}
    public String toString() {return "gt";}
}

// Equals to conditional
class eqNode implements RobotConditionNode{
	public eqNode(){}
    public boolean evaluate(Robot robot) {
		return true;
	}
    public String toString() {return "eq";}
}


//--  SENSORS CLASSES  ---------------------------------------------------------------------------------------------------------------------
// see if the robot has feul left
class fuelLeftNode implements RobotSensorNode{
	public fuelLeftNode(){}
	public int evaluate(Robot robot){return robot.getFuel();}
    public String toString() {return "fuelLeft";}
}

// get the x coordinate of the opposition
class oppLRNode implements RobotSensorNode{
	public oppLRNode(){}
	public int evaluate(Robot robot){return robot.getOpponentLR();}
    public String toString() {return "oppLR";}
}

// get the y coordinate of the opposition
class oppFBNode implements RobotSensorNode{
	public oppFBNode(){}
	public int evaluate(Robot robot){return robot.getOpponentFB();}
    public String toString() {return "oppLR";}
}

// get the x coordinate of the closest barrel
class barrelLRNode implements RobotSensorNode{
	public barrelLRNode(){}
	public int evaluate(Robot robot){
		System.out.println("Running barrelLR");
		System.out.println("Num barrels: " +robot.numBarrels());
		return robot.getClosestBarrelLR();
	}
    public String toString() {return "barrelLR";}
}

// get the y coordinate of the closest barrel
class barrelFBNode implements RobotSensorNode{
	public barrelFBNode(){}
	public int evaluate(Robot robot){return robot.getClosestBarrelFB();}
    public String toString() {return "barrelFB";}
}

// get the number of barrels
class numBarrelsNode implements RobotSensorNode{
	public numBarrelsNode(){}
	public int evaluate(Robot robot){return robot.numBarrels();}
    public String toString() {return "numBarrels";}
}

// get the distance to the wall infront of the robot
class wallDistNode implements RobotSensorNode{
	public wallDistNode(){}
	public int evaluate(Robot robot){return robot.getDistanceToWall();}
    public String toString() {return "wallDist";}
}

// --  OP NODES  ------------------------------------------------------------------------------------------------------------------------------
// Number node
class numNode implements RobotSensorNode{
	int num; 
	public numNode(int n){
		this.num = n;
	}

	public void setNum(int n){this.num = n;}
	public int evaluate(Robot robot){
		return this.num;
	}

	public String toString(){
		return String.valueOf(this.num); 
	}
}

// Addition
class addNode implements RobotSensorNode{
	RobotSensorNode conditionOne;
	RobotSensorNode conditionTwo;

	public addNode(RobotSensorNode one, RobotSensorNode two){
		this.conditionOne = one;
		this.conditionTwo = two; 
	}

	public void setConOne(RobotSensorNode n){this.conditionOne = n;}
	public void setConTwo(RobotSensorNode n){this.conditionTwo = n;}

	public int evaluate(Robot robot){
		return conditionOne.evaluate(robot) + conditionTwo.evaluate(robot);
	}

    public String toString() {
		return "add(" + conditionOne.toString() + "+" + conditionTwo.toString() + ")";
	}
}

// Subtraction
class subNode implements RobotSensorNode{
	RobotSensorNode conditionOne;
	RobotSensorNode conditionTwo;

	public subNode(RobotSensorNode one, RobotSensorNode two){
		this.conditionOne = one;
		this.conditionTwo = two; 
	}

	public void setConOne(RobotSensorNode n){this.conditionOne = n;}
	public void setConTwo(RobotSensorNode n){this.conditionTwo = n;}

	public int evaluate(Robot robot){
		return conditionOne.evaluate(robot) - conditionTwo.evaluate(robot);
	}
	
    public String toString() {
		return "sub(" + conditionOne.toString() + "-" + conditionTwo.toString() + ")";
	}
}

// multiplication
class mulNode implements RobotSensorNode{
	RobotSensorNode conditionOne;
	RobotSensorNode conditionTwo;

	public mulNode(RobotSensorNode one, RobotSensorNode two){
		this.conditionOne = one;
		this.conditionTwo = two; 
	}

	public void setConOne(RobotSensorNode n){this.conditionOne = n;}
	public void setConTwo(RobotSensorNode n){this.conditionTwo = n;}

	public int evaluate(Robot robot){
		return conditionOne.evaluate(robot) * conditionTwo.evaluate(robot);
	}
	
    public String toString() {
		return "mul(" + conditionOne.toString() + "*" + conditionTwo.toString() + ")";
	}
}

// division
class divNode implements RobotSensorNode{
	RobotSensorNode conditionOne;
	RobotSensorNode conditionTwo;

	public divNode(RobotSensorNode one, RobotSensorNode two){
		this.conditionOne = one;
		this.conditionTwo = two; 
	}

	public void setConOne(RobotSensorNode n){this.conditionOne = n;}
	public void setConTwo(RobotSensorNode n){this.conditionTwo = n;}

	public int evaluate(Robot robot){
		return conditionOne.evaluate(robot) / conditionTwo.evaluate(robot);
	}
	
    public String toString() {
		return "div(" + conditionOne.toString() + "/" + conditionTwo.toString() + ")";
	}
}

// --  CONDITIONAL OPPERATIONS  --------------------------------------------------------------------------------------------------------------------------------
// and
class andNode implements RobotConditionNode{
	RobotConditionNode conditionOne;
	RobotConditionNode conditionTwo; 
	public andNode(RobotConditionNode one, RobotConditionNode two){
		this.conditionOne = one;
		this.conditionTwo = two; 
	}

	public boolean evaluate(Robot robot){
		return conditionOne.evaluate(robot) && conditionTwo.evaluate(robot);
	}
}

// or
class orNode implements RobotConditionNode{
	RobotConditionNode conditionOne;
	RobotConditionNode conditionTwo; 
	public orNode(RobotConditionNode one, RobotConditionNode two){
		this.conditionOne = one;
		this.conditionTwo = two; 
	}

	public boolean evaluate(Robot robot){
		return conditionOne.evaluate(robot) || conditionTwo.evaluate(robot);
	}
}

// not
class notNode implements RobotConditionNode{
	RobotConditionNode condition;
	public notNode(RobotConditionNode c){
		this.condition = c;
	}

	public boolean evaluate(Robot robot){
		return !condition.evaluate(robot);
	}
}