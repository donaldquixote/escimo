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
package org.apache.directory.scim;


import java.util.ArrayList;
import java.util.List;


/**
 * TODO ListResponse.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ListResponse
{
    private int totalResults = 0;

    private List<ServerResource> Resources;

    private int startIndex = -1;

    private int itemsPerPage = -1;


    public ListResponse()
    {
        Resources = new ArrayList<ServerResource>();
    }


    public void addResource( ServerResource res )
    {
        Resources.add( res );
        totalResults++;
    }


    /**
     * @return the totalResults
     */
    public int getTotalResults()
    {
        return totalResults;
    }


    /**
     * @return the resources
     */
    public List<ServerResource> getResources()
    {
        return Resources;
    }


    /**
     * @return the startIndex
     */
    public int getStartIndex()
    {
        return startIndex;
    }


    /**
     * @return the itemsPerPage
     */
    public int getItemsPerPage()
    {
        return itemsPerPage;
    }

}
