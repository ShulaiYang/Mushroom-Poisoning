import java.io.File;
import java.io.IOException;
import java.util.Scanner;

// object class representing a single data item
class Mushroom {
	public String classification;	// "poisonous" or "edible"
	public String[] values;	// attribute vector
	
	public Mushroom(String c, String[] v) {
		classification = c;
		values = v;
	}
}

// node within the DecisionTree
class Node {
	public Node[] children;	// depending on the attribute, a variable number of children
	// note that children will be assumed to be attached in the same order as the values
	// are listed in the "values" array of the DecisionTree class
	public int index;	// index of the attribute associated with this node if it is internal
	public String classification;	// classification to return if this node is a leaf
	
	public Node() {
		children = null;
		index = -1;
		classification = null;
	}
	
	public Node(int num, int i) {
		children = new Node[num];
		index = i;
		classification = null;
	}
	
	public Node(String clss){
		children = null;
		index = -1;
		classification = clss;
	}
}

// class implementing a decision tree
class DecisionTree {
	public Node root;	// link to the root node of the tree
	public int numAttributes;	// number of attributes tree can be built over
	public String[][] values;	// set of possible values for all attributes

	public DecisionTree(String[][] v) {
		values = v;
		numAttributes = values.length;
		root = null;
	}

	// recursive method that, given a list of data to construct a decision tree
	// over an a list indicating which attributes have and have not been used
	// yet, selects out the node with the lowest weighted average impurity to
	// be the root and recurses to generate valid decision trees for the
	// children - returns the entire decision tree
	public Node recBuild(MushList list, boolean[] usedAttribute) {
		// checks to see if should stop recursing
		// if all attributes have been used already, return a node
		// classifying based on the consensus of the list
		boolean allUsed = true;
		for (int i=0; i<usedAttribute.length; i++){
			allUsed = allUsed && usedAttribute[i];
		}
		if (allUsed) return new Node(list.consensus());
		
		// if current list has all items with the same classification, return
		//  a node with no index but the appropriate classification indicated
		if (list.allPoison()) return new Node("poisonous");
		if (list.allEdible()) return new Node("edible");

		// calculate the weighted average impurity for a root node with each
		// unused attribute as a possible attribute
		double[] wimpurities = new double[usedAttribute.length];
		int listsize = list.getNum();
		
		for (int i=0; i<wimpurities.length; i++) {
			// i is the index of the current attribute considering
			// only proceed if that attribute has not been used yet
			if (!usedAttribute[i]) {
				wimpurities[i] = 0;
				// look up how many values the attribute at that index has
				int numValues = values[i].length;

				for (int j=0; j<numValues; j++) {
					// number of items in list with value
					double numValue = list.getValueCount(i, values[i][j]);
					// number of items in list with value that are poisonous
					double numPoison = list.getClassCount(i, values[i][j], "poisonous");
					// number of items in list with value that are edible
					double numEdible = list.getClassCount(i, values[i][j], "edible");
					double term = (numValue/listsize)*(1-(numEdible/numValue)-(numPoison/numValue));
					wimpurities[i] += term;
				}
			}
		}
		
		//wimpurities stores the weighted average impurity for each attribute over list
		// find the lowest one, make the associated attribute the node to return
		double lowest = Integer.MAX_VALUE;
		int lowAttribute = -1;
		for (int i=0; i<numAttributes; i++){
			if (!usedAttribute[i] && wimpurities[i]<lowest){
				lowest = wimpurities[i];
				lowAttribute = i;
			}
		}

		// if there is no such attribute, simply return a node that classifies
		// based on the consensus of the list
		if (lowAttribute == -1) return new Node(list.consensus());
		
		// an attribute has been selected for the root of the decision tree
		Node newNode = new Node(values[lowAttribute].length, lowAttribute);
		
		// recurse on each child with the appropriate sublist
		usedAttribute[lowAttribute] = true;
		for (int i=0; i<values[lowAttribute].length; i++){
			MushList sublist = list.getSublist(lowAttribute, values[lowAttribute][i]);
			newNode.children[i] = recBuild(sublist, usedAttribute);
		}
		
		return newNode;
	}

	// non-recursive method called to initiate construction of a decision tree
	// initialize all attributes as unused
	public void build(MushList list) {
		boolean[] usedAttribute = new boolean[numAttributes];
		for (int i=0; i<numAttributes; i++) usedAttribute[i] = false;
		root = recBuild(list, usedAttribute);
	}
}

// Link within a linked list of Mushrooms used as data to process
class Link {
	public Mushroom data;
	public Link next;

	public Link(Mushroom d) {
		data = d;
		next = null;
	}
}

// singly linked list of Mushroom objects, with support methods provided for
// decision tree construction
class MushList {
	private Link first;
	private int numItems;

	public MushList() {
		first = null;
		numItems = 0;
	}
	
	public int getNum() {return numItems;}

	// all new Mushrooms inserted at front of list
	public void insert(Mushroom d) {
		Link newLink = new Link(d);
		newLink.next = first;       
		first = newLink;
		numItems++;
   }

