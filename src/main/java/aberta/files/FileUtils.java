/*
MIT License

Copyright (c) 2019 Aberta Ltd.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package aberta.files;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility functions for handling files
 *
 * @author chris
 */
public class FileUtils {

    /**
     * Moves a file, residing in one directory to a destination file or
     * directory. Refer to the documentation for
     * {@link #moveFile(Logger, String, Object...)}.
     *
     * @param sourceFilePath the absolute or relative path to the file to move
     * @param toPathSegments a destination directory and/or file name to move
     * to.
     * @return the path of the moved file
     */
    public static String moveFile(String sourceFilePath,
                                  Object... toPathSegments) {
        return moveFile(null, sourceFilePath, toPathSegments);
    }

    /**
     * Moves a file, residing in one directory to a destination file or
     * directory. Refer to the documentation for
     * {@link #move(Logger, Path,Path,boolean,boolean)}.
     *
     * @param logger optional Logger
     * @param sourceFilePath the absolute or relative path to the file to move
     * @param toPathSegments a destination directory and/or file name to move
     * to. If the path does not exist and the final segment in the path does not
     * contain a file extension or ends in a file separator ("\" in Windows, "/"
     * in Linux) then the destination is treated as directory. If you have
     * dynamic elements to the path you can pass each segment as a separate
     * parameters, e.g. move("myfile.dat", "archive", year, month). If a path
     * segment is not a String then toString() is applied to the segment.
     * @return the path of the moved file
     */
    public static String moveFile(Logger logger, String sourceFilePath,
                                  Object... toPathSegments) {
        if (logger != null) {
            logger.log(Level.INFO,
                       "moveFile(" + sourceFilePath + ", " + toPathSegments + ")");
        }

        if (sourceFilePath == null || sourceFilePath.trim().isEmpty()) {
            throw new RuntimeException("no source path specified");
        }

        if (toPathSegments.length == 0) {
            throw new RuntimeException("no destination path specified");
        }

        List<String> segments = new ArrayList<>();
        for (Object segment : toPathSegments) {
            if (segment == null) {
                throw new IllegalArgumentException(
                        "destination path contains a null");
            }
            segments.add(segment.toString());
        }

        FileSystem fs = FileSystems.getDefault();
        Path source = null;
        Path destination = null;

        try {
            source = fs.getPath(sourceFilePath);

            String first = segments.get(0);
            String last = segments.get(segments.size() - 1);

            if (segments.size() > 1) {
                List<String> subdirs = segments.subList(1, segments.size());
                destination = fs.getPath(first, subdirs.toArray(new String[0]));
            } else {
                destination = fs.getPath(first);
            }

            boolean isDir = isOrlooksLikeADirectory(destination);
            if (!isDir) {
                isDir = last.endsWith(File.separator);
            }
            return move(logger, source, destination, isDir, false).toString();

        } catch (InvalidPathException ex) {
            throw new RuntimeException(
                    (source == null)
                    ? "Invalid source path '" + sourceFilePath + "'"
                    : "Invalid destination path '" + destination + "'",
                    ex);
        }
    }

    /**
     * Moves a file, residing in one directory to a destination file or
     * directory. Refer to the documentation for
     * {@link #moveFile(Logger,String,Object...)}.
     *
     * @param sourceDir the source directory
     * @param sourceFile the source file name
     * @param destinationPath the destination file or directory
     * @return the path of the moved file
     */
    public static String moveDirectoryFile(String sourceDir,
                                           String sourceFile,
                                           Object... destinationPath) {
        return moveFile(null, sourceDir + File.separator + sourceFile,
                        destinationPath);
    }

    /**
     * Moves a file, residing in one directory to a destination file or
     * directory. Refer to the documentation for
     * {@link #moveFile(Logger,String,Object...)}.
     *
     * @param logger optional Logger
     * @param sourceDir the source directory
     * @param sourceFile the source file name
     * @param destinationPath the destination file or directory
     * @return the path of the moved file
     */
    public static String moveDirectoryFile(Logger logger, String sourceDir,
                                           String sourceFile,
                                           Object... destinationPath) {
        if (logger != null) {
            logger.log(Level.INFO,
                       "moveDirectoryFile(" + sourceDir + ", " + sourceFile + ", " + destinationPath + ")");
        }
        return moveFile(logger, sourceDir + File.separator + sourceFile,
                        destinationPath);
    }

