//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.11.13 at 07:00:40 PM MEZ 
//


package gde.device;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

import gde.GDE;


/**
 * <p>Java class for stop_bits_types.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="stop_bits_types">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="STOPBITS_1"/>
 *     &lt;enumeration value="STOPBITS_2"/>
 *     &lt;enumeration value="STOPBITS_1_5"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "stop_bits_types") //$NON-NLS-1$
@XmlEnum
public enum StopBitsTypes {

    STOPBITS_1,
    STOPBITS_2,
    STOPBITS_1_5;

    public String value() {
        return name();
    }

    public static StopBitsTypes fromValue(String v) {
        return valueOf(v);
    }

  	public static String[] valuesAsStingArray() {
  		StringBuilder sb = new StringBuilder();
  		for (StopBitsTypes element : StopBitsTypes.values()) {
  			sb.append(element).append(GDE.STRING_DASH);
  		}
  		return sb.toString().split(GDE.STRING_DASH);
  	}
}
