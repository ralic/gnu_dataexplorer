//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.12.20 at 01:36:51 PM MEZ 
//


package gde.device;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for SettlementType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SettlementType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="symbol" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="unit" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="active" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="evaluation" type="{}EvaluationType" minOccurs="0"/>
 *         &lt;element name="property" type="{}PropertyType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="trailDisplay" type="{}TrailDisplayType" minOccurs="0"/>
 *         &lt;element name="label" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="settlementId" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SettlementType", propOrder = {
    "name",
    "symbol",
    "unit",
    "active",
    "evaluation",
    "property",
    "trailDisplay",
    "label"
})
public class SettlementType {

    @XmlElement(required = true)
    protected String name;
    @XmlElement(required = true)
    protected String symbol;
    @XmlElement(required = true)
    protected String unit;
    protected boolean active;
    protected EvaluationType evaluation;
    protected List<PropertyType> property;
    protected TrailDisplayType trailDisplay;
    protected String label;
    @XmlAttribute(required = true)
    protected int settlementId;

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the symbol property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * Sets the value of the symbol property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSymbol(String value) {
        this.symbol = value;
    }

    /**
     * Gets the value of the unit property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUnit() {
        return unit;
    }

    /**
     * Sets the value of the unit property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUnit(String value) {
        this.unit = value;
    }

    /**
     * Gets the value of the active property.
     * 
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Sets the value of the active property.
     * 
     */
    public void setActive(boolean value) {
        this.active = value;
    }

    /**
     * Gets the value of the evaluation property.
     * 
     * @return
     *     possible object is
     *     {@link EvaluationType }
     *     
     */
    public EvaluationType getEvaluation() {
        return evaluation;
    }

    /**
     * Sets the value of the evaluation property.
     * 
     * @param value
     *     allowed object is
     *     {@link EvaluationType }
     *     
     */
    public void setEvaluation(EvaluationType value) {
        this.evaluation = value;
    }

    /**
	 * Gets the value of the score property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list,
	 * not a snapshot. Therefore any modification you make to the
	 * returned list will be present inside the JAXB object.
	 * This is why there is not a <CODE>set</CODE> method for the property property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * <pre>
	 *    getProperty().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list
	 * {@link PropertyType }
	 * 
	 * 
	 */
	public List<PropertyType> getScores() {
		return null; //todo scores in settlements not supported anymore
	}

	/**
	 * @param scoreKey
	 * @param type
	 * @param value
	 */
	private void createScore(String scoreKey, DataTypes type, Object value) {
		ObjectFactory factory = new ObjectFactory();
		PropertyType newProperty = factory.createPropertyType();
		newProperty.setName(scoreKey);
		newProperty.setType(type);
		newProperty.setValue("" + value); //$NON-NLS-1$
		this.getScores().add(newProperty);
	}

	/**
	 * remove all score types
	 */
	public void removeScores() {
		Iterator<PropertyType> iterator = this.getProperty().iterator();

		while (iterator.hasNext()) {
			iterator.next();
			iterator.remove();
		}
	}

	/**
	 * remove score type with given key (IDevice.OFFSET, ...)
	 * @param scoreKey
	 */
	public void removeScore(String scoreKey) {
		Iterator<PropertyType> iterator = this.getProperty().iterator();

		while (iterator.hasNext()) {
			PropertyType tmpProp = iterator.next();
			if (tmpProp.name.equals(scoreKey))
				iterator.remove();
		}
	}

	/**
	 * get score type with given key (IDevice.OFFSET, ...)
	 * @param scoreKey
	 * @return PropertyType object
	 */
	public PropertyType getScore(String scoreKey) {
		PropertyType tmpScore = null;
		List<PropertyType> scores = this.getScores();
		for (PropertyType scoreType : scores) {
			if (scoreType.getName().equals(scoreKey)) {
				tmpScore = scoreType;
				break;
			}
		}
		return tmpScore;
	}

	/**
     * Gets the value of the property property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the property property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getProperty().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PropertyType }
     * 
     * 
     */
    public List<PropertyType> getProperty() {
        if (property == null) {
            property = new ArrayList<PropertyType>();
        }
        return this.property;
    }

    /**
     * Gets the value of the trailDisplay property.
     * 
     * @return
     *     possible object is
     *     {@link TrailDisplayType }
     *     
     */
    public Optional<TrailDisplayType> getTrailDisplay() {
      return Optional.ofNullable(trailDisplay);
    }

