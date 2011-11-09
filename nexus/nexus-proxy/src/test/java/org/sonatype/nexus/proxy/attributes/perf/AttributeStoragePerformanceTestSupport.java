/**
 * Copyright (c) 2008-2011 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions
 *
 * This program is free software: you can redistribute it and/or modify it only under the terms of the GNU Affero General
 * Public License Version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License Version 3
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License Version 3 along with this program.  If not, see
 * http://www.gnu.org/licenses.
 *
 * Sonatype Nexus (TM) Open Source Version is available from Sonatype, Inc. Sonatype and Sonatype Nexus are trademarks of
 * Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation. M2Eclipse is a trademark of the Eclipse Foundation.
 * All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.proxy.attributes.perf;

import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.carrotsearch.junitbenchmarks.BenchmarkRule;
import com.carrotsearch.junitbenchmarks.annotation.AxisRange;
import com.carrotsearch.junitbenchmarks.annotation.BenchmarkHistoryChart;
import com.carrotsearch.junitbenchmarks.annotation.BenchmarkMethodChart;
import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.RunWith;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.mime.MimeUtil;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.access.AccessManager;
import org.sonatype.nexus.proxy.attributes.AttributeStorage;
import org.sonatype.nexus.proxy.attributes.DefaultAttributesHandler;
import org.sonatype.nexus.proxy.attributes.StorageFileItemInspector;
import org.sonatype.nexus.proxy.attributes.StorageItemInspector;
import org.sonatype.nexus.proxy.attributes.perf.internal.MockRepository;
import org.sonatype.nexus.proxy.attributes.perf.internal.OrderedRunner;
import org.sonatype.nexus.proxy.attributes.perf.internal.TestRepositoryItemUid;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.DummyRepositoryItemUidFactory;
import org.sonatype.nexus.proxy.item.LinkPersister;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StringContentLocator;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.local.fs.DefaultFSLocalRepositoryStorage;
import org.sonatype.nexus.proxy.storage.local.fs.DefaultFSPeer;
import org.sonatype.nexus.proxy.storage.local.fs.FileContentLocator;
import org.sonatype.nexus.proxy.wastebasket.Wastebasket;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The performance tests for specific implementations of AttributesStorage.
 */
@BenchmarkHistoryChart()
@BenchmarkMethodChart()
@AxisRange(min = 0)
@RunWith( OrderedRunner.class )
public abstract class AttributeStoragePerformanceTestSupport
{

    @Rule
    public MethodRule benchmarkRun = new BenchmarkRule();

    private Repository repository;

    private AttributeStorage attributeStorage;

    private ApplicationConfiguration applicationConfiguration;

    final protected String testFilePath = "content/file.txt";

    private DefaultFSLocalRepositoryStorage localRepositoryStorageUnderTest;

    protected long originalLastAccessTime;

    final private DummyRepositoryItemUidFactory repositoryItemUidFactory = new DummyRepositoryItemUidFactory();

    final private File CONTENT_TEST_FILE = new File( "target/" + getClass().getSimpleName() + "/testContent.txt" );

    final private String SHA1_ATTRIBUTE_KEY = "digest.sha1";

    final private String SHA1_ATTRIBUTE_VALUE = "100f4ae295695335ef5d3346b42ed81ecd063fc9";

    final private static int ITERATION_COUNT = 100;

    @Before
    public void setup() throws Exception
    {
        File repoStorageDir = new File( "target/"+ getClass().getSimpleName() + "/repo-storage/" );
        String repoLocalURL = repoStorageDir.getAbsoluteFile().toURI().toString();

        // write a test file
        File testFile = new File( repoStorageDir, testFilePath );
        FileUtils.writeStringToFile( testFile, "CONTENT" );

        FileUtils.writeStringToFile( CONTENT_TEST_FILE, "CONTENT" );

         // Mocks
        Wastebasket wastebasket = mock( Wastebasket.class );
        LinkPersister linkPersister = mock( LinkPersister.class );
        MimeUtil mimeUtil = mock( MimeUtil.class );
        Map<String, Long> repositoryContexts = Maps.newHashMap();

        // Application Config
        applicationConfiguration = mock( ApplicationConfiguration.class );
        when( applicationConfiguration.getWorkingDirectory( eq("proxy/attributes")) ).thenReturn( new File( "target/"+ this.getClass().getSimpleName() +"/attributes" ) );

        // remove any event inspectors from the timing
        List<StorageItemInspector> itemInspectorList = new ArrayList<StorageItemInspector>();
        List<StorageFileItemInspector> fileItemInspectorList = new ArrayList<StorageFileItemInspector>();

        // set the AttributeStorage on the Attribute Handler
        attributeStorage = getAttributeStorage();
        DefaultAttributesHandler attributesHandler = new DefaultAttributesHandler(applicationConfiguration, attributeStorage, itemInspectorList, fileItemInspectorList );

        // Need to use the MockRepository
        // Do NOT us a mockito mock, using answers does not play well with junitbenchmark
        repository = new MockRepository( "testRetieveItem-repo", new DummyRepositoryItemUidFactory() );
        repository.setLocalUrl( repoLocalURL );
        repository.setAttributesHandler( attributesHandler );

        // setup the class under test
        localRepositoryStorageUnderTest = new DefaultFSLocalRepositoryStorage( wastebasket, linkPersister, mimeUtil, repositoryContexts, new DefaultFSPeer() );

        // prime the retrieve
        ResourceStoreRequest resourceRequest = new ResourceStoreRequest( testFilePath );
        originalLastAccessTime = localRepositoryStorageUnderTest.retrieveItem( repository, resourceRequest ).getLastRequested();
        Thread.sleep( 1 );
    }

