package simple.io;

import java.io.File;

import simple.util.HexUtil;
import simple.util.do_str;

/**Depends on simple.io.ParseException, simple.util.do_str
 * Rename Tokens:
 * <dl>
 * <dt>$N</dt>
 * <dd>File name without extension</dd>
 * <dt>$N(n)</dt>
 * <dd>Characters starting from n of the file name without extension. (inclusive)
 * You can use negative numbers to start from the end of the filename.
 * E.g. 'hello.txt' with  $N(-3) will give 'llo'.</dd>
 * <dt>$N(n,m)</dt>
 * <dd>Characters n to m of the file name without extension. (inclusive, exclusive)
 * You can use negative numbers to start from the end of the filename.
 * E.g. 'hello.txt' with  $N(-2,-1) will give 'l'.</dd>
 * <dt>$F</dt>
 * <dd>File name with extension.</dd>
 * <dt>$F(n,m)</dt>
 * <dd>Characters n to m of the file name with extension. (inclusive, exclusive)
 * You can use negative numbers to start from the end of the filename.
 * E.g. 'hello.txt' with  $F(-2,-1) will give 'x'; $F(0,-3) yields 'hello.'</dd>
 * <dt>$F(n)</dt>
 * <dd>Characters starting from n of the file name with extension. (inclusive)
 * You can use negative numbers to start from the end of the filename.
 * E.g. 'hello.txt' with  $F(-3) will give 'txt'.</dd>
 * <dt>$E</dt>
 * <dd>Extension of file with '.'
 * E.g. 'hello.txt.log' with $E will give '.log'</dd>
 * <dt>$D</dt>
 * <dd>Path without the ending '\'</dd>
 * <dt>$D(n)</dt>
 * <dd>Part n of the directory name from the lowest level.
 * E.g. $D(1) of "C:\program files\games\doom.exe" = "games"</dd>
 * <dt>$M</dt>
 * <dd>Moves the file up one directory. Equivalent to $M(1).</dd>
 * <dt>$M(n)</dt>
 * <dd>Moves the file up the directory tree n times.</dd>
 * <dt>$#</dt>
 * <dd>Number starting from 0. Additional '#' can be added to pad with '0's.
 * (e.g. $## would result in a scheme like 00, 01, 02, etc.)</dd>
 * <dt>$P</dt>
 * <dd>The path with out the drive letter.(includes start and end slash)</dd>
 * </dl>
 * <br>Created: Jun 24, 2008
 * @author Kenneth Pierce
 */
