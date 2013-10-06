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
package org.apache.directory.scim.schema;

import org.apache.directory.scim.json.ResourceSerializer;
import static org.apache.directory.scim.json.ResourceSerializer.*;


/**
 * TODO BaseType.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class BaseType
{
    private String uri;

    private String name;
    
    private boolean show = true;

    private String atHandlerName;
    
    private boolean readOnly = false;
    
    public BaseType( String uri, String name, boolean show )
    {
        this.uri = uri;
        this.name = name;
        this.show = show;
    }


    public String getName()
    {
        return name;
    }


    public boolean isShow()
    {
        return show;
    }


    public boolean isSimple()
    {
        return false;
    }


    public boolean isComplex()
    {
        return false;
    }


    public boolean isTyped()
    {
        return false;
    }

    public boolean isCoreAttribute()
    {
        return ( CORE_USER_URI.equals( uri ) || 
                 CORE_GROUP_URI.equals( uri ) ||
                 CORE_EXT_USER_URI.equals( uri ) );
    }

    /**
     * @return the uri
     */
    public String getUri()
    {
        return uri;
    }


    public void setAtHandlerName( String atHandlerName )
    {
        this.atHandlerName = atHandlerName;
    }


    public String getAtHandlerName()
    {
        return atHandlerName;
    }

    
    public boolean hasHandler()
    {
        return (atHandlerName != null);
    }
    

    public boolean isDynamic()
    {
        return false;
    }


    /**
     * @return the readOnly
     */
    public boolean isReadOnly()
    {
        return readOnly;
    }


    /**
     * @param readOnly the readOnly to set
     */
    public void setReadOnly( boolean readOnly )
    {
        this.readOnly = readOnly;
    }

}
