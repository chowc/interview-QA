package bignum;

/**
 * Created by chowc on 2018/10/24.
 * 实现整数加法
 */
public class NumAdder {

	static String add(String num1, String num2) {
		if (!isNum(num1)) {
			System.out.println(String.format("illegal input num: %s", num1));
		}
		if (!isNum(num2)) {
			System.out.println(String.format("illegal input num: %s", num2));
		}
		short[] nums1 = str2Array(num1);
		short[] nums2 = str2Array(num2);

	}

	static short[] add(short[] nums1, short[] nums2) {
		int small = nums1.length > nums2.length ? nums2.length : nums1.length;
		int big = nums1.length < nums2.length ? nums2.length : nums1.length;
		int overflow = 0;
		short[] result = new short[big+1];
		int i=1;
		int index1 = nums1.length-i;
		int index2 = nums2.length-i;
		int index = result.length-i;
		for (; i<=small; i++) {
			if (overflow+nums1[index1]+nums2[index2]>9) {
				result[index] = (short) (overflow+nums1[index1]+nums2[index2]-10);
				overflow = 1;
			} else {
				result[index] = (short) (overflow+nums1[index1]+nums2[index2]);
				overflow = 0;
			}
		}
		while (i++ < nums1.length) {
			result[index] = nums1[index1];
		}
		while (i++ < nums2.length) {
			result[index] = nums2[index2];
		}
		result[0] = (short) overflow;
		return result;
	}

	static boolean isNum(String num) {
		if (num==null || num.length()==0) {
			return false;
		}
		char[] ca = num.toCharArray();
		for (char c : ca) {
			if (c<'0' || c>'9') {
				return false;
			}
		}
		return true;
	}

	// 123   -> [1][2][3]
	// 12345 -> [1][2][3][4][5]
	static short[] str2Array(String num) {
		char[] ca = num.toCharArray();
		short[] nums = new short[ca.length];
		for (int i=0; i<nums.length; i++) {
			nums[i] = (short) (ca[i]-'0');
		}
		return nums;
	}
	public static void main(String[] args) {

	}
}