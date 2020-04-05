import java.util.*;

public class Compiler {
    public static void main(String[] args) {
        Parser c = new Parser();
        boolean tokenBool = false;
		boolean parseTreeBool = false;
        boolean symbolTableBool = false;
		boolean fileSet = false;
		String fileName = null;

        for(String s : args){
			if(s.equals("-t")){
				tokenBool = true;
                System.out.println("-t");
			} else if (s.equals("-pt")){
				parseTreeBool = true;
			} else if (s.equals("-s")) {
                symbolTableBool = true;
            } else if (!fileSet){
				fileName = s;
				fileSet = true;
			} else {
				System.out.println("Error: More than one file passed as argument.");
			}
		}
        Node root = c.run(tokenBool, parseTreeBool, fileSet, fileName);
        if(symbolTableBool) {
            SymbolTable symRoot;
            symRoot = SymbolTable.createSymbolTable(root);
            SymbolTable.printSymbolTable(symRoot);
        }
	Intermediate n = new Intermediate();
	n.run(root);
    }
}
