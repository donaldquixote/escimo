/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.scim;


import java.io.PrintWriter;
import java.io.StringWriter;


/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ScimUtil
{
    public static final String CORE_USER_URI = "urn:scim:schemas:core:2.0:User";
    public static final String CORE_GROUP_URI = "urn:scim:schemas:core:2.0:Group";
    public static final String CORE_EXT_USER_URI = "urn:scim:schemas:extension:enterprise:2.0:User";

    public static String exceptionToStr( Exception e )
    {
        StringWriter sw = new StringWriter();

        PrintWriter pw = new PrintWriter( sw );

        pw.write( e.getMessage() );
        
        e.printStackTrace( pw );

        pw.close();

        return sw.toString();
    }

    
    public static boolean isCoreAttribute( String uri )
    {
        return ( CORE_USER_URI.equals( uri ) || 
                 CORE_GROUP_URI.equals( uri ) );
    }

}
