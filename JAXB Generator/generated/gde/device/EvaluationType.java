//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.12.20 at 02:08:46 PM MEZ 
//


package gde.device;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for EvaluationType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="EvaluationType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="calculation" type="{}CalculationType" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="min" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="max" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="avg" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="sigma" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="sum" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="first" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="last" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="comment" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EvaluationType", propOrder = {
    "calculation"
})
public class EvaluationType {

    protected CalculationType calculation;
    @XmlAttribute(required = true)
    protected boolean min;
    @XmlAttribute(required = true)
    protected boolean max;
    @XmlAttribute(required = true)
    protected boolean avg;
    @XmlAttribute(required = true)
    protected boolean sigma;
    @XmlAttribute(required = true)
    protected boolean sum;
    @XmlAttribute(required = true)
    protected boolean first;
    @XmlAttribute(required = true)
    protected boolean last;
    @XmlAttribute
    protected String comment;

    /**
     * Gets the value of the calculation property.
     * 
     * @return
     *     possible object is
     *     {@link CalculationType }
     *     
     */
    public CalculationType getCalculation() {
        return calculation;
    }

    /**
     * Sets the value of the calculation property.
     * 
     * @param value
     *     allowed object is
     *     {@link CalculationType }
     *     
     */
    public void setCalculation(CalculationType value) {
        this.calculation = value;
    }

    /**
     * Gets the value of the min property.
     * 
     */
    public boolean isMin() {
        return min;
    }

    /**
     * Sets the value of the min property.
     * 
     */
    public void setMin(boolean value) {
        this.min = value;
    }

    /**
     * Gets the value of the max property.
     * 
     */
    public boolean isMax() {
        return max;
    }

    /**
     * Sets the value of the max property.
     * 
     */
    public void setMax(boolean value) {
        this.max = value;
    }

    /**
     * Gets the value of the avg property.
     * 
     */
    public boolean isAvg() {
        return avg;
    }

    /**
     * Sets the value of the avg property.
     * 
     */
    public void setAvg(boolean value) {
        this.avg = value;
    }

    /**
     * Gets the value of the sigma property.
     * 
     */
    public boolean isSigma() {
        return sigma;
    }

    /**
     * Sets the value of the sigma property.
     * 
     */
    public void setSigma(boolean value) {
        this.sigma = value;
    }

    /**
     * Gets the value of the sum property.
     * 
     */
    public boolean isSum() {
        return sum;
    }

    /**
     * Sets the value of the sum property.
     * 
     */
    public void setSum(boolean value) {
        this.sum = value;
    }

    /**
     * Gets the value of the first property.
     * 
     */
    public boolean isFirst() {
        return first;
    }

    /**
     * Sets the value of the first property.
     * 
     */
    public void setFirst(boolean value) {
        this.first = value;
    }

    /**
     * Gets the value of the last property.
     * 
     */
    public boolean isLast() {
        return last;
    }

    /**
     * Sets the value of the last property.
     * 
     */
    public void setLast(boolean value) {
        this.last = value;
    }

    /**
     * Gets the value of the comment property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getComment() {
        return comment;
    }

    /**
     * Sets the value of the comment property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setComment(String value) {
        this.comment = value;
    }

}
