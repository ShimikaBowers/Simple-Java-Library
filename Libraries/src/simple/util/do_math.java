package simple.util;

/**
 * <br>Created: 2006
 * @author Kenneth Pierce
 */
public final class do_math {
	public static long sum(int[] x) {
		long sum = 0;
		for(int i = 0;i<x.length;i++) {
			sum += x[i];
		}
		return sum;
	}
	public static long sum(short[] x) {
		long sum = 0;
		for(int i = 0;i<x.length;i++) {
			sum += x[i];
		}
		return sum;
	}
	public static long sum(char[] x) {
		long sum = 0;
		for(int i = 0;i<x.length;i++) {
			sum += x[i];
		}
		return sum;
	}
	public static double sum(float[] x) {
		double sum = 0;
		for(int i = 0;i<x.length;i++) {
			sum += x[i];
		}
		return sum;
	}
	public static double sum(double[] x) {
		double sum = 0;
		for(int i = 0;i<x.length;i++) {
			sum += x[i];
		}
		return sum;
	}
	public static long sum(long[] x) {
		long sum = 0;
		for(int i = 0;i<x.length;i++) {
			sum += x[i];
		}
		return sum;
	}
	public static long factorial(long n) {
		long ans = 1;
		for(; n>1 ; n--)
			ans *= n;
		return ans;
	}
	private do_math(){}
}
