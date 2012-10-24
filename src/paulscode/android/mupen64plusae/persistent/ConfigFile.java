package paulscode.android.mupen64plusae.persistent;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Set;

import android.util.Log;

/**
 * The Config class is used to load the parameters from a config file.
 * The file must follow a specific syntax:
 * Parameters are assigned to values with the equal sign (ex: param=value  ).
 * Parameters are allowed to have empty assignments (ex: param=  ).
 * Each parameter=value pair must be on a single line.
 * Values may be enclosed in double-quotes (optional)  (ex:  param="value"  ).
 * All parameters must be after a section title.
 * Section titles are enclosed in brackets (ex. "[SECTION]")
 * Comments consist of single lines, and begin with # or ; or // (ex:  ;comment  ).
 * Leading and trailing whitespace in lines, param names, and values is discarded.
 * Whitespace inside brackets or double-quotes is not discarded.
 *
 * @author: Paul Lamb
 * 
 * http://www.paulscode.com
 * 
 */
public class ConfigFile
{
    private String mFilename;  // Name of the config file.
    private HashMap<String, ConfigSection> mConfigMap;  // Sections mapped by title for easy lookup
    private LinkedList<ConfigSection> mConfigList;      // Sections in the proper order for easy saving

    /**
     * Constructor: Reads the entire config file, and saves the data in 'configMap'
     * @param filename The config file to read from.
     */
    public ConfigFile( String filename )  // Reads the entire config file and saves it in configMap
    {
        this.mFilename = filename;
        load( filename );
    }

    /**
     * Looks up a config section that matches the specified regex (not necessarily the only match)
     * @param regex A regular expression to match a section title from.
     * @return ConfigSection containing parameters for a config section that matches, or null if no matches were found.
     */
    public ConfigSection match( String regex )
    {
        String sectionTitle;
        Set<String> keys = mConfigMap.keySet();
        Iterator<String> iter = keys.iterator();
        
        if( mConfigMap == null )
            return null;  // No configuration to look up.. quit
        
        while( iter.hasNext() )
        {
            sectionTitle = iter.next();
            if( sectionTitle.matches( regex ) )
                return mConfigMap.get( sectionTitle );
        }
        return null;
    }

    /**
     * Looks up a config section by its title
     * @param sectionTitle Title of the section containing the parameter.
     * @return ConfigSection containing parameters, or null if not found.
     */
    public ConfigSection get( String sectionTitle )
    {
        if( mConfigMap == null )
            return null;  // No configuration to look up.. quit
        
        return mConfigMap.get( sectionTitle );
    }

    /**
     * Looks up the specified parameter under the specified section title
     * @param sectionTitle Title of the section containing the parameter.
     * @param parameter Name of the parameter.
     * @return Value of the parameter, or null if not found.
     */
    public String get( String sectionTitle, String parameter )
    {
        if( mConfigMap == null )
            return null;  // No configuration to look up.. quit
        
        ConfigSection section = mConfigMap.get( sectionTitle );
        if( section == null || section.parameters == null )
            return null;  // The specified section doesn't exist or is empty.. quit
        
        ConfigParameter confParam = section.parameters.get( parameter );
        if( confParam == null )
            return null;  // The specified parameter doesn't exist.. quit
        
        return confParam.value;  // got it
    }

    /**
     * Assigns the specified value to the specified parameter under the
     * specified section.
     * @param sectionTitle Title of the section to contain the parameter.
     * @param parameter Name of the parameter.
     * @param value value to give the parameter.
     */
    public void put( String sectionTitle, String parameter, String value )
    {
        if( mConfigMap == null )
            return;  // No configuration to look up.. quit
        
        ConfigSection section = mConfigMap.get( sectionTitle );
        if( section == null )
        {  
            // Add a new section
            section = new ConfigSection( sectionTitle );
            mConfigMap.put( sectionTitle, section );
            mConfigList.add( section );
        }
        section.put( parameter, value );
    }

