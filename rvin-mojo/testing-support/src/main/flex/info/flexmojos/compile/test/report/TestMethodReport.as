/**
 * Flexmojos is a set of maven goals to allow maven users to compile, optimize and test Flex SWF, Flex SWC, Air SWF and Air SWC.
 * Copyright (C) 2008-2012  Marvin Froeder <marvin@flexmojos.net>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package info.flexmojos.compile.test.report {

    [Bindable]
    [RemoteClass(alias="info.flexmojos.compile.test.report.TestMethodReport")]
    public class TestMethodReport extends TestMethodReportBase {
    	
    	/*
		    <testMethodReport>
		      <error>
		        <message>message</message>
		        <stackTrace>test</stackTrace>
		        <type>my.error.1</type>
		      </error>
		      <failure>
		        <message>message</message>
		        <stackTrace>test</stackTrace>
		        <type>my.error.1</type>
		      </failure>
		      <name>testMethod</name>
		      <time>25.3</time>
		    </testMethodReport>
    	 */
    	public function toXml():XML
    	{
    		var xml:XML =
		    <testMethodReport>
		      <name> { name } </name>
		      <time> { time } </time>
		    </testMethodReport>;
    			
    		if(error != null) {
				var errorXml:XML =
			      <error>
			        <message> {error.message} </message>
			        <stackTrace> { error.stackTrace } </stackTrace>
			        <type> { error.type } </type>
			      </error>;
 
    			xml = xml.appendChild(errorXml);
    		}

    		if(failure != null) {
				var failureXml:XML =
			      <failure>
			        <message> { failure.message } </message>
			        <stackTrace> { failure.stackTrace } </stackTrace>
			        <type> { failure.type } </type>
			      </failure>;
 
    			xml = xml.appendChild(failureXml);
    		}

    		return xml;
    	}
    }
}