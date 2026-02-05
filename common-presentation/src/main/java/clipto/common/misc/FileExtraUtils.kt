package clipto.common.misc

import java.io.*
import java.math.BigInteger
import java.nio.channels.FileChannel
import java.util.*

/**
 * General file manipulation utilities.
 *
 *
 * Facilities are provided in the following areas:
 *
 *  * writing to a file
 *  * reading from a file
 *  * make a directory including parent directories
 *  * copying files and directories
 *  * deleting files and directories
 *  * converting to and from a URL
 *  * listing files and directories by filter and extension
 *  * comparing file content
 *  * file last changed date
 *  * calculating a checksum
 *
 *
 *
 * Origin of code: Excalibur, Alexandria, Commons-Utils
 *
 * @version $Id: FileUtils.java 1349509 2012-06-12 20:39:23Z ggregory $
 */
object FileExtraUtils {
    /**
     * The number of bytes in a kilobyte.
     */
    private const val ONE_KB: Long = 1024

    /**
     * The number of bytes in a megabyte.
     */
    private const val ONE_MB = ONE_KB * ONE_KB

    /**
     * The file copy buffer size (30 MB)
     */
    private const val FILE_COPY_BUFFER_SIZE = ONE_MB * 4
    /**
     * Copies a file to a directory optionally preserving the file date.
     *
     *
     * This method copies the contents of the specified source file
     * to a file of the same name in the specified destination directory.
     * The destination directory is created if it does not exist.
     * If the destination file exists, then this method will overwrite it.
     *
     *
     * **Note:** Setting `preserveFileDate` to
     * `true` tries to preserve the file's last modified
     * date/times using [File.setLastModified], however it is
     * not guaranteed that the operation will succeed.
     * If the modification operation fails, no indication is provided.
     *
     * @param srcFile          an existing file to copy, must not be `null`
     * @param destDir          the directory to place the copy in, must not be `null`
     * @param preserveFileDate true if the file date of the copy
     * should be the same as the original
     * @throws NullPointerException if source or destination is `null`
     * @throws IOException          if source or destination is invalid
     * @throws IOException          if an IO error occurs during copying
     * @see .copyFile
     * @since 1.3
     */
    //-----------------------------------------------------------------------
    /**
     * Copies a file to a directory preserving the file date.
     *
     *
     * This method copies the contents of the specified source file
     * to a file of the same name in the specified destination directory.
     * The destination directory is created if it does not exist.
     * If the destination file exists, then this method will overwrite it.
     *
     *
     * **Note:** This method tries to preserve the file's last
     * modified date/times using [File.setLastModified], however
     * it is not guaranteed that the operation will succeed.
     * If the modification operation fails, no indication is provided.
     *
     * @param srcFile an existing file to copy, must not be `null`
     * @param destDir the directory to place the copy in, must not be `null`
     * @throws NullPointerException if source or destination is null
     * @throws IOException          if source or destination is invalid
     * @throws IOException          if an IO error occurs during copying
     * @see .copyFile
     */
    @JvmOverloads
    @Throws(IOException::class)
    fun copyFileToDirectory(srcFile: File, destDir: File?, preserveFileDate: Boolean = true) {
        if (destDir == null) {
            throw NullPointerException("Destination must not be null")
        }
        require(!(destDir.exists() && destDir.isDirectory == false)) { "Destination '$destDir' is not a directory" }
        val destFile = File(destDir, srcFile.name)
        copyFile(srcFile, destFile, preserveFileDate)
    }
    /**
     * Copies a file to a new location.
     *
     *
     * This method copies the contents of the specified source file
     * to the specified destination file.
     * The directory holding the destination file is created if it does not exist.
     * If the destination file exists, then this method will overwrite it.
     *
     *
     * **Note:** Setting `preserveFileDate` to
     * `true` tries to preserve the file's last modified
     * date/times using [File.setLastModified], however it is
     * not guaranteed that the operation will succeed.
     * If the modification operation fails, no indication is provided.
     *
     * @param srcFile          an existing file to copy, must not be `null`
     * @param destFile         the new file, must not be `null`
     * @param preserveFileDate true if the file date of the copy
     * should be the same as the original
     * @throws NullPointerException if source or destination is `null`
     * @throws IOException          if source or destination is invalid
     * @throws IOException          if an IO error occurs during copying
     * @see .copyFileToDirectory
     */
    /**
     * Copies a file to a new location preserving the file date.
     *
     *
     * This method copies the contents of the specified source file to the
     * specified destination file. The directory holding the destination file is
     * created if it does not exist. If the destination file exists, then this
     * method will overwrite it.
     *
     *
     * **Note:** This method tries to preserve the file's last
     * modified date/times using [File.setLastModified], however
     * it is not guaranteed that the operation will succeed.
     * If the modification operation fails, no indication is provided.
     *
     * @param srcFile  an existing file to copy, must not be `null`
     * @param destFile the new file, must not be `null`
     * @throws NullPointerException if source or destination is `null`
     * @throws IOException          if source or destination is invalid
     * @throws IOException          if an IO error occurs during copying
     * @see .copyFileToDirectory
     */
    @JvmOverloads
    @Throws(IOException::class)
    fun copyFile(srcFile: File?, destFile: File?,
                 preserveFileDate: Boolean = true) {
        if (srcFile == null) {
            throw NullPointerException("Source must not be null")
        }
        if (destFile == null) {
            throw NullPointerException("Destination must not be null")
        }
        if (srcFile.exists() == false) {
            throw FileNotFoundException("Source '$srcFile' does not exist")
        }
        if (srcFile.isDirectory) {
            throw IOException("Source '$srcFile' exists but is a directory")
        }
        if (srcFile.canonicalPath == destFile.canonicalPath) {
            throw IOException("Source '$srcFile' and destination '$destFile' are the same")
        }
        val parentFile = destFile.parentFile
        if (parentFile != null) {
            if (!parentFile.mkdirs() && !parentFile.isDirectory) {
                throw IOException("Destination '$parentFile' directory cannot be created")
            }
        }
        if (destFile.exists() && destFile.canWrite() == false) {
            throw IOException("Destination '$destFile' exists but is read-only")
        }
        doCopyFile(srcFile, destFile, preserveFileDate)
    }