    /**
     * Moves a file to a destination file or directory. The move is performed
     * atomically unless the underlying file system does not support atomic
     * moves. All required directories are created before the file is moved. If
     * the file already exists in the destination directory then a numeric
     * suffix is added to the file (before the extension). For example, if
     * order.dat already exists then order_1.dat will be created. If that file
     * exists too then the suffix will be incremented, and so on (up to 100).
     *
     * @param source the file to be moved
     * @param destination the destination file or directory
     * @param destinationIsDirectory if true the destination is treated as
     * directory
     * @param allowReplaceExisting if true an existing file can be replaced
     * @param logger optional Logger
     * @return the path of the moved file
     */
    public static Path move(Logger logger, Path source, Path destination,
                            boolean destinationIsDirectory,
                            boolean allowReplaceExisting
    ) {
        if (logger != null) {
            logger.log(Level.INFO,
                       "move(" + source + ", " + destination + ", destinationIsDir:" + destinationIsDirectory + ", allowReplaceExisting:" + allowReplaceExisting + ")");
        }

        if (source == null) {
            throw new RuntimeException("no source path specified");
        }
        if (Files.isDirectory(source)) {
            throw new RuntimeException(
                    "source '" + source.toString() + "' is a directory");
        }
        if (!Files.exists(source)) {
            throw new RuntimeException(
                    "source file '" + source.toString() + "' does not exist");
        }

        if (destination == null) {
            throw new RuntimeException("no destination path specified");
        }

        Path dirToCheck = destinationIsDirectory
                          ? destination : destination.getParent();
        try {
            if (dirToCheck != null && !Files.exists(dirToCheck)) {
                try {
                    if (logger != null) {
                        logger.log(Level.INFO,
                                   "creating directory " + dirToCheck);
                    }

                    Files.createDirectories(dirToCheck);
                } catch (FileAlreadyExistsException ex) {
                    throw new RuntimeException(
                            dirToCheck.toString() + " already exists and is not a directory");
                } catch (IOException ex) {
                    throw new RuntimeException(
                            "failed to create directories for " + dirToCheck.
                            toString());
                }
            }

            String sourceFile = source.getFileName().toString();

            Path toPath = createPath(logger, source, destination,
                                     allowReplaceExisting);
            Path resultingPath = null;

            try {
                if (allowReplaceExisting) {
                    resultingPath = Files.move(source, toPath,
                                               StandardCopyOption.ATOMIC_MOVE,
                                               StandardCopyOption.REPLACE_EXISTING);
                } else {
                    resultingPath = Files.move(source, toPath,
                                               StandardCopyOption.ATOMIC_MOVE);
                }

            } catch (AtomicMoveNotSupportedException ex) {

                if (logger != null) {
                    logger.log(Level.WARNING,
                               "atomic move not supported, trying without option ATOMIC_MOVE");
                }

                if (allowReplaceExisting) {
                    resultingPath = Files.move(source, toPath,
                                               StandardCopyOption.REPLACE_EXISTING);
                } else {
                    resultingPath = Files.move(source, toPath);
                }
            }

            if (logger != null) {
                logger.log(Level.INFO,
                           "file  " + source + " moved to " + resultingPath);
            }
            return resultingPath;
            
        } catch (IOException ex) {
            throw new RuntimeException(
                    "Cannot move file " + source + " to " + destination, ex);
        }
    }

    private static Path createPath(Logger logger, Path from, Path to,
                                   boolean allowReplaceExisting) {

        String fileName = (Files.isDirectory(to) ? from : to)
                .getFileName()
                .toString();

        String file;
        String extension = null;

        Matcher m = Pattern.compile("(\\S+)\\.(\\S+)").matcher(fileName);
        if (m.matches()) {
            file = m.group(1);
            extension = m.group(2);
        } else {
            file = fileName;
        }

        Path dir = Files.isDirectory(to) ? to : to.getParent();
        if (dir == null) {
            dir = Paths.get("");
        }

        Path newPath = dir.resolve(fileName);

        if (from.equals(newPath) || allowReplaceExisting) {
            return newPath;
        }

        if (Files.exists(newPath)) {
            if (logger != null) {
                logger.
                        log(Level.WARNING,
                            "file  " + newPath + " already exists");
            }

            int i = 1;

            while (Files.exists(newPath)) {
                if (i > 100) {
                    throw new RuntimeException(
                            "limit of duplicate file names reached");
                }

                fileName = file + "_" + (i++);
                if (extension != null) {
                    fileName = fileName + "." + extension;
                }

                newPath = dir.resolve(fileName);
            }
        }
        return newPath;
    }

    private static boolean isOrlooksLikeADirectory(Path p) {

        if (Files.isDirectory(p)) {
            return true;
        }
        if (Files.exists(p)) {
            return true;
        }

        boolean hasExtension = Pattern.compile(".*\\..*")
                .matcher(p.getFileName().toString()).matches();
        return (!hasExtension) || p.endsWith(File.separator);
    }

    /**
     * @param args move source-file destination path,...
     */
    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println(
                    "Usage: move <source file> <destination path>,...");
            System.exit(1);
        }

        Object[] destination = Arrays
                .asList(args)
                .subList(2, args.length).toArray();

        moveFile(Logger.getGlobal(), args[1], destination);
    }
}
