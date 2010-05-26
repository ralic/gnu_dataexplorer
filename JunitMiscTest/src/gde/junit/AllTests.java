/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright (c) 2008 - 2010 Winfried Bruegmann
****************************************************************************************/
package gde.junit;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for gde.junit");
		//$JUnit-BEGIN$
		suite.addTestSuite(JarInspectAndExportTest.class);
		suite.addTestSuite(TestFileReaderWriter.class);
		suite.addTestSuite(TestObjectKeyScanner.class);
		suite.addTestSuite(TestMathUtils.class);
		suite.addTestSuite(LogViewReaderTester.class);
		//$JUnit-END$
		return suite;
	}

}
