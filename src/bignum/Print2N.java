package bignum;

/**
 * Created by chowc on 2018/10/25.
 */
public class Print2N {
	/**
	 * 空间复杂度 O(n)，时间复杂度
	 * @param n
	 */
	static void print2N(int n) {
		if (n <= 0) {
			System.out.println(String.format("illegal input: %s", n));
			return;
		}
		System.out.println(String.format("start printing number to %s size", n));
		int[] nums = new int[n];
		while (!addOne(nums)) {
			printNum(nums);
		}
	}

	static void printNum(int[] nums) {
		boolean first = true;
		for (int i=0; i<nums.length; i++) {
			if (first && nums[i]==0) {
				continue;
			}
			System.out.print(nums[i]);
			first = false;
		}
		System.out.println();
	}

	static boolean addOne(int[] nums) {
		for (int i=nums.length-1; i>=0; i--) {
			if (i==0 && nums[0]==9){
				return true;
			}
			if (nums[i]==9) {
				nums[i]=0;
				continue;
			}else {
				nums[i] += 1;
				break;
			}
		}
		return false;
	}
	public static void main(String[] args) {
		print2N(0);
		print2N(1);
		print2N(4);
		print2N(30);
		print2N(-1);
	}
}
