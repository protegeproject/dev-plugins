package edu.stanford.smi.protege.query;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JList;

import edu.stanford.smi.protege.model.query.Query;
import edu.stanford.smi.protege.query.querytypes.AndQuery;
import edu.stanford.smi.protege.query.querytypes.OWLRestrictionQuery;
import edu.stanford.smi.protege.query.querytypes.OrQuery;
import edu.stanford.smi.protege.query.querytypes.OwnSlotValueQuery;
import edu.stanford.smi.protege.ui.FrameRenderer;
import edu.stanford.smi.protege.util.DefaultRenderer;
import edu.stanford.smi.protege.util.ModalDialog;

/**
 * Extends {@link FrameRenderer} to be used with 
 * queries.  Call {@link QueryFrameRenderer#setQuery(Query)} and then
 * when the renderer is used each string being painted will be compared 
 * to the query string.  Any matching parts are bolded by default.
 * You can also choose a different color for matched strings. 
 * 
 * @author Chris Callendar
 * @date 7-Nov-06
 */
public class QueryFrameRenderer extends FrameRenderer {

	private String queryString;	
	private Pattern pattern;
	private boolean appendQuery;
	private boolean bold;
	private Color matchColor;
	private Font oldFont;
	private FontMetrics oldMetrics;
	private Color oldColor;

	public QueryFrameRenderer() {
		super();
		this.queryString = null;
		this.pattern = null;
		this.appendQuery = false;
		this.bold = true;
		this.matchColor = null;
		this.oldFont = null;
		this.oldMetrics = null;
		this.oldColor = null;
	}
	
	/**
	 * Sets the color to use for matched strings.
	 * If null then the string is painted with same color (default).
	 * @param c the color to paint matched strings
	 */
	public void setMatchColor(Color c) {
		this.matchColor = c;
	}
	
	public Color getMatchColor() {
		return matchColor;
	}
	
	/**
	 * Sets whether matches should be painted using a bold font.
	 * This is true by default.
	 */
	public void setBoldMatches(boolean useBolding) {
		this.bold = useBolding;
	}
	
	public boolean isBoldMatches() {
		return bold;
	}
	
	/**
	 * Sets the query string based on the {@link Query}.
	 * At the moment {@link OWLRestrictionQuery} are not dealt with.
	 * If the query is an {@link OrQuery} or an {@link AndQuery}
	 * then each query string is combined together.  
	 * @param q the query
	 */
	public void setQuery(Query q) {
		if (q instanceof AndQuery) {
			AndQuery and = (AndQuery) q;
			appendQuery = true;
			for (Query query : and.getConjuncts()) {
				setQuery(query);
			}
			appendQuery = false;
		} else if (q instanceof OrQuery) {
			OrQuery or = (OrQuery) q;
			appendQuery = true;
			for (Query query : or.getDisjuncts()) {
				setQuery(query);
			}
			appendQuery = false;
		} else if (q instanceof OwnSlotValueQuery) {
			OwnSlotValueQuery ownQuery = (OwnSlotValueQuery) q;
			setQueryString(ownQuery.getExpr());
		} else if (q == null) {
			setQueryString(null);	// reset the pattern
		}
	}
	
	/**
	 * Sets the query string to use.  This will be 
	 * matched against any text that is rendered and the matching
	 * parts will be highlighted.
	 * @param string
	 */
	public void setQueryString(String string) {
		if (string != null) {
			if ((string.length() > 0) && !string.equalsIgnoreCase(queryString)) {
				if (appendQuery && (queryString != null)) {
					queryString += "|" + removeMetaChars(string); 
				} else {
					queryString = removeMetaChars(string);
				}
				pattern = Pattern.compile(queryString, Pattern.CASE_INSENSITIVE);
			}
		} else {
			pattern = null;
		}
	}
	
