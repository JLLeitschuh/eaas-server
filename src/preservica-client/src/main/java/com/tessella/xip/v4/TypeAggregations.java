//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 generiert 
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2016.11.16 um 03:18:14 PM CET 
//


package com.tessella.xip.v4;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse für typeAggregations complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="typeAggregations">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Accumulation" type="{http://www.tessella.com/XIP/v4}typeAccumulation" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="Accession" type="{http://www.tessella.com/XIP/v4}typeAccession" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "typeAggregations", propOrder = {
    "accumulation",
    "accession"
})
public class TypeAggregations {

    @XmlElement(name = "Accumulation")
    protected List<TypeAccumulation> accumulation;
    @XmlElement(name = "Accession")
    protected List<TypeAccession> accession;

    /**
     * Gets the value of the accumulation property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the accumulation property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAccumulation().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TypeAccumulation }
     * 
     * 
     */
    public List<TypeAccumulation> getAccumulation() {
        if (accumulation == null) {
            accumulation = new ArrayList<TypeAccumulation>();
        }
        return this.accumulation;
    }

    /**
     * Gets the value of the accession property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the accession property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAccession().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TypeAccession }
     * 
     * 
     */
    public List<TypeAccession> getAccession() {
        if (accession == null) {
            accession = new ArrayList<TypeAccession>();
        }
        return this.accession;
    }

}