    public abstract AttributeStorage getAttributeStorage();

    //////////////
    // Test writes
    //////////////
    @Test
    public void test0PrimePutAndGettAttribute()
    {
        writeEntryToAttributeStorage( "/prime.txt" );
        getStorageItemFromAttributeStore( "/prime.txt" );
    }

    @Test
    public void test1PutAttribute()
    {
        writeEntryToAttributeStorage( "/a.txt" );
    }

    @Test
    public void test2PutAttributeX100()
    {
        for( int ii=0; ii< ITERATION_COUNT; ii++)
        {
            writeEntryToAttributeStorage( "/"+ii+".txt" );
        }
    }

    @Test
    public void test3GetAttribute()
    {
        AbstractStorageItem storageItem = getStorageItemFromAttributeStore( "/a.txt" );
        assertThat( storageItem.getAttributes().get( SHA1_ATTRIBUTE_KEY ), equalTo( SHA1_ATTRIBUTE_VALUE) );
    }

    @Test
    public void test4GetAttributeX100()
    {
        for( int ii=0; ii< ITERATION_COUNT; ii++)
        {
            getStorageItemFromAttributeStore( "/"+ii+".txt" );
        }
    }

//    @Test
    public void test5DeleteAttributes()
    {
        deleteStorageItemFromAttributeStore( "/a.txt" );
    }

//    @Test
    public void test6DeleteAttributesX100()
    {
        for( int ii=0; ii< ITERATION_COUNT; ii++)
        {
            deleteStorageItemFromAttributeStore( "/"+ii+".txt" );
        }
    }

    @BenchmarkOptions(benchmarkRounds = 1000, warmupRounds = 1 )
    @Test
    public void testRetieveItemWithLastAccessUpdate()
        throws LocalStorageException, ItemNotFoundException
    {
        ResourceStoreRequest resourceRequest = new ResourceStoreRequest( testFilePath );
        resourceRequest.getRequestContext().put( AccessManager.REQUEST_REMOTE_ADDRESS, "127.0.0.1" );

        AbstractStorageItem storageItem = localRepositoryStorageUnderTest.retrieveItem( repository, resourceRequest );

        MatcherAssert.assertThat( storageItem, Matchers.notNullValue() );
        MatcherAssert.assertThat( storageItem.getLastRequested(), Matchers.greaterThan( originalLastAccessTime ) );
    }

    @BenchmarkOptions(benchmarkRounds = 1000, warmupRounds = 1)
    @Test
    public void testRetieveItemWithoutLastAccessUpdate()
        throws LocalStorageException, ItemNotFoundException
    {
        ResourceStoreRequest resourceRequest = new ResourceStoreRequest( testFilePath );

        AbstractStorageItem storageItem = localRepositoryStorageUnderTest.retrieveItem( repository, resourceRequest );

        MatcherAssert.assertThat( storageItem, Matchers.notNullValue() );
        MatcherAssert.assertThat( storageItem.getLastRequested(), Matchers.equalTo( originalLastAccessTime ) );
    }

    protected void writeEntryToAttributeStorage( String path )
    {
        StorageFileItem storageFileItem = new DefaultStorageFileItem( repository, new ResourceStoreRequest( path ), true, true, getContentLocator() );

        storageFileItem.getAttributes().put( SHA1_ATTRIBUTE_KEY, SHA1_ATTRIBUTE_VALUE );
        storageFileItem.getAttributes().put( "digest.md5", "f62472816fb17de974a87513e2257d63" );
        storageFileItem.getAttributes().put( "request.address", "127.0.0.1" );
        
        attributeStorage.putAttribute( storageFileItem );
    }

    protected AbstractStorageItem getStorageItemFromAttributeStore( String path )
    {
        RepositoryItemUid repositoryItemUid = new TestRepositoryItemUid(repositoryItemUidFactory, repository, path );

        AbstractStorageItem storageItem = attributeStorage.getAttributes( repositoryItemUid );
        return storageItem;
    }

    private void deleteStorageItemFromAttributeStore( String path)
    {
        RepositoryItemUid repositoryItemUid = new TestRepositoryItemUid(repositoryItemUidFactory, repository, path );
        assertThat( "Attribute was not removed from store.", attributeStorage.deleteAttributes( repositoryItemUid ) );
    }

    private ContentLocator getContentLocator()
    {
        if( true )
        {
            return new StringContentLocator( "CONTENT" );
        }
        else
        {
            return new FileContentLocator( CONTENT_TEST_FILE, "text/plain" );
        }
    }

}