    /**
     * Internal copy file method.
     *
     * @param srcFile          the validated source file, must not be `null`
     * @param destFile         the validated destination file, must not be `null`
     * @param preserveFileDate whether to preserve the file date
     * @throws IOException if an error occurs
     */
    @Throws(IOException::class)
    private fun doCopyFile(srcFile: File, destFile: File, preserveFileDate: Boolean) {
        if (destFile.exists() && destFile.isDirectory) {
            throw IOException("Destination '$destFile' exists but is a directory")
        }
        var fis: FileInputStream? = null
        var fos: FileOutputStream? = null
        var input: FileChannel? = null
        var output: FileChannel? = null
        try {
            fis = FileInputStream(srcFile)
            fos = FileOutputStream(destFile)
            input = fis.channel
            output = fos.channel
            val size = input.size()
            var pos: Long = 0
            var count: Long = 0
            while (pos < size) {
                count = if (size - pos > FILE_COPY_BUFFER_SIZE) FILE_COPY_BUFFER_SIZE else size - pos
                pos += output.transferFrom(input, pos, count)
            }
        } finally {
            IoUtils.close(output, fos, input, fis)
        }
        if (srcFile.length() != destFile.length()) {
            throw IOException("Failed to copy full contents from '" +
                    srcFile + "' to '" + destFile + "'")
        }
        if (preserveFileDate) {
            destFile.setLastModified(srcFile.lastModified())
        }
    }
    //-----------------------------------------------------------------------
    /**
     * Copies a directory to within another directory preserving the file dates.
     *
     *
     * This method copies the source directory and all its contents to a
     * directory of the same name in the specified destination directory.
     *
     *
     * The destination directory is created if it does not exist.
     * If the destination directory did exist, then this method merges
     * the source with the destination, with the source taking precedence.
     *
     *
     * **Note:** This method tries to preserve the files' last
     * modified date/times using [File.setLastModified], however
     * it is not guaranteed that those operations will succeed.
     * If the modification operation fails, no indication is provided.
     *
     * @param srcDir  an existing directory to copy, must not be `null`
     * @param destDir the directory to place the copy in, must not be `null`
     * @throws NullPointerException if source or destination is `null`
     * @throws IOException          if source or destination is invalid
     * @throws IOException          if an IO error occurs during copying
     * @since 1.2
     */
    @Throws(IOException::class)
    fun copyDirectoryToDirectory(srcDir: File?, destDir: File?) {
        if (srcDir == null) {
            throw NullPointerException("Source must not be null")
        }
        require(!(srcDir.exists() && srcDir.isDirectory == false)) { "Source '$destDir' is not a directory" }
        if (destDir == null) {
            throw NullPointerException("Destination must not be null")
        }
        require(!(destDir.exists() && destDir.isDirectory == false)) { "Destination '$destDir' is not a directory" }
        copyDirectory(srcDir, File(destDir, srcDir.name), true)
    }
    /**
     * Copies a whole directory to a new location.
     *
     *
     * This method copies the contents of the specified source directory
     * to within the specified destination directory.
     *
     *
     * The destination directory is created if it does not exist.
     * If the destination directory did exist, then this method merges
     * the source with the destination, with the source taking precedence.
     *
     *
     * **Note:** Setting `preserveFileDate` to
     * `true` tries to preserve the files' last modified
     * date/times using [File.setLastModified], however it is
     * not guaranteed that those operations will succeed.
     * If the modification operation fails, no indication is provided.
     *
     * @param srcDir           an existing directory to copy, must not be `null`
     * @param destDir          the new directory, must not be `null`
     * @param preserveFileDate true if the file date of the copy
     * should be the same as the original
     * @throws NullPointerException if source or destination is `null`
     * @throws IOException          if source or destination is invalid
     * @throws IOException          if an IO error occurs during copying
     * @since 1.1
     */
    /**
     * Copies a whole directory to a new location preserving the file dates.
     *
     *
     * This method copies the specified directory and all its child
     * directories and files to the specified destination.
     * The destination is the new location and name of the directory.
     *
     *
     * The destination directory is created if it does not exist.
     * If the destination directory did exist, then this method merges
     * the source with the destination, with the source taking precedence.
     *
     *
     * **Note:** This method tries to preserve the files' last
     * modified date/times using [File.setLastModified], however
     * it is not guaranteed that those operations will succeed.
     * If the modification operation fails, no indication is provided.
     *
     * @param srcDir  an existing directory to copy, must not be `null`
     * @param destDir the new directory, must not be `null`
     * @throws NullPointerException if source or destination is `null`
     * @throws IOException          if source or destination is invalid
     * @throws IOException          if an IO error occurs during copying
     * @since 1.1
     */
    @JvmOverloads
    @Throws(IOException::class)
    fun copyDirectory(srcDir: File?, destDir: File?,
                      preserveFileDate: Boolean = true) {
        copyDirectory(srcDir, destDir, null, preserveFileDate)
    }
    /**
     * Copies a filtered directory to a new location.
     *
     *
     * This method copies the contents of the specified source directory
     * to within the specified destination directory.
     *
     *
     * The destination directory is created if it does not exist.
     * If the destination directory did exist, then this method merges
     * the source with the destination, with the source taking precedence.
     *
     *
     * **Note:** Setting `preserveFileDate` to
     * `true` tries to preserve the files' last modified
     * date/times using [File.setLastModified], however it is
     * not guaranteed that those operations will succeed.
     * If the modification operation fails, no indication is provided.
     *
     *
     * <h4>Example: Copy directories only</h4>
     * <pre>
     * // only copy the directory structure
     * FileUtils.copyDirectory(srcDir, destDir, DirectoryFileFilter.DIRECTORY, false);
    </pre> *
     *
     *
     * <h4>Example: Copy directories and txt files</h4>
     * <pre>
     * // Create a filter for ".txt" files
     * IOFileFilter txtSuffixFilter = FileFilterUtils.suffixFileFilter(".txt");
     * IOFileFilter txtFiles = FileFilterUtils.andFileFilter(FileFileFilter.FILE, txtSuffixFilter);
     *
     * // Create a filter for either directories or ".txt" files
     * FileFilter filter = FileFilterUtils.orFileFilter(DirectoryFileFilter.DIRECTORY, txtFiles);
     *
     * // Copy using the filter
     * FileUtils.copyDirectory(srcDir, destDir, filter, false);
    </pre> *
     *
     * @param srcDir           an existing directory to copy, must not be `null`
     * @param destDir          the new directory, must not be `null`
     * @param filter           the filter to apply, null means copy all directories and files
     * @param preserveFileDate true if the file date of the copy
     * should be the same as the original
     * @throws NullPointerException if source or destination is `null`
     * @throws IOException          if source or destination is invalid
     * @throws IOException          if an IO error occurs during copying
     * @since 1.4
     */
    /**
     * Copies a filtered directory to a new location preserving the file dates.
     *
     *
     * This method copies the contents of the specified source directory
     * to within the specified destination directory.
     *
     *
     * The destination directory is created if it does not exist.
     * If the destination directory did exist, then this method merges
     * the source with the destination, with the source taking precedence.
     *
     *
     * **Note:** This method tries to preserve the files' last
     * modified date/times using [File.setLastModified], however
     * it is not guaranteed that those operations will succeed.
     * If the modification operation fails, no indication is provided.
     *
     *
     * <h4>Example: Copy directories only</h4>
     * <pre>
     * // only copy the directory structure
     * FileUtils.copyDirectory(srcDir, destDir, DirectoryFileFilter.DIRECTORY);
    </pre> *
     *
     *
     * <h4>Example: Copy directories and txt files</h4>
     * <pre>
     * // Create a filter for ".txt" files
     * IOFileFilter txtSuffixFilter = FileFilterUtils.suffixFileFilter(".txt");
     * IOFileFilter txtFiles = FileFilterUtils.andFileFilter(FileFileFilter.FILE, txtSuffixFilter);
     *
     * // Create a filter for either directories or ".txt" files
     * FileFilter filter = FileFilterUtils.orFileFilter(DirectoryFileFilter.DIRECTORY, txtFiles);
     *
     * // Copy using the filter
     * FileUtils.copyDirectory(srcDir, destDir, filter);
    </pre> *
     *
     * @param srcDir  an existing directory to copy, must not be `null`
     * @param destDir the new directory, must not be `null`
     * @param filter  the filter to apply, null means copy all directories and files
     * should be the same as the original
     * @throws NullPointerException if source or destination is `null`
     * @throws IOException          if source or destination is invalid
     * @throws IOException          if an IO error occurs during copying
     * @since 1.4
     */
    @JvmOverloads
    @Throws(IOException::class)
    fun copyDirectory(srcDir: File?, destDir: File?,
                      filter: FileFilter?, preserveFileDate: Boolean = true) {
        if (srcDir == null) {
            throw NullPointerException("Source must not be null")
        }
        if (destDir == null) {
            throw NullPointerException("Destination must not be null")
        }
        if (srcDir.exists() == false) {
            throw FileNotFoundException("Source '$srcDir' does not exist")
        }
        if (srcDir.isDirectory == false) {
            throw IOException("Source '$srcDir' exists but is not a directory")
        }
        if (srcDir.canonicalPath == destDir.canonicalPath) {
            throw IOException("Source '$srcDir' and destination '$destDir' are the same")
        }

        // Cater for destination being directory within the source directory (see IO-141)
        var exclusionList: MutableList<String?>? = null
        if (destDir.canonicalPath.startsWith(srcDir.canonicalPath)) {
            val srcFiles = if (filter == null) srcDir.listFiles() else srcDir.listFiles(filter)
            if (srcFiles != null && srcFiles.size > 0) {
                exclusionList = ArrayList(srcFiles.size)
                for (srcFile in srcFiles) {
                    val copiedFile = File(destDir, srcFile.name)
                    exclusionList.add(copiedFile.canonicalPath)
                }
            }
        }
        doCopyDirectory(srcDir, destDir, filter, preserveFileDate, exclusionList)
    }

    /**
     * Internal copy directory method.
     *
     * @param srcDir           the validated source directory, must not be `null`
     * @param destDir          the validated destination directory, must not be `null`
     * @param filter           the filter to apply, null means copy all directories and files
     * @param preserveFileDate whether to preserve the file date
     * @param exclusionList    List of files and directories to exclude from the copy, may be null
     * @throws IOException if an error occurs
     * @since 1.1
     */
    @Throws(IOException::class)
    private fun doCopyDirectory(srcDir: File, destDir: File, filter: FileFilter?,
                                preserveFileDate: Boolean, exclusionList: List<String?>?) {
        // recurse
        val srcFiles = (if (filter == null) srcDir.listFiles() else srcDir.listFiles(filter))
                ?: // null if abstract pathname does not denote a directory, or if an I/O error occurs
                throw IOException("Failed to list contents of $srcDir")
        if (destDir.exists()) {
            if (destDir.isDirectory == false) {
                throw IOException("Destination '$destDir' exists but is not a directory")
            }
        } else {
            if (!destDir.mkdirs() && !destDir.isDirectory) {
                throw IOException("Destination '$destDir' directory cannot be created")
            }
        }
        if (destDir.canWrite() == false) {
            throw IOException("Destination '$destDir' cannot be written to")
        }
        for (srcFile in srcFiles) {
            val dstFile = File(destDir, srcFile.name)
            if (exclusionList == null || !exclusionList.contains(srcFile.canonicalPath)) {
                if (srcFile.isDirectory) {
                    doCopyDirectory(srcFile, dstFile, filter, preserveFileDate, exclusionList)
                } else {
                    doCopyFile(srcFile, dstFile, preserveFileDate)
                }
            }
        }

        // Do this last, as the above has probably affected directory metadata
        if (preserveFileDate) {
            destDir.setLastModified(srcDir.lastModified())
        }
    }
    //-----------------------------------------------------------------------
    /**
     * Deletes a directory recursively.
     *
     * @param directory directory to delete
     * @throws IOException in case deletion is unsuccessful
     */
    @Throws(IOException::class)
    fun deleteDirectory(directory: File) {
        if (!directory.exists()) {
            return
        }
        if (!isSymlink(directory)) {
            cleanDirectory(directory)
        }
        if (!directory.delete()) {
            val message = "Unable to delete directory $directory."
            throw IOException(message)
        }
    }