	/**
	 * This might be overkill... removes all special regex characters.
	 * The important one to remove is the '*' which is appended and prepended
	 * to many queries (contains, starts with, ends with).
	 * @param string
	 * @return the same string with any special regex characters removed
	 */
	private static String removeMetaChars(String string) {
		StringBuffer buffer = new StringBuffer(string.length());
		for (char c : string.toCharArray()) {
			switch (c) {
				case '|':
				case '(':
				case ')':
				case '[':
				case ']':
				case '{':
				case '}':
				case '\\':
				case '/':
				case '.':
				case '^':
				case '?':
				case '*':
				case '$':
				case '+':
					break;
				default:
					buffer.append(c);
					break;
			}
		}
		return buffer.toString();
	}
		
	/**
	 * Overrides the
	 * {@link DefaultRenderer#paintString(Graphics, String, Point, Color, Dimension)}
	 * method to bold part of the text that matches the query string. If no
	 * match is found then the super method is called.
	 */
	@Override
	protected void paintString(Graphics g, String text, Point position, Color color, Dimension size) {
		boolean callSuper = true;
		if (pattern != null) {
			// this code is copied from the super method in DefaultRenderer
			if (color != null) {
	            g.setColor(color);
	        }
	        int y = (size.height + _fontMetrics.getAscent()) / 2 - 2; // -2 is a bizarre fudge factor that makes it look better!

	        // start matching queryString to text
			Matcher m = pattern.matcher(text);
	        int start = 0;
	        String extra = "";
			while (m.find(start)) {
		        // BEFORE
				String before = text.substring(start, m.start());
				paintString(g, before, position, y, false);
				
				// MATCHED
				highlightOn(g);
				paintString(g, m.group(), position, y, true);
				highlightOff(g);
				
		        // AFTER
				extra = text.substring(m.end());
		        
				start = m.end();
				callSuper = false;
			}
			
			// EXTRA - any leftover charaters at the end that weren't matched
			paintString(g, extra, position, y, false);
			
		}
		// call the super method if we didn't paint the string already
		if (callSuper) {
			super.paintString(g, text, position, color, size);
		}
	}
	
	private void highlightOn(Graphics g) {
		// set the font and color
		if (bold) {
			oldFont = g.getFont();
			g.setFont(oldFont.deriveFont(Font.BOLD));
			oldMetrics = _fontMetrics;
			_fontMetrics = g.getFontMetrics();
		}
		if (matchColor != null) {
			oldColor = g.getColor();
			g.setColor(matchColor);
		}
	}
	
	/** Restore the original font and color. */
	private void highlightOff(Graphics g) {
		if (oldFont != null) {
			g.setFont(oldFont);
		}
		if (oldMetrics != null) {
			_fontMetrics = oldMetrics;
		}
		if (oldColor != null) {
			g.setColor(oldColor);
		}
	}
	
	/**
	 * Paints the string at the given position.
	 * @param text the string to paint
	 * @param position the x-position for the string (after painting the value is updated to the next x-position)
	 * @param y the y-position for the string
	 * @param highlight if the string should be bolded and/or colored
	 */
	protected void paintString(Graphics g, String text, Point position, int y, boolean highlight) {
		if (text.length() > 0) {
			g.drawString(text, position.x, y);
			position.x += _fontMetrics.stringWidth(text);
		}
	}
	
	// tester
	public static void main(String[] args) {
		QueryFrameRenderer renderer = new QueryFrameRenderer();
		OwnSlotValueQuery q1 = new OwnSlotValueQuery(null, "Chromosome");
		OwnSlotValueQuery q2 = new OwnSlotValueQuery(null, "some");
		ArrayList<Query> queries = new ArrayList<Query>(2);
		queries.add(q1);
		queries.add(q2);
		OrQuery query = new OrQuery(queries);
		renderer.setQuery(query);
		renderer.setMatchColor(new Color(24, 72, 128));
		renderer.setBoldMatches(true);
		
		String[] strings = { "Some_Chromosome_2", "chromosome\"s", "Chromosome_2", "Some_Chromosome", 
							"Kromosome", "Chromosomes are sometimes", "A long chromosome test" };

		JList list = new JList();
		list.setBorder(BorderFactory.createLoweredBevelBorder());
		list.setCellRenderer(renderer);
		list.setPreferredSize(new Dimension(300, 300));
		list.setListData(strings);
		
		ModalDialog.showDialog(null, list, "Title", ModalDialog.MODE_CLOSE);
	}
}
