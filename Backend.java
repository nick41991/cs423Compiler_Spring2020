// This class converts an IR into ASM
// This will be primarily done by reading a line of IR and generating appropriate ASM for that line
import java.util.ArrayList;
import java.util.Stack;
import java.util.regex.*;


public class Backend {
	public ArrayList<String> output;
	public IntRep ir;

	private SymbolTable st; // Store main symbol table
	private SymbolTable context; // Working table
	private ArrayList<String> functions; //Functions written
	private Stack<String> labelStack; //Used for labels auto generated by compiler
	private MemoryManager memory;

	private boolean mainSet;

	private int jumpLabel;


	public Backend(IntRep irep, SymbolTable sym){
		output = new ArrayList<String>();
		ir = irep;
		st = sym;
		jumpLabel = 0;
		memory = new MemoryManager();
	}

	public void print(){
		for(String s: output){
			System.out.println(s);
		}
	}

	public void run(){
		init();
		state_switch(0);
		print();
	}

	/*Write the three necessary data sections to output*/
	public void init(){
		output.add(".section .data");
		output.add(".section .bss");
		output.add(".section .text");
	}

	/**
	Cases to handle:		id ir	write asm`
		Function declaration	+	-
		LabeledStatement	+	-
		SelectionStatement	+	-
		IterationStatement	+	-
		JumpStatement     	+	-
		returns			+	-
		ExpressionStatement	+	- //Fallthrough case
	*/

	//Main control function, controls state of backend
	public int state_switch(int i){
		String s;
		for(; i < ir.rep.size(); i++){
			s = ir.rep.get(i);
			//Function Declaration
			if(Pattern.matches("[a-z][a-zA-Z_0-9]*[(][)][{]", s)){
				i = functionHeader(s, i);
			} else if (Pattern.matches("[A-Z][a-zA-Z_0-9]*[:]", s)){
				i = label(s,i);
			} else if (Pattern.matches("jmp [a-zA-Z][a-zA-Z_0-9]*", s)){
				i = jump(s, i);
			} else if (Pattern.matches("while [a-zA-Z][a-zA-Z_0-9]* [{]", s)){
				i = iterator(s, i);
			} else if (Pattern.matches("if [a-zA-Z][a-zA-Z_0-9]* [{]", s)){
				i = selectionIf(s, i);
			} else if (Pattern.matches("[}] else if [a-zA-Z][a-zA-Z_0-9]* [{]", s)){
				i = selectionElseIf(s, i);
			} else if (Pattern.matches("[}] else [{]", s)){
				i = selectionElse(s, i);
			} else if (Pattern.matches("return [a-zA-Z][a-zA-Z_0-9]*", s)){
				i = returns(s, i);

			} else if (Pattern.matches("[}]", s)){
				//End of ifs (w/out else), elses, whiles, and functions
				//Signals end of block
				return i;
			} else {
				//Assume expression
				expression(s, i);
			}

			//Regex Test Print
			//System.out.println(s + " " + Pattern.matches("[}] else if [a-zA-Z][a-zA-Z_0-9]* [{]", s));
		}
		return 0;
	}

	private int functionHeader(String s, int i){
		// write function head. Allocate space for variables on stack via symbol table.
		// call state_switch() to write function until "}" forces return

		//Get function name-- S format: "name(){"
		String name = "";
		for(int j = 0; j < s.length(); j++){
			if(s.charAt(j) == '('){
				name = s.substring(0, j);
				break;
			}
		}
		if(name.equals("")){ //Name resolution error should not happen without a significant bug
			System.out.println("Error: Function was identified but name could not be resolved: " + s);
			return i;
		}
		//Set entry point, using _start for main makes life easier

		output.add(".globl " + name);
		output.add(name + ":");

		/*Initialize stack for function*/
		output.add("pushq %rbp");
		output.add("movq %rsp, %rbp");

		//Use this table to allocate space on stack for local variables
		SymbolTable namespace = st.getTable(name);

		//Default return
		return state_switch(i + 1);


	}

