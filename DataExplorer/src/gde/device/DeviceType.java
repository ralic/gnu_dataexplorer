//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2010.01.01 at 01:20:48 PM GMT+01:00 
//


package osde.device;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * <p>Java class for DeviceType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DeviceType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="name">
 *           &lt;complexType>
 *             &lt;simpleContent>
 *               &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *                 &lt;attribute name="implementation" type="{http://www.w3.org/2001/XMLSchema}string" />
 *               &lt;/extension>
 *             &lt;/simpleContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="manufacturer" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="manufacturerURL" type="{http://www.w3.org/2001/XMLSchema}anyURI"/>
 *         &lt;element name="group" type="{}device_types"/>
 *         &lt;element name="image" type="{http://www.w3.org/2001/XMLSchema}anyURI"/>
 *         &lt;element name="usage" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DeviceType", propOrder = { //$NON-NLS-1$
    "name", //$NON-NLS-1$
    "manufacturer", //$NON-NLS-1$
    "manufacturerURL", //$NON-NLS-1$
    "group", //$NON-NLS-1$
    "image", //$NON-NLS-1$
    "usage" //$NON-NLS-1$
})
public class DeviceType {

    @XmlElement(required = true)
    protected DeviceType.Name name;
    @XmlElement(required = true)
    protected String manufacturer;
    @XmlElement(required = true)
    @XmlSchemaType(name = "anyURI") //$NON-NLS-1$
    protected String manufacturerURL;
    @XmlElement(required = true)
    protected DeviceTypes group;
    @XmlElement(required = true)
    @XmlSchemaType(name = "anyURI") //$NON-NLS-1$
    protected String image;
    protected boolean usage;

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link DeviceType.Name }
     *     
     */
    public DeviceType.Name getName() {
        return this.name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link DeviceType.Name }
     *     
     */
    public void setName(DeviceType.Name value) {
        this.name = value;
    }

    /**
     * Gets the value of the manufacturer property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getManufacturer() {
        return this.manufacturer;
    }

    /**
     * Sets the value of the manufacturer property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setManufacturer(String value) {
        this.manufacturer = value;
    }

    /**
     * Gets the value of the manufacturerURL property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getManufacturerURL() {
        return this.manufacturerURL;
    }

    /**
     * Sets the value of the manufacturerURL property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setManufacturerURL(String value) {
        this.manufacturerURL = value;
    }

    /**
     * Gets the value of the group property.
     * 
     * @return
     *     possible object is
     *     {@link DeviceTypes }
     *     
     */
    public DeviceTypes getGroup() {
        return this.group;
    }

    /**
     * Sets the value of the group property.
     * 
     * @param value
     *     allowed object is
     *     {@link DeviceTypes }
     *     
     */
    public void setGroup(DeviceTypes value) {
        this.group = value;
    }

    /**
     * Gets the value of the image property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getImage() {
        return this.image;
    }

    /**
     * Sets the value of the image property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setImage(String value) {
        this.image = value;
    }

    /**
     * Gets the value of the usage property.
     * 
     */
    public boolean isUsage() {
        return this.usage;
    }

    /**
     * Sets the value of the usage property.
     * 
     */
    public void setUsage(boolean value) {
        this.usage = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;simpleContent>
     *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
     *       &lt;attribute name="implementation" type="{http://www.w3.org/2001/XMLSchema}string" />
     *     &lt;/extension>
     *   &lt;/simpleContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "value"
    })
    public static class Name {

        @XmlValue
        protected String value;
        @XmlAttribute
        protected String implementation;

        /**
         * Gets the value of the value property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getValue() {
            return value;
        }

        /**
         * Sets the value of the value property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setValue(String value) {
            this.value = value;
        }

        /**
         * Gets the value of the implementation property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getImplementation() {
            return implementation;
        }

        /**
         * Sets the value of the implementation property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setImplementation(String value) {
            this.implementation = value;
        }

    }

}
