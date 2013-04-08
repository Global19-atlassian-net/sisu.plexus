/*******************************************************************************
 * Copyright (c) 2010, 2013 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stuart McCulloch (Sonatype, Inc.) - initial API and implementation
 *
 * Minimal facade required to be binary-compatible with legacy Plexus API
 *******************************************************************************/
package org.codehaus.plexus.component.configurator.converters.composite;

import java.util.Collection;

import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.AbstractConfigurationConverter;
import org.codehaus.plexus.component.configurator.converters.ConfigurationConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;

public abstract class AbstractCollectionConverter
    extends AbstractConfigurationConverter
{
    // ----------------------------------------------------------------------
    // Customizable methods
    // ----------------------------------------------------------------------

    protected abstract Collection<Object> instantiateCollection( final PlexusConfiguration configuration,
                                                                 final Class<?> type, final ClassLoader loader )
        throws ComponentConfigurationException;

    // ----------------------------------------------------------------------
    // Shared methods
    // ----------------------------------------------------------------------

    protected final Collection<Object> fromChildren( final ConverterLookup lookup,
                                                     final PlexusConfiguration configuration, final Class<?> type,
                                                     final Class<?> enclosingType, final ClassLoader loader,
                                                     final ExpressionEvaluator evaluator,
                                                     final ConfigurationListener listener, final Class<?> elementType )
        throws ComponentConfigurationException
    {
        final Collection<Object> elements = instantiateCollection( configuration, type, loader );
        for ( int i = 0, size = configuration.getChildCount(); i < size; i++ )
        {
            final PlexusConfiguration xml = configuration.getChild( i );
            final Class<?> childType = getChildType( xml, enclosingType, loader, elementType );
            final ConfigurationConverter converter = lookup.lookupConverterForType( childType );
            elements.add( converter.fromConfiguration( lookup, xml, childType, enclosingType, //
                                                       loader, evaluator, listener ) );
        }
        return elements;
    }

    protected final Class<?> getChildType( final PlexusConfiguration childConfiguration, final Class<?> enclosingType,
                                           final ClassLoader loader, final Class<?> elementType )
        throws ComponentConfigurationException
    {
        final String childName = fromXML( childConfiguration.getName() );
        Class<?> childType = getClassForImplementationHint( null, childConfiguration, loader );
        Throwable cause = null;

        if ( null == childType && childName.indexOf( '.' ) > 0 )
        {
            try
            {
                childType = loader.loadClass( childName );
            }
            catch ( final Exception e )
            {
                cause = e;
            }
            catch ( final LinkageError e )
            {
                cause = e;
            }
        }
        if ( null == childType )
        {
            try
            {
                childType = loader.loadClass( alignPackageName( enclosingType.getName(), childName ) );
            }
            catch ( final Exception e )
            {
                cause = e;
            }
            catch ( final LinkageError e )
            {
                cause = e;
            }
        }
        if ( null == childType )
        {
            if ( null != elementType && Object.class != elementType )
            {
                childType = elementType;
            }
            else if ( childConfiguration.getChildCount() == 0 )
            {
                childType = String.class;
            }
            else
            {
                throw new ComponentConfigurationException( "Cannot determine child type", cause );
            }
        }
        return childType;
    }

    protected final static PlexusConfiguration csvToXml( final PlexusConfiguration configuration, final String csv )
    {
        final PlexusConfiguration xml = new XmlPlexusConfiguration( configuration.getName() );
        if ( csv.length() > 0 )
        {
            for ( final String token : csv.split( ",", -1 ) )
            {
                xml.addChild( "#", token );
            }
        }
        return xml;
    }

    // ----------------------------------------------------------------------
    // Implementation methods
    // ----------------------------------------------------------------------

    private static String alignPackageName( final String enclosingName, final String childName )
    {
        final String pkgPrefix = enclosingName.substring( 0, enclosingName.lastIndexOf( '.' ) + 1 );
        final String clzSuffix = Character.toTitleCase( childName.charAt( 0 ) ) + childName.substring( 1 );
        return pkgPrefix + clzSuffix;
    }
}