public final class RenameFormat {
	public static final int
		OK = 0,
		BADTARGET = 1,
		BADSOURCE = 2,
		CANTUNDO = 3,
		PARSEEXCEPTION = 4,
		IOERRORMKDIR = 5,
		IOERRORRENAME = 6;
	//Error messages
	private static final String[] ERR = new String[] {"OK", "Destination already exist", "Source doesn't exist", "Can't Undo: Hasn't been renamed.", "Parse Exception: ", "System error making dirs", "System error renaming"};
	public static boolean fRESOLVEURLESCAPED=false;
	/**
	 * URI safe filename
	 */
	public static boolean fURISAFEF=false;
	/**
	 * URI safe directory
	 */
	public static boolean fURISAFED=false;
	static private int number = 0;
	private File
		file,
		undo = null;
	private boolean canundo = false;
	private String
		syn,
		destName = null;
	private ParseException pe = null;
	private int errorNum = -1;
	private static final int
		TRANS_PATH_UC=1,
		TRANS_PATH_UCF=2,
		TRANS_PATH_LC=4,
		TRANS_FILE_UC=8,
		TRANS_FILE_UCF=16,
		TRANS_FILE_LC=32;
	private RenameFormat() {}
	public RenameFormat(final File fil, final String syntax) {
		file = fil;
		syn = syntax;
	}
	public static synchronized void setNumber(final int i) {
		RenameFormat.number = i;
	}
	private static synchronized int increment() {
		return RenameFormat.number++;
	}
	public int rename() {
		if (!file.exists()) return (errorNum=RenameFormat.BADSOURCE);
		if (destName==null) {
			try {
				destName = parse();
			} catch (final ParseException e) {
				pe = e;
				return (errorNum=RenameFormat.PARSEEXCEPTION);
			}
		}
		final int tmp = destName.lastIndexOf('\\');
		if (tmp > 0) {
			undo = new File(destName.substring(0, tmp+1));
			undo.mkdirs();
		}
		undo = new File(destName);
		if (undo.exists() && !file.equals(undo)) {return (errorNum=RenameFormat.BADTARGET);}
		if (!file.renameTo(undo)) {return (errorNum=RenameFormat.IOERRORRENAME);}
		canundo = true;
		return (errorNum=RenameFormat.OK);
	}
	public int mockRename() {
		if (!file.exists()) return (errorNum=RenameFormat.BADSOURCE);
		if (destName==null) {
			try {
				destName = parse();
			} catch (final ParseException e) {
				pe = e;
				return (errorNum=RenameFormat.PARSEEXCEPTION);
			}
		}
		undo = new File(destName);
		if (undo.exists() && !file.equals(undo)) {return (errorNum=RenameFormat.BADTARGET);}
		return (errorNum=RenameFormat.OK);
	}
	public String parse() throws ParseException {
		String pathsplit="\\Q"+File.separator+"\\E";
		final String path = file.getAbsolutePath(), fileName,ext;
		String fullName;
		final StringBuffer buf = new StringBuffer(file.getAbsolutePath().length());
		int begin = path.indexOf(File.separatorChar);
		int end = path.lastIndexOf(File.separatorChar);
		final int lastSlash = end < 0?0:end;
		final int firstSlash = begin < 0?0:begin;
		end = path.lastIndexOf('.');
		final int extDot = (end < lastSlash)?-1:end;
		fullName = path.substring(lastSlash+1);
		if(RenameFormat.fRESOLVEURLESCAPED){
			//Resolve escaped URL characters
			final StringBuilder resolved=new StringBuilder(fullName.length());
			int lend=0;
			for(int i=0;i<fullName.length();i++){
				switch(fullName.charAt(i)){
				case '-':
				case '_':
					//dashes and underscores to spaces
					resolved.append(' ');
					break;
				case '%':
					lend=i+3;
					i++;
					if((i+2)>fullName.length()){
						//Last is malformed, skip it
						resolved.append('%');
						continue;
					}
					if(fullName.charAt(lend)=='%')
						while(fullName.charAt(lend+3)=='%'){
							//check for and append back to back encoded characters
							lend+=3;
							if((lend+3)>fullName.length())break;
						}
					//resolve all found
					resolved.append(new String(HexUtil.fromHex(fullName.substring(i,lend).replace("%",""))));
					i=lend-1;
					break;
					default:
						resolved.append(fullName.charAt(i));
				}
			}
			fullName=resolved.toString();
		}
		if (extDot == -1) {
			//no extension
			fileName = fullName;
			ext = "";
		} else {
			fileName = fullName.substring(0, fullName.length()-(path.length()-extDot));
			ext = path.substring(extDot);
		}
		//initialize
		begin = end = 0;
		int i=0,transformations=0;
		//check for transformations
		if(syn.charAt(0)=='&'){
			out:
			do{
				i++;
				switch(syn.charAt(i)){
					case 'D':
						if(do_str.startsWith(syn,"DUCF",i)){
							transformations+=TRANS_PATH_UCF;
							i+=4;
						}else if(do_str.startsWith(syn,"DUC",i)){
							transformations+=TRANS_PATH_UC;
							i+=3;
						}else if(do_str.startsWith(syn,"DLC",i)){
							transformations+=TRANS_PATH_LC;
							i+=3;
						}
					break;
					case 'F':
						if(do_str.startsWith(syn,"FUCF",i)){
							transformations+=TRANS_FILE_UCF;
							i+=4;
						}else if(do_str.startsWith(syn,"FUC",i)){
							transformations+=TRANS_FILE_UC;
							i+=3;
						}else if(do_str.startsWith(syn,"FLC",i)){
							transformations+=TRANS_FILE_LC;
							i+=3;
						}
					break;
					default:
						//no transformations; reset index
						i--;
						break out;
				}
			}while(syn.charAt(i)=='&');
		}
		for (;i<syn.length();i++) {
			// parse tokens
			if (syn.charAt(i)=='$') {
				switch(syn.charAt(++i)) {
				case 'D'://$M$D(1)$N.jpg
				//All or part of the path
					if (syn.charAt(i+1)=='(') {
						begin = i+2;
						end = syn.indexOf(')', i+1);
						if (end==-1)
							throw new ParseException("Missing matching ')' at " + begin);

						if (!do_str.isNaN(syn.substring(begin, end)))
							buf.append(getDir(path, Integer.parseInt(syn.substring(begin, end))));
						else
							throw new ParseException("Expected number in $D(#), got $D(" + syn.substring(begin, end) + ") instead.");
						i = end;
					} else {
						buf.append(path.substring(0,lastSlash));
					}
					break;
				case 'N':
				//fileName
					if ((i+1) < syn.length() && syn.charAt(i+1)=='(') {
						final String[] sTmp = syn.substring(i+2,syn.indexOf(')',i+1)).split(",");
						if (sTmp.length==2) {
							if (!do_str.isNaN(sTmp[0])&&!do_str.isNaN(sTmp[1])) {
								begin = Integer.parseInt(sTmp[0]);
								end = Integer.parseInt(sTmp[1]);

								//Check for negative indices
								if (begin < 0)
									begin = fileName.length() + begin;
								if (end < 0)
									end = fileName.length() + end;

								// Error checking
								if (end < begin)
									throw new ParseException("End index is before begin index.");
								if(end == begin)
									throw new ParseException("Beginning and End cannot be equal.");
								if (end < 0)
									throw new ParseException("End index comes before the start of the filename by "+(0-end)+".");
								if (begin < 0)
									throw new ParseException("Begin index comes before the start of the filename by "+(0-begin)+".");
								if(begin >= fileName.length())
									throw new ParseException("Begin index exceeds the length of the file name by "+(begin-fileName.length())+".");

								if(end >= fileName.length())
									// be lenient with this error
									buf.append(fileName.substring(begin));
								else
									buf.append(fileName.substring(begin, end));
							}
						} else {
							if (!do_str.isNaN(sTmp[0])) {
								begin = Integer.parseInt(sTmp[0]);

								// Correct for a negative index
								if (begin < 0)
									begin = fileName.length() - 1 + begin;

								// Error checks
								if (begin < 0)
									throw new ParseException("Begin index comes before the start of the filename by "+(0-begin)+".");
								if(begin >= fileName.length())
									throw new ParseException("Begin index exceeds the length of the file name by "+(begin-fileName.length())+".");

								buf.append(fileName.substring(begin));
							}
						}
						i=syn.indexOf(')',i+1);
						if (end==-1)
							throw new ParseException("Missing matching ')' at " + begin);
					} else {
						buf.append(fileName);
					}
					break;
				case '#':
				//number format
//$D\akina_$##$E
					final String tmp = do_str.padLeft(syn.lastIndexOf('#')+1-i,'0',Integer.toString(RenameFormat.increment()));
					buf.append(tmp);
					i=syn.lastIndexOf('#');
					break;
				case 'P':
				//Full path with trailing separator
					buf.append(path.substring(firstSlash, lastSlash+1));
					break;
				case 'F':
				//file name with extension
					if ((i+1) < syn.length() && syn.charAt(i+1)=='(') {
						final String[] sTmp = syn.substring(i+2,syn.indexOf(')',i+1)).split(",");
						if (sTmp.length==2) {
							if (!do_str.isNaN(sTmp[0])&&!do_str.isNaN(sTmp[1])) {
								begin = Integer.parseInt(sTmp[0]);
								end = Integer.parseInt(sTmp[1]);

								//fix negative indices
								if (begin < 0)
									begin = fullName.length() - 1 + begin;
								if (end < 0)
									end = fullName.length() + end;

								//error checking
								if (end < begin)
									throw new ParseException("End index is before begin index.");
								if(end == begin)
									throw new ParseException("Beginning and End cannot be equal.");
								if (end < 0)
									throw new ParseException("End index comes before the start of the filename by "+(0-end)+".");
								if (begin < 0)
									throw new ParseException("Begin index comes before the start of the filename by "+(0-begin)+".");
								if(begin >= fullName.length())
									throw new ParseException("Begin index exceeds the length of the filename by "+(begin- fullName.length())+".");

								if(end >= fullName.length())
									// Be lenient with this error
									buf.append(fullName.substring(begin));
								else
									buf.append(fullName.substring(begin, end));
							}
						} else {
							if (!do_str.isNaN(sTmp[0])) {
								begin = Integer.parseInt(sTmp[0]);

								//fix negative index
								if (begin < 0)
									begin = fullName.length() - 1 + begin;

								//error checking
								if (begin < 0)
									throw new ParseException("Begin index comes before the start of the filename by "+(0-begin)+".");
								if(begin >= fullName.length())
									throw new ParseException("Begin index exceeds the length of the filename by "+(fullName.length()-begin)+".");

								buf.append(fullName.substring(begin));
							}
						}
						i=syn.indexOf(')',i+1);
						if (end==-1)
							throw new ParseException("Missing matching ')' at " + begin);
					} else {
						buf.append(fullName);
					}
					break;
				case 'M':
				//move up directory
					final String[] sTmp = path.split(pathsplit);
					end = 1;
					if (syn.charAt(i+1)=='(') {
						if (!do_str.isNaN(syn.substring(i+2,syn.indexOf(')',i+1))))
							end = Integer.parseInt(syn.substring(i+2, syn.indexOf(')',i+1)));
						i=syn.indexOf(')',i+1);
						if (end==-1)
							throw new ParseException("Missing matching ')' at " + begin);
					}
					for(int ind = 0;ind<(sTmp.length-(end+1));ind++) {
						buf.append(sTmp[ind]+File.separator);
					}
					break;
				case 'E':
				//file extension
					buf.append(ext);
					break;
				default:
					buf.append(syn.charAt(--i));
				}
				//System.out.println(buf.toString());
			} else {
				buf.append(syn.charAt(i));
			}
		}
		String dir=buf.toString();
		String file=dir.substring(dir.lastIndexOf(File.separatorChar)+1);
		dir=dir.substring(0,dir.length()-file.length());
//System.out.println(dir+" --- "+file);
		if(RenameFormat.fURISAFED || RenameFormat.fURISAFEF){
			if(RenameFormat.fURISAFED)
				dir=dir.toLowerCase().replace(' ','-').replace('.','-');
			if(RenameFormat.fURISAFEF)
				file=file.toLowerCase().replace(' ','-');

			return (dir+file).replace("--","-");
		}else{
			if((transformations&TRANS_PATH_LC)==TRANS_PATH_LC){
				dir=dir.toLowerCase();
			}else if((transformations&TRANS_PATH_UC)==TRANS_PATH_UC){
				dir=dir.toUpperCase();
			}else if((transformations&TRANS_PATH_UCF)==TRANS_PATH_UCF){
				String[] dirp=dir.split(pathsplit);
				StringBuilder dirb=new StringBuilder(dir.length());
				for(String dirt : dirp){
					dirb.append(do_str.capitalize(dirt)+File.separator);
				}
				dir=dirb.toString();
			}
			if((transformations&TRANS_FILE_LC)==TRANS_FILE_LC){
				file=file.toLowerCase();
			}else if((transformations&TRANS_FILE_UC)==TRANS_FILE_UC){
				file=file.toUpperCase();
			}else if((transformations&TRANS_FILE_UCF)==TRANS_FILE_UCF){
				file=do_str.capitalize(file);
			}
			return dir+file;
		}
	}
	private static String getDir(final String path, final int i) {
		String[]x =  path.split("\\Q"+File.separator+"\\E");
		return x[x.length-i-1];
	}
	public void setFormat(final String format) {
		canundo = false;
		destName = null;
		syn = format;
	}
	public void setFile(final File f) {
		canundo = false;
		destName = null;
		file = f;
	}
	public int undo() {
		if (!canundo) return (errorNum=RenameFormat.CANTUNDO);
		if (!undo.exists()) return (errorNum=RenameFormat.BADSOURCE);
		/*
		 * !file.equals(undo) is a check for case insensitive file systems.
		 * Relying on the fact that file.equals(undo) will be true on case insensitive
		 * systems and false on case sensitive systems if the only change was a
		 * case transformation..
		 */
		if (file.exists() && !file.equals(undo)) return (errorNum=RenameFormat.BADTARGET);
		file.getParentFile().mkdirs();
		undo.renameTo(file);
		canundo = false;
		return (errorNum=RenameFormat.OK);
	}
	public int getErrorNum() {
		return errorNum;
	}
	/**Gets a user friendly description of the error
	 * @return the error string
	 */
	public String getError() {
		if (errorNum>=0&&errorNum<RenameFormat.ERR.length) {
			if (errorNum==RenameFormat.PARSEEXCEPTION) {
				return RenameFormat.ERR[errorNum]+pe.toString();
			} else {
				return RenameFormat.ERR[errorNum];
			}
		} else {
			return "No error.";
		}
	}
	/**Gets a user friendly description of the error
	 * @param errorNum Error number
	 * @return the error string
	 */
	public String getError(final int errorNum) {
		if (errorNum>=0&&errorNum<RenameFormat.ERR.length) {
			if (errorNum==RenameFormat.PARSEEXCEPTION) {
				return RenameFormat.ERR[errorNum]+pe.toString();
			} else {
				return RenameFormat.ERR[errorNum];
			}
		} else {
			return "No error.";
		}
	}
	/** Gets the undo value.
	 * @return The undo value or null if this has not been run.
	 */
	public String toStringTarget() {
		if (undo==null) return null;
		return undo.getAbsolutePath();
	}
	/** Gets the current value.
	 * @return The current value
	 */
	@Override
	public String toString() {
		if (file!=null)
			return file.getAbsolutePath();
		else
			return "";
	}
	public String saveState() {
		return file+"\t"+
				undo+"\t"+
				syn+"\t"+
				pe+"\t"+
				errorNum+"\n";
	}
	public static RenameFormat loadState(final String state) throws ParseException {
		final String[] args = state.split("\t");
		if (args.length!=5)
			throw new ParseException("Invalid number of parameters. Expected 5.");
		final RenameFormat temp = new RenameFormat();
		temp.setFile(new File(args[0]));
		if (!"null".equals(args[1])) {
			temp.undo = new File(args[1]);
			temp.canundo = true;
		}
		temp.setFormat(args[2]);
		temp.pe = new ParseException(args[3]);
		temp.errorNum = Integer.parseInt(args[4]);
		return null;
	}
}