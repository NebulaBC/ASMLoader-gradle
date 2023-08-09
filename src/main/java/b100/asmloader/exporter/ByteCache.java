package b100.asmloader.exporter;

import java.util.ArrayList;
import java.util.List;

class ByteCache {
	
	private List<CacheEntry> allBuffers = new ArrayList<>();
	
	public void put(byte[] bytes, int offset, int length) {
		CacheEntry cacheEntry = new CacheEntry();
		cacheEntry.bytes = bytes;
		cacheEntry.length = length;
		cacheEntry.offset = offset;
		allBuffers.add(cacheEntry);
	}
	
	public byte[] getAll() {
		int totalSize = 0;
		int buffers = allBuffers.size();
		
		for(int i=0; i < buffers; i++) {
			totalSize += allBuffers.get(i).length;
		}
		
		byte[] allBytes = new byte[totalSize];
		int offset = 0;
		
		for(int i=0; i < buffers; i++) {
			CacheEntry cacheEntry = allBuffers.get(i);
			
			byte[] bytes = cacheEntry.bytes;
			for(int j=0; j < cacheEntry.length; j++) {
				allBytes[offset + j] = bytes[cacheEntry.offset + j];
			}
			offset += cacheEntry.length;
		}
		
		return allBytes;
	}
	
	private class CacheEntry {
		
		public byte[] bytes;
		public int offset;
		public int length;
		
	}

}