    /**
     * Deletes a file, never throwing an exception. If file is a directory, delete it and all sub-directories.
     *
     *
     * The difference between File.delete() and this method are:
     *
     *  * A directory to be deleted does not have to be empty.
     *  * No exceptions are thrown when a file or directory cannot be deleted.
     *
     *
     * @param file file or directory to delete, can be `null`
     * @return `true` if the file or directory was deleted, otherwise
     * `false`
     * @since 1.4
     */
    fun deleteQuietly(file: File?): Boolean {
        if (file == null) {
            return false
        }
        try {
            if (file.isDirectory) {
                cleanDirectory(file)
            }
        } catch (ignored: Exception) {
        }
        return try {
            file.delete()
        } catch (ignored: Exception) {
            false
        }
    }

    /**
     * Cleans a directory without deleting it.
     *
     * @param directory directory to clean
     * @throws IOException in case cleaning is unsuccessful
     */
    @Throws(IOException::class)
    fun cleanDirectory(directory: File) {
        if (!directory.exists()) {
            val message = "$directory does not exist"
            throw IllegalArgumentException(message)
        }
        if (!directory.isDirectory) {
            val message = "$directory is not a directory"
            throw IllegalArgumentException(message)
        }
        val files = directory.listFiles()
                ?: // null if security restricted
                throw IOException("Failed to list contents of $directory")
        var exception: IOException? = null
        for (file in files) {
            try {
                forceDelete(file)
            } catch (ioe: IOException) {
                exception = ioe
            }
        }
        if (null != exception) {
            throw exception
        }
    }
    //-----------------------------------------------------------------------
    /**
     * Waits for NFS to propagate a file creation, imposing a timeout.
     *
     *
     * This method repeatedly tests [File.exists] until it returns
     * true up to the maximum time specified in seconds.
     *
     * @param file    the file to check, must not be `null`
     * @param seconds the maximum time in seconds to wait
     * @return true if file exists
     * @throws NullPointerException if the file is `null`
     */
    fun waitFor(file: File, seconds: Int): Boolean {
        var timeout = 0
        var tick = 0
        while (!file.exists()) {
            if (tick++ >= 10) {
                tick = 0
                if (timeout++ > seconds) {
                    return false
                }
            }
            try {
                Thread.sleep(100)
            } catch (ignore: InterruptedException) {
                // ignore exception
            } catch (ex: Exception) {
                break
            }
        }
        return true
    }
    //-----------------------------------------------------------------------
    /**
     * Deletes a file. If file is a directory, delete it and all sub-directories.
     *
     *
     * The difference between File.delete() and this method are:
     *
     *  * A directory to be deleted does not have to be empty.
     *  * You get exceptions when a file or directory cannot be deleted.
     * (java.io.File methods returns a boolean)
     *
     *
     * @param file file or directory to delete, must not be `null`
     * @throws NullPointerException  if the directory is `null`
     * @throws FileNotFoundException if the file was not found
     * @throws IOException           in case deletion is unsuccessful
     */
    @Throws(IOException::class)
    fun forceDelete(file: File) {
        if (file.isDirectory) {
            deleteDirectory(file)
        } else {
            val filePresent = file.exists()
            if (!file.delete()) {
                if (!filePresent) {
                    throw FileNotFoundException("File does not exist: $file")
                }
                val message = "Unable to delete file: $file"
                throw IOException(message)
            }
        }
    }

