//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.07.18 at 01:45:39 PM CEST 
//


package pl.edu.icm.maven.oozie.plugin.pigscripts.configuration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for mainProjectPigType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="mainProjectPigType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="scripts" type="{}scriptHandlingType"/>
 *         &lt;element name="macros" type="{}scriptHandlingType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "mainProjectPigType", propOrder = {
    "scripts",
    "macros"
})
public class MainProjectPigType {

    @XmlElement(required = true)
    protected ScriptHandlingType scripts;
    @XmlElement(required = true)
    protected ScriptHandlingType macros;

    /**
     * Gets the value of the scripts property.
     * 
     * @return
     *     possible object is
     *     {@link ScriptHandlingType }
     *     
     */
    public ScriptHandlingType getScripts() {
        return scripts;
    }

    /**
     * Sets the value of the scripts property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScriptHandlingType }
     *     
     */
    public void setScripts(ScriptHandlingType value) {
        this.scripts = value;
    }

    /**
     * Gets the value of the macros property.
     * 
     * @return
     *     possible object is
     *     {@link ScriptHandlingType }
     *     
     */
    public ScriptHandlingType getMacros() {
        return macros;
    }

    /**
     * Sets the value of the macros property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScriptHandlingType }
     *     
     */
    public void setMacros(ScriptHandlingType value) {
        this.macros = value;
    }

}
