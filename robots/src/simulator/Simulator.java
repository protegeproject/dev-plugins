package simulator;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.logging.Logger;

import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protege.util.SystemUtilities;

public class Simulator {
    private static Logger log = Log.getLogger(Simulator.class);
    
    private String hostname;
    private int port;
    private String project;
    private long timeout1;
    private long timeout2;
    private boolean logResults = false;
    
    private List<Robot> robots;
    private Map<Class,List<Float>> results = new HashMap<Class, List<Float>>();
    private boolean done = false;
    
    public Simulator(String[] args) {
        hostname = args[0];
        port = Integer.parseInt(args[1]);
        project = args[2];
        timeout1 = Long.parseLong(args[3]);
        timeout2 = Long.parseLong(args[4]);
        log.info("Connecting to rmi://" + hostname + ":" + port + "/" + project);
        log.info("Starting measurements after " + timeout1 + " seconds");
        log.info("Terminating after " + timeout2 + " additional seconds");
    }
    
    public void run() {
        getRobots();
        for (Robot r : robots) {
            log.info("" + r + "/" + r.getProperty(AbstractRobot.CLASS_PROP) + 
                               " each measured cycle = " + getRunsPerMeasurement(r) + " runs.");
            r.login(hostname, port, project);
        }
        for (final Robot r : robots) {
            new Thread(new RunRobot(r)).start();
        }
        manageRobots();
    }

 
    public void getRobots() {
        robots = new ArrayList<Robot>();
        Class[] args = { Properties.class };
        File root = new File(".");
        File[] files = root.listFiles(new FileFilter() {

            public boolean accept(File pathname) {
                return pathname.getPath().endsWith(".properties");
            }

        });
        for (File f : files) {
            log.info("file = " + f);
            final Properties p = new Properties();
            try {
                p.load(new FileInputStream(f));
                int count = Integer.parseInt(p.getProperty(AbstractRobot.THREAD_COUNT_PROP));
                for (int i = 1; i <= count; i++) {
                    Class c = null;
                    try {
                        Object[] initargs = { p };
                        c = Class.forName(p.getProperty(AbstractRobot.CLASS_PROP));
                        Constructor construct = c.getConstructor(args);
                        robots.add((Robot) construct.newInstance(initargs));
                    }
                    catch (Exception e) {
                        log.info("Could not create robot of class " + c);
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                log.info("Could not load user with file " + f);
            }
        }
    }
    
    class RunRobot implements Runnable {
        Robot r;
        RunRobot(Robot r) {
            this.r = r;
        }
        
        public void run() {
            int normalize = getRunsPerMeasurement(r);
            long startTime;
            try {
                do {
                    startTime = System.currentTimeMillis();
                    for (int i = 1; i <= normalize; i++) {
                        try {
                            r.run();
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                    if (doLog()) {
                        float interval = ((float) (System.currentTimeMillis() - startTime))/((float) normalize);
                        addResult(r, interval);
                    }
                } while (!isDone());
            } catch (Throwable t) {
                t.printStackTrace();
            }
            finally {
                r.logout();
                log.info("" + r + " reporting in");
                synchronized (robots) {
                    robots.remove(r);
                    robots.notify();
                }
            }
        }
    }

    private void manageRobots() {
        synchronized (robots) {
            log.info("" +  robots.size() + " sessions running.");
        }
        try {
            if (timeout1 != 0) Thread.sleep(timeout1 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        synchronized (robots) {
            log.info("Starting timings of robots");
            logResults = true;
        }
        
        try {
            Thread.sleep(timeout2 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("Shutting down - waiting for robots to report in");
        synchronized (robots) {
            done = true;
            while (!robots.isEmpty()) {
                try {
                    robots.wait();
                }
                catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
        report();
    }
    
    
    private void addResult(Robot r, float interval) {
        synchronized (robots) {
            List<Float> intervals = results.get(r.getClass());
            if (intervals == null) {
                intervals = new ArrayList<Float>();
                results.put(r.getClass(), intervals);
            }	
            intervals.add(interval);
        }
    }
    
    private boolean isDone() {
        synchronized (robots) {
            return done;
        }
    }
    
    private boolean doLog() {
        synchronized (robots) {
            return logResults;
        }
    }
    
    private void report() {
        for (Entry<Class, List<Float>> entry : results.entrySet()) {
            Class c = entry.getKey();
            List<Float> intervals  = entry.getValue();
            if (intervals != null) {
                log.info("Results for robot class " + c);
                int count = 0;
                float total = 0;
                float totalsq = 0;
                float min = 0;
                float max = 0;
                for (float interval : intervals) {
                    count++;
                    if (count == 1) {
                        min = interval;
                        max = interval;
                    }
                    else {
                        if (interval < min) min = interval;
                        if (interval > max) max = interval;
                    }
                    total += interval;
                    totalsq += interval * interval;
                }
                log.info("\tmin interval = " + min);
                log.info("\tmax interval = " + max);
                log.info("\taverage interval = " + total / count);
                float std = (float) Math.sqrt((totalsq - total * total/count)/count);
                log.info("\tstd deviation = " + std);
                log.info("\tTotal runs/normalization = " + intervals.size());
            }
        }
    }
    
    private int getRunsPerMeasurement(Robot r) {
        return Integer.parseInt(r.getProperty(AbstractRobot.RUNS_PER_MEASUREMENT_PROP));
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        SystemUtilities.initialize();
        Simulator s = new Simulator(args);
        s.run();
    } 

}