	// return true if all Mushrooms within this list are classified as poisonous
   public boolean allPoison(){
	   boolean all = true;
	   Link curr = first;
	   while (curr != null) {
		   all = all && curr.data.classification.equalsIgnoreCase("poisonous");
		   curr = curr.next;
	   }
	   return all;
   }

   // return true if all Mushrooms within this list are classified as edible
   public boolean allEdible(){
	   boolean all = true;
	   Link curr = first;
	   while (curr != null) {
		   all = all && curr.data.classification.equalsIgnoreCase("edible");
		   curr = curr.next;
	   }
	   return all;
   }
   
   // return "poisonous" or "edible" indicating which classification the majority
   // of the Mushrooms in this last have
   public String consensus() {
	   int poison = 0;
	   int edible = 0;
	   Link curr = first;
	   while (curr != null) {
		   if (curr.data.classification.equalsIgnoreCase("edible")) edible++;
		   else poison++;
		   curr = curr.next;
	   }
	   if (poison >= edible) return "poisonous";
	   else return "edible";
   }

   // given an attribute, a particular value for that attribute, and either
   // "poisonous" or "edible", return the number of items in this list that
   // have both the given value and the designated classification
   public int getClassCount(int attribute, String value, String clss){
	   int count = 0;
	   Link curr = first;
	   while (curr != null) {
		   if (curr.data.values[attribute].equalsIgnoreCase(value) &&
				   curr.data.classification.equalsIgnoreCase(clss)) count++;
		   curr = curr.next;
	   }
	   return count;	   
   }

   // given an attribute and a particular value for that attribute, return the
   // number of items in this list that have the given value for that
   // attribute
   public int getValueCount(int attribute, String value) {
	   int count = 0;
	   Link curr = first;
	   while (curr != null) {
		   if (curr.data.values[attribute].equalsIgnoreCase(value)) count++;
		   curr = curr.next;
	   }
	   return count;
   }
   
   // given an attribute and a particular value for that attribute, return the
   // sublist of this list which has only the Mushroom objects in it with
   // the given value for the attribute
   public MushList getSublist(int attribute, String value){
	   MushList newList = new MushList();
	   Link curr = first;
	   while (curr != null) {
		   if (curr.data.values[attribute].equalsIgnoreCase(value)) {
			   newList.insert(curr.data);
		   }
		   curr = curr.next;
	   }
	   return newList;
   }
}

public class MushroomDT {
	public static void main (String[] args) throws IOException {
		int numAttributes = 22;	// number of attributes
		String[] labels; 	// label associated with each attribute
		String[][] values = {{"bell","conical","convex","flat","knobbed","sunken"},
				{"fibrous","grooves","scaly","smooth"},
				{"brown","buff","cinnamon","gray","green","pink","purple","red","white","yellow"},
				{"bruises","no"},
				{"almond","anise","creosote","fishy","foul","musty","none","pungent","spicy"},
				{"attached","descending","free","notched"},
				{"close","crowded","distant"},
				{"broad","narrow"},
				{"black","brown","buff","chocolate","gray","green","orange","pink","purple","red","white","yellow"},
				{"enlarging","tapering"},
				{"bulbous","club","cup","equal","rhizomorphs","rooted","?"},
				{"fibrous","scaly","silky","smooth"},
				{"fibrous","scaly","silky","smooth"},
				{"brown","buff","cinnamon","gray","orange","pink","red","white","yellow"},
				{"brown","buff","cinnamon","gray","orange","pink","red","white","yellow"},
				{"partial","universal"},
				{"brown","orange","white","yellow"},
				{"none","one","two"},
				{"cobwebby","evanescent","flaring","large","none","pendant","sheathing","zone"},
				{"black","brown","buff","chocolate","green","orange","purple","white","yellow"},
				{"abundant","clustered","numerous","scattered","several","solitary"},
				{"grasses","leaves","meadows","paths","urban","waste","woods"}};

		Scanner fileScan = new Scanner(new File("mushrooms.csv"));
		String header = fileScan.nextLine();
		// read in the label associated with each attribute from the first line
		// of the csv file
		Scanner labelScan = new Scanner(header);
		labelScan.useDelimiter(",");
		labelScan.next();
		labels = new String[numAttributes];
		for (int i=0; i<numAttributes; i++){
			labels[i] = labelScan.next();
		}

		// a linked list for storing the data over which a decision tree will be built
		MushList training = new MushList();

		// read in each line of the csv file, create a Mushroom object storing
		// the data from that line, and then add the new Mushroom to the
		// training data list
		while (fileScan.hasNext()){
			String rowData = fileScan.nextLine();
			Scanner mushScan = new Scanner(rowData);
			mushScan.useDelimiter(",");

			String classification;
			String[] data = new String[numAttributes];
			
			classification = mushScan.next();
			for (int i=0; i<numAttributes; i++) {
				data[i] = mushScan.next();
			}						
			Mushroom newMush = new Mushroom(classification, data);
			training.insert(newMush);
			mushScan.close();
		}
		fileScan.close();
		labelScan.close();

		System.out.println("Data read in...");
		
		// build a decision tree that classifies Mushroom objects as
		// "poisonous" or "edible" given the data in the training list
		DecisionTree tree = new DecisionTree(values);
		tree.build(training);
		
		System.out.println("Tree built...");
		
	}
}