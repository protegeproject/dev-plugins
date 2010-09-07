/**
 * This class processes an OWL version of FMA (single-file version) to have it show up better in BioPortal. 
 * Specifically, we perform the following clean-up actions:
 *  - remove all user-defined metaclasses
 *  - make all classes that are instances of the removed metaclasses to be instances of owl:Class
 *  - for all properties (annotation 
 *  and regular properties) that have OWL individuals as values (such as Concept name instances and instances of various attributed relations)
 *  	- replace the property value with the browser key instead of the instance (hence it is critical that the input project has browser keys set correctly)
 *      - delete the correpsonding instance (we assume that each instance is used only once; fair assumption for the OWL version of the FMA)
 *  	- convert the property form object property to datatype property
 * 
 * @author Natasha Noy
 * 
 */
package edu.stanford.smi.fmatoowl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protegex.owl.model.OWLObjectProperty;
import edu.stanford.smi.protegex.owl.model.RDFIndividual;
import edu.stanford.smi.protegex.owl.model.RDFProperty;
import edu.stanford.smi.protegex.owl.model.RDFSClass;
import edu.stanford.smi.protegex.owl.model.RDFSDatatype;
import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;
import edu.stanford.smi.protegex.owl.model.impl.AbstractOWLModel;
import edu.stanford.smi.protegex.owl.model.impl.DefaultOWLNamedClass;

public class FlattenFMA {
	private static Project _sourceProject = null;
	private static AbstractOWLModel _sourceKB = null;
	
	private static RDFSNamedClass _owlClass = null;
	private static RDFSNamedClass _rdfsClass = null;
	private static RDFSNamedClass _annotationPropertyClass = null;
	private static RDFSNamedClass _owlDatatypePropertyClass = null;
	private static RDFSDatatype _stringDatatype = null;

	
//	private static RDFSNamedClass _anatomicalEntity = null;
	//Collection of properties that will need to change from Object properties to datatype properties
	//and whose values are converted from instances to their browser keys
	private static HashSet<OWLObjectProperty> _instanceProperties = new HashSet<OWLObjectProperty> ();

	public static void main(String[] args) {
		Collection<String> errors = new ArrayList<String>();
		
		Log.getLogger().info("Start conversion at " + new Date ());

		String sourceFileName =  args[0];
		_sourceProject = new Project(sourceFileName, errors);
		if (errors.size() != 0) {
			displayErrors(errors);
			return;
		}
		
		_sourceKB = (AbstractOWLModel)_sourceProject.getKnowledgeBase();
	
		
		processKnowledgeBase ();
		
//		_sourceProject.save(errors);
		
		Log.getLogger().info("DONE: " + new Date());
	}
	
	private static void processKnowledgeBase () {
		Collection<RDFSClass> roots = _sourceKB.getRootClses();
		
		
		Vector<RDFSClass> classesInQueue = new Vector<RDFSClass> ();
		classesInQueue.addAll(roots);

		HashSet<RDFSClass> classesProcessed = new HashSet<RDFSClass> (_sourceKB.getClsCount());
		
		_owlClass = _sourceKB.getSystemFrames().getOwlNamedClassClass();
		_rdfsClass = (RDFSNamedClass)_sourceKB.getCls("http://www.w3.org/2000/01/rdf-schema#Class");
		
		_annotationPropertyClass = _sourceKB.getSystemFrames().getOwlAnnotationPropertyClass();
		_owlDatatypePropertyClass = _sourceKB.getSystemFrames().getOwlDatatypePropertyClass();
		
//		_anatomicalEntity = _sourceKB.getRDFSNamedClass("Anatomical_entity");
		
		_stringDatatype = _sourceKB.getXSDstring(); 
		
		int counter = 0;

		while (!classesInQueue.isEmpty()) {
			RDFSClass next = (RDFSClass)classesInQueue.firstElement();
    		classesInQueue.remove(0);
    		
    		if (classesProcessed.contains(next)) continue;
    		classesProcessed.add(next);
    	
       		if (!(next instanceof RDFSNamedClass)) {
       			Log.getLogger().info("Not a class: " + next);
       			continue;
       		}
       	 
       		if (counter % 100 == 0)
    			Log.getLogger().info("Processing: " + counter + ":" + next);

     	
    		processClass ((RDFSNamedClass)next);

    		Collection<RDFSClass> nextSubs = next.getSubclasses(false);
    		if (nextSubs != null)
    			classesInQueue.addAll(0,  nextSubs);
    		
     		counter++;
		}
		
		Log.getLogger().info ("Changing property domains");
		
		changeDomainForAllProperties ();

		Log.getLogger().info ("Changing property ranges");
		
		changeRangeForInstanceProperties ();
		
	}

