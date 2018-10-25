package tree;

/**
 * Created by chowc on 2018/10/18.
 */
public class Tree<T> {
	T data;
	Tree<T> left;
	Tree<T> right;
	Tree(T data) {this.data=data;}
}