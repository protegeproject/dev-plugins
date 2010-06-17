package simulator;

import edu.stanford.smi.protege.model.KnowledgeBase;

public interface Robot {
	
	void login(String hostname, int port, String projectName);
	
	void run();
	
	void logout();
	
	String getProperty(String key);
	
	KnowledgeBase getKnowledgeBase();
}
