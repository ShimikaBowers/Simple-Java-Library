package simple.util.regex;

import java.util.regex.Pattern;

/**
 * Provides a simpler to use Pattern.<br>
 * Main difference: a pattern of "*" will now match what ".*" can match.
 * <br>Created: 2004
 * @author KP
 * @deprecated 2015-1-1
 */
@Deprecated
public class SimplePattern {
	private final static char NULL = (char)0;
	public static Pattern compile(String regex) throws Exception {
		return Pattern.compile(parse(regex));
	}
	public static boolean matches(String regex, String test) throws Exception {
		return Pattern.matches(parse(regex), test);
	}
	private static String parse(String regex) throws Exception {
		StringBuilder buf = new StringBuilder(regex.length()+15);
		char[] ca = regex.toCharArray();
		char c = NULL;
		char prev = NULL;
		char next = NULL;
		int depth = 0;
		for (int i =0;i<ca.length;i++) {
			c = ca[i];
			next = i==ca.length-1?NULL:ca[i+1];
			switch(c) {
			case '[':
				buf.append(c);
				depth++;
				break;
			case ']':
				buf.append(c);
				depth--;
				break;
			case '*':
				if (prev!=']') {
					buf.append('.');
				}
				buf.append(c);
				break;
			case '\\':
				buf.append(c);
				buf.append(next);
				i++;
				break;
			case '.':
				buf.append('\\');
				//buf.append('\\');
				buf.append(c);
				break;
			case '?':
				buf.append('\\');
				//buf.append('\\');
				buf.append(c);
				break;
			default:
				buf.append(c);
			}

			prev = ca[i];
		}
		if(depth!=0)throw new Exception("Brackets don't match.");
		return buf.toString();
	}
}
