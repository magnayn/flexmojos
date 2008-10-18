package info.rvin.flexmojos.utilities;

import info.flexmojos.utilities.ApplicationHandler;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Build;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import eu.cedarsoft.utils.ZipExtractor;

/**
 * Utility class to help get information from Maven objects like files, source paths, resolve dependencies, etc.
 * 
 * @author velo.br
 */
public class MavenUtils
{

    private static final String WINDOWS_OS = "windows";

    private static final String MAC_OS = "mac os x";

    private static final String MAC_OS_DARWIN = "darwin";

    private static final String LINUX_OS = "linux";

    private static final String VISTA = "vista";

    private MavenUtils()
    {
    }

    /**
     * Parse an MXML file and returns true if the file is an application one
     * 
     * @param file the file to be parsed
     * @return true if the file is an application one
     */
    public static boolean isApplicationFile( File file )
    {
        try
        {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            parser.getXMLReader().setFeature( "http://xml.org/sax/features/namespaces", true );
            parser.getXMLReader().setFeature( "http://xml.org/sax/features/namespace-prefixes", true );
            ApplicationHandler h = new ApplicationHandler();
            parser.parse( file, h );
            return h.isApplicationFile();
        }
        catch ( Exception e )
        {
            return false;
        }
    }

    /**
     * Resolve a source file in a maven project
     * 
     * @param project maven project
     * @param sourceFile sugested name on pom
     * @return source file or null if source not found
     */
    @SuppressWarnings( "unchecked" )
    public static File resolveSourceFile( MavenProject project, String sourceFile )
    {
        List<String> sourceRoots = project.getCompileSourceRoots();
        for ( String sourceRoot : sourceRoots )
        {
            File sourceDirectory = new File( sourceRoot );

            if ( sourceFile != null )
            {
                return new File( sourceDirectory, sourceFile );
            }
            else
            {
                File[] files = sourceDirectory.listFiles( new FileFilter()
                {
                    public boolean accept( File pathname )
                    {
                        return pathname.isFile()
                            && ( pathname.getName().endsWith( ".mxml" ) || pathname.getName().endsWith( ".as" ) );
                    }
                } );

                if ( files.length == 1 )
                {
                    return files[0];
                }
                if ( files.length > 1 )
                {
                    for ( File file : files )
                    {
                        if ( file.getName().equalsIgnoreCase( "Main.mxml" )
                            || file.getName().equalsIgnoreCase( "Main.as" ) )
                        {
                            return file;
                        }
                    }
                    for ( File file : files )
                    {
                        if ( file.getName().equalsIgnoreCase( "Index.mxml" )
                            || file.getName().equalsIgnoreCase( "Index.as" ) )
                        {
                            return file;
                        }
                    }

                    List<File> appFiles = new ArrayList<File>();
                    for ( File file : files )
                    {
                        if ( file.getName().endsWith( ".mxml" ) && isApplicationFile( file ) )
                        {
                            appFiles.add( file );
                        }
                    }
                    if ( appFiles.size() == 1 )
                    {
                        File file = appFiles.get( 0 );
                        return file;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Resolve a resource file in a maven project resources folders
     * 
     * @param project maven project
     * @param resourceFile sugested name on pom
     * @return source file or null if source not found
     */
    @SuppressWarnings( "unchecked" )
    public static File resolveResourceFile( MavenProject project, String resourceFile )
    {

        List<Resource> resources = project.getBuild().getResources();

        for ( Resource resourceFolder : resources )
        {
            File resource = new File( resourceFolder.getDirectory(), resourceFile );
            if ( resource.exists() )
            {
                return resource;
            }
        }

        return null;
    }

    /**
     * Get dependency artifacts for a project using the local and remote repositories to resolve the artifacts
     * 
     * @param project maven project
     * @param resolver artifact resolver
     * @param localRepository artifact repository
     * @param remoteRepositories List of remote repositories
     * @param artifactMetadataSource artifactMetadataSource
     * @return all dependencies from the project
     * @throws MojoExecutionException thrown if an exception occured during artifact resolving
     */
    @SuppressWarnings( "unchecked" )
    public static Set<Artifact> getDependencyArtifacts( MavenProject project, ArtifactResolver resolver,
                                                        ArtifactRepository localRepository, List remoteRepositories,
                                                        ArtifactMetadataSource artifactMetadataSource )
        throws MojoExecutionException
    {
        ArtifactResolutionResult arr;
        try
        {
            arr =
                resolver.resolveTransitively( project.getDependencyArtifacts(), project.getArtifact(),
                                              remoteRepositories, localRepository, artifactMetadataSource );
        }
        catch ( AbstractArtifactResolutionException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        Set<Artifact> result = arr.getArtifacts();
        return result;
    }

    /**
     * Get the file reference of an SWC artifact.<br>
     * If the artifact file does not exist in the [build-dir]/libraries/[scope] directory, the artifact file is copied
     * to that location.
     * 
     * @param a artifact for which to retrieve the file reference
     * @param scope scope of the library
     * @param build build for which to get the artifact
     * @return swc artifact file reference
     * @throws MojoExecutionException thrown if an IOException occurs while copying the file to the
     *             [build-dir]/libraries/[scope] directory
     */
    public static File getArtifactFile( Artifact a, Build build )
        throws MojoExecutionException
    {
        File dest = new File( build.getOutputDirectory(), "libraries/" + a.getArtifactId() + ".swc" );
        if ( !dest.exists() )
        {
            try
            {
                FileUtils.copyFile( a.getFile(), dest );
                dest.deleteOnExit();
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( e.getMessage(), e );
            }
        }
        return dest;
    }

    /**
     * Use the resolver to resolve the given artifact in the local or remote repositories.
     * 
     * @param artifact Artifact to be resolved
     * @param resolver ArtifactResolver to use for resolving the artifact
     * @param localRepository ArtifactRepository
     * @param remoteRepositories List of remote artifact repositories
     * @throws MojoExecutionException thrown if an exception occured during artifact resolving
     */
    @SuppressWarnings( "unchecked" )
    public static void resolveArtifact( Artifact artifact, ArtifactResolver resolver,
                                        ArtifactRepository localRepository, List remoteRepositories )
        throws MojoExecutionException
    {
        try
        {
            resolver.resolve( artifact, remoteRepositories, localRepository );
        }
        catch ( AbstractArtifactResolutionException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

    /**
     * Returns file reference to config.xml file. Copies the config file to the build directory.
     * 
     * @param build Build for which to get the config.xml file
     * @return file reference to config.xml file
     * @throws MojoExecutionException thrown if the config file could not be copied to the build directory
     */
    public static File getConfigFile( Build build )
        throws MojoExecutionException
    {
        URL url = MavenUtils.class.getResource( "/configs/config.xml" );
        File configFile = new File( build.getOutputDirectory(), "config.xml" );
        try
        {
            FileUtils.copyURLToFile( url, configFile );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error generating config file.", e );
        }
        return configFile;
    }

    /**
     * Returns the file reference to the fonts file. Depending on the os, the correct fonts.ser file is used. The fonts
     * file is copied to the build directory.
     * 
     * @param build Build for which to get the fonts file
     * @return file reference to fonts file
     * @throws MojoExecutionException thrown if the config file could not be copied to the build directory
     */
    public static File getFontsFile( Build build )
        throws MojoExecutionException
    {
        URL url;
        if ( MavenUtils.isMac() )
        {
            url = MavenUtils.class.getResource( "/fonts/macFonts.ser" );
        }
        else
        {
            // TODO And linux?!
            // if(os.contains("windows")) {
            url = MavenUtils.class.getResource( "/fonts/winFonts.ser" );
        }
        File fontsSer = new File( build.getOutputDirectory(), "fonts.ser" );
        try
        {
            FileUtils.copyURLToFile( url, fontsSer );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error copying fonts file.", e );
        }
        return fontsSer;
    }

    /**
     * Returns the file reference to a localize resourceBundlePath. Replaces the {locale} variable in the given
     * resourceBundlePath with given locale.
     * 
     * @param resourceBundlePath Path to resource bundle.
     * @param locale Locale
     * @throws MojoExecutionException thrown if the resourceBundlePath for given locale can not be found
     * @return File reference to the resourceBundlePath for given locale
     */
    public static File getLocaleResourcePath( String resourceBundlePath, String locale )
        throws MojoExecutionException
    {
        String path = resourceBundlePath.replace( "{locale}", locale );
        File localePath = new File( path );
        if ( !localePath.exists() )
        {
            throw new MojoExecutionException( "Unable to find locales path: " + path );
        }
        return localePath;
    }

    public static String osString()
    {
        return System.getProperty( "os.name" ).toLowerCase();
    }

    /**
     * Return a boolean to show if we are running on Windows.
     * 
     * @return true if we are running on Windows.
     */
    public static boolean isWindows()
    {
        return osString().startsWith( WINDOWS_OS );
    }

    /**
     * Return a boolean to show if we are running on Linux.
     * 
     * @return true if we are running on Linux.
     */
    public static boolean isLinux()
    {
        return osString().startsWith( LINUX_OS );
    }

    /**
     * Return a boolean to show if we are running on Mac OS X.
     * 
     * @return true if we are running on Mac OS X.
     */
    public static boolean isMac()
    {
        return osString().startsWith( MAC_OS ) || osString().startsWith( MAC_OS_DARWIN );
    }

    /**
     * Return a boolean to show if we are running on Windows Vista.
     * 
     * @return true if we are running on Windows Vista.
     */
    public static boolean isWindowsVista()
    {
        return osString().startsWith( WINDOWS_OS ) && osString().contains( VISTA );
    }

    public static Artifact searchFor( Collection<Artifact> artifacts, String groupId, String artifactId,
                                      String version, String type, String classifier )
    {
        for ( Artifact artifact : artifacts )
        {
            if ( equals( artifact.getGroupId(), groupId ) && equals( artifact.getArtifactId(), artifactId )
                && equals( artifact.getVersion(), version ) && equals( artifact.getType(), type )
                && equals( artifact.getClassifier(), classifier ) )
            {
                return artifact;
            }
        }

        return null;
    }

    private static boolean equals( String str1, String str2 )
    {
        // If is null is not relevant
        if ( str1 == null || str2 == null )
        {
            return true;
        }

        return str1.equals( str2 );
    }

    @SuppressWarnings( "unchecked" )
    public static Map<String, File> getFDKNamespaces( File configZip, Build build )
        throws MojoExecutionException
    {
        File outputFolder = new File( build.getOutputDirectory(), "configs" );
        outputFolder.mkdirs();

        ZipExtractor ze;
        try
        {
            ze = new ZipExtractor( configZip );
            ze.extract( outputFolder );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Unable to extract configurations", e );
        }

        Map<String, File> namespace = new LinkedHashMap<String, File>();
        File configFile = new File( outputFolder, "flex-config.xml" );
        if ( !configFile.isFile() )
        {
            return namespace;
        }

        SAXReader reader = new SAXReader();
        Document document;
        try
        {
            document = reader.read( configFile );
        }
        catch ( DocumentException e )
        {
            throw new MojoExecutionException( "Unable to parse config file " + configFile.getAbsolutePath(), e );
        }
        List<Node> list = document.selectNodes( "//compiler/namespaces/namespace" );

        for ( Node node : list )
        {
            String uri = node.valueOf( "//uri" );
            File manifest = new File( outputFolder, node.valueOf( "//manifest" ) );
            if ( !manifest.exists() )
            {
                throw new MojoExecutionException( "Namespace " + uri + " not found " + manifest.getAbsolutePath() );
            }
            namespace.put( uri, manifest );
        }

        return namespace;
    }
}