    /**
     * Schedules a file to be deleted when JVM exits.
     * If file is directory delete it and all sub-directories.
     *
     * @param file file or directory to delete, must not be `null`
     * @throws NullPointerException if the file is `null`
     * @throws IOException          in case deletion is unsuccessful
     */
    @Throws(IOException::class)
    fun forceDeleteOnExit(file: File) {
        if (file.isDirectory) {
            deleteDirectoryOnExit(file)
        } else {
            file.deleteOnExit()
        }
    }

    /**
     * Schedules a directory recursively for deletion on JVM exit.
     *
     * @param directory directory to delete, must not be `null`
     * @throws NullPointerException if the directory is `null`
     * @throws IOException          in case deletion is unsuccessful
     */
    @Throws(IOException::class)
    private fun deleteDirectoryOnExit(directory: File) {
        if (!directory.exists()) {
            return
        }
        directory.deleteOnExit()
        if (!isSymlink(directory)) {
            cleanDirectoryOnExit(directory)
        }
    }

    /**
     * Cleans a directory without deleting it.
     *
     * @param directory directory to clean, must not be `null`
     * @throws NullPointerException if the directory is `null`
     * @throws IOException          in case cleaning is unsuccessful
     */
    @Throws(IOException::class)
    private fun cleanDirectoryOnExit(directory: File) {
        if (!directory.exists()) {
            val message = "$directory does not exist"
            throw IllegalArgumentException(message)
        }
        if (!directory.isDirectory) {
            val message = "$directory is not a directory"
            throw IllegalArgumentException(message)
        }
        val files = directory.listFiles()
                ?: // null if security restricted
                throw IOException("Failed to list contents of $directory")
        var exception: IOException? = null
        for (file in files) {
            try {
                forceDeleteOnExit(file)
            } catch (ioe: IOException) {
                exception = ioe
            }
        }
        if (null != exception) {
            throw exception
        }
    }

    /**
     * Makes a directory, including any necessary but nonexistent parent
     * directories. If a file already exists with specified name but it is
     * not a directory then an IOException is thrown.
     * If the directory cannot be created (or does not already exist)
     * then an IOException is thrown.
     *
     * @param directory directory to create, must not be `null`
     * @throws NullPointerException if the directory is `null`
     * @throws IOException          if the directory cannot be created or the file already exists but is not a directory
     */
    @Throws(IOException::class)
    fun forceMkdir(directory: File) {
        if (directory.exists()) {
            if (!directory.isDirectory) {
                val message = ("File "
                        + directory
                        + " exists and is "
                        + "not a directory. Unable to create directory.")
                throw IOException(message)
            }
        } else {
            if (!directory.mkdirs()) {
                // Double-check that some other thread or process hasn't made
                // the directory in the background
                if (!directory.isDirectory) {
                    val message = "Unable to create directory $directory"
                    throw IOException(message)
                }
            }
        }
    }
    //-----------------------------------------------------------------------
    /**
     * Returns the size of the specified file or directory. If the provided
     * [File] is a regular file, then the file's length is returned.
     * If the argument is a directory, then the size of the directory is
     * calculated recursively. If a directory or subdirectory is security
     * restricted, its size will not be included.
     *
     * @param file the regular file or directory to return the size
     * of (must not be `null`).
     * @return the length of the file, or recursive size of the directory,
     * provided (in bytes).
     * @throws NullPointerException     if the file is `null`
     * @throws IllegalArgumentException if the file does not exist.
     * @since 2.0
     */
    fun sizeOf(file: File): Long {
        if (!file.exists()) {
            val message = "$file does not exist"
            throw IllegalArgumentException(message)
        }
        return if (file.isDirectory) {
            sizeOfDirectory(file)
        } else {
            file.length()
        }
    }

