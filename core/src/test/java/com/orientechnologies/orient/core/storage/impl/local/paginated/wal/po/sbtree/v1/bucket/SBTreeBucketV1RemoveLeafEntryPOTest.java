package com.orientechnologies.orient.core.storage.impl.local.paginated.wal.po.sbtree.v1.bucket;

import com.orientechnologies.common.directmemory.OByteBufferPool;
import com.orientechnologies.common.directmemory.OPointer;
import com.orientechnologies.common.serialization.types.OByteSerializer;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.serialization.serializer.binary.impl.OLinkSerializer;
import com.orientechnologies.orient.core.storage.cache.OCacheEntry;
import com.orientechnologies.orient.core.storage.cache.OCacheEntryImpl;
import com.orientechnologies.orient.core.storage.cache.OCachePointer;
import com.orientechnologies.orient.core.storage.impl.local.paginated.wal.OOperationUnitId;
import com.orientechnologies.orient.core.storage.impl.local.paginated.wal.po.PageOperationRecord;
import com.orientechnologies.orient.core.storage.index.sbtree.local.v1.OSBTreeBucketV1;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.List;

public class SBTreeBucketV1RemoveLeafEntryPOTest {
  @Test
  public void testRedo() {
    final int pageSize = 64 * 1024;
    final OByteBufferPool byteBufferPool = new OByteBufferPool(pageSize);
    try {
      final OPointer pointer = byteBufferPool.acquireDirect(false);
      final OCachePointer cachePointer = new OCachePointer(pointer, byteBufferPool, 0, 0);
      final OCacheEntry entry = new OCacheEntryImpl(0, 0, cachePointer);

      OSBTreeBucketV1<Byte, OIdentifiable> bucket = new OSBTreeBucketV1<>(entry);
      bucket.init(true);

      bucket.addLeafEntry(0, new byte[] { 0 }, OLinkSerializer.INSTANCE.serializeNativeAsWhole(new ORecordId(0, 0)));
      bucket.addLeafEntry(1, new byte[] { 1 }, OLinkSerializer.INSTANCE.serializeNativeAsWhole(new ORecordId(1, 1)));
      bucket.addLeafEntry(2, new byte[] { 2 }, OLinkSerializer.INSTANCE.serializeNativeAsWhole(new ORecordId(2, 2)));

      entry.clearPageOperations();

      final OPointer restoredPointer = byteBufferPool.acquireDirect(false);
      final OCachePointer restoredCachePointer = new OCachePointer(restoredPointer, byteBufferPool, 0, 0);
      final OCacheEntry restoredCacheEntry = new OCacheEntryImpl(0, 0, restoredCachePointer);

      final ByteBuffer originalBuffer = cachePointer.getBufferDuplicate();
      final ByteBuffer restoredBuffer = restoredCachePointer.getBufferDuplicate();

      Assert.assertNotNull(originalBuffer);
      Assert.assertNotNull(restoredBuffer);

      restoredBuffer.put(originalBuffer);

      bucket.removeLeafEntry(1, new byte[] { 1 }, OLinkSerializer.INSTANCE.serializeNativeAsWhole(new ORecordId(1, 1)));

      final List<PageOperationRecord> operations = entry.getPageOperations();
      Assert.assertEquals(1, operations.size());

      Assert.assertTrue(operations.get(0) instanceof SBTreeBucketV1RemoveLeafEntryPO);

      final SBTreeBucketV1RemoveLeafEntryPO pageOperation = (SBTreeBucketV1RemoveLeafEntryPO) operations.get(0);

      OSBTreeBucketV1<Byte, OIdentifiable> restoredBucket = new OSBTreeBucketV1<>(restoredCacheEntry);
      Assert.assertEquals(3, restoredBucket.size());

      Assert.assertEquals(new ORecordId(0, 0),
          restoredBucket.getValue(0, false, OByteSerializer.INSTANCE, OLinkSerializer.INSTANCE).getValue());
      Assert.assertEquals(new ORecordId(1, 1),
          restoredBucket.getValue(1, false, OByteSerializer.INSTANCE, OLinkSerializer.INSTANCE).getValue());
      Assert.assertEquals(new ORecordId(2, 2),
          restoredBucket.getValue(2, false, OByteSerializer.INSTANCE, OLinkSerializer.INSTANCE).getValue());

      pageOperation.redo(restoredCacheEntry);

      Assert.assertEquals(2, restoredBucket.size());

      Assert.assertEquals(new ORecordId(0, 0),
          restoredBucket.getValue(0, false, OByteSerializer.INSTANCE, OLinkSerializer.INSTANCE).getValue());
      Assert.assertEquals(new ORecordId(2, 2),
          restoredBucket.getValue(1, false, OByteSerializer.INSTANCE, OLinkSerializer.INSTANCE).getValue());

      byteBufferPool.release(pointer);
      byteBufferPool.release(restoredPointer);
    } finally {
      byteBufferPool.clear();
    }
  }