    /**
     * Erases any previously loaded data.
     */
    public void clear()
    {
        if( mConfigMap != null )
            mConfigMap.clear();
        
        if( mConfigList != null )
            mConfigList.clear();  // Ready to start fresh
    }

    /**
     * Reads the entire config file, and saves the data in 'configMap'
     * @param filename The config file to read from.
     * @return True if successful.
     */
    @SuppressWarnings("unused")
    public boolean load( String filename )
    {   
        clear();  // Free any previously loaded data
        if( mConfigMap == null )  // Create the configMap if it hasn't been already
            mConfigMap = new HashMap<String, ConfigSection>();
        
        if( mConfigList == null )  // Create the configList if it hasn't been already
            mConfigList = new LinkedList<ConfigSection>();
        
        if( filename == null || filename.length() < 1 )
            return false;   // Make sure a file was actually specified

        FileInputStream fstream = null;
        try
        {
            fstream = new FileInputStream( filename );
        }
        catch( FileNotFoundException fnfe )
        {
            return false;  // File not found.. we can't continue
        }
        
        if( fstream == null )
            return false;  // Some problem, just quit
        
        DataInputStream in = new DataInputStream( fstream );
        BufferedReader br = new BufferedReader( new InputStreamReader( in ) );

        String sectionName = "[<sectionless!>]";
        ConfigSection section = new ConfigSection( sectionName, br ); // Read the 'sectionless' section
        mConfigMap.put( sectionName, section );   // Save the data to 'configMap'
        mConfigList.add( section );  // add it to the list as well
        
        while( section.nextName != null && section.nextName.length() > 0 )
        {   // Loop through reading the remaining sections
            sectionName = section.nextName;  // get the next section name
            if( sectionName != null && sectionName.length() > 0 )
            {   // load the next section
                section = new ConfigSection( sectionName, br );
                mConfigMap.put( sectionName, section );  // save the data to 'configMap'
                mConfigList.add( section );  // add it to the list as well
            }
        }
        try
        {
            in.close();  // Finished.  Close the file.
        }
        catch( IOException ioe )
        {}  // (don't care)
        return true;  // Success
    }
    
    /**
     * Saves the data from 'configMap' back to the config file.
     * @return True if successful.
     */
    public boolean save()
    {
        if( mFilename == null || mFilename.length() < 1 )
        {   // No filename was specified.
            Log.e( "Config", "Filename not specified in method save()" );
            return false;   // quit
        }
        
        if( mConfigList == null )
        {  // No config data to save.
            Log.e( "Config", "No config data to save in method save()" );
            return false;   // quit
        }
        
        File f = new File( mFilename );
        if( f.exists() )
        {   // Delete it if it already exists.
            if( !f.delete() )
            {   // Some problem deleting the file.
                Log.e( "Config", "Error deleting file " + mFilename );
                return false;   // quit
            }
        }
        
        try
        {
            FileWriter fw = new FileWriter( mFilename );  // For writing to the config file
            ListIterator<ConfigSection> iter = mConfigList.listIterator( 0 );
            ConfigSection section;

            while( iter.hasNext() )
            {   // Loop through the sections
                section = iter.next();
                if( section != null )
                    section.save( fw );
            }

            fw.flush();
            fw.close();
        }
        catch( IOException ioe )
        {
            Log.e( "Config", "IOException creating file " + mFilename + ", error message: " + ioe.getMessage() );
            return false;  // Some problem creating the file.. quit
        }
        return true;  // Success
    }

    /**
     * Returns a handle to the configList iterator.
     * @return ListIterator for accessing all sections in order.
     */
    public ListIterator<ConfigSection> listIterator()
    {
        return mConfigList.listIterator();
    }
    
    /**
     * Returns a handle to the configMap keyset.
     * @return keyset containing all the config section titles.
     */
    public Set<String> keySet()
    {
         return mConfigMap.keySet();
    }
    
    /**
     * The ConfigParameter class associates a parameter with its value.
     */
    private static class ConfigParameter
    {
        public String parameter;
        public String value;
        
