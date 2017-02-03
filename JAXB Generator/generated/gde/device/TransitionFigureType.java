//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2017.02.04 at 11:40:43 AM MEZ 
//


package gde.device;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for TransitionFigureType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="TransitionFigureType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="transitionGroupId" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="figureType" use="required" type="{}figure_types" />
 *       &lt;attribute name="comment" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TransitionFigureType")
public class TransitionFigureType {

    @XmlAttribute(required = true)
    protected int transitionGroupId;
    @XmlAttribute(required = true)
    protected FigureTypes figureType;
    @XmlAttribute
    protected String comment;

    /**
     * Gets the value of the transitionGroupId property.
     * 
     */
    public int getTransitionGroupId() {
        return transitionGroupId;
    }

    /**
     * Sets the value of the transitionGroupId property.
     * 
     */
    public void setTransitionGroupId(int value) {
        this.transitionGroupId = value;
    }

    /**
     * Gets the value of the figureType property.
     * 
     * @return
     *     possible object is
     *     {@link FigureTypes }
     *     
     */
    public FigureTypes getFigureType() {
        return figureType;
    }

    /**
     * Sets the value of the figureType property.
     * 
     * @param value
     *     allowed object is
     *     {@link FigureTypes }
     *     
     */
    public void setFigureType(FigureTypes value) {
        this.figureType = value;
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
