# FileUtils

FileUtils is a *very* simple Java library for (currently) moving files.  The motivation for creating FileUtils was to be able to easily move file system files (either locally or to a file share) within a Groovy script in [Dell Boomi](https://boomi.com/).

## Usage

There are three variations on moving files:

* `moveFile` - move a file by specifying a single string for the path of the file to be moved.  
* `moveDirectoryFile` - this is identical to `moveFile` except the source file is split into a directory and a file.
* `move` - moves from one [`Path`](https://docs.oracle.com/javase/8/docs/api/index.html?java/io/File.html) to another.

## `moveFile()`, `moveDirectoryFile()`
`moveFile` takes one parameter for the source file.  `moveDirectoryFile` takes a directory and a file name.  Otherwise the behaviour of both of these methods is the same.  These two methods are the more useful in a Boomi context.

### How to Specify the Destination
You can either specify the complete destination path (which can either be a directory or a file) or split it into individual components.  You are not
restricted to using only `String`s: any object will do as long as the `toString()` method returns the value you want.  If the destination path does not already exist then these methods determine that it is a directory if the file segment in the path either contains no file extension or if the final character is the operating system's file separator (i.e. `\` on Windows and '/' on Linux).  If you want to write to a directory with a file extension (and that directory does not already exist) then use the `move` function.

### Overwrite Protection
These methods will ensure that a file never overwrites another (Use `move()` if you want to allow a file to be replaceable).  If a file already exists then a numeric suffix will be appended to the name (before the file extension).  For example, if `order.csv` already exists then the file will be moved to `order_1.csv`.  If that file also exists then the suffix is increment (up to 100).

### Example
In the following example a Groovy script is processing documents that have been fetched using the Disk Connector.  The Connector creates a number of properties (e.g. `connector.track.disk.directory` and `connector.track.disk.filename`) that can be passed to `moveDirectoryFile`.  The directory to move the file to is taken from a Dynamic Process Property (`DPP_ARCHIVE_DIR`).  Appended to that is the year, month and day so that the archive becomes a hierarchy of folders.
```Groovy
import static com.boomi.execution.ExecutionUtil.*
import static aberta.files.FileUtils.*

for( int i = 0; i < dataContext.getDataCount(); i++ ) {
    Properties props = dataContext.getProperties(i)

    def dir = props.get("connector.track.disk.directory")
    def filename = props.get("connector.track.disk.filename")
    
    def cal = Calendar.getInstance()
    def year = cal.get(Calendar.YEAR)
    def month = cal.get(Calendar.MONTH)+1 // get() returns 0 for January
    def day = cal.get(Calendar.DAY_OF_MONTH)

    def archive_dir = getDynamicProcessProperty("DPP_ARCHIVE_DIR")

    moveDirectoryFile(dir, filename, archive_dir, year, month, day)
}
```

## `move()`

```Java
    public static Path move(Path source, Path destination,
                            boolean destinationIsDirectory,
                            boolean allowReplaceExisting)
```

This method is the one that actually performs the move (both `moveFile` and `moveDirectoryFile` call this method) and gives you more control (at the expensive of being more complicated to use from a Groovy script in Boomi).  See the [`Java API documentation`](https://docs.oracle.com/javase/8/docs/api/index.html?java/io/File.html) for more information on how to create a `Path` object.


## Installation

To be conveniently incorporated into a Boomi process FileUtils is written in [one source file](src/main/java/aberta/fileutils/FileUtils.java).  You can simple cut-and-paste the source code into a *Custom Scripting* step in a *Data Process Shape*.  

Alternatively you can install the `.jar` file into your Account and create a [Custom Library](https://help.boomi.com/bundle/integration/page/c-atm-Custom_Library_components_8844439e-657e-43eb-ab44-27568c52abed.html).

If you want to complile from the source and have a Java SDK and [Maven](https://maven.apache.org/) installed then you can simply clone/download this repository and run `mvn package` to produce your own `.jar` file.