  @Test
  public void testUndo() {
    final int pageSize = 64 * 1024;

    final OByteBufferPool byteBufferPool = new OByteBufferPool(pageSize);
    try {
      final OPointer pointer = byteBufferPool.acquireDirect(false);
      final OCachePointer cachePointer = new OCachePointer(pointer, byteBufferPool, 0, 0);
      final OCacheEntry entry = new OCacheEntryImpl(0, 0, cachePointer);

      OSBTreeBucketV1<Byte, OIdentifiable> bucket = new OSBTreeBucketV1<>(entry);
      bucket.init(true);

      bucket.addLeafEntry(0, new byte[] { 0 }, OLinkSerializer.INSTANCE.serializeNativeAsWhole(new ORecordId(0, 0)));
      bucket.addLeafEntry(1, new byte[] { 1 }, OLinkSerializer.INSTANCE.serializeNativeAsWhole(new ORecordId(1, 1)));
      bucket.addLeafEntry(2, new byte[] { 2 }, OLinkSerializer.INSTANCE.serializeNativeAsWhole(new ORecordId(2, 2)));

      entry.clearPageOperations();

      bucket.removeLeafEntry(1, new byte[] { 1 }, OLinkSerializer.INSTANCE.serializeNativeAsWhole(new ORecordId(1, 1)));

      final List<PageOperationRecord> operations = entry.getPageOperations();
      Assert.assertEquals(1, operations.size());

      Assert.assertTrue(operations.get(0) instanceof SBTreeBucketV1RemoveLeafEntryPO);

      final SBTreeBucketV1RemoveLeafEntryPO pageOperation = (SBTreeBucketV1RemoveLeafEntryPO) operations.get(0);

      final OSBTreeBucketV1<Byte, OIdentifiable> restoredBucket = new OSBTreeBucketV1<>(entry);

      Assert.assertEquals(2, restoredBucket.size());

      Assert.assertEquals(new ORecordId(0, 0),
          restoredBucket.getValue(0, false, OByteSerializer.INSTANCE, OLinkSerializer.INSTANCE).getValue());
      Assert.assertEquals(new ORecordId(2, 2),
          restoredBucket.getValue(1, false, OByteSerializer.INSTANCE, OLinkSerializer.INSTANCE).getValue());

      pageOperation.undo(entry);

      Assert.assertEquals(3, restoredBucket.size());

      Assert.assertEquals(new ORecordId(0, 0),
          restoredBucket.getValue(0, false, OByteSerializer.INSTANCE, OLinkSerializer.INSTANCE).getValue());
      Assert.assertEquals(new ORecordId(1, 1),
          restoredBucket.getValue(1, false, OByteSerializer.INSTANCE, OLinkSerializer.INSTANCE).getValue());
      Assert.assertEquals(new ORecordId(2, 2),
          restoredBucket.getValue(2, false, OByteSerializer.INSTANCE, OLinkSerializer.INSTANCE).getValue());

      byteBufferPool.release(pointer);
    } finally {
      byteBufferPool.clear();
    }
  }

  @Test
  public void testSerialization() {
    OOperationUnitId operationUnitId = OOperationUnitId.generateId();

    SBTreeBucketV1RemoveLeafEntryPO operation = new SBTreeBucketV1RemoveLeafEntryPO(1, new byte[] { 2, 4 }, new byte[] { 4, 2 });

    operation.setFileId(42);
    operation.setPageIndex(24);
    operation.setOperationUnitId(operationUnitId);

    final int serializedSize = operation.serializedSize();
    final byte[] stream = new byte[serializedSize + 1];
    int pos = operation.toStream(stream, 1);

    Assert.assertEquals(serializedSize + 1, pos);

    SBTreeBucketV1RemoveLeafEntryPO restoredOperation = new SBTreeBucketV1RemoveLeafEntryPO();
    restoredOperation.fromStream(stream, 1);

    Assert.assertEquals(42, restoredOperation.getFileId());
    Assert.assertEquals(24, restoredOperation.getPageIndex());
    Assert.assertEquals(operationUnitId, restoredOperation.getOperationUnitId());

    Assert.assertEquals(1, restoredOperation.getIndex());
    Assert.assertArrayEquals(new byte[] { 2, 4 }, restoredOperation.getKey());
    Assert.assertArrayEquals(new byte[] { 4, 2 }, restoredOperation.getValue());
  }
}
