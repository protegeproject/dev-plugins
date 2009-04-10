package edu.stanford.smi.protegex.util;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.storage.database.DatabaseKnowledgeBaseFactory;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protege.util.MessageError;
import edu.stanford.smi.protege.util.PropertyList;
import edu.stanford.smi.protege.util.SystemUtilities;
import edu.stanford.smi.protege.util.URIUtilities;
import edu.stanford.smi.protegex.owl.ProtegeOWL;
import edu.stanford.smi.protegex.owl.database.CreateOWLDatabaseFromFileProjectPlugin;
import edu.stanford.smi.protegex.owl.database.OWLDatabaseKnowledgeBaseFactory;
import edu.stanford.smi.protegex.owl.model.OWLModel;

public class ConvertToDb {

	private final static String CONVERT_STREAMING = "streaming";
	private final static String CONVERT_NONSTREAMING = "nonstreaming";

	private static String driver = "com.mysql.jdbc.Driver";
	private static String dbUrl = "jdbc:mysql://localhost/protege";	
	private static String table = "protegeTable";
	private static String user = "protege";
	private static String password = "";
	private static String owlFileUrl = "file.owl";
	private static String convertMode = CONVERT_NONSTREAMING;


	public static void main(String[] args) throws Exception {
		if (args.length < 7) {
			Log.getLogger().info("Usage: " +
			"convertToDB convertMode owlFileUrl dbDriver dbUrl dbTable dbUser dbPassword");
			return;
		}
		convertMode = args[0];
		owlFileUrl = args[1];
		driver = args[2];
		dbUrl = args[3];
		table = args[4];
		user = args[5];
		password = args[6];

		SystemUtilities.logSystemInfo();
		Log.getLogger().info("\n===== Started " + convertMode + " conversion to database on " + new Date());
		Log.getLogger().info("=== OWL File: " + owlFileUrl);
		Log.getLogger().info("=== Conversion mode: " + convertMode);
		Log.getLogger().info("=== Database URL: " + dbUrl);
		Log.getLogger().info("=== Database table: " + table + "\n");
		
		try {
			if (convertMode.equals(CONVERT_NONSTREAMING)) {
				convertToDatabaseProjectNonStreaming();
			} else if (convertMode.equals(CONVERT_STREAMING)) {
				convertToDatabaseProjectStreaming();
			} else {
				Log.getLogger().info("Unrecognized conversion mode. Valid options are: " +
						CONVERT_NONSTREAMING + "/" + CONVERT_STREAMING);
			}
		} catch (Exception e) {
			Log.getLogger().log(Level.WARNING, "Errors at conversion: " + e);
		}

		Log.getLogger().info("\n===== End " + convertMode + " conversion of " 
				+ owlFileUrl + " to database on " + new Date());
	}


	@SuppressWarnings("unchecked")
	private static OWLModel convertToDatabaseProjectNonStreaming() throws Exception {
		OWLModel fileModel = ProtegeOWL.createJenaOWLModelFromURI(owlFileUrl);
		
		List errors = new ArrayList();
		Project fileProject = fileModel.getProject();
		OWLDatabaseKnowledgeBaseFactory factory = new OWLDatabaseKnowledgeBaseFactory();
		PropertyList sources = PropertyList.create(fileProject.getInternalProjectKnowledgeBase());

		DatabaseKnowledgeBaseFactory.setSources(sources, driver, dbUrl, table, user, password);
		factory.saveKnowledgeBase(fileModel, sources, errors);

		displayErrors(errors); 
		if (!errors.isEmpty()) {
			return null;
		}
		return fileModel;
		//Following lines are optional.
		//Use them if you want to have a pprj generated for the DB project (you usually want this)
		//If not, you can always create a new project from existing sources in the Protege editor

		/*
		Project dbProject = Project.createNewProject(factory, errors);
		DatabaseKnowledgeBaseFactory.setSources(dbProject.getSources(), driver, dbUrl, table, user, password);

		dbProject.createDomainKnowledgeBase(factory, errors, true);
		dbProject.setProjectURI(URIUtilities.createURI(dbProjectFile));
		dbProject.save(errors);

		displayErrors(errors);  //forget this and it will be a mystery when things go wrong, see method below
		return (OWLModel) dbProject.getKnowledgeBase();		 
		 */
	}


	public static Project convertToDatabaseProjectStreaming() throws URISyntaxException {
		CreateOWLDatabaseFromFileProjectPlugin creator = new  CreateOWLDatabaseFromFileProjectPlugin();
		creator.setKnowledgeBaseFactory(new OWLDatabaseKnowledgeBaseFactory());
		creator.setDriver(driver);
		creator.setURL(dbUrl);
		creator.setTable(table);
		creator.setUsername(user);
		creator.setPassword(password);
		creator.setOntologyInputSource(URIUtilities.createURI(owlFileUrl));	
		creator.setUseExistingSources(true);

		Project p = creator.createProject();
		List errors = new ArrayList();
		p.save(errors);
		displayErrors(errors);
		return p;
	}


	public static void displayErrors(Collection errors) {
		Iterator i = errors.iterator();
		while (i.hasNext()) {
			Object elem = i.next();
			if (elem instanceof Throwable) {
				Log.getLogger().log(Level.WARNING, "Warnings at loading changes project", (Throwable)elem);
			} else if (elem instanceof MessageError) {
				Log.getLogger().log(Level.WARNING, ((MessageError)elem).getMessage(), ((MessageError)elem).getException());
			} else {
				Log.getLogger().warning(elem.toString());
			}
		}
	}


}

