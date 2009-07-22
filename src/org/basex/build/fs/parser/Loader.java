package org.basex.build.fs.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.basex.BaseX;

/**
 * Some utility methods for loading class files from folders, packages or jar
 * files.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-09, ISC License
 * @author Bastian Lemke
 */
public final class Loader extends ClassLoader {

  // [BL] method for loading all subclasses of an abstract class or interface
  /*
   *  load(Package pkg, Class c) {
   *    ...
   *    c.isInstance(Class.forName(classname).newInstance());
   *    ...
   *  }
   *
   *  Also possible without instantiating the class?
   */

  /** The ClassLoader instance to use for loading classes. */
  private static final Loader INSTANCE = new Loader();

  /** Hidden constructor. */
  private Loader() { /* */}

  /**
   * <p>
   * Load some classes from a package that is on the classpath. The classes may
   * either be in a directory or inside a java archive file. If one of the
   * classes is already loaded, nothing is done (but the class will be included
   * in the results).
   * </p>
   * <p>
   * This method breaks after the first error. Subsequent classes are not
   * loaded.
   * </p>
   * @param pkg the package to load the classes from.
   * @param fileNamePattern the pattern used for matching the class names.
   * @return an array with all the loaded classes.
   * @throws IOException if the classes are located inside a JAR file and any
   *           error occurs while reading from this file.
   */
  public static Class<?>[] load(final Package pkg, //
      final Pattern fileNamePattern) throws IOException {
    final ArrayList<Class<?>> loadedClasses = new ArrayList<Class<?>>();
    try {
      final String pkgName = pkg.getName();
      String pkgPath = pkgName.replace('.', '/');
      if(!pkgPath.startsWith("/")) pkgPath = "/" + pkgPath;
      Matcher matcher;
      final URL pkgUrl = Loader.class.getResource(pkgPath);
      if(pkgUrl == null) return new Class<?>[0];
      final File packageDir = new File(pkgUrl.getFile());
      if(packageDir.exists()) { // package located on disk (as directory)
        for(final File f : packageDir.listFiles()) {
          String fileName = f.getName();
          if(!fileName.endsWith(".class")) continue;
          fileName = fileName.substring(0, fileName.length() - 6);
          matcher = fileNamePattern.matcher(fileName);
          if(matcher.matches()) {
            final String clazz = pkgName + "." + fileName;
            loadedClasses.add(Class.forName(clazz));
          }
        }
      } else { // package is inside a JAR file
        final JarURLConnection conn =
          (JarURLConnection) pkgUrl.openConnection();
        final JarFile jfile = conn.getJarFile();
        final String starts = conn.getEntryName();
        final Enumeration<JarEntry> e = jfile.entries();
        while(e.hasMoreElements()) {
          // [BL] avoid sequential scan of ALL jar entries
          final JarEntry entry = e.nextElement();
          final String name = entry.getName();
          if(name.startsWith(starts) && //
              name.lastIndexOf('/') <= starts.length() && // skip sub-pkgs
              name.endsWith(".class")) {
            String classname = name.substring(0, name.length() - 6);
            final int i = classname.lastIndexOf('/') + 1;
            final String shortName = classname.substring(i);
            matcher = fileNamePattern.matcher(shortName);
            if(matcher.matches()) {
              if(classname.startsWith("/")) classname = classname.substring(1);
              classname = classname.replace('/', '.');
              loadedClasses.add(Class.forName(classname));
            }
          }
        }

      }
    } catch(final IOException e) {
      throw e;
    } catch(final Throwable t) {
      // catch all exceptions and JVM errors and break after the first error.
      BaseX.errln("Failed to load class (%)", t.getMessage());
    }
    return loadedClasses.toArray(new Class<?>[loadedClasses.size()]);
  }

  /**
   * <p>
   * Load some classes from the given directory that is not on the classpath. If
   * one of the classes is already loaded, nothing is done (but the class will
   * be included in the results). This method breaks after the first error.
   * Subsequent classes are not loaded.
   * </p>
   * <p>
   * There may be restrictions for the usage of this classes (e.g. if they
   * extend an abstract class that was loaded before with a different
   * {@link ClassLoader}). Classes that were loaded with a different
   * {@link ClassLoader} may not be able to access fields or methods from these
   * classes.
   * </p>
   * <p>
   * <b> Whenever possible, use {@link #load(Package, Pattern)} instead of this
   * method to avoid problems. </b> Only use this method if the classes are not
   * on the classpath.
   * </p>
   * @param directory the directory to load the classes from.
   * @param fileNamePattern the pattern used for matching the class names. All
   *          inner (sub)subclasses that lives in extra class files (e.g.
   *          MP3Parser$Frame and MP3Parser$Frame$1) must be included in the
   *          pattern.
   * @return an array with all the loaded classes.
   * @throws IOException if any error occurs while reading from a file.
   */
  public static Class<?>[] load(final File directory,
      final Pattern fileNamePattern) throws IOException {
    Matcher matcher;
    final ArrayList<Class<?>> foundClasses = new ArrayList<Class<?>>();
    final ArrayList<File> subClasses = new ArrayList<File>();
    final ArrayList<File> subSubClasses = new ArrayList<File>();
    try {
      if(!directory.isDirectory()) throw new IllegalArgumentException(
          "Is not a directory.");
      for(final File f : directory.listFiles()) {
        String fileName = f.getName();
        fileName = fileName.substring(0, fileName.length() - 6);
        matcher = fileNamePattern.matcher(fileName);
        // [BL] detect (sub)subclasses (not matched by the pattern)
        if(matcher.matches()) {
          // hack to detect (sub)subclasses that must be loaded after the
          // classes
          if(fileName.contains("$")) {
            if(fileName.indexOf('$') != fileName.lastIndexOf('$')) {
              subSubClasses.add(f);
              continue;
            }
            subClasses.add(f);
            continue;
          }
          final Class<?> c = load(f);
          foundClasses.add(c);
        }
      }
      // load subclasses
      for(final File f : subClasses)
        load(f);
      // load subsubclasses
      for(final File f : subSubClasses)
        load(f);
    } catch(final IOException e) {
      throw e;
    } catch(final Throwable t) {
      // catch all exceptions and JVM errors and break after the first error.
      BaseX.errln("Failed to load class (%)", t.getMessage());
    }
    final int counter = initializeClasses(foundClasses);
    // return only the correctly initialized classes
    return foundClasses.subList(0, counter).toArray(new Class<?>[counter]);
  }

  /**
   * <p>
   * Load some classes from the given jar file. If one of the classes is already
   * loaded, nothing is done (but the class will be included in the results).
   * This method breaks after the first error. Subsequent classes are not
   * loaded.
   * </p>
   * <p>
   * There may be restrictions for the usage of this classes (e.g. if they
   * extend an abstract class that was loaded before with a different
   * {@link ClassLoader}). Classes that were loaded with a different
   * {@link ClassLoader} may not be able to access fields or methods from these
   * classes!
   * </p>
   * <p>
   * <b>Whenever possible, use {@link #load(Package, Pattern)} instead of this
   * method to avoid problems.</b> Only use this method if the classes are not
   * on the classpath.
   * </p>
   * @param jar the {@link JarFile} to load the classes from.
   * @return an array with all the loaded classes.
   * @throws IOException if one of the classes could not be read.
   */
  public static Class<?>[] load(final JarFile jar) throws IOException {
    final Enumeration<JarEntry> e = jar.entries();
    final ArrayList<Class<?>> foundClasses = new ArrayList<Class<?>>();
    final ArrayList<JarEntry> subClasses = new ArrayList<JarEntry>();
    final ArrayList<JarEntry> subSubClasses = new ArrayList<JarEntry>();
    try {
      while(e.hasMoreElements()) {
        final JarEntry entry = e.nextElement();
        final String name = entry.getName();
        if(entry.isDirectory() || name.endsWith("MANIFEST.MF")) {
          continue;
        }
        // hack to detect (sub)subclasses that must be loaded after the classes
        if(name.contains("$")) {
          if(name.indexOf("$") != name.lastIndexOf("$")) {
            subSubClasses.add(entry);
            continue;
          }
          subClasses.add(entry);
          continue;
        }
        final Class<?> c = load(jar, entry);
        foundClasses.add(c);
      }
      // load subclasses
      for(final JarEntry entry : subClasses)
        load(jar, entry);
      // load subsubclasses
      for(final JarEntry entry : subSubClasses)
        load(jar, entry);
    } catch(final IOException ex) {
      throw ex;
    } catch(final Throwable t) { // catch all exceptions an JVM errors
      BaseX.errln("Failed to load class (%)", t.getMessage());
    }
    final int counter = initializeClasses(foundClasses);
    jar.close();
    // return only the correctly initialized classes
    return foundClasses.subList(0, counter).toArray(new Class<?>[counter]);
  }

  /**
   * Reads a single class from the file.
   * @param f the file to read from.
   * @return the (uninitialized) class.
   * @throws IOException if any error occurs while reading from the file.
   */
  private static Class<?> load(final File f) throws IOException {
    final long len = f.length();
    if(len > Integer.MAX_VALUE) throw new IOException(
        "Class file too long to load.");
    final byte[] buf = new byte[(int) len];
    final FileChannel ch = new RandomAccessFile(f, "r").getChannel();
    ch.read(ByteBuffer.wrap(buf));
    ch.close();
    return INSTANCE.defineClass(null, buf, 0, buf.length);
  }

  /**
   * Reads a single class from the jar file.
   * @param jar the jar file to read from.
   * @param je the entry to read from the jar file.
   * @return the (uninitialized) class.
   * @throws IOException if any error occurs while reading from the file.
   */
  private static Class<?> load(final JarFile jar, final JarEntry je)
      throws IOException {
    final long len = je.getSize();
    if(len > Integer.MAX_VALUE) throw new IOException(
        "Class file too long to load.");
    if(len == -1) throw new IOException("Unknown class file size.");
    final InputStream in = jar.getInputStream(je);
    final byte[] buf = new byte[(int) len];
    int pos = 0;
    while(len - pos > 0) {
      final int read = in.read(buf, pos, (int) len - pos);
      if(read == -1) break;
      pos += read;
    }
    return INSTANCE.defineClass(null, buf, 0, buf.length);
  }

  /**
   * Initializes the given classes. Breaks after the first error. Subsequent
   * classes are not initialized.
   * @param classes the classes to initialize.
   * @return the number of successfully initialized classes (breaks after the
   *         first error).
   */
  private static int initializeClasses(final ArrayList<Class<?>> classes) {
    int counter = 0;
    for(final Class<?> c : classes) {
      try {
        Class.forName(c.getName(), true, INSTANCE);
        counter++;
      } catch(final Throwable t) {
        // catch everything and break after an error.
        BaseX.errln("Failed to load class (%)", t.getMessage());
        break;
      }
    }
    return counter;
  }
}
