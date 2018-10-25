package tree;


import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by chowc on 2018/10/25.
 *
 * 《剑指 offer》 第 题
 */
public class RebuildBinaryTree {
	static <T> Tree rebuild(T[] preArray, T[] midArray) {
		if (preArray == null || midArray == null) {
			throw new IllegalArgumentException("null array");
		}
		if (preArray.length != midArray.length) {
			throw new IllegalArgumentException("illegal input");
		}
		if (preArray.length == 0) {
			return null;
		}
		T headData = preArray[0];
		Tree<T> head = new Tree(headData);
		if (preArray.length == 1) {
			return head;
		}

		int headIndex = locateTarget(midArray, headData);
		T[] midLeftArray = Arrays.copyOfRange(midArray, 0, headIndex);
		T[] midRightArray = Arrays.copyOfRange(midArray, headIndex+1, midArray.length);
		T[] preLeftArray = Arrays.copyOfRange(preArray, 1, midLeftArray.length+1);
		T[] preRightArray = Arrays.copyOfRange(preArray, midLeftArray.length+1, preArray.length);
		head.left = rebuild(preLeftArray, midLeftArray);
		head.right = rebuild(preRightArray, midRightArray);
		return head;
	}

	static <T> int locateTarget(T[] array, T target) {
		// target null
		for (int i = 0; i < array.length; i++) {
			if (target.equals(array[i])) {
				return i;
			}
		}
		throw new IllegalArgumentException("illegal input");
	}

	static void printLevel(int level) {
		int count = 6 - level;
		int i = 0;
		while (i++ < count) {
			System.out.print("\t");
		}

	}
	static <T> void printTree(Tree<T> tree, int level) {
		// bignum tree
		if (tree != null) {
			printLevel(level);
			if (tree.left != null) {
				System.out.print(tree.left.data+"\t");
			}
			if (tree.right != null) {
				System.out.print(tree.right.data+"\t");
			}
			System.out.println();
			printTree(tree.left, level+1);
			printTree(tree.right, level+1);
		}

	}
	public static void main(String[] args) {
		String[] pre = {"1", "2", "4", "7", "3", "5", "6", "8"};
		String[] mid = {"4", "7", "2", "1", "5", "3", "8", "6"};
		Tree<String> tree = rebuild(pre, mid);
		List<String> list = new LinkedList<>();
		printTree(tree, 0);
	}
}
