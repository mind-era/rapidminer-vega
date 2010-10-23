package com.rapidminer.test;

import static com.rapidminer.test.TestUtils.assertArrayEquals;
import static junit.framework.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.parameter.ParameterTypeTupel;
import com.rapidminer.parameter.Parameters;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.container.Pair;

/** Tests {@link Tools#escape(String, char, char[])} and {@link Tools#unescape(String, char, char[], char)}.
 * 
 * @author Simon Fischer
 *
 */
public class EscapeTest {

	@Test
	public void testEscape() {
		assertEquals("test\\\\tost", Tools.escape("test\\tost", '\\', new char[0]));
		assertEquals("test\\\ntost", Tools.escape("test\ntost", '\\', new char[] {'\n'}));		
		assertEquals("one\\.two\\.three\\.\\.five", Tools.escape("one.two.three..five", '\\', new char[] {'.'}));
	}
	
	@Test
	public void testUnescape() {
		List<String> result = new LinkedList<String>();
		result.add("test\\tost");
		assertEquals(result, Tools.unescape("test\\\\tost", '\\', new char[] {'\\'}, (char)-1));
		
		result = new LinkedList<String>();
		result.add("line1");
		result.add("line.two");
		result.add("");
		result.add("last.line");
		assertEquals(result, Tools.unescape("line1.line\\.two..last\\.line", '\\', new char[0], '.'));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testException() {
		Tools.unescape("illegal\\escape character", '\\', new char[] {'a', 'b'}, (char)-1);
	}
	
	@Test
	public void testParameterTypeTuple() {
		assertArrayEquals(new String[] { "fi"+Parameters.PAIR_SEPARATOR+"rst", "sec"+Parameters.PAIR_SEPARATOR+"ond" }, ParameterTypeTupel.transformString2Tupel("fi\\"+Parameters.PAIR_SEPARATOR+"rst"+Parameters.PAIR_SEPARATOR+"sec\\"+Parameters.PAIR_SEPARATOR+"ond"));
		
		assertEquals("fi\\"+Parameters.PAIR_SEPARATOR+"rst"+Parameters.PAIR_SEPARATOR+"sec\\"+Parameters.PAIR_SEPARATOR+"ond", ParameterTypeTupel.transformTupel2String("fi"+Parameters.PAIR_SEPARATOR+"rst", "sec"+Parameters.PAIR_SEPARATOR+"ond"));
		assertEquals("fi\\"+Parameters.PAIR_SEPARATOR+"rst"+Parameters.PAIR_SEPARATOR+"sec\\"+Parameters.PAIR_SEPARATOR+"ond", ParameterTypeTupel.transformTupel2String(new String[] { "fi"+Parameters.PAIR_SEPARATOR+"rst", "sec"+Parameters.PAIR_SEPARATOR+"ond" }));
		assertEquals("fi\\"+Parameters.PAIR_SEPARATOR+"rst"+Parameters.PAIR_SEPARATOR+"sec\\"+Parameters.PAIR_SEPARATOR+"ond", ParameterTypeTupel.transformTupel2String(new Pair<String,String>("fi"+Parameters.PAIR_SEPARATOR+"rst", "sec"+Parameters.PAIR_SEPARATOR+"ond")));
	}
	
	@Test
	public void testParameterTypeEnumeration() {
		assertArrayEquals(
				new String[] { 
						"fi"+Parameters.RECORD_SEPARATOR+"rst",
						"sec"+Parameters.RECORD_SEPARATOR+"ond",
						"third"+Parameters.RECORD_SEPARATOR+"" }, 
				ParameterTypeEnumeration.transformString2Enumeration("fi\\"+Parameters.RECORD_SEPARATOR+"rst"+Parameters.RECORD_SEPARATOR+"sec\\"+Parameters.RECORD_SEPARATOR+"ond"+Parameters.RECORD_SEPARATOR+"third\\"+Parameters.RECORD_SEPARATOR+""));
		List<String> enumeration = new LinkedList<String>();
		enumeration.add("fi"+Parameters.RECORD_SEPARATOR+"rst");
		enumeration.add("sec"+Parameters.RECORD_SEPARATOR+"ond");
		enumeration.add("third"+Parameters.RECORD_SEPARATOR+"");
		assertEquals("fi\\"+Parameters.RECORD_SEPARATOR+"rst"+Parameters.RECORD_SEPARATOR+"sec\\"+Parameters.RECORD_SEPARATOR+"ond"+Parameters.RECORD_SEPARATOR+"third\\"+Parameters.RECORD_SEPARATOR+"", ParameterTypeEnumeration.transformEnumeration2String(enumeration));
	}
}