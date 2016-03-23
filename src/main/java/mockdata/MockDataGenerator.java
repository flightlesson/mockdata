package mockdata;

import java.io.File;
import java.util.Random;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * For performance-testing an SQL database.
 */
public class MockDataGenerator implements Runnable {
    static final Logger LOGGER = Logger.getLogger(MockDataGenerator.class.getName());

    static final String CREATE_TABLE_SQL =
        "    CREATE TABLE mock (\n"
        +"      handle INT NOT NULL\n"
        +"      ,lowest INT NOT NULL\n"
        +"      ,highest INT NOT NULL\n"
        +"      ,stuff TEXT\n"
        +"    );"
    ;  
    
    static private final String USAGE = "<app> [<option> ...]";
    static private final String HEADER = "Generates mock data.\nOptions are:";
    static private final String FOOTER = ""
            +"\nGenerates test data for"
            +"\n" + CREATE_TABLE_SQL
            +"\nThat gets queried with WHERE clauses like"
            +"\n  WHERE handle_id = ? AND earliest >= ? AND latest <= ?"
            +"\n"
            +"\nThe test data can be used to populate a database in order to test"
            +"various index and query schemes."
            +"\n"
            +"\nEach row has a random handle_id and earliest. A width is computed and used in "
            +"latest = earliest + width."
            +"\n"
            ;
    static private final Options OPTIONS;
    
    static final String NROWS_DEFAULT        = "1000000";
    static final String HANDLES_DEFAULT      =   "10000";
    static final String LOWEST_DEFAULT       =       "0";
    static final String HIGHEST_DEFAULT      = "1000000";
    static final String MEAN_WIDTH_DEFAULT   =   "10000";
    static final String STDDEV_WIDTH_DEFAULT =    "3000";
    
    static {
        OPTIONS = new Options();
        OPTIONS.addOption("h","help",false,"Print this message.");
	OPTIONS.addOption("v","verbose",false,"Turn on verbose output.");
        OPTIONS.addOption(null,"debug",false,"Set fallback log4j configurationlevel to DEBUG.");
        OPTIONS.addOption(null,"l4jconfig",true,"Path to the log4j configuration file. [./l4j.lcf]");
        OPTIONS.addOption("n","nrows",true,"Number of rows to generate. ["+NROWS_DEFAULT+"]");
        OPTIONS.addOption(null,"handles",true,"Handles range from 1 to this. ["+HANDLES_DEFAULT+"]");
        OPTIONS.addOption(null,"lowest",true,"Lowest range value. ["+LOWEST_DEFAULT+"]");
        OPTIONS.addOption(null,"highest",true,"Highest range value. ["+HIGHEST_DEFAULT+"]");
        OPTIONS.addOption(null,"mean-width",true,"Width mean value. ["+MEAN_WIDTH_DEFAULT+"]");
        OPTIONS.addOption(null,"stddev-width", true, "Width standard deviation. ["+STDDEV_WIDTH_DEFAULT+"]");
        OPTIONS.addOption(null,"create-table", false, "Implies SQL. Prepend output with 'CREATE TABLE ...'");
        OPTIONS.addOption(null,"sql", false, "Generate SQL statements instead of CSV values.");
        // Add application specific options here.
    }
    
    static public void main(String[] args) {
        try {
            CommandLine cmdline = (new DefaultParser()).parse(OPTIONS,args);
            if (cmdline.hasOption("help")) {
                (new HelpFormatter()).printHelp(USAGE,HEADER,OPTIONS,FOOTER,false);
                System.exit(1);
            }
            configureLog4j(cmdline.getOptionValue("l4jconfig","l4j.lcf"),cmdline.hasOption("debug"));
        
            MockDataGenerator application = new MockDataGenerator(cmdline.hasOption("verbose"),
                    cmdline.hasOption("sql") || cmdline.hasOption("create-table"),
                    cmdline.hasOption("create-table"),
                    Integer.parseInt(cmdline.getOptionValue("nrows",NROWS_DEFAULT)),
                    Integer.parseInt(cmdline.getOptionValue("handles",HANDLES_DEFAULT)),
                    Integer.parseInt(cmdline.getOptionValue("earliest",LOWEST_DEFAULT)),
                    Integer.parseInt(cmdline.getOptionValue("latest",HIGHEST_DEFAULT)),
                    Integer.parseInt(cmdline.getOptionValue("mean-width",MEAN_WIDTH_DEFAULT)),
                    Integer.parseInt(cmdline.getOptionValue("stddev-width",STDDEV_WIDTH_DEFAULT))
                    );
            application.run();
        } catch (ParseException ex) {
            // can't use logger; it's not configured
            System.err.println(ex.getMessage());
            (new HelpFormatter()).printHelp(USAGE,HEADER,OPTIONS,FOOTER,false);
        }
    }
    
    static void configureLog4j(String l4jconfig,boolean debug) {
        if ((new File(l4jconfig)).canRead()) {
            if (l4jconfig.matches(".*\\.xml$")) {
                DOMConfigurator.configureAndWatch(l4jconfig);
            } else {
                PropertyConfigurator.configureAndWatch(l4jconfig);
            }
        } else {
            BasicConfigurator.configure();
            LOGGER.setLevel(debug?Level.DEBUG:Level.INFO);
        }
    }

    private final boolean verbose;
    private final boolean sql;
    private final boolean createTable;
    private final int nrows;
    private final int madsets;
    private final int earliest;
    private final int latest;
    private final int mean;
    private final int stddev;
    
    public MockDataGenerator(boolean verbose, boolean sql, boolean createTable, int nrows, int madsets, int earliest, int latest, int mean, int stddev) {
        this.verbose = verbose;
        this.sql = sql;
        this.createTable = createTable;
        this.nrows = nrows;
        this.madsets = madsets;
        this.earliest = earliest;
        this.latest = latest;
        this.mean = mean;
        this.stddev = stddev;
    }

    @Override
    public void run() {
        
        if (createTable) {
            System.out.println(CREATE_TABLE_SQL);
        }
        
        String separator = ",";
        
        if (sql) {
            System.out.println("COPY mock (handle, lowest, highest, stuff) FROM stdin;");
            separator = "\t";
        }
        
        Random rand = new Random();
        for (int i=0; i < nrows; ++i) {
            int h = rand.nextInt(madsets-1)+1;
            int e = rand.nextInt(latest);
            int d = (int) (rand.nextGaussian() * stddev + mean);
            if (d < 0) d = 0;
            System.out.println(h + separator + e + separator + (e+d) + separator + "'" + i + ":" + d + "'");
        }
        
        if (sql) {
            System.out.println("\\.\n");
        }
    }
}