        /**
         * Constructor: Associate the parameter and value
         * @param parameter Parameter name.
         * @param value Parameter's value.
         */
        public ConfigParameter( String parameter, String value )
        {
            this.parameter = parameter;
            this.value = value;
        }
    }
    
    /**
     * The ConfigLine class stores each line of the config file (including comments).
     */
    private static class ConfigLine
    {
        public static final int LINE_GARBAGE = 0;  // Comment, whitespace, or blank line
        public static final int LINE_SECTION = 1;  // Section title
        public static final int LINE_PARAM = 2;    // Parameter=value pair

        public int lineType = 0;     // LINE_GARBAGE, LINE_SECTION, or LINE_PARAM
        public String strLine = "";  // Actual line from the config file
        public ConfigParameter confParam = null;  // Null unless this line has a parameter
        
        /**
         * Constructor: Saves the relevant information about the line.
         * @param type
         * @param line
         * @param param
         */
        public ConfigLine( int type, String line, ConfigParameter param )
        {
            lineType = type;
            strLine = line;
            confParam = param;
        }
        
        public void save( FileWriter fw ) throws IOException
        {
            int x;
            if( lineType == LINE_PARAM )
            {
                if( !strLine.contains( "=" ) || confParam == null )
                    return;  // This shouldn't happen
                x = strLine.indexOf( "=" );
                if( x < 1 )
                    return;  // This shouldn't happen either
                if( x < strLine.length() )
                    fw.write( strLine.substring( 0, x + 1 ) + confParam.value + "\n" );
            }
            else
            {
                fw.write( strLine );
            }
        }
    }

    /**
     * The ConfigSection class reads all the parameters in the next section of the config file.
     * Saves the name of the next section (or null if end of file or error).
     * Can also be used to add a new section to an existing configuration.
     */
    public static class ConfigSection
    {
        public String name;  // Section name
        private HashMap<String, ConfigParameter> parameters;  // Parameters sorted by name for easy lookup
        private LinkedList<ConfigLine> lines;  // All the lines in this section, including comments
        
        // Name of the next section, or null if there are no sections left to read in the file:
        public String nextName = null;

        /**
         * Constructor: Creates an empty config section
         * @param sectionName The section title.
         */
        public ConfigSection( String sectionName )
        {
            parameters = new HashMap<String, ConfigParameter>();
            lines = new LinkedList<ConfigLine>();
            if( sectionName != null && sectionName.length() > 0 && !sectionName.equals( "[<sectionless!>]" ) )
                lines.add( new ConfigLine( ConfigLine.LINE_SECTION, "[" + sectionName + "]\n", null ) );
            name = sectionName;
        }
        
