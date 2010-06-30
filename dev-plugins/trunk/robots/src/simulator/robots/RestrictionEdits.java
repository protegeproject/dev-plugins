package simulator.robots;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import simulator.AbstractRobot;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protegex.owl.model.OWLClass;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.OWLNamedClass;
import edu.stanford.smi.protegex.owl.model.OWLObjectProperty;
import edu.stanford.smi.protegex.owl.model.RDFSClass;


public class RestrictionEdits extends AbstractRobot {
	private static Logger logger = Log.getLogger(RestrictionEdits.class);
	
	public static final String WAIT_BETWEEN_WRITES_PROPERTY="wait.between.writes";
	
	private long waitBetweenWrites;
	private boolean doCleanup = false;
	
	private OWLModel model;
		
	private Random r = new Random();
	
	private OWLNamedClass root;
	private Set<OWLObjectProperty> createdProperties = new HashSet<OWLObjectProperty>();
	
	private String classPrefix = UUID.randomUUID().toString().replace("-","_");
	private int classCounter = 0;

	private String propertyPrefix = UUID.randomUUID().toString().replace("-","_");
	private int propertyCounter = 0;

	public RestrictionEdits(Properties properties) {
		super(properties);
		try {
		    waitBetweenWrites = Integer.parseInt(getProperty(WAIT_BETWEEN_WRITES_PROPERTY));
		}
		catch (Throwable t) {
		    waitBetweenWrites = 0;
		}
	}
	
	@Override
	public void login(String hostname, int port, String projectName) {
		super.login(hostname, port, projectName);
		this.model = (OWLModel) getKnowledgeBase();
		root = model.createOWLNamedClass(UUID.randomUUID().toString().replace("-", "_"));
	}
	
	@Override
	public void logout() {
	    if (doCleanup) {
	        root.delete();
	        for (OWLObjectProperty p : createdProperties) {
	            p.delete();
	        }
	    }
	    super.logout();
	}

	@Override
	public void run() {
		newClassAndProperty();
		changeDefined();
		if (waitBetweenWrites != 0) {
		    try {
		        Thread.sleep(waitBetweenWrites);
		    }
		    catch (InterruptedException ie) {
		        logger.log(Level.WARNING, "shouldn't", ie);
		    }
		}
	}
	
	private void newClassAndProperty() {
		OWLNamedClass c = null;
		while (c == null) {
			try {
				c  = model.createOWLNamedClass(classPrefix + classCounter++);
			}
			catch (Throwable t) {
				logger.log(Level.INFO, "create class failed", t);
				randomSleep();
			}
		}

		c.addSuperclass(root);
		c.removeSuperclass(model.getOWLThingClass());
		
		OWLObjectProperty p = null;
		while (p == null) {
			try {
				p = model.createOWLObjectProperty(propertyPrefix + propertyCounter++);
			}
			catch (Throwable t) {
				logger.log(Level.INFO, "property create failed", t);
				randomSleep();
			}
		}
		createdProperties.add(p);
		boolean success = false;
		while (!success) {
			try {
			    OWLNamedClass c1 = nextRandomClass();
			    OWLObjectProperty p2 = chooseObjectProperty(model);
			    OWLNamedClass c2 = chooseClass(model);
				model.beginTransaction("define a class");
				List<OWLClass> disjuncts = new ArrayList<OWLClass>();
				disjuncts.add(c1);
				disjuncts.add(model.createOWLSomeValuesFrom(p2, c2));
				c.addEquivalentClass(model.createOWLUnionClass(disjuncts));
				model.commitTransaction();
				success = true;
			}
			catch (Throwable t) {
				model.rollbackTransaction();
				logger.log(Level.INFO, "could not give class a definition", t);
				randomSleep();	
			}
		}
	}
	
	private void changeDefined() {
		boolean success = false;
		OWLNamedClass c = nextRandomClass();
		if (c.getDefinition() != null) {
			RDFSClass definition = c.getDefinition();
			while (!success) {
				try {
					model.beginTransaction("Definition to necessary condition");
					c.addSuperclass(definition.createClone());
					c.removeEquivalentClass(definition);
					model.commitTransaction();
					success = true;
				}
				catch (Throwable t) {
					model.rollbackTransaction();
					logger.log(Level.INFO, "could not give class a definition", t);
					randomSleep();
				}
			}
		}
		else {
			while (!success) {
				try {
					model.beginTransaction("Necessary condition to definition");
					RDFSClass superClass = null;
					for (Object o : c.getSuperclasses(false)) {
						if (o instanceof RDFSClass && !(o instanceof OWLNamedClass)) {
							superClass = (RDFSClass) o;
						}		
					}
					c.addEquivalentClass(superClass.createClone());
					c.removeSuperclass(superClass);
					model.commitTransaction();
					success = true;
				}
				catch (Throwable t) {
					model.rollbackTransaction();
					logger.log(Level.INFO, "could not give class a definition", t);
					randomSleep();
				}
			}
		}
	}
	
	private OWLNamedClass nextRandomClass() {
		return model.getOWLNamedClass(classPrefix + r.nextInt(classCounter));
	}
	
	
	private void randomSleep() {
		long period = r.nextInt(1000);
		try {
			logger.info("Collision found - sleeping for " + period);
			Thread.sleep(period);
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

}