	private int label(String s, int i){
		//A user defined label in the code

		//Default return
		return i;
	}

	private int jump(String s, int i){
		//Unconditional YEET to a label

		//Default return
		return i;
	}

	private int iterator(String s, int i){
		//For s == "while cond {"

		/* code to be written
			test cond
			jump to x if false		//Push x onto label stack
			label y:			//y needs to be stored localy though until we return from state_switch()
				looped code (state_switch() to write)
				Condition recalculation is included in looped code via IR
 			test cond
			jump to y if true		//write jump using stored label y
			label x:			//pop x from label stack
				continuation of code outside this structure
		*/

		//Default return
		return i;

	}

	private int selectionIf(String s, int i){ //if else
		//FOR: s == "if cond {"

		// For asm: test ph
		// jump to label x if test fails //label x is the next test for chained statements
		// code for if test doesn't fail, written by call to state_switch()
		// at the bottom of this code jump (unconditionally) to label y, the end of the if-else chain
		// place label x below the unconditional jump
		// x and y may point to the same instruction

		/*
			test cond
			jump if false to label x		push z to label stack then push x
				code for if true		written by call to state_switch()
				jump (unconditionally) to z
			label x:
			//if else will function just like a basic if.
			//x is popped before calling selectionIf() again
			test cond
			jump if false to label x		push z to label stack then push x
				code for if true
				jump (unconditionally) to z
			label x:

			label z:
				continuation of code outside this structure
		*/

		//Default return
		return i;

	}

	private int selectionElseIf(String s, int i){ //if else
		//FOR: s == "if cond {"
		//Should also work for s = "} else if cond {"

		/*Pop label x from previous if*/
		//popLabel();

		/*
			//if else will function just like a basic if except z will not be pushed to stack,
			// since it is already there.
			label x:
			test cond
			jump if false to label y		push label y to stack
				code for if true
				jump (unconditionally) to z
			label y:

			label z:
				continuation of code outside this structure
		*/

		//Default return
		return i;

	}

	private int selectionElse(String s, int i){
		//FOR: s == "else {"

		//popLabel(); //Place label from prior if (x if no else-ifs)
		//Write code for else block via state_switch()
		//popLabel(); //Place final label (label z)

		//Default return
		return i;
	}

	private int returns(String s, int i){
		//FOR s == "return exp"

		/*
			Want to check register array and if need be stack for exp Location
			then load exp into %rax
			ret
		*/
		//output.add("movl $1, %eax"); //<= see expression
		//Currently assuming return has been preloaded into %eax. Need code to enforce this.
		output.add("popq %rbp");
		output.add("ret");

		//Default return
		return i;
	}

	private int expression(String s, int i){
		//An expression may be of the 7 following forms:
		// L1 = const
		// L1 = L2 (or variable) < = These should be optimized away ideally
		// L1 = functioncall
		// L1 = functioncallL2L3..Lx
		// L1 = L2 [op] L3
		// L1 = (functioncallL1...Lx) [op] Ly
		// L1 = L2 [op] function

		//Identify pattern of code:
		String[] tokens = s.split("=");
		//tokens[0] is destination of expression
		// Resolve what is in tokens[1]
		if(Pattern.matches("[0-9]*", tokens[1])) { // Constant asssigned to LHS
			ArrayList<String> r = memory.accessReference(tokens[0], "main");
			// for(String x: r){	//Debug print
			// 	System.out.println(x);
			// }
			output.add("movl $" + tokens[1] + ", " + r.get(r.size() - 1));
		}
		//break down tokens[1] further to implement
		//Default return
		return i;
	}

	/*Remove label from label stack and write label to output*/
	private void popLabel(){
		String label = labelStack.pop();
		output.add(label + ":");
	}

}