	/**
	 * For properties that had a metaclass as its domain, replace that domain with owl:Class
	 * Therefore all relevant properties will be possible for all classes and will appear on their forms
	 */
	private static void changeDomainForAllProperties() {
		Log.getLogger().info("Changing domains for properties");
		int count = 0;
		Collection<RDFProperty> properties = _sourceKB.getRDFProperties();
		for (RDFProperty property : properties) {
			if (property.isSystem()) continue;

			if (count % 10 == 0) 
				Log.getLogger().info ("" + count + ": Processing property " + property);
			
			Collection<RDFSNamedClass> domains = property.getUnionDomain();
			HashSet<RDFSNamedClass> newDomains = new HashSet<RDFSNamedClass> (domains.size());
			
			for (RDFSNamedClass domain : domains) {
				if (domain.isMetaclass() && !domain.equals(_owlClass))
					newDomains.add(_owlClass);
				else
					newDomains.add(domain);
			}
			
			property.setDomains(newDomains);	
			count ++;
		}
	}

	/**
	 * For properties for which we have changed the values form OWL Individuals to strings
	 * convert the property itself from datatype property to object property
	 */
	private static void changeRangeForInstanceProperties() {
		Log.getLogger().info("Changing ranges for instance properties");
		RDFProperty location = _sourceKB.getRDFProperty("location");
		int count = 0;
		for (OWLObjectProperty property : _instanceProperties) {
			if (property.isSystem()) continue;
			
			if (property.equals(location)) continue;
			
//			if (count % 10 == 0) 
				Log.getLogger().info ("" + count + ": Processing property " + property);
			
			boolean annotation = property.hasRDFType(_annotationPropertyClass); //remember whether it is an annotation property, to set it back later
			property.setRDFType(_owlDatatypePropertyClass);
			property.setRange(_stringDatatype);
			if (annotation)
				property.addRDFType(_annotationPropertyClass);
			count++;
		}
		
	}

	/**
	 * Process a single class:
	 * 	- change its metaclass to owl:Class
	 *  - change values for its properties from individual instances to their browser keys
	 * 
	 * @param cls
	 */
	private static void processClass(RDFSNamedClass cls) {
		if (cls.isSystem()) return;
		
		if (cls instanceof DefaultOWLNamedClass)
			cls.setRDFType(_owlClass); //change the metaclass (aka rdf:type)
		else // it is an RDF class
			cls.setRDFType(_rdfsClass);
		
		Collection<RDFProperty> properties = null;
		properties = cls.getRDFProperties();
		
		for (RDFProperty property : properties) {
			if (property.isSystem()) continue;
			
			Collection<Object> values = cls.getPropertyValues(property);
			if (values == null || values.isEmpty()) continue;
			
			Collection<String> flatValues = new ArrayList<String> (values.size());
			boolean instances = flattenValues (values, flatValues);
			
			if (!instances) continue; 
			
			cls.setPropertyValues(property, flatValues);
			_instanceProperties.add ((OWLObjectProperty)property);
		}		
	}

	/**
	 * For each value in the collection values, if that value is an instance, place its browser key in the collection flatValues
	 * We assume that either all values in collection values are instances, or none of them
	 * 
	 * @param values
	 * @param flatValues
	 * @return true if flatValues is different from values (i.e., the values were indeed instances and we replaced them with their browser keys;
	 * 		   false otherwise
	 */
	private static boolean flattenValues(Collection values,	Collection<String> flatValues) {
		for (Object value : values) {
			if (!(value instanceof RDFIndividual)) return false; //only flatten instance values; assume that all the values are of the same type
			
			flatValues.add(unQuote (((RDFIndividual)value).getBrowserText()));
			_sourceKB.deleteSimpleInstance((RDFIndividual)value); //assuming all instances are used only once.
		}
		return true;
	}

	/**
	 * Removes leading and trailing single-quotes from a string (only if both
	 * are present). The rest of single-quotes remain intact.
	 */
	private final static char SINGLE_QUOTE_CHAR = '\'';
	private static String unQuote(String string) {
		if (string == null)
			return (null);
		if (string.length() <= 0)
			return ("");
		if (string.length() == 1)
			return (string); // You can't unquote a single quote!
		if (string.charAt(0) == SINGLE_QUOTE_CHAR
				&& string.charAt(string.length() - 1) == SINGLE_QUOTE_CHAR) {
			return (string.substring(1, string.length() - 1));
		} else {
			return (string);
		}
	}


	private static void displayErrors(Collection errors) {
		Iterator i = errors.iterator();
		while (i.hasNext()) {
			System.out.println("Error: " + i.next());
		}
	}
	}
