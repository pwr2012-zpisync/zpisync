package zpisync.shared.services;

import java.util.Date;
import java.util.List;

import zpisync.shared.FileInfo;

/**
 * Interface for REST service.
 */
public interface SyncService {

	Date getLastModificationDate();

	/**
	 * Flat list of all files
	 * 
	 * @param modifiedSince
	 *            only files modified since (can be null)
	 */
	List<FileInfo> getFileList(Date modifiedSince);

	// TODO need to know which files were deleted/renamed

	FileInfo getFileInfo(String path);
}
