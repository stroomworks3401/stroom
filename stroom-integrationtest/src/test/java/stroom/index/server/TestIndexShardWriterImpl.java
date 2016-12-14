/*
 * Copyright 2016 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.index.server;

import java.io.IOException;

import javax.annotation.Resource;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.junit.Assert;
import org.junit.Test;

import stroom.entity.server.util.BaseEntityDeProxyProcessor;
import stroom.entity.shared.BaseCriteria.OrderByDirection;
import stroom.entity.shared.BaseResultList;
import stroom.entity.shared.DocRef;
import stroom.entity.shared.FolderService;
import stroom.entity.shared.Range;
import stroom.index.shared.FindIndexCriteria;
import stroom.index.shared.FindIndexShardCriteria;
import stroom.index.shared.Index;
import stroom.index.shared.Index.PartitionBy;
import stroom.index.shared.IndexService;
import stroom.index.shared.IndexShard;
import stroom.index.shared.IndexShardKey;
import stroom.AbstractCoreIntegrationTest;
import stroom.CommonTestControl;
import stroom.CommonTestScenarioCreator;
import stroom.index.shared.IndexShardService;
import stroom.node.server.NodeCache;
import stroom.node.shared.FindVolumeCriteria;
import stroom.node.shared.Node;
import stroom.node.shared.Volume;
import stroom.node.shared.VolumeService;
import stroom.query.shared.IndexField;
import stroom.query.shared.IndexFields;
import stroom.util.date.DateUtil;

public class TestIndexShardWriterImpl extends AbstractCoreIntegrationTest {
    @Resource
    private CommonTestScenarioCreator commonTestScenarioCreator;
    @Resource
    private IndexShardWriterCache indexShardWriterCache;
    @Resource
    private CommonTestControl commonTestControl;

    @Override
    public void onBefore() {
        indexShardWriterCache.shutdown();
    }

    @Test
    public void testSimple() throws IOException {
        Assert.assertEquals(0, commonTestControl.countEntity(IndexShard.class));

        // Do some work.
        final FieldType fieldType = FieldTypeFactory.createBasic();
        final Field field = new Field("test", "test", fieldType);
        final Document document = new Document();
        document.add(field);

        final Index index1 = commonTestScenarioCreator.createIndex("TEST_2010");
        final IndexShardKey indexShardKey1 = IndexShardKeyUtil.createTestKey(index1);

        final Index index2 = commonTestScenarioCreator.createIndex("TEST_2011");
        final IndexShardKey indexShardKey2 = IndexShardKeyUtil.createTestKey(index2);

        // Create 2 writers in the pool.
        final IndexShardWriter writer1 = indexShardWriterCache.get(indexShardKey1);
        final IndexShardWriter writer2 = indexShardWriterCache.get(indexShardKey2);

        // Assert that there are 2 writers in the pool.
        Assert.assertEquals(2, commonTestControl.countEntity(IndexShard.class));

        final FindIndexShardCriteria criteria = new FindIndexShardCriteria();
        criteria.getIndexIdSet().setMatchAll(true);

        Assert.assertEquals(0, writer1.getDocumentCount());
        Assert.assertEquals(0, writer1.getIndexShard().getDocumentCount());
        writer1.addDocument(document);
        Assert.assertEquals(1, writer1.getDocumentCount());
        Assert.assertEquals(0, writer1.getIndexShard().getDocumentCount());
        indexShardWriterCache.findFlush(criteria);
        Assert.assertEquals(1, writer1.getDocumentCount());
        Assert.assertEquals(1, writer1.getIndexShard().getDocumentCount());

        writer1.addDocument(document);
        Assert.assertEquals(2, writer1.getDocumentCount());
        Assert.assertEquals(1, writer1.getIndexShard().getDocumentCount());
        indexShardWriterCache.findFlush(criteria);
        Assert.assertEquals(2, writer1.getDocumentCount());
        Assert.assertEquals(2, writer1.getIndexShard().getDocumentCount());

        Assert.assertEquals(0, writer2.getDocumentCount());
        Assert.assertEquals(0, writer2.getIndexShard().getDocumentCount());
        writer2.addDocument(document);
        Assert.assertEquals(1, writer2.getDocumentCount());
        Assert.assertEquals(0, writer2.getIndexShard().getDocumentCount());
        indexShardWriterCache.findClose(criteria);
        Assert.assertEquals(1, writer2.getDocumentCount());
        Assert.assertEquals(1, writer2.getIndexShard().getDocumentCount());

        // Make sure that writer1 was closed.
        Assert.assertFalse(writer1.isOpen());

        // Make sure that adding to writer1 reopens the index.
        final boolean added = writer1.addDocument(document);
        Assert.assertTrue(added);
        Assert.assertTrue(writer1.isOpen());

        // Close indexes again.
        indexShardWriterCache.findClose(criteria);

        // Make sure that writer1 was closed.
        Assert.assertFalse(writer1.isOpen());
    }

    @Test
    public void testSimpleRoll() throws IOException {
        // Do some work.
        final FieldType fieldType = FieldTypeFactory.createBasic();
        final Field field = new Field("test", "test", fieldType);
        final Document document = new Document();
        document.add(field);

        // final Folder folder = commonTestScenarioCreator
        // .getGlobalGroup();
        //
        final Index index1 = commonTestScenarioCreator.createIndex("TEST_2010",
                commonTestScenarioCreator.createIndexFields(), 10);
        // index1.setMaxDocsPerShard(10);
        // index1.setName("TEST/2010");
        // index1.setFolder(folder);
        // index1 = indexService.save(index1);

        final IndexShardKey indexShardKey1 = IndexShardKeyUtil.createTestKey(index1);

        final IndexShardWriter toFillWriter = indexShardWriterCache.get(indexShardKey1);

        for (int i = 0; i < 10; i++) {
            Assert.assertFalse(toFillWriter.isFull());
            Assert.assertTrue(toFillWriter.addDocument(document));
        }

        // Make sure the writer is full.
        Assert.assertTrue(toFillWriter.isFull());
        // Try and add a document and make sure that it returns false as the
        // writer is full.
        final boolean added = toFillWriter.addDocument(document);
        Assert.assertFalse(added);
        // Make sure the writer is still open.
        Assert.assertTrue(toFillWriter.isOpen());
        // Remove the item from the pool.
        indexShardWriterCache.remove(indexShardKey1);
        // Make sure the writer is closed when the pool destroys it.
        Assert.assertFalse(toFillWriter.isOpen());
        // Make sure the pool doesn't destroy items more than once.
        indexShardWriterCache.remove(indexShardKey1);
        Assert.assertFalse(toFillWriter.isOpen());

        final IndexShardWriter newWriter = indexShardWriterCache.get(indexShardKey1);

        Assert.assertTrue(newWriter.addDocument(document));

        // Force the pool to load up a load which should close off the full
        // writer
        for (int i = 0; i < 10; i++) {
            final IndexShardWriter poolItem = indexShardWriterCache.get(indexShardKey1);
            // poolItems.add(poolItem);
        }

        Assert.assertFalse(toFillWriter.isOpen());

        // // Return all poolItems to the pool.
        // for (final PoolItem<IndexShardKey, IndexShardWriter> poolItem :
        // poolItems) {
        // indexShardWriterCache.returnObject(poolItem, true);
        // }
    }

    public static class TestIndexServiceImpl extends AbstractCoreIntegrationTest {
        @Resource
        private IndexService indexService;
        @Resource
        private FolderService folderService;

        private Index testIndex;
        private Index refIndex;

        @Override
        protected void onBefore() {
            final DocRef testFolder = DocRef.create(folderService.create(null, "Test Group"));
            refIndex = indexService.create(testFolder, "Ref index");
            testIndex = indexService.create(testFolder, "Test index");

            final IndexFields indexFields = IndexFields.createStreamIndexFields();
            indexFields.add(IndexField.createDateField("TimeCreated"));
            indexFields.add(IndexField.createField("User"));
            testIndex.setIndexFieldsObject(indexFields);
            testIndex = indexService.save(testIndex);
        }

        @Test
        public void testIndexRetrieval() {
            final FindIndexCriteria criteria = new FindIndexCriteria();
            final BaseResultList<Index> list = indexService.find(criteria);

            Assert.assertEquals(2, list.size());

            final Index index = list.get(1);

            Assert.assertNotNull(index);
            Assert.assertEquals("Test index", index.getName());

            final StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.1\" encoding=\"UTF-8\"?>\n");
            sb.append("<fields>\n");
            sb.append("   <field>\n");
            sb.append("      <analyzerType>KEYWORD</analyzerType>\n");
            sb.append("      <caseSensitive>false</caseSensitive>\n");
            sb.append("      <fieldName>StreamId</fieldName>\n");
            sb.append("      <fieldType>ID</fieldType>\n");
            sb.append("      <indexed>true</indexed>\n");
            sb.append("      <stored>true</stored>\n");
            sb.append("      <termPositions>false</termPositions>\n");
            sb.append("   </field>\n");
            sb.append("   <field>\n");
            sb.append("      <analyzerType>KEYWORD</analyzerType>\n");
            sb.append("      <caseSensitive>false</caseSensitive>\n");
            sb.append("      <fieldName>EventId</fieldName>\n");
            sb.append("      <fieldType>ID</fieldType>\n");
            sb.append("      <indexed>true</indexed>\n");
            sb.append("      <stored>true</stored>\n");
            sb.append("      <termPositions>false</termPositions>\n");
            sb.append("   </field>\n");
            sb.append("   <field>\n");
            sb.append("      <analyzerType>ALPHA_NUMERIC</analyzerType>\n");
            sb.append("      <caseSensitive>false</caseSensitive>\n");
            sb.append("      <fieldName>TimeCreated</fieldName>\n");
            sb.append("      <fieldType>DATE_FIELD</fieldType>\n");
            sb.append("      <indexed>true</indexed>\n");
            sb.append("      <stored>false</stored>\n");
            sb.append("      <termPositions>false</termPositions>\n");
            sb.append("   </field>\n");
            sb.append("   <field>\n");
            sb.append("      <analyzerType>ALPHA_NUMERIC</analyzerType>\n");
            sb.append("      <caseSensitive>false</caseSensitive>\n");
            sb.append("      <fieldName>User</fieldName>\n");
            sb.append("      <fieldType>FIELD</fieldType>\n");
            sb.append("      <indexed>true</indexed>\n");
            sb.append("      <stored>false</stored>\n");
            sb.append("      <termPositions>false</termPositions>\n");
            sb.append("   </field>\n");
            sb.append("</fields>\n");
            Assert.assertEquals(sb.toString(), index.getIndexFields());
        }

        @Test
        public void testLoad() {
            Index index = new Index();
            index.setId(testIndex.getId());
            index = indexService.load(index);

            Assert.assertNotNull(index);
            Assert.assertEquals("Test index", index.getName());
        }

        @Test
        public void testLoadById() {
            final Index index = indexService.loadById(testIndex.getId());
            Assert.assertNotNull(index);
            Assert.assertEquals("Test index", index.getName());
        }

        @Test
        public void testClientSideStuff1() {
            Index index = indexService.loadById(refIndex.getId());
            index = ((Index) new BaseEntityDeProxyProcessor(true).process(index));
            indexService.save(index);

        }

        @Test
        public void testClientSideStuff2() {
            Index index = indexService.loadById(testIndex.getId());
            index = ((Index) new BaseEntityDeProxyProcessor(true).process(index));
            indexService.save(index);
        }
    }

    public static class TestIndexShardServiceImpl extends AbstractCoreIntegrationTest {
        @Resource
        private FolderService folderService;
        @Resource
        private IndexService indexService;
        @Resource
        private IndexShardService indexShardService;
        @Resource
        private NodeCache nodeCache;
        @Resource
        private VolumeService volumeService;

        @Override
        protected void onBefore() {
            clean();
        }

        /**
         * Test.
         */
        @Test
        public void test() {
            final Volume volume = volumeService.find(new FindVolumeCriteria()).getFirst();

            final DocRef testFolder = DocRef.create(folderService.create(null, "Test Group"));

            Index index1 = indexService.create(testFolder, "Test Index 1");
            index1.getVolumes().add(volume);
            index1 = indexService.save(index1);
            final IndexShardKey indexShardKey1 = IndexShardKeyUtil.createTestKey(index1);

            Index index2 = indexService.create(testFolder, "Test Index 2");
            index2.getVolumes().add(volume);
            index2 = indexService.save(index2);
            final IndexShardKey indexShardKey2 = IndexShardKeyUtil.createTestKey(index2);

            final Node node = nodeCache.getDefaultNode();

            final IndexShard call1 = indexShardService.createIndexShard(indexShardKey1, node);
            final IndexShard call2 = indexShardService.createIndexShard(indexShardKey1, node);
            final IndexShard call3 = indexShardService.createIndexShard(indexShardKey1, node);
            final IndexShard call4 = indexShardService.createIndexShard(indexShardKey2, node);

            Assert.assertNotNull(call1);
            Assert.assertNotNull(call2);
            Assert.assertNotNull(call3);
            Assert.assertNotNull(call4);

            final FindIndexShardCriteria criteria = new FindIndexShardCriteria();
            // Find all index shards.
            Assert.assertEquals(4, indexShardService.find(criteria).size());

            // Find shards for index 1
            criteria.getIndexIdSet().clear();
            criteria.getIndexIdSet().add(index1);
            Assert.assertEquals(3, indexShardService.find(criteria).size());

            // Find shards for index 2
            criteria.getIndexIdSet().clear();
            criteria.getIndexIdSet().add(index2);
            Assert.assertEquals(1, indexShardService.find(criteria).size());

            // Set all the filters
            criteria.setDocumentCountRange(new Range<>(Integer.MAX_VALUE, null));
            Assert.assertEquals(0, indexShardService.find(criteria).size());
        }

        @Test
        public void testOrderBy() {
            final Volume volume = volumeService.find(new FindVolumeCriteria()).getFirst();

            final DocRef testFolder = DocRef.create(folderService.create(null, "Test Group"));

            Index index = indexService.create(testFolder, "Test Index 1");
            index.getVolumes().add(volume);
            index.setPartitionBy(PartitionBy.MONTH);
            index.setPartitionSize(1);
            index = indexService.save(index);

            final Node node = nodeCache.getDefaultNode();

            createShard(index, node, "2013-05-01T00:00:00.000Z", 1);
            createShard(index, node, "2013-05-01T00:00:00.000Z", 2);
            createShard(index, node, "2013-06-01T00:00:00.000Z", 3);
            createShard(index, node, "2013-02-01T00:00:00.000Z", 4);
            createShard(index, node, "2013-02-01T00:00:00.000Z", 5);
            createShard(index, node, "2012-01-01T00:00:00.000Z", 6);
            createShard(index, node, "2011-02-01T00:00:00.000Z", 7);
            createShard(index, node, "2014-08-01T00:00:00.000Z", 8);
            createShard(index, node, "2011-01-01T00:00:00.000Z", 9);
            createShard(index, node, "2011-02-01T00:00:00.000Z", 10);

            final FindIndexShardCriteria findIndexShardCriteria = new FindIndexShardCriteria();
            // Order by partition name and key.
            findIndexShardCriteria.addOrderBy(FindIndexShardCriteria.ORDER_BY_PARTITION, OrderByDirection.DESCENDING);
            findIndexShardCriteria.addOrderBy(FindIndexShardCriteria.ORDER_BY_ID, OrderByDirection.DESCENDING);

            // Find data.
            final BaseResultList<IndexShard> list = indexShardService.find(findIndexShardCriteria);

            Assert.assertEquals(10, list.size());

            IndexShard lastShard = null;
            for (final IndexShard indexShard : list) {
                if (lastShard != null) {
                    if (lastShard.getPartition().equals(indexShard.getPartition())) {
                        // Compare ids
                        Assert.assertTrue(indexShard.getId() < lastShard.getId());
                    } else {
                        Assert.assertTrue(indexShard.getPartition().compareTo(lastShard.getPartition()) < 0);
                    }
                }

                lastShard = indexShard;
            }
        }

        private void createShard(final Index index, final Node node, final String dateTime, final int shardNo) {
            final long timeMs = DateUtil.parseNormalDateTimeString(dateTime);
            final IndexShardKey key = IndexShardKeyUtil.createTimeBasedKey(index, timeMs, shardNo);
            indexShardService.createIndexShard(key, node);
        }
    }
}
