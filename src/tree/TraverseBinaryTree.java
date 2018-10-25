package tree;

import java.util.HashMap;
import java.util.Stack;

/**
 * Created by chowc on 2018/10/16.
 */
public class TraverseBinaryTree {

	static Tree initTree() {
		// 前序：A	B	D	E	C	F	G
		// 中序：D	B	E	A	F	C	G
		// 后序：D	E	B	F	G	C	A
		Tree head = new Tree("A");
		head.left = new Tree("B");
		head.right = new Tree("C");
		head.left.left = new Tree("D");
		head.left.right = new Tree("E");
		head.right.left = new Tree("F");
		head.right.right = new Tree("G");
		return head;
	}
	static Tree allLeftTree() {
		// 前序： A	B	C	D	E	F
		// 中序： F	E	D	C	B	A
		// 后序： F	E	D	C	B	A
		Tree head = new Tree("A");
		head.left = new Tree("B");
		head.left.left = new Tree("C");
		head.left.left.left = new Tree("D");
		head.left.left.left.left = new Tree("E");
		head.left.left.left.left.left = new Tree("F");
		return head;
	}
	static Tree allRightTree() {
		// 前序： A	B	C	D	E	F
		// 中序： A	B	C	D	E	F
		// 后序： F	E	D	C	B	A
		Tree head = new Tree("A");
		head.right = new Tree("B");
		head.right.right = new Tree("C");
		head.right.right.right = new Tree("D");
		head.right.right.right.right = new Tree("E");
		head.right.right.right.right.right = new Tree("F");
		return head;
	}
	static Tree strangeTree() {
		// 前序：A	B	C	D	E	F	G
		// 中序：B	D	C	E	F	G	A
		// 后序：D	G	F	E	C	B	A
		Tree head = new Tree("A");
		head.left = new Tree("B");
		head.left.right = new Tree("C");
		head.left.right.left = new Tree("D");
		head.left.right.right = new Tree("E");
		head.left.right.right.right = new Tree("F");
		head.left.right.right.right.right = new Tree("G");
		return head;
	}
	static void print(Tree tree) {
		System.out.print(tree.data + "\t");
	}
	public static void main(String[] args) {
		System.out.println("preOrderTranverse");
		preOrderTranverse(null);
		System.out.println();
		preOrderTranverse(initTree());
		System.out.println();
		preOrderTranverse(allLeftTree());
		System.out.println();
		preOrderTranverse(allRightTree());
		System.out.println();
		preOrderTranverse(strangeTree());
		System.out.println("\nmidOrderTranverse");
		midOrderTranverse(null);
		System.out.println();
		midOrderTranverse(initTree());
		System.out.println();
		midOrderTranverse(allLeftTree());
		System.out.println();
		midOrderTranverse(allRightTree());
		System.out.println();
		midOrderTranverse(strangeTree());
		System.out.println("\npostOrderTranverse();");
		midOrderTranverse(null);
		System.out.println();
		postOrderTranverse(initTree());
		System.out.println();
		postOrderTranverse(allLeftTree());
		System.out.println();
		postOrderTranverse(allRightTree());
		System.out.println();
		postOrderTranverse(strangeTree());
		System.out.println("\npostOrderTranverseVersion2();");
		postOrderTranverseVersion2(null);
		System.out.println();
		postOrderTranverseVersion2(initTree());
		System.out.println();
		postOrderTranverseVersion2(allLeftTree());
		System.out.println();
		postOrderTranverseVersion2(allRightTree());
		System.out.println();
		postOrderTranverseVersion2(strangeTree());

	}


	static void preOrderTranverse(Tree tree) {

		Stack<Tree> stack = new Stack<>();
		Tree now = tree;
		while (now != null || !stack.isEmpty()) {
			if (now != null) {
				print(now);
				stack.push(now);
				now = now.left;
			} else {
				now = stack.pop().right;
			}

		}
	}

	static void midOrderTranverse(Tree tree) {
		Stack<Tree> stack = new Stack<>();
		Tree now = tree;
		while (now != null || !stack.isEmpty()) {
			if (now != null) {
				stack.push(now);
				now = now.left;
			} else {
				now = stack.pop();
				print(now);
				now = now.right;
			}
		}
	}

	static void postOrderTranverse(Tree tree) {
		Stack<Tree> stack = new Stack<>();
		Tree now = tree;
		HashMap<Tree, Integer> accessRecord = new HashMap<>();
		while (now != null || !stack.isEmpty()) {
			if (now != null) {
				stack.push(now);
				now = now.left;
			} else {
				now = stack.pop();
				if (!accessRecord.containsKey(now)) {
					accessRecord.put(now, 1);
					stack.push(now);
					now = now.right;
				} else {
					accessRecord.remove(now);
					print(now);
					// 需要将出栈的节点置为空，防止重复进栈
					now = null;
				}
			}

		}
	}

	// 更容易理解
	static void postOrderTranverseVersion2(Tree tree) {
		if (tree == null) {
			return;
		}
		Stack<Tree> stack = new Stack<>();
		Tree pre = null;
		stack.push(tree);
		while (!stack.isEmpty()) {
			Tree now = stack.peek();
			// 没有左右子节点，或者上一个出栈的是它的左右子节点 => 出栈
			// 为什么不是判断上一个出栈的是它的右子节点？ 因为根据进栈顺序，一个节点的左右子节点(如果有的话)，
			// 在栈的位置一定是在它自己的上面的(即子节点比父节点进栈晚)。
			if ((now.left==null && now.right==null) ||
					(pre!=null && (pre==now.left || pre==now.right))){
				pre = now;
				print(now);
				stack.pop();
			} else {
				// 进栈顺序：先右再左。
				if (now.right != null) {
					stack.push(now.right);
				}
				if (now.left != null) {
					stack.push(now.left);
				}
			}
		}
	}
}