    /**
     * Returns the size of the specified file or directory. If the provided
     * [File] is a regular file, then the file's length is returned.
     * If the argument is a directory, then the size of the directory is
     * calculated recursively. If a directory or subdirectory is security
     * restricted, its size will not be included.
     *
     * @param file the regular file or directory to return the size
     * of (must not be `null`).
     * @return the length of the file, or recursive size of the directory,
     * provided (in bytes).
     * @throws NullPointerException     if the file is `null`
     * @throws IllegalArgumentException if the file does not exist.
     * @since 2.4
     */
    fun sizeOfAsBigInteger(file: File): BigInteger {
        if (!file.exists()) {
            val message = "$file does not exist"
            throw IllegalArgumentException(message)
        }
        return if (file.isDirectory) {
            sizeOfDirectoryAsBigInteger(file)
        } else {
            BigInteger.valueOf(file.length())
        }
    }

    /**
     * Counts the size of a directory recursively (sum of the length of all files).
     *
     * @param directory directory to inspect, must not be `null`
     * @return size of directory in bytes, 0 if directory is security restricted, a negative number when the real total
     * is greater than [Long.MAX_VALUE].
     * @throws NullPointerException if the directory is `null`
     */
    fun sizeOfDirectory(directory: File): Long {
        checkDirectory(directory)
        val files = directory.listFiles()
                ?: // null if security restricted
                return 0L
        var size: Long = 0
        for (file in files) {
            try {
                if (!isSymlink(file)) {
                    size += sizeOf(file)
                    if (size < 0) {
                        break
                    }
                }
            } catch (ioe: IOException) {
                // Ignore exceptions caught when asking if a File is a symlink.
            }
        }
        return size
    }

    /**
     * Counts the size of a directory recursively (sum of the length of all files).
     *
     * @param directory directory to inspect, must not be `null`
     * @return size of directory in bytes, 0 if directory is security restricted.
     * @throws NullPointerException if the directory is `null`
     * @since 2.4
     */
    fun sizeOfDirectoryAsBigInteger(directory: File): BigInteger {
        checkDirectory(directory)
        val files = directory.listFiles()
                ?: // null if security restricted
                return BigInteger.ZERO
        var size = BigInteger.ZERO
        for (file in files) {
            try {
                if (!isSymlink(file)) {
                    size = size.add(BigInteger.valueOf(sizeOf(file)))
                }
            } catch (ioe: IOException) {
                // Ignore exceptions caught when asking if a File is a symlink.
            }
        }
        return size
    }

    /**
     * Checks that the given `File` exists and is a directory.
     *
     * @param directory The `File` to check.
     * @throws IllegalArgumentException if the given `File` does not exist or is not a directory.
     */
    private fun checkDirectory(directory: File) {
        require(directory.exists()) { "$directory does not exist" }
        require(directory.isDirectory) { "$directory is not a directory" }
    }
    //-----------------------------------------------------------------------
    /**
     * Tests if the specified `File` is newer than the reference
     * `File`.
     *
     * @param file      the `File` of which the modification date must
     * be compared, must not be `null`
     * @param reference the `File` of which the modification date
     * is used, must not be `null`
     * @return true if the `File` exists and has been modified more
     * recently than the reference `File`
     * @throws IllegalArgumentException if the file is `null`
     * @throws IllegalArgumentException if the reference file is `null` or doesn't exist
     */
    fun isFileNewer(file: File?, reference: File?): Boolean {
        requireNotNull(reference) { "No specified reference file" }
        require(reference.exists()) {
            ("The reference file '"
                    + reference + "' doesn't exist")
        }
        return isFileNewer(file, reference.lastModified())
    }

    /**
     * Tests if the specified `File` is newer than the specified
     * `Date`.
     *
     * @param file the `File` of which the modification date
     * must be compared, must not be `null`
     * @param date the date reference, must not be `null`
     * @return true if the `File` exists and has been modified
     * after the given `Date`.
     * @throws IllegalArgumentException if the file is `null`
     * @throws IllegalArgumentException if the date is `null`
     */
    fun isFileNewer(file: File?, date: Date?): Boolean {
        requireNotNull(date) { "No specified date" }
        return isFileNewer(file, date.time)
    }

