//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.10.14 at 12:44:59 PM CEST 
//


package osde.device;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for DevicePropertiesType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DevicePropertiesType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Device" type="{}DeviceType"/>
 *         &lt;element name="SerialPort" type="{}SerialPortType" minOccurs="0"/>
 *         &lt;element name="TimeBase" type="{}TimeBaseType"/>
 *         &lt;element name="DataBlock" type="{}DataBlockType" minOccurs="0"/>
 *         &lt;element name="ModeState" type="{}ModeStateType" minOccurs="0"/>
 *         &lt;element name="Channel" type="{}ChannelType" maxOccurs="unbounded"/>
 *         &lt;element name="Desktop" type="{}DesktopType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DevicePropertiesType", propOrder = {
    "device",
    "serialPort",
    "timeBase",
    "dataBlock",
    "modeState",
    "channel",
    "desktop"
})
public class DevicePropertiesType {

    @XmlElement(name = "Device", required = true)
    protected DeviceType device;
    @XmlElement(name = "SerialPort")
    protected SerialPortType serialPort;
    @XmlElement(name = "TimeBase", required = true)
    protected TimeBaseType timeBase;
    @XmlElement(name = "DataBlock")
    protected DataBlockType dataBlock;
    @XmlElement(name = "ModeState")
    protected ModeStateType modeState;
    @XmlElement(name = "Channel", required = true)
    protected List<ChannelType> channel;
    @XmlElement(name = "Desktop", required = true)
    protected DesktopType desktop;

    /**
     * Gets the value of the device property.
     * 
     * @return
     *     possible object is
     *     {@link DeviceType }
     *     
     */
    public DeviceType getDevice() {
        return device;
    }

    /**
     * Sets the value of the device property.
     * 
     * @param value
     *     allowed object is
     *     {@link DeviceType }
     *     
     */
    public void setDevice(DeviceType value) {
        this.device = value;
    }

    /**
     * Gets the value of the serialPort property.
     * 
     * @return
     *     possible object is
     *     {@link SerialPortType }
     *     
     */
    public SerialPortType getSerialPort() {
        return serialPort;
    }

    /**
     * Sets the value of the serialPort property.
     * 
     * @param value
     *     allowed object is
     *     {@link SerialPortType }
     *     
     */
    public void setSerialPort(SerialPortType value) {
        this.serialPort = value;
    }

    /**
     * Gets the value of the timeBase property.
     * 
     * @return
     *     possible object is
     *     {@link TimeBaseType }
     *     
     */
    public TimeBaseType getTimeBase() {
        return timeBase;
    }

    /**
     * Sets the value of the timeBase property.
     * 
     * @param value
     *     allowed object is
     *     {@link TimeBaseType }
     *     
     */
    public void setTimeBase(TimeBaseType value) {
        this.timeBase = value;
    }

    /**
     * Gets the value of the dataBlock property.
     * 
     * @return
     *     possible object is
     *     {@link DataBlockType }
     *     
     */
    public DataBlockType getDataBlock() {
        return dataBlock;
    }

    /**
     * Sets the value of the dataBlock property.
     * 
     * @param value
     *     allowed object is
     *     {@link DataBlockType }
     *     
     */
    public void setDataBlock(DataBlockType value) {
        this.dataBlock = value;
    }

    /**
     * Gets the value of the modeState property.
     * 
     * @return
     *     possible object is
     *     {@link ModeStateType }
     *     
     */
    public ModeStateType getModeState() {
        return modeState;
    }

    /**
     * Sets the value of the modeState property.
     * 
     * @param value
     *     allowed object is
     *     {@link ModeStateType }
     *     
     */
    public void setModeState(ModeStateType value) {
        this.modeState = value;
    }

    /**
     * Gets the value of the channel property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the channel property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getChannel().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ChannelType }
     * 
     * 
     */
    public List<ChannelType> getChannel() {
        if (channel == null) {
            channel = new ArrayList<ChannelType>();
        }
        return this.channel;
    }

    /**
     * Gets the value of the desktop property.
     * 
     * @return
     *     possible object is
     *     {@link DesktopType }
     *     
     */
    public DesktopType getDesktop() {
        return desktop;
    }

    /**
     * Sets the value of the desktop property.
     * 
     * @param value
     *     allowed object is
     *     {@link DesktopType }
     *     
     */
    public void setDesktop(DesktopType value) {
        this.desktop = value;
    }

}
