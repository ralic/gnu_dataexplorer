//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.12.31 at 07:19:32 PM GMT+01:00 
//


package osde.device;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for DataBlockType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DataBlockType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="format" type="{}format_types"/>
 *         &lt;element name="size" type="{http://www.w3.org/2001/XMLSchema}integer"/>
 *         &lt;element name="commaSeparator" type="{}comma_separator_types" minOccurs="0"/>
 *         &lt;element name="decimalSeparator" type="{}decimal_separator_types" minOccurs="0"/>
 *         &lt;sequence minOccurs="0">
 *           &lt;element name="checkSum" type="{}checksum_types"/>
 *           &lt;element name="checkSumFormat" type="{}format_types"/>
 *         &lt;/sequence>
 *         &lt;element name="ending" type="{}line_ending_types" minOccurs="0"/>
 *         &lt;element name="preferredDataLocation" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="preferredFileExtention" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DataBlockType", propOrder = {
    "format",
    "size",
    "commaSeparator",
    "decimalSeparator",
    "checkSum",
    "checkSumFormat",
    "ending",
    "preferredDataLocation",
    "preferredFileExtention"
})
public class DataBlockType {

    @XmlElement(required = true)
    protected FormatTypes format;
    @XmlElement(required = true)
    protected Integer size;
    protected CommaSeparatorTypes commaSeparator;
    protected DecimalSeparatorTypes decimalSeparator;
    protected ChecksumTypes checkSum;
    protected FormatTypes checkSumFormat;
    protected LineEndingTypes ending;
    protected String preferredDataLocation;
    protected String preferredFileExtention;

    /**
     * Gets the value of the format property.
     * 
     * @return
     *     possible object is
     *     {@link FormatTypes }
     *     
     */
    public FormatTypes getFormat() {
        return format;
    }

    /**
     * Sets the value of the format property.
     * 
     * @param value
     *     allowed object is
     *     {@link FormatTypes }
     *     
     */
    public void setFormat(FormatTypes value) {
        this.format = value;
    }

    /**
     * Gets the value of the size property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public Integer getSize() {
        return size;
    }

    /**
     * Sets the value of the size property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setSize(Integer value) {
        this.size = value;
    }

    /**
     * Gets the value of the commaSeparator property.
     * 
     * @return
     *     possible object is
     *     {@link CommaSeparatorTypes }
     *     
     */
    public CommaSeparatorTypes getCommaSeparator() {
        return commaSeparator;
    }

    /**
     * Sets the value of the commaSeparator property.
     * 
     * @param value
     *     allowed object is
     *     {@link CommaSeparatorTypes }
     *     
     */
    public void setCommaSeparator(CommaSeparatorTypes value) {
        this.commaSeparator = value;
    }

    /**
     * Gets the value of the decimalSeparator property.
     * 
     * @return
     *     possible object is
     *     {@link DecimalSeparatorTypes }
     *     
     */
    public DecimalSeparatorTypes getDecimalSeparator() {
        return decimalSeparator;
    }

    /**
     * Sets the value of the decimalSeparator property.
     * 
     * @param value
     *     allowed object is
     *     {@link DecimalSeparatorTypes }
     *     
     */
    public void setDecimalSeparator(DecimalSeparatorTypes value) {
        this.decimalSeparator = value;
    }

    /**
     * Gets the value of the checkSum property.
     * 
     * @return
     *     possible object is
     *     {@link ChecksumTypes }
     *     
     */
    public ChecksumTypes getCheckSum() {
        return checkSum;
    }

    /**
     * Sets the value of the checkSum property.
     * 
     * @param value
     *     allowed object is
     *     {@link ChecksumTypes }
     *     
     */
    public void setCheckSum(ChecksumTypes value) {
        this.checkSum = value;
    }

    /**
     * Gets the value of the checkSumFormat property.
     * 
     * @return
     *     possible object is
     *     {@link FormatTypes }
     *     
     */
    public FormatTypes getCheckSumFormat() {
        return checkSumFormat;
    }

    /**
     * Sets the value of the checkSumFormat property.
     * 
     * @param value
     *     allowed object is
     *     {@link FormatTypes }
     *     
     */
    public void setCheckSumFormat(FormatTypes value) {
        this.checkSumFormat = value;
    }

    /**
     * Gets the value of the ending property.
     * 
     * @return
     *     possible object is
     *     {@link LineEndingTypes }
     *     
     */
    public LineEndingTypes getEnding() {
        return ending;
    }

    /**
     * Sets the value of the ending property.
     * 
     * @param value
     *     allowed object is
     *     {@link LineEndingTypes }
     *     
     */
    public void setEnding(LineEndingTypes value) {
        this.ending = value;
    }

    /**
     * Gets the value of the preferredDataLocation property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPreferredDataLocation() {
        return preferredDataLocation;
    }

    /**
     * Sets the value of the preferredDataLocation property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPreferredDataLocation(String value) {
        this.preferredDataLocation = value;
    }

    /**
     * Gets the value of the preferredFileExtention property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPreferredFileExtention() {
        return preferredFileExtention;
    }

    /**
     * Sets the value of the preferredFileExtention property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPreferredFileExtention(String value) {
        this.preferredFileExtention = value;
        if (!this.preferredFileExtention.startsWith("*.")) this.preferredFileExtention = "*." + this.preferredFileExtention; 
        else if (!this.preferredFileExtention.startsWith("*")) this.preferredFileExtention = "*" + this.preferredFileExtention; 
    }

}