    /**
     * Tests if the specified `File` is newer than the specified
     * time reference.
     *
     * @param file       the `File` of which the modification date must
     * be compared, must not be `null`
     * @param timeMillis the time reference measured in milliseconds since the
     * epoch (00:00:00 GMT, January 1, 1970)
     * @return true if the `File` exists and has been modified after
     * the given time reference.
     * @throws IllegalArgumentException if the file is `null`
     */
    fun isFileNewer(file: File?, timeMillis: Long): Boolean {
        requireNotNull(file) { "No specified file" }
        return if (!file.exists()) {
            false
        } else file.lastModified() > timeMillis
    }
    //-----------------------------------------------------------------------
    /**
     * Tests if the specified `File` is older than the reference
     * `File`.
     *
     * @param file      the `File` of which the modification date must
     * be compared, must not be `null`
     * @param reference the `File` of which the modification date
     * is used, must not be `null`
     * @return true if the `File` exists and has been modified before
     * the reference `File`
     * @throws IllegalArgumentException if the file is `null`
     * @throws IllegalArgumentException if the reference file is `null` or doesn't exist
     */
    fun isFileOlder(file: File?, reference: File?): Boolean {
        requireNotNull(reference) { "No specified reference file" }
        require(reference.exists()) {
            ("The reference file '"
                    + reference + "' doesn't exist")
        }
        return isFileOlder(file, reference.lastModified())
    }

    /**
     * Tests if the specified `File` is older than the specified
     * `Date`.
     *
     * @param file the `File` of which the modification date
     * must be compared, must not be `null`
     * @param date the date reference, must not be `null`
     * @return true if the `File` exists and has been modified
     * before the given `Date`.
     * @throws IllegalArgumentException if the file is `null`
     * @throws IllegalArgumentException if the date is `null`
     */
    fun isFileOlder(file: File?, date: Date?): Boolean {
        requireNotNull(date) { "No specified date" }
        return isFileOlder(file, date.time)
    }

    /**
     * Tests if the specified `File` is older than the specified
     * time reference.
     *
     * @param file       the `File` of which the modification date must
     * be compared, must not be `null`
     * @param timeMillis the time reference measured in milliseconds since the
     * epoch (00:00:00 GMT, January 1, 1970)
     * @return true if the `File` exists and has been modified before
     * the given time reference.
     * @throws IllegalArgumentException if the file is `null`
     */
    fun isFileOlder(file: File?, timeMillis: Long): Boolean {
        requireNotNull(file) { "No specified file" }
        return if (!file.exists()) {
            false
        } else file.lastModified() < timeMillis
    }

    /**
     * Moves a directory.
     *
     *
     * When the destination directory is on another file system, do a "copy and delete".
     *
     * @param srcDir  the directory to be moved
     * @param destDir the destination directory
     * @throws NullPointerException if source or destination is `null`
     * @throws IOException          if the destination directory exists
     * @throws IOException          if source or destination is invalid
     * @throws IOException          if an IO error occurs moving the file
     * @since 1.4
     */
    @Throws(IOException::class)
    fun moveDirectory(srcDir: File?, destDir: File?) {
        if (srcDir == null) {
            throw NullPointerException("Source must not be null")
        }
        if (destDir == null) {
            throw NullPointerException("Destination must not be null")
        }
        if (!srcDir.exists()) {
            throw FileNotFoundException("Source '$srcDir' does not exist")
        }
        if (!srcDir.isDirectory) {
            throw IOException("Source '$srcDir' is not a directory")
        }
        if (destDir.exists()) {
            throw IOException("Destination '$destDir' already exists")
        }
        val rename = srcDir.renameTo(destDir)
        if (!rename) {
            if (destDir.canonicalPath.startsWith(srcDir.canonicalPath)) {
                throw IOException("Cannot move directory: $srcDir to a subdirectory of itself: $destDir")
            }
            copyDirectory(srcDir, destDir)
            deleteDirectory(srcDir)
            if (srcDir.exists()) {
                throw IOException("Failed to delete original directory '" + srcDir +
                        "' after copy to '" + destDir + "'")
            }
        }
    }

    /**
     * Moves a directory to another directory.
     *
     * @param src           the file to be moved
     * @param destDir       the destination file
     * @param createDestDir If `true` create the destination directory,
     * otherwise if `false` throw an IOException
     * @throws NullPointerException if source or destination is `null`
     * @throws IOException          if the directory exists in the destination directory
     * @throws IOException          if source or destination is invalid
     * @throws IOException          if an IO error occurs moving the file
     * @since 1.4
     */
    @Throws(IOException::class)
    fun moveDirectoryToDirectory(src: File?, destDir: File?, createDestDir: Boolean) {
        if (src == null) {
            throw NullPointerException("Source must not be null")
        }
        if (destDir == null) {
            throw NullPointerException("Destination directory must not be null")
        }
        if (!destDir.exists() && createDestDir) {
            destDir.mkdirs()
        }
        if (!destDir.exists()) {
            throw FileNotFoundException("Destination directory '" + destDir +
                    "' does not exist [createDestDir=" + createDestDir + "]")
        }
        if (!destDir.isDirectory) {
            throw IOException("Destination '$destDir' is not a directory")
        }
        moveDirectory(src, File(destDir, src.name))
    }