        /**
         * Constructor: Reads the next section of the config file, and saves it in 'parameters'
         * @param sectionName The section title.
         * @param br Config file to read from.
         */
        public ConfigSection( String sectionName, BufferedReader br )
        {
            parameters = new HashMap<String, ConfigParameter>();
            lines = new LinkedList<ConfigLine>();

            if( sectionName != null && sectionName.length() > 0 && !sectionName.equals( "[<sectionless!>]" ) )
                lines.add( new ConfigLine( ConfigLine.LINE_SECTION, "[" + sectionName + "]\n", null ) );

            name = sectionName;
            if( br == null )
                return;   // No file to read from.  Quit.
            String fullLine, strLine, p, v;
            ConfigParameter confParam;
            int x, y;
            try
            {
                while( ( fullLine = br.readLine() ) != null )
                {
                    strLine = fullLine.trim();
                    if( (strLine.length() < 1) ||
                        (strLine.substring( 0, 1 ).equals( "#" )) ||
                        (strLine.substring( 0, 1 ).equals( ";" )) ||
                        ( (strLine.length() > 1) && (strLine.substring( 0, 2 ).equals( "//" )) ) )  //NOTE: isEmpty() not supported on some devices
                    {  // A comment or blank line.
                        lines.add( new ConfigLine( ConfigLine.LINE_GARBAGE, fullLine + "\n", null ) );
                    }
                    else if( strLine.contains( "=" ) )
                    {   // This should be a "parameter=value" pair:
                        x = strLine.indexOf( "=" );
                        if( x < 1 )
                            return;  // This shouldn't happen (bad syntax).  Quit.
                        if( x < (strLine.length() - 1) )
                        {
                            p = strLine.substring( 0, x ).trim();
                            if( p.length() < 1 )
                                return;  // This shouldn't happen (bad syntax).  Quit.
                            v = strLine.substring( x + 1, strLine.length() ).trim();
                            //v = v.replace( "\"", "" );  // I'm doing this later, so I can save back without losing them
                            
                            if( v.length() > 0 )
                            {  // Save the parameter=value pair
                                confParam = parameters.get( p );
                                if( confParam != null )
                                    confParam.value = v;
                                else
                                {
                                    confParam = new ConfigParameter( p, v );
                                    lines.add( new ConfigLine( ConfigLine.LINE_PARAM, fullLine + "\n", confParam ) );
                                    parameters.put( p, confParam );  // Save the pair.
                                }
                            }
                        }  // It's ok to have an empty assignment (such as "param=")
                    }
                    else if( strLine.contains( "[" ) )
                    {   // This should be the beginning of the next section
                        if( (strLine.length() < 3) || (!strLine.contains( "]" )) )
                            return;   // This shouldn't happen (bad syntax).  Quit.
                        x = strLine.indexOf( "[" );
                        y = strLine.indexOf( "]" );
                        if( (y <= x + 1) || (x == -1) || (y == -1) )
                            return;  // This shouldn't happen (bad syntax).  Quit.
                        p = strLine.substring( x + 1, y ).trim();
                        if( p == null || p.length() < 1 )
                            return;  // This shouldn't happen (bad syntax).  Quit.
                        nextName = p;  // Save the name of the next section.
                        return;  // Done reading parameters.  Return.
                    }
                    else
                    {   // This shouldn't happen (bad syntax).  Quit.
                        return;
                    }
                }
            }
            catch( IOException ioe )
            {}
            return;  // Reached end of file or error.. either way, just quit
        }
        
        /**
         * Returns a handle to the parameter keyset.
         * @return keyset containing all the parameters.
         */
        public Set<String> keySet()
        {
            return parameters.keySet();
        }
        
        /**
         * Returns the value of the specified parameter.
         * @param parameter Name of the parameter.
         * @return Parameter's value, or null if not found.
         */
        public String get( String parameter )
        {
            if( parameters == null || parameter == null || parameter.length() < 1 )
                return null;  // Error: no parameters, or parameter was null
            ConfigParameter confParam = parameters.get( parameter );
            if( confParam == null )
                return null;  // Parameter not found
            return confParam.value;  // Got it
        }
        
        /**
         * Adds the specified parameter to this config section, updates
         * the value if it already exists, or removes the parameter.
         * @param parameter Name of the parameter.
         * @param value Parameter's value, or null to remove.
         */
        public void put( String parameter, String value )
        {
            ConfigParameter confParam = parameters.get( parameter );
            if( confParam == null )  // New parameter
            {
                if( value != null && value.length() > 0 )
                {
                    confParam = new ConfigParameter( parameter, value );
                    lines.add( new ConfigLine( ConfigLine.LINE_PARAM, parameter + "=" + value + "\n", confParam ) );
                    parameters.put( parameter, confParam );
                }
            }
            else
            {
                // Change the parameter's value
                confParam.value = value;
            }
        }

        /**
         * Writes the entire section to file.
         * @param fw File to write to.
         * @throws IOException
         */
        public void save( FileWriter fw ) throws IOException
        {
            ConfigLine line;
            ListIterator<ConfigLine> iter = lines.listIterator( 0 );
            while( iter.hasNext() )
            {
                line = iter.next();
                if( line != null )
                    line.save( fw );
            }
        }
    }
}
