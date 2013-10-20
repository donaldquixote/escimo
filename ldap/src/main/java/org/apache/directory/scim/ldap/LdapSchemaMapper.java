/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.scim.ldap;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.scim.AttributeHandler;
import org.apache.directory.scim.SchemaMapper;
import org.apache.directory.scim.ldap.schema.ComplexType;
import org.apache.directory.scim.ldap.schema.GroupSchema;
import org.apache.directory.scim.ldap.schema.MultiValType;
import org.apache.directory.scim.ldap.schema.ResourceSchema;
import org.apache.directory.scim.ldap.schema.SimpleType;
import org.apache.directory.scim.ldap.schema.SimpleTypeGroup;
import org.apache.directory.scim.ldap.schema.UserSchema;
import org.apache.directory.scim.schema.JsonSchema;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TODO LdapSchemaMapper.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapSchemaMapper implements SchemaMapper
{
    private SchemaManager ldapSchema;

    private static final Logger LOG = LoggerFactory.getLogger( LdapSchemaMapper.class );

    private GroupSchema groupSchema;

    private UserSchema userSchema;

    private Map<String,JsonSchema> jsonSchemas;
    
    public LdapSchemaMapper( Map<String,JsonSchema> jsonSchemas, SchemaManager ldapSchema )
    {
        this.jsonSchemas = jsonSchemas;
        this.ldapSchema = ldapSchema;
    }


    /**
     * @return the userSchema
     */
    public UserSchema getUserSchema()
    {
        return userSchema;
    }


    public GroupSchema getGroupSchema()
    {
        return groupSchema;
    }


    /**
     * @return the ldapSchema
     */
    public SchemaManager getLdapSchema()
    {
        return ldapSchema;
    }

    
    public AttributeType getLdapAttributeType( String name )
    {
        return ldapSchema.getAttributeType( name );
    }

    
    public void loadMappings()
    {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream( "escimo-ldap-mapping.xml" );
        loadMappings( in );
    }


    public void loadMappings( InputStream in )
    {
        if ( in == null )
        {
            throw new IllegalArgumentException( "Mapping file inputstream cannot be null" );
        }

        BufferedReader r = new BufferedReader( new InputStreamReader( in ) );

        try
        {
            StringBuilder sb = new StringBuilder();
            String s = null;

            while ( ( s = r.readLine() ) != null )
            {
                sb.append( s )
                    .append( "\n" );
            }

            Document doc = DocumentHelper.parseText( sb.toString() );

            // the entities element
            Element root = doc.getRootElement();
            if ( root.elements().isEmpty() )
            {
                throw new IllegalStateException( "Invalid schema mapping file" );
            }

            Element elmUser = root.element( "userType" );

            String baseDn = elmUser.attributeValue( "baseDn" );
            String filter = elmUser.attributeValue( "filter" );

            Map<String, AttributeHandler> atHandlersMap = loadAtHandlers( root.element( "atHandlers" ) );
            
            userSchema = new UserSchema( baseDn, filter );
            userSchema.setAtHandlers( atHandlersMap );

            List<Element> lstSchema = root.elements( "schema" );
            
            parseResourceSchema( elmUser, lstSchema, userSchema );

            Element elmGroup = root.element( "groupType" );
            String groupBaseDn = elmGroup.attributeValue( "baseDn" );
            String groupFilter = elmGroup.attributeValue( "filter" );

            groupSchema = new GroupSchema( groupBaseDn, groupFilter );
            groupSchema.setAtHandlers( atHandlersMap );
            parseResourceSchema( elmGroup, lstSchema, groupSchema );
        }
        catch ( Exception e )
        {
            LOG.warn( "Failed to load the schema mappings", e );
            throw new RuntimeException( e );
        }
        finally
        {
            if ( r != null )
            {
                try
                {
                    r.close();
                }
                catch ( IOException e )
                {
                    LOG.warn( "Failed to close the inputstream of the schema mapping file", e );
                }
            }
        }
    }


    private void parseResourceSchema( Element elmResourceSchema, List<Element> lstSchema, ResourceSchema resourceSchema )
    {
        Element elmObjectClass = elmResourceSchema.element( "objectClasses" );
        List<Element> elmOcs = elmObjectClass.elements( "objectClass" );
        for( Element el : elmOcs )
        {
            resourceSchema.addObjectClass( el.getText() );
        }
        
        List<Element> lstRef = elmResourceSchema.elements( "schemaRef" );
        
        for ( Element ref : lstRef )
        {
            String refId = ref.attributeValue( "id" );
            for ( Element elmSchema : lstSchema )
            {
                String schemaId = elmSchema.attributeValue( "id" );
                if ( refId.equals( schemaId ) )
                {
                    parseSchema( elmSchema, resourceSchema );
                    break;
                }
            }
        }

    }


    private void parseSchema( Element schemaRoot, ResourceSchema resourceSchema )
    {
        String uri = schemaRoot.attributeValue( "uri" );

        resourceSchema.addUri( uri );

        JsonSchema json = jsonSchemas.get( uri );
        
        List<Element> simpleAtElmList = schemaRoot.elements( "attribute" );

        for ( Element el : simpleAtElmList )
        {
            SimpleType st = parseSimpleType( el, uri );
            if ( st != null )
            {
                st.setReadOnly( json.isReadOnly( st.getName() ) );
                resourceSchema.addAttributeType( st.getName(), st );
            }
        }

        // load complex-attributes
        List<Element> complexAtElmList = schemaRoot.elements( "complex-attribute" );

        for ( Element elmComplex : complexAtElmList )
        {
            String name = elmComplex.attributeValue( "name" );

            if ( Strings.isEmpty( name ) )
            {
                throw new IllegalStateException( "name is missing in the complex-attribute configuration element "
                    + elmComplex.asXML() );
            }

            boolean show = getShowVal( elmComplex );
            
            Element atGrpElm = elmComplex.element( "at-group" );
            SimpleTypeGroup stg = parseAtGroup( atGrpElm, uri, name );
            ComplexType ct = null;
            if ( stg != null )
            {
                ct = new ComplexType( uri, name, show, stg );
            }
            
            String handlerRef = elmComplex.attributeValue( "handlerRef" );
            
            if( Strings.isEmpty( handlerRef ) ) 
            {
                handlerRef = null;
            }
            
            // if attribute handler is present then create the type
            if( ( ct == null ) && ( handlerRef != null ) )
            {
                ct = new ComplexType( uri, name, show, null );
            }
            
            if( ct != null )
            {
                ct.setAtHandlerName( handlerRef );
                ct.setReadOnly( json.isReadOnly( name ) );
                resourceSchema.addAttributeType( name, ct );
            }
        }

        // load multival-attributes
        List<Element> multivalAtElmList = schemaRoot.elements( "multival-attribute" );

        for ( Element elmMultiVal : multivalAtElmList )
        {
            String name = elmMultiVal.attributeValue( "name" );

            if ( Strings.isEmpty( name ) )
            {
                throw new IllegalStateException( "name is missing in the multival-attribute configuration element "
                    + elmMultiVal.asXML() );
            }

            String baseDn = elmMultiVal.attributeValue( "baseDn" );
            String filter = elmMultiVal.attributeValue( "filter" );

            boolean showMultiVal = getShowVal( elmMultiVal );
            
            MultiValType mt = null;
            
            Element elmAtGroup = elmMultiVal.element( "at-group" );
            if ( elmAtGroup != null )
            {
                SimpleTypeGroup stg = parseAtGroup( elmAtGroup, uri, name );
                if ( stg != null )
                {
                    mt = new MultiValType( uri, name, showMultiVal, stg, baseDn, filter );
                }

            }
            
            String handlerRef = elmMultiVal.attributeValue( "handlerRef" );
            
            if( Strings.isEmpty( handlerRef ) ) 
            {
                handlerRef = null;
            }

            if( ( mt == null ) && ( handlerRef != null ) )
            {
                mt = new MultiValType( uri, name, showMultiVal, ( SimpleTypeGroup ) null, baseDn, filter );
            }
            
            if( mt != null )
            {
                mt.setAtHandlerName( handlerRef );
                mt.setReadOnly( json.isReadOnly( name ) );
                resourceSchema.addAttributeType( name, mt );
            }
        }
    }


    private SimpleTypeGroup parseAtGroup( Element elmAtGroup, String uri, String parentAtName )
    {
        SimpleTypeGroup stg = null;

        List<SimpleType> lstSTypes = null;
        
        JsonSchema json = jsonSchemas.get( uri );
        
        if ( elmAtGroup != null )
        {
            lstSTypes = new ArrayList<SimpleType>();

            List<Element> elmSTypes = elmAtGroup.elements( "attribute" );
            for ( Element elmAt : elmSTypes )
            {
                SimpleType st = parseSimpleType( elmAt, uri );
                if ( st != null )
                {
                    st.setReadOnly( json.isReadOnly( parentAtName + "." + st.getName() ) );
                    lstSTypes.add( st );
                }
            }

            String format = null;

            Element elmFormat = elmAtGroup.element( "formatted" );
            if ( elmFormat != null )
            {
                format = elmFormat.attributeValue( "format" );
            }

            if ( !lstSTypes.isEmpty() )
            {
                stg = new SimpleTypeGroup( lstSTypes, format );
            }
        }

        return stg;
    }


    private SimpleType parseSimpleType( Element el, String uri )
    {
        String name = el.attributeValue( "name" );

        if ( Strings.isEmpty( name ) )
        {
            throw new IllegalStateException( "name is missing in the attribute configuration element " + el.asXML() );
        }

        String mappedTo = el.attributeValue( "mappedTo" );
        String handlerRef = el.attributeValue( "handlerRef" );

        if ( Strings.isEmpty( mappedTo ) && Strings.isEmpty( handlerRef ) )
        {
            LOG.debug( "Neither LDAP attribute or a attribute handler was mapped to the SCIM attribute {}, skipping", name );
            return null;
        }

        boolean show = getShowVal( el );

        SimpleType st = new SimpleType( uri, name, show, mappedTo );
        st.setAtHandlerName( handlerRef );
        
        return st;
    }
    
    private boolean getShowVal(Element el)
    {
        String showVal = el.attributeValue( "show" );

        if ( Strings.isEmpty( showVal ) )
        {
            showVal = "true";
        }
        
        return Boolean.parseBoolean( showVal );
    }
    
    
    private Map<String, AttributeHandler> loadAtHandlers(Element atHndlrRoot)
    {
        if( atHndlrRoot == null )
        {
            return Collections.EMPTY_MAP;
        }
        
        Map<String, AttributeHandler> mapHandlers = new HashMap<String, AttributeHandler>();
        
        List<Element> elmHandlerList = atHndlrRoot.elements( "handler" );
        
        for( Element el : elmHandlerList )
        {
            String fqcn = el.attributeValue( "class" );
            String name = el.attributeValue( "name" );
            
            if( Strings.isEmpty( name ) )
            {
                throw new IllegalStateException( "Name is missing in the handler element " + el.asXML() );
            }
            
            try
            {
                AttributeHandler handler = (AttributeHandler ) Class.forName( fqcn ).newInstance();
                mapHandlers.put( name, handler );
            }
            catch( Exception e )
            {
                throw new RuntimeException( "Failed to load the attribute handler " + fqcn, e );
            }
        }
        
        return mapHandlers;
    }
    
}