    /**
     * Moves a file.
     *
     *
     * When the destination file is on another file system, do a "copy and delete".
     *
     * @param srcFile  the file to be moved
     * @param destFile the destination file
     * @throws NullPointerException if source or destination is `null`
     * @throws IOException          if the destination file exists
     * @throws IOException          if source or destination is invalid
     * @throws IOException          if an IO error occurs moving the file
     * @since 1.4
     */
    @Throws(IOException::class)
    fun moveFile(srcFile: File?, destFile: File?) {
        if (srcFile == null) {
            throw NullPointerException("Source must not be null")
        }
        if (destFile == null) {
            throw NullPointerException("Destination must not be null")
        }
        if (!srcFile.exists()) {
            throw FileNotFoundException("Source '$srcFile' does not exist")
        }
        if (srcFile.isDirectory) {
            throw IOException("Source '$srcFile' is a directory")
        }
        if (destFile.exists()) {
            throw IOException("Destination '$destFile' already exists")
        }
        if (destFile.isDirectory) {
            throw IOException("Destination '$destFile' is a directory")
        }
        val rename = srcFile.renameTo(destFile)
        if (!rename) {
            copyFile(srcFile, destFile)
            if (!srcFile.delete()) {
                deleteQuietly(destFile)
                throw IOException("Failed to delete original file '" + srcFile +
                        "' after copy to '" + destFile + "'")
            }
        }
    }

    /**
     * Moves a file to a directory.
     *
     * @param srcFile       the file to be moved
     * @param destDir       the destination file
     * @param createDestDir If `true` create the destination directory,
     * otherwise if `false` throw an IOException
     * @throws NullPointerException if source or destination is `null`
     * @throws IOException          if the destination file exists
     * @throws IOException          if source or destination is invalid
     * @throws IOException          if an IO error occurs moving the file
     * @since 1.4
     */
    @Throws(IOException::class)
    fun moveFileToDirectory(srcFile: File?, destDir: File?, createDestDir: Boolean) {
        if (srcFile == null) {
            throw NullPointerException("Source must not be null")
        }
        if (destDir == null) {
            throw NullPointerException("Destination directory must not be null")
        }
        if (!destDir.exists() && createDestDir) {
            destDir.mkdirs()
        }
        if (!destDir.exists()) {
            throw FileNotFoundException("Destination directory '" + destDir +
                    "' does not exist [createDestDir=" + createDestDir + "]")
        }
        if (!destDir.isDirectory) {
            throw IOException("Destination '$destDir' is not a directory")
        }
        moveFile(srcFile, File(destDir, srcFile.name))
    }

    /**
     * Moves a file or directory to the destination directory.
     *
     *
     * When the destination is on another file system, do a "copy and delete".
     *
     * @param src           the file or directory to be moved
     * @param destDir       the destination directory
     * @param createDestDir If `true` create the destination directory,
     * otherwise if `false` throw an IOException
     * @throws NullPointerException if source or destination is `null`
     * @throws IOException          if the directory or file exists in the destination directory
     * @throws IOException          if source or destination is invalid
     * @throws IOException          if an IO error occurs moving the file
     * @since 1.4
     */
    @Throws(IOException::class)
    fun moveToDirectory(src: File?, destDir: File?, createDestDir: Boolean) {
        if (src == null) {
            throw NullPointerException("Source must not be null")
        }
        if (destDir == null) {
            throw NullPointerException("Destination must not be null")
        }
        if (!src.exists()) {
            throw FileNotFoundException("Source '$src' does not exist")
        }
        if (src.isDirectory) {
            moveDirectoryToDirectory(src, destDir, createDestDir)
        } else {
            moveFileToDirectory(src, destDir, createDestDir)
        }
    }

    /**
     * Determines whether the specified file is a Symbolic Link rather than an actual file.
     *
     *
     * Will not return true if there is a Symbolic Link anywhere in the path,
     * only if the specific file is.
     *
     *
     * **Note:** the current implementation always returns `false` if the system
     * is detected as Windows using \\
     *
     * @param file the file to check
     * @return true if the file is a Symbolic Link
     * @throws IOException if an IO error occurs while checking the file
     * @since 2.0
     */
    @Throws(IOException::class)
    fun isSymlink(file: File?): Boolean {
        if (file == null) {
            throw NullPointerException("File must not be null")
        }
        if (File.separatorChar == '\\') {
            return false
        }
        var fileInCanonicalDir: File? = null
        fileInCanonicalDir = if (file.parent == null) {
            file
        } else {
            val canonicalDir = file.parentFile.canonicalFile
            File(canonicalDir, file.name)
        }
        return fileInCanonicalDir.canonicalFile != fileInCanonicalDir.absoluteFile
    }
}