    /**
     * Sets the value of the trailDisplay property.
     * 
     * @param value
     *     allowed object is
     *     {@link TrailDisplayType }
     *     
     */
    public void setTrailDisplay(TrailDisplayType value) {
        this.trailDisplay = value;
    }

    /**
     * Gets the value of the label property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the value of the label property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLabel(String value) {
        this.label = value;
    }

    /**
     * Gets the value of the settlementId property.
     * 
     */
    public int getSettlementId() {
        return settlementId;
    }

    /**
     * Sets the value of the settlementId property.
     * 
     */
    public void setSettlementId(int value) {
        this.settlementId = value;
    }

    /**
	 * @param propertyKey
	 * @param type
	 * @param value
	 */
	private void createProperty(String propertyKey, DataTypes type, Object value) {
		ObjectFactory factory = new ObjectFactory();
		PropertyType newProperty = factory.createPropertyType();
		newProperty.setName(propertyKey);
		newProperty.setType(type);
		newProperty.setValue("" + value); //$NON-NLS-1$
		this.getProperty().add(newProperty);
	}

	/**
	 * remove all property types
	 * @param propertyKey
	 * @return PropertyType object
	 */
	public void removeProperties() {
		Iterator<PropertyType> iterator = this.getProperty().iterator();

		while (iterator.hasNext()) {
			iterator.next();
			iterator.remove();
		}
	}

	/**
	 * remove property type with given key (IDevice.OFFSET, ...)
	 * @param propertyKey
	 * @return PropertyType object
	 */
	public void removeProperty(String propertyKey) {
		Iterator<PropertyType> iterator = this.getProperty().iterator();

		while (iterator.hasNext()) {
			PropertyType tmpProp = iterator.next();
			if (tmpProp.name.equals(propertyKey))
				iterator.remove();
		}
	}

	/**
	 * get property type with given key (IDevice.OFFSET, ...)
	 * @param propertyKey
	 * @return PropertyType object
	 */
	public PropertyType getProperty(String propertyKey) {
		PropertyType tmpProperty = null;
		List<PropertyType> properties = this.getProperty();
		for (PropertyType propertyType : properties) {
			if (propertyType.getName().equals(propertyKey)) {
				tmpProperty = propertyType;
				break;
			}
		}
		return tmpProperty;
	}

	/**
	 * get the offset value
	 * @return the offset, if property does not exist return 0.0 as default value
	 */
	public double getOffset() {
		double value = 0.0;
		PropertyType tmpProperty = this.getProperty(IDevice.OFFSET);
		if (tmpProperty != null)
			value = new Double(tmpProperty.getValue()).doubleValue();

		return value;
	}

	/**
	 * set new value for offset
	 * @param offset the offset to set
	 */
	public void setOffset(double offset) {
		PropertyType tmpProperty = this.getProperty(IDevice.OFFSET);
		if (tmpProperty == null) {
			createProperty(IDevice.OFFSET, DataTypes.DOUBLE, offset);
		} else {
			tmpProperty.setValue("" + offset); //$NON-NLS-1$
		}
	}

	/**
	 * get the reduction value
	 * @return the offset, if property does not exist return 0.0 as default value
	 */
	public double getReduction() {
		double value = 0.0;
		PropertyType tmpProperty = this.getProperty(IDevice.REDUCTION);
		if (tmpProperty != null)
			value = new Double(tmpProperty.getValue()).doubleValue();

		return value;
	}

	/**
	 * set new value for reduction
	 * @param reduction the offset to set
     */
	public void setReduction(double reduction) {
		PropertyType tmpProperty = this.getProperty(IDevice.REDUCTION);
		if (tmpProperty == null) {
			createProperty(IDevice.REDUCTION, DataTypes.DOUBLE, reduction);
		} else {
			tmpProperty.setValue("" + reduction); //$NON-NLS-1$
		}
    }

    /**
	 * get the factor value
	 * @return the factor, if property does not exist return 1.0 as default value
     */
	public double getFactor() {
		double value = 1.0;
		PropertyType tmpProperty = getProperty(IDevice.FACTOR);
		if (tmpProperty != null)
			value = new Double(tmpProperty.getValue()).doubleValue();

		return value;
    }

	/**
	 * set new value for factor
	 * @param factor the offset to set
	 */
	public void setFactor(double factor) {
		PropertyType tmpProperty = this.getProperty(IDevice.FACTOR);
		if (tmpProperty == null) {
			createProperty(IDevice.FACTOR, DataTypes.DOUBLE, factor);
		} else {
			tmpProperty.setValue("" + factor); //$NON-NLS-1$
		}
	}

 }
