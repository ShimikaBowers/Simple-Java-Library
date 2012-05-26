/**
 *
 */
package simple.ml;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.ParseException;

import simple.io.DoubleParsePosition;
import simple.io.ReadWriterFactory;
import simple.util.do_str;
import simple.util.logging.Log;
import simple.util.logging.LogFactory;

/**Parses ML without recursion. Will allow tags without end tags that are not
 * self-closing. Some may be interpreted as self-closing and have the children
 * tags moved to the parent. To define a self closing tag add it to
 * {@link ParserConstants}.<br>
 * Other dependents:<br>
 * simple.io.DoubleParsePosition<br>
 * simple.util.do_str<br>
 * simple.util.logging.Log<br>
 * simple.util.logging.LogFactory<br>
 * <br>Created: Nov 4, 2010
 * @author Kenneth Pierce
 */
public class InlineLooseParser {
	protected static final Log log = LogFactory.getLogFor(InlineLooseParser.class);
	/** See {@link #parse(CharSequence, ParserConstants)} for important notes.
	 * @param src The text.
	 * @return A {@link simple.ml.Page Page} object that represents the source.
	 * @throws ParseException
	 */
	public static Page parse(final CharSequence src) throws ParseException, IOException {
		return parse(src, new ParserConstants());
	}
	/** See {@link #parse(Reader, ParserConstants)} for important notes.
	 * @param in The Reader.
	 * @return A {@link simple.ml.Page Page} object that represents the source.
	 * @throws ParseException
	 */
	public static Page parse(final Reader in) throws ParseException, IOException {
		return parse(in, new ParserConstants());
	}
	public static Page parse(final Reader in, final ParserConstants pconst) throws IOException, ParseException {
		final BufferedReader bin = ReadWriterFactory.getBufferedReader(in);
		final Page page = new Page();
		final DoubleParsePosition pos = new DoubleParsePosition();
		Tag cur = null;//tag currently adding to. defaults to page if cur==null
		Tag tag = null;//tag currently creating
		final StringBuilder buf = new StringBuilder(500);
		char c;
		int retc;
		//int index = 0;
		boolean dquote = false, squote = false;
		String tmp;
		while (true) {
			pos.reset();
			//log.debug("---- LOOP ----");
			if (buf.length()==0) {
				retc = ReadWriterFactory.skipWhitespace(bin);
				c= (char)retc;
				//log.debug("retc="+retc+":c='"+c+"'");
				if (retc==-1) {
					break;
				}
				//index +=1;
				buf.append(c);
			} else {
				c = buf.charAt(0);
			}
			if (c!='<') {
				buf.append(ReadWriterFactory.readUntil(bin, '<'));
				if (buf.charAt(buf.length()-1)=='<') {
					buf.deleteCharAt(buf.length()-1);
				}
				log.debug("buf",buf);
				log.debug("CDATA");
				if (cur != null) {
					cur.addChild(new Tag(Tag.CDATA, buf.toString(), true));
				} else {
					page.addTag(new Tag(Tag.CDATA, buf.toString(), true), null);
				}
				buf.setLength(0);
				buf.append('<');
				continue;
			}
			buf.append(ReadWriterFactory.readUntil(bin, '>'));
			if (buf.length()>3 && buf.substring(0, 4).equals("<!--")) {
				if (!buf.substring(buf.length()-3).equals("-->")) {
					buf.append(ReadWriterFactory.readUntil(bin,"-->"));
				}
				log.debug("HTML comment");
				if (cur==null) {
					page.addTag(new Tag(Tag.HTMLCOMM, buf.toString(), true), null);
				} else {
					cur.addChild(new Tag(Tag.HTMLCOMM, buf.toString(), true));
				}
				buf.setLength(0);
				continue;
			}
			if(buf.length()<2)break;
			if (buf.charAt(1)=='!'){
				// <!DOCTYPE
				if (buf.length()>9){
					final String ttmp=buf.substring(0,9);
					if(ttmp.equals("<!CDATA[[")){//NOTE: SGML CDATA
						if (!buf.substring(buf.length()-4).equals("]]>")) {
							buf.append(ReadWriterFactory.readUntil(bin,"]]>"));
						}
						log.debug("SGML CDATA");
						if (cur==null) {
							page.addTag(new Tag(Tag.SGMLCDATA, buf.toString(), true), null);
						} else {
							cur.addChild(new Tag(Tag.SGMLCDATA, buf.toString(), true));
						}
						buf.setLength(0);
						continue;
					}else if(ttmp.toUpperCase().equals("<!DOCTYPE")){
						log.debug("DOCTYPE");
						page.addTag(new Tag(Tag.META,buf.toString(),true), null);
						buf.setLength(0);
						continue;
					}
				}
			}
			if (buf.charAt(pos.start+1)=='/') {//NOTE: found an end tag
				log.debug(pos);
				pos.end = do_str.indexOfAny(buf, ">", pos.start);
				if (!pos.validEnd() || do_str.isWhiteSpace(buf.charAt(pos.end)))
					throw new ParseException("Missing matching '>' for '<' at index "+pos.start+" of "+buf, pos.start);
				final String name = buf.substring(pos.start+2, pos.end).trim();
				log.debug("End tag", "'"+name+"'|"+pos);
				if (cur != null) {
					//end the tags whose end tags are optional
					while (!cur.getName().equals(name) && pconst.isOptionalEnder(cur.getName())) {
						log.debug("optional ender","ended: '"+cur.getName()+"'");
						cur = (Tag) cur.getParent();
					}
					log.debug("current: '"+cur.getName()+"'");
					if (cur.getName().equals(name)) {
						cur = (Tag) cur.getParent();
						log.debug("ended: '"+name+"' current: '"+(cur!=null?cur.getName():"[NONE]")+"'");
					} else {
						tag = cur;
						boolean found = false;
						while (tag.hasParent()) {//check the upper tags(cause: self-closing tag without special closure or empty body followed by closing tag)
							tag = (Tag) tag.getParent();
							if (tag.getName().equals(name)) {
								found = true;
								do {
									log.warning("lazy self-closing tag found: '"+cur.getName()+"'");
									cur.setSelfClosing(true);//this will copy all children to parent
									cur = (Tag) cur.getParent();
								} while (cur != tag);
								cur = (Tag) cur.getParent();
								break;
							}
						}
						if (!found)
						{
							log.warning("Missing opening tag for closing tag "
									+buf.subSequence(pos.start, pos.end+1)
									+" found at "+pos);
							//if (cur!=null)
							//	log.information("current(after closing): "+cur.getName());
						}
					}
				} else {
					log.warning("Missing opening tag for closing tag "
							+buf.subSequence(pos.start, pos.end+1)
							+" found at "+pos+".(At document root)");
				}
				buf.setLength(0);
				continue;
			}//end handle end tag
			//make sure these are defaulted
			dquote = false;
			squote = false;
			try{
			//search for ending > [had a case where the alt attr of an img tag contained a > and it screwed the parser]
			while (dquote || squote || buf.charAt(pos.end)!='>') {
				if (!squote && buf.charAt(pos.end)=='"' /*&& buf.charAt(pos.end-1)!='\\'*/) {
					dquote = !dquote;
				}
				if (!dquote && buf.charAt(pos.end)=='\'' /*&& buf.charAt(pos.end-1)!='\\'*/) {
					squote = !squote;
				}
				pos.end++;
				if (pos.end == buf.length()) {
					tmp = ReadWriterFactory.readUntil(bin, '>');
					if (tmp==null) {
						log.debug(buf);
						throw new ParseException(pos+" Reached end of file while parsing. Cause: "+buf, pos.start);
					}
					buf.append(tmp);
				}
			}
			}catch(final StringIndexOutOfBoundsException e){
				int start=pos.end-100,end=pos.end+100;
				if(start<0)start=0;
				if(end>buf.length())end=buf.length();
				throw new StringIndexOutOfBoundsException(buf.substring(start,end));
			}
			tag = createTag(buf, pos);
			log.debug("Start tag","'"+tag.getName()+"' | "+pos);
			if (cur==null) {
				page.addTag(tag, null);
			} else {
				if (pconst.isOptionalEnder(cur.getName()) && pconst.isOptionalEnderEnd(cur.getName(),tag.getName())) {
					log.debug("optional ender","ended: "+cur.getName()+" current: "+(cur.getParent()!=null?cur.getParent().getName():"[NONE]"));
					cur = (Tag) cur.getParent();
				}
				cur.addChild(tag);
			}
			if (pconst.isPcdataTag(tag.getName())) {//check for tags filled with gibberish to us
				try {
					buf.setLength(0);
					buf.append(ReadWriterFactory.readUntil(bin, "</"+tag.getName()+">", true));
				} catch (final EOFException e) {
					throw new ParseException("Missing end tag for PCDATA tag "+tag.getName()+" found at "+pos,pos.start);
				}
				try{
					tag.addChild(new Tag(Tag.CDATA, buf.substring(0, Math.max(do_str.CI.lastIndexOf(buf,"</"+tag.getName())-(tag.getName().length()+3),0)).trim(), true));
				}catch(final StringIndexOutOfBoundsException e){
					final StringIndexOutOfBoundsException e2= new StringIndexOutOfBoundsException(buf.toString());
					e2.initCause(e);
					throw e2;
				}
				log.debug("PCDATA tag");
				buf.setLength(0);
				continue;//add the data and continue(skips the adding of sub tags)
			}
			if (tag.isSelfClosing()) {
				buf.setLength(0);
				continue;
			} else if (pconst.isSelfCloser(tag.getName())) {
				log.debug("pre-defined self closer");
				tag.setSelfClosing(true);
				buf.setLength(0);
				continue;
			}
			cur = tag;
			buf.setLength(0);
			//			log.information("current: "+cur.getName());
		}
		if (cur != null) {
			do {
				log.warning("Missing end tag for " + cur.getName()+"|"+pos);
				cur = (Tag) cur.getParent();
			} while (cur != null);
		}
		//log.debug(page);
		page.rebuildCache();
		log.debug(page);
		return page;
	}
	/** Parses the text.
	 * NOTE: the plain text of a tag is added as a sub-tag with the name "CDATA".
	 * You can also use Tag.CDATA for the reference.
	 * This is done since the CDATA could be split by a sub-tag.(e.g. [p] text [span]text[/span] text [/p])
	 * @param src The text.
	 * @param pconst Options specific to this source's format.
	 * @return A {@link simple.ml.Page Page} object that represents the source.
	 * @see simple.ml.ParserConstants
	 * @throws ParseException
	 */
	public static Page parse(final CharSequence src, final ParserConstants pconst) throws ParseException, IOException {
		return parse(new StringReader(src.toString()),pconst);
		/*final Page page = new Page();
		final DoubleParsePosition pos = new DoubleParsePosition();
		Tag cur = null;//tag currently adding to. defaults to page if cur==null
		Tag tag = null;//tag currently creating
		boolean dquote = false;
		boolean squote = false;
		while (true) {
			log.debug("---- LOOP ----");
			pos.start = do_str.skipWhitespace(src, pos.start);
			pos.end = do_str.indexOf(src, '<', pos.start);
			if (!pos.validEnd()) {
				if (pos.start == src.length()) {
					break;
				}
				log.debug("CDATA");
				if (cur!=null) {
					cur.addChild(new Tag(Tag.CDATA, src.subSequence(pos.start, src.length()).toString(), true));
				} else {
					page.addTag(new Tag(Tag.CDATA, src.subSequence(pos.start, src.length()).toString(), true), null);
				}
				break;//reached the end
			} else if (pos.start!=pos.end) {
				log.debug("CDATA");
				if (cur != null) {
					cur.addChild(new Tag(Tag.CDATA, src.subSequence(pos.start, pos.end).toString(), true));
				} else {
					page.addTag(new Tag(Tag.CDATA, src.subSequence(pos.start, pos.end).toString(), true), null);
				}
				pos.resetStart();
				continue;
			}
			if (do_str.startsWith(src, "<!--", pos.start)){//NOTE: html comment
				pos.end = do_str.indexOf(src, "-->", pos.start);
				if (!pos.validEnd()) throw new ParseException(pos+" Failed to find closing tag for comment.", pos.start);
				log.debug("HTML comment");
				if (cur==null) {
					page.addTag(new Tag(Tag.HTMLCOMM, src.subSequence(pos.start, pos.end).toString(), true), null);
				} else {
					cur.addChild(new Tag(Tag.HTMLCOMM, src.subSequence(pos.start, pos.end).toString(), true));
				}
				pos.end+=4;
				pos.resetStart();
				continue;
			}
			if (do_str.startsWith(src, "<!CDATA[[", pos.start)){//NOTE: html comment
				pos.end = do_str.indexOf(src, "]]>", pos.start);
				if (!pos.validEnd()) throw new ParseException(pos+" Failed to find closing tag for comment.", pos.start);
				log.debug("SGML CDATA");
				if (cur==null) {
					page.addTag(new Tag(Tag.SGMLCDATA, src.subSequence(pos.start, pos.end).toString(), true), null);
				} else {
					cur.addChild(new Tag(Tag.SGMLCDATA, src.subSequence(pos.start, pos.end).toString(), true));
				}
				pos.end+=4;
				pos.resetStart();
				continue;
			}
			//end == start; no need to reset start

			if (src.charAt(pos.start+1)=='/') {//NOTE: found an end tag
				pos.end = do_str.indexOfAny(src, "> \r\n\t", pos.start);
				if (!pos.validEnd() || do_str.isWhiteSpace(src.charAt(pos.end)))
					throw new ParseException("Missing matching '>' for '<' at index "+pos.start, pos.start);
				final String name = src.subSequence(pos.start+2, pos.end).toString();
				log.debug("End tag", "'"+name+"'|"+pos);
				if (cur != null) {
					//end the tags whose end tags are optional
					while (!cur.getName().equals(name) && pconst.isOptionalEnder(cur.getName())) {
						log.debug("optional ender","ended: '"+cur.getName()+"'");
						cur = (Tag) cur.getParent();
					}
					log.debug("current: '"+cur.getName()+"'");
					if (cur.getName().equals(name)) {
						cur = (Tag) cur.getParent();
						log.debug("ended: '"+name+"' current: '"+(cur!=null?cur.getName():"[NONE]")+"'");
					} else {
						//log.information(pos+" out of place end tag: "+name);
						tag = cur;
						boolean found = false;
						while (tag.hasParent()) {//check the upper tags(cause: self-closing tag without special closure or empty body followed by closing tag)
							tag = (Tag) tag.getParent();
							if (tag.getName().equals(name)) {
								found = true;
								do {
									log.warning("lazy self-closing tag found: '"+cur.getName()+"'");
									cur.setSelfClosing(true);//this will copy all children to parent
									cur = (Tag) cur.getParent();
								} while (cur != tag);
								cur = (Tag) cur.getParent();
								break;
							}
						}
						if (!found)
						{
							log.warning("Missing opening tag for closing tag "
									+src.subSequence(pos.start, pos.end+1)
									+" found at "+pos);
							//						if (cur!=null)
							//							log.information("current(after closing): "+cur.getName());
						}
					}
				} else {
					log.warning("Missing opening tag for closing tag "
							+src.subSequence(pos.start, pos.end+1)
							+" found at "+pos+".(At document root)");
				}
				pos.resetStart();
				pos.incStart();
				continue;
			}//end handle end tag
			//make sure these are defaulted
			dquote = false;
			squote = false;
			//search for ending > [had a case where the alt attr of an img tag contained a > and it screwed the parser]
			while (dquote || squote || src.charAt(pos.end)!='>') {
				if (!squote && src.charAt(pos.end)=='"' && src.charAt(pos.end-1)!='\\') {
					dquote = !dquote;
				}
				if (!dquote && src.charAt(pos.end)=='\'' && src.charAt(pos.end-1)!='\\') {
					squote = !squote;
				}
				pos.end++;
				if (pos.end == src.length()) {
					log.debug(src);
					throw new ParseException(pos+" Reached end of file while parsing. Cause: "+src.subSequence(pos.start, src.length()), pos.start);
				}
			}
			tag = createTag(src, pos);
			log.debug("Start tag","'"+tag.getName()+"' | "+pos);
			pos.start = pos.end+1;
			if (cur==null) {
				page.addTag(tag, null);
			} else {
				if (pconst.isOptionalEnder(cur.getName()) && pconst.isOptionalEnderEnd(cur.getName(),tag.getName())) {
					log.debug("optional ender","ended: "+cur.getName()+" current: "+(cur.getParent()!=null?cur.getParent().getName():"[NONE]"));
					cur = (Tag) cur.getParent();
				}
				cur.addChild(tag);
			}
			if (pconst.isPcdataTag(tag.getName())) {//check for tags filled with gibberish to us
				pos.end = do_str.indexOf(src, "</"+tag.getName(), pos.start);
				if (!pos.validEnd()) throw new ParseException("Missing end tag for PCDATA tag "+tag.getName()+" found at "+pos,pos.start);
				tag.addChild(new Tag(Tag.CDATA, src.subSequence(pos.start, pos.end).toString(), true));
				pos.start = do_str.indexOf(src, '>', pos.end)+1;
				pos.resetEnd();
				log.debug("PCDATA tag");
				continue;//add the data and continue(skips the adding of sub tags)
			}
			if (tag.isSelfClosing()) {
				continue;
			} else if (pconst.isSelfCloser(tag.getName())) {
				log.debug("pre-defined self closer");
				tag.setSelfClosing(true);
				continue;
			}
			cur = tag;
			//			log.information("current: "+cur.getName());
		}
		if (cur != null) {
			do {
				log.warning("Missing end tag for " + cur.getName()+"|"+pos);
				cur = (Tag) cur.getParent();
			} while (cur != null);
		}
		//log.debug(page);
		page.rebuildCache();
		log.debug(page);
		return page;*/
	}
	/**Creates a Tag based on the raw data passed to it. Fills in attributes and the self-closing flag.
	 * @param src The complete source
	 * @param limits The object that represents the location of &lt; and &gt;
	 * @return The created tag.
	 * @throws ParseException when certain elements are missing
	 */
	private static Tag createTag(final CharSequence src, final DoubleParsePosition limits) throws ParseException {
		final DoubleParsePosition pos = limits.clone();
		log.debug("Tag:"+src.subSequence(limits.start, limits.end+1));
		if (src.charAt(pos.start+1)=='!' || src.charAt(pos.start+1)=='?')
			return new Tag("META", src.subSequence(pos.start, pos.end+1).toString(), true);
		final Tag tag = new Tag(src.charAt(limits.end-1)=='/');
		pos.end = do_str.indexOf(src, ' ', pos.start, limits.end);
		if (!pos.validEnd()) {//no space so no attributes.
			if (tag.isSelfClosing()) {
				tag.setName(src.subSequence(limits.start+1, limits.end-1).toString());
			} else {
				tag.setName(src.subSequence(limits.start+1, limits.end).toString());
			}
			return tag;
		}
		tag.setName(src.subSequence(limits.start+1, pos.end).toString());
		String attrn = "";
		while (true) {
			pos.start = pos.end+1;
			pos.start = do_str.skipWhitespace(src, pos.start);
			pos.end = do_str.indexOfAny(src, "= ", pos.start, limits.end);
			if (!pos.validEnd()) {
				break;
			}
			attrn = src.subSequence(pos.start, pos.end).toString();
			try {
				if (src.charAt(pos.end)=='=') {//distinguish between name=value and name
					pos.start = pos.end = pos.end+1;
					if (src.charAt(pos.start)=='\'') {
						do {
							pos.end = do_str.indexOf(src, '\'', pos.start+1, limits.end);
						} while (pos.validEnd() && src.charAt(pos.end-1)=='\\');
						if (!pos.validEnd()) throw new ParseException(pos+" Missing matching single quote in:'"+src.subSequence(limits.start, limits.end+1)+"' for "+attrn, pos.start);
						pos.start++;
					} else if (src.charAt(pos.start)=='"') {
						pos.end = do_str.indexOf(src, '"', pos.start+1, limits.end);
						/*do {
						} while (pos.validEnd() && src.charAt(pos.end-1)=='\\');*/
						if (!pos.validEnd()) throw new ParseException(pos+" Missing matching double quote in:'"+src.subSequence(limits.start, limits.end+1)+"' for "+attrn, pos.start);
						pos.start++;
					} else {
						pos.end = do_str.indexOfAny(src, " >", pos.start, limits.end);// space, or >
						if (!pos.validEnd()) throw new ParseException(pos+" Missing matching end of tag('>') in:'"+src.subSequence(limits.start, limits.end+1)+"' for "+attrn, pos.start);
						if(src.charAt(pos.end)=='>')
							if(src.charAt(pos.end-1)=='/'){
								pos.end--;
								if (!pos.validEnd()) throw new ParseException(pos+" Missing matching end of tag('>') in:'"+src.subSequence(limits.start, limits.end+1)+"' for "+attrn, pos.start);
							}
					}
					//log.debug(pos+attrn+"="+src.subSequence(pos.start, pos.end));
					tag.setProperty(attrn, src.subSequence(pos.start, pos.end).toString());
				} else {//attribute with no value
					tag.setProperty(attrn, attrn);
					//log.debug(pos+attrn+"="+src.subSequence(pos.start, pos.end));
				}
			} catch (final StringIndexOutOfBoundsException e) {
				log.error(pos.toString(),e);
			}
		}
		return tag;
	}
}
