package com.lukegb.mojo.build;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileStatus;
import org.apache.maven.scm.ScmResult;
import org.apache.maven.scm.log.ScmLogDispatcher;
import org.apache.maven.scm.log.ScmLogger;
import org.codehaus.plexus.util.StringUtils;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;

/**
 * Goal which sets project properties for describer from the
 * current Git repository.
 * 
 * @author Luke Granger-Brown
 * @goal gitdescribe
 * @requiresProject
 * @since 1.0-beta-4
 */
public class GitDescribeMojo
    extends AbstractMojo
{

    private ScmLogDispatcher logger = new ScmLogDispatcher();

    /**
     * The maven project.
     * 
     * @parameter expression="${project}"
     * @readonly
     */
    private MavenProject project;

    /**
     * Local directory to be used to issue SCM actions
     * 
     * @parameter expression="${maven.changeSet.scmDirectory}" default-value="${basedir}
     * @since 1.0
     */
    private File scmDirectory;

    /**
     * String to append to git describe/shorttag output
     *
     * @parameter default-value=""
     */
    private String outputPostfix;

    /**
     * String to prepend to git describe/shorttag output
     *
     * @parameter default-value="git-"
     */
    private String outputPrefix;

    /**
     * String indicating full output if getting version fails
     *
     * @parameter default-value="unknown"
     */
    private String failOutput;

    private void checkResult( ScmResult result )
        throws MojoExecutionException
    {
        if ( !result.isSuccess() )
        {
            getLog().debug( "Provider message:" );
            getLog().debug( result.getProviderMessage() == null ? "" : result.getProviderMessage() );
            getLog().debug( "Command output:" );
            getLog().debug( result.getCommandOutput() == null ? "" : result.getCommandOutput() );
            throw new MojoExecutionException( "Command failed."
                + StringUtils.defaultString( result.getProviderMessage() ) );
        }
    }

    public void execute()
        throws MojoExecutionException
    {
        try
        {
            String previousDescribe = getDescribeProperty();
            if ( previousDescribe == null )
            {
                String describe = getDescriber();
                getLog().info( "Setting Git Describe: " + describe );
                setDescribeProperty( describe );
            }
        }
        catch ( ScmException e )
        {
            throw new MojoExecutionException( "SCM Exception", e );
        }
    }

    protected String getDescriber()
        throws ScmException, MojoExecutionException
    {
        if (outputPrefix == null) outputPrefix = "";
        if (outputPostfix == null) outputPostfix = "";
        // scmDirectory
        String command[] = {"git","describe"};
        String line = commandExecutor(command);
        if (line == null) {
            String commandtwo[] = {"git","log","--pretty=format:\"%h\""};
            line = commandExecutor(commandtwo);
            if (line == null) {
                line = failOutput;
            }
        }
        return outputPrefix + line + outputPostfix;
    }


    private String commandExecutor(String[] command)
    {
        try {
          Process p = new ProcessBuilder(command).directory(scmDirectory).start();
          InputStream is = p.getInputStream();
          InputStreamReader isr = new InputStreamReader(is);
          BufferedReader br = new BufferedReader(isr);
          String line;
          line = br.readLine();
          return line;
        } catch (Exception e) { return null; }
    }

    protected String getDescribeProperty()
    {
        return getProperty( "describer" );
    }

    protected String getProperty( String property )
    {
        return project.getProperties().getProperty( property );
    }

    private void setDescribeProperty( String describer )
    {
        setProperty( "describe", describer );
    }

    private void setProperty( String property, String value )
    {
        if ( value != null )
        {
            project.getProperties().put( property, value );
        }
    }

}
