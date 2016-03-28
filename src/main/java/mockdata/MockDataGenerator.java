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

    static final String CREATE_TABLE_SQL =
        "    CREATE TABLE mock (\n"
        +"      handle INT NOT NULL\n"
        +"      ,range_low <type> NOT NULL\n"
        +"      ,range_high <type> NOT NULL\n"
        +"      ,stuff TEXT\n"
        +"    );"
    ;  
    
    static private final String USAGE = "<app> [<option> ...]";
    static private final String HEADER = "Generates mock data.\nOptions are:";
    static private final String FOOTER = ""
            +"\nGenerates test data for"
            +"\n" + CREATE_TABLE_SQL
            +"\nThat gets queried with WHERE clauses like"
            +"\n  WHERE handle_id = target_id AND range_high >= target_low AND range_low <= target_high"
            +"\n"
            +"\n(I.e., queries are for rows with a specific handle_id whose range overlaps a target range)."
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
        OPTIONS.addOption("h","help",false,
                "Print this message.");
	OPTIONS.addOption("v","verbose",false,
                "Turn on verbose output.");
        OPTIONS.addOption("n","nrows",true,
                "Number of rows to generate. ["+NROWS_DEFAULT+"]");
        OPTIONS.addOption(null,"handles",true,
                "Handles range from 1 to this. ["+HANDLES_DEFAULT+"]");
        OPTIONS.addOption(null,"lowest",true,
                "Lowest range_low value. ["+LOWEST_DEFAULT+"]");
        OPTIONS.addOption(null,"highest",true,
                "Highest range_low value. ["+HIGHEST_DEFAULT+"]");
        OPTIONS.addOption(null,"mean-width",true,
                "Width mean value. ["+MEAN_WIDTH_DEFAULT+"]");
        OPTIONS.addOption(null,"stddev-width", true, 
                "Width standard deviation. ["+STDDEV_WIDTH_DEFAULT+"]");
        OPTIONS.addOption(null,"sql", false, 
                "Generate SQL statements instead of CSV values."
                +" The generated SQL will be an \"INSERT INTO ...\" statement unless the -create-table option is specified."
                +" When -create-table is specified the generated SQL will be a \"COPY ... FROM stdin\" statement.");
        OPTIONS.addOption(null,"create-table", false, 
                "Implies SQL. See discussion with the -sql option.");
        //OPTIONS.addOption(null,"type", true, 
        //        "Data type for the range endpoint. [INT]");
        OPTIONS.addOption(null,"seed", true,
                "Seed value for the random number generator.");
    }
    
    static public void main(String[] args) {
        try {
            CommandLine cmdline = (new DefaultParser()).parse(OPTIONS,args);
            if (cmdline.hasOption("help")) {
                (new HelpFormatter()).printHelp(USAGE,HEADER,OPTIONS,FOOTER,false);
                System.exit(1);
            }
            
            if (cmdline.hasOption("seed")) {
                
            }
        
            MockDataGenerator application = new MockDataGenerator(cmdline.hasOption("verbose"),
                    cmdline.hasOption("sql") || cmdline.hasOption("create-table"),
                    cmdline.hasOption("create-table"),
                    Integer.parseInt(cmdline.getOptionValue("nrows",NROWS_DEFAULT)),
                    Integer.parseInt(cmdline.getOptionValue("handles",HANDLES_DEFAULT)),
                    Integer.parseInt(cmdline.getOptionValue("earliest",LOWEST_DEFAULT)),
                    Integer.parseInt(cmdline.getOptionValue("latest",HIGHEST_DEFAULT)),
                    Integer.parseInt(cmdline.getOptionValue("mean-width",MEAN_WIDTH_DEFAULT)),
                    Integer.parseInt(cmdline.getOptionValue("stddev-width",STDDEV_WIDTH_DEFAULT)),
                    cmdline.getOptionValue("type","INT")
                    );
            application.run();
        } catch (ParseException ex) {
            // can't use logger; it's not configured
            System.err.println(ex.getMessage());
            (new HelpFormatter()).printHelp(USAGE,HEADER,OPTIONS,FOOTER,false);
        }
    }

    private final boolean verbose;
    private final boolean sql;
    private final boolean createTable;
    private final int nrows;
    private final int n_handles;
    private final int earliest;
    private final int latest;
    private final int mean;
    private final int stddev;
    private final String range_type;
    
    public MockDataGenerator(boolean verbose, boolean sql, boolean createTable, int nrows, int n_handles, int earliest, int latest, int mean, int stddev, String range_type) {
        this.verbose = verbose;
        this.sql = sql;
        this.createTable = createTable;
        this.nrows = nrows;
        this.n_handles = n_handles;
        this.earliest = earliest;
        this.latest = latest;
        this.mean = mean;
        this.stddev = stddev;
        this.range_type = range_type;
    }

    @Override
    public void run() {
        
        if (verbose) {
            System.out.println("-- Generating nrows of mock data with handles madsets");
        }
        
        if (createTable) {
            System.out.println(CREATE_TABLE_SQL);
        }
        
        String comma = "";
        
        if (createTable) {
            System.out.println("COPY mock (handle, lowest, highest, stuff) FROM stdin;");
        } else if (sql) {
            System.out.println("INSERT INTO mock (handle, lowest, highest, stuff) VALUES ");
        }
        
        Random rand = new Random();
        for (int i=0; i < nrows; ++i) {
            int h = rand.nextInt(n_handles-1)+1;
            int e = rand.nextInt(latest);
            int d = (int) (rand.nextGaussian() * stddev + mean);
            if (d < 0) d = 0;
            String lowest;
            String highest;
            String stuff = "'"+i+":"+d+"'";
            switch (range_type) {
                case "timestamp":
                    lowest = "to_timestamp(" + e + ")";
                    highest = "to_timestamp(" + (e+d) + ")";
                    break;
                default:
                    lowest =  String.valueOf(e);
                    highest = String.valueOf(e+d);
                    break;
            }
            if (createTable) {
                System.out.println(h + "\t" + lowest + "\t" + highest + "\t" + stuff);
            } else if (sql) {
                System.out.println(comma + "(" + h + "," + lowest + "," + highest + "," + stuff + ")");
                comma = ",";
            } else {
                System.out.println(h + "," + lowest + "," + highest + "," + stuff);
            }
        }
        
        if (createTable) {
            System.out.println("\\.\n");
        }
    }
}
