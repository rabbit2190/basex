package org.basex.build.fs.parser;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import org.basex.BaseX;
import org.basex.build.fs.NewFSParser;
import org.basex.build.fs.parser.Metadata.DataType;
import org.basex.build.fs.parser.Metadata.Definition;
import org.basex.build.fs.parser.Metadata.Element;
import org.basex.build.fs.parser.Metadata.MimeType;
import org.basex.build.fs.parser.Metadata.Type;
import static org.basex.util.Token.*;

/**
 * Parser for MP3 audio files.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-09, ISC License
 * @author Alexander Holupirek
 * @author Bastian Lemke
 *
 * @see <a href="http://www.id3.org/id3v2.4.0-structure">ID3v2.4.0 structure</a>
 * @see <a href="http://www.id3.org/id3v2.4.0-frames">ID3v2.4.0 frames</a>
 */
public final class MP3Parser extends AbstractParser {

  // ---------------------------------------------------------------------------
  // ----- static stuff --------------------------------------------------------
  // ---------------------------------------------------------------------------

  /** ID3v2 header lenght. */
  static final int HEADER_LENGTH = 10;
  /**
   * ID3v2 frame header length.
   * @see "ID3v2.4.0 structure"
   */
  static final int FRAME_HEADER_LENGTH = 10;
  /**
   * A tag MUST contain at least one frame. A frame must be at least 1 byte big,
   * excluding the header.)
   * @see "ID3v2.4.0 structure"
   */
  static final int MINIMAL_FRAME_SIZE = FRAME_HEADER_LENGTH + 1;
  /**
   * All available ID3v1.1 genres. Order is important! ID3v1.1 genres are stored
   * as a one byte value (as last byte in the ID3v1.1 tag). The position in the
   * array represents the "code" of the genre, the textual representation of the
   * genre X is stored at GENRES[X].
   */
  static final byte[][] GENRES = new byte[][] {
      new byte[] { 66, 108, 117, 101, 115}, // Blues
      new byte[] { 67, 108, 97, 115, 115, 105, 99, 32, 82, 111, //
          99, 107}, // Classic Rock
      new byte[] { 67, 111, 117, 110, 116, 114, 121}, // Country
      new byte[] { 68, 97, 110, 99, 101}, // Dance
      new byte[] { 68, 105, 115, 99, 111}, // Disco
      new byte[] { 70, 117, 110, 107}, // Funk
      new byte[] { 71, 114, 117, 110, 103, 101}, // Grunge
      new byte[] { 72, 105, 112, 45, 72, 111, 112}, // Hip-Hop
      new byte[] { 74, 97, 122, 122}, // Jazz
      new byte[] { 77, 101, 116, 97, 108}, // Metal
      new byte[] { 78, 101, 119, 32, 65, 103, 101}, // New Age
      new byte[] { 79, 108, 100, 105, 101, 115}, // Oldies
      new byte[] { 79, 116, 104, 101, 114}, // Other
      new byte[] { 80, 111, 112}, // Pop
      new byte[] { 82, 38, 66}, // R&B
      new byte[] { 82, 97, 112}, // Rap
      new byte[] { 82, 101, 103, 103, 97, 101}, // Reggae
      new byte[] { 82, 111, 99, 107}, // Rock
      new byte[] { 84, 101, 99, 104, 110, 111}, // Techno
      new byte[] { 73, 110, 100, 117, 115, 116, 114, 105, //
          97, 108}, // Industrial
      new byte[] { 65, 108, 116, 101, 114, 110, 97, 116, 105, //
          118, 101}, // Alternative
      new byte[] { 83, 107, 97}, // Ska
      new byte[] { 68, 101, 97, 116, 104, 32, 77, 101, 116, //
          97, 108}, // Death Metal
      new byte[] { 80, 114, 97, 110, 107, 115}, // Pranks
      new byte[] { 83, 111, 117, 110, 100, 116, 114, 97, 99, 107}, // Soundtrack
      new byte[] { 69, 117, 114, 111, 45, 84, 101, 99, 104, 110, //
          111}, // Euro-Techno
      new byte[] { 65, 109, 98, 105, 101, 110, 116}, // Ambient
      new byte[] { 84, 114, 105, 112, 45, 72, 111, 112}, // Trip-Hop
      new byte[] { 86, 111, 99, 97, 108}, // Vocal
      new byte[] { 74, 97, 122, 122, 43, 70, 117, 110, 107}, // Jazz+Funk
      new byte[] { 70, 117, 115, 105, 111, 110}, // Fusion
      new byte[] { 84, 114, 97, 110, 99, 101}, // Trance
      new byte[] { 67, 108, 97, 115, 115, 105, 99, 97, 108}, // Classical
      new byte[] { 73, 110, 115, 116, 114, 117, 109, 101, 110, //
          116, 97, 108}, // Instrumental
      new byte[] { 65, 99, 105, 100}, // Acid
      new byte[] { 72, 111, 117, 115, 101}, // House
      new byte[] { 71, 97, 109, 101}, // Game
      new byte[] { 83, 111, 117, 110, 100, 32, 67, 108, 105, 112}, // Sound Clip
      new byte[] { 71, 111, 115, 112, 101, 108}, // Gospel
      new byte[] { 78, 111, 105, 115, 101}, // Noise
      new byte[] { 65, 108, 116, 101, 114, 110, 82, 111, 99, 107}, // AlternRock
      new byte[] { 66, 97, 115, 115}, // Bass
      new byte[] { 83, 111, 117, 108}, // Soul
      new byte[] { 80, 117, 110, 107}, // Punk
      new byte[] { 83, 112, 97, 99, 101}, // Space
      new byte[] { 77, 101, 100, 105, 116, 97, 116, 105, //
          118, 101}, // Meditative
      new byte[] { 73, 110, 115, 116, 114, 117, 109, 101, 110, 116, 97, 108,
          32, 80, 111, 112}, // Instrumental Pop
      new byte[] { 73, 110, 115, 116, 114, 117, 109, 101, 110, 116, 97, 108,
          32, 82, 111, 99, 107}, // Instrumental Rock
      new byte[] { 69, 116, 104, 110, 105, 99}, // Ethnic
      new byte[] { 71, 111, 116, 104, 105, 99}, // Gothic
      new byte[] { 68, 97, 114, 107, 119, 97, 118, 101}, // Darkwave
      new byte[] { 84, 101, 99, 104, 110, 111, 45, 73, 110, 100, 117, 115, 116,
          114, 105, 97, 108}, // Techno-Industrial
      new byte[] { 69, 108, 101, 99, 116, 114, 111, 110, 105, 99}, // Electronic
      new byte[] { 80, 111, 112, 45, 70, 111, 108, 107}, // Pop-Folk
      new byte[] { 69, 117, 114, 111, 100, 97, 110, 99, 101}, // Eurodance
      new byte[] { 68, 114, 101, 97, 109}, // Dream
      new byte[] { 83, 111, 117, 116, 104, 101, 114, 110, 32, 82, //
          111, 99, 107}, // Southern Rock
      new byte[] { 67, 111, 109, 101, 100, 121}, // Comedy
      new byte[] { 67, 117, 108, 116}, // Cult
      new byte[] { 71, 97, 110, 103, 115, 116, 97}, // Gangsta
      new byte[] { 84, 111, 112, 32, 52, 48}, // Top 40
      new byte[] { 67, 104, 114, 105, 115, 116, 105, 97, 110, 32, 82, //
          97, 112}, // Christian Rap
      new byte[] { 80, 111, 112, 47, 70, 117, 110, 107}, // Pop/Funk
      new byte[] { 74, 117, 110, 103, 108, 101}, // Jungle
      new byte[] { 78, 97, 116, 105, 118, 101, 32, 65, 109, 101, 114, 105, 99,
          97, 110}, // Native American
      new byte[] { 67, 97, 98, 97, 114, 101, 116}, // Cabaret
      new byte[] { 78, 101, 119, 32, 87, 97, 118, 101}, // New Wave
      new byte[] { 80, 115, 121, 99, 104, 97, 100, 101, 108, 105, //
          99}, // Psychadelic
      new byte[] { 82, 97, 118, 101}, // Rave
      new byte[] { 83, 104, 111, 119, 116, 117, 110, 101, 115}, // Showtunes
      new byte[] { 84, 114, 97, 105, 108, 101, 114}, // Trailer
      new byte[] { 76, 111, 45, 70, 105}, // Lo-Fi
      new byte[] { 84, 114, 105, 98, 97, 108}, // Tribal
      new byte[] { 65, 99, 105, 100, 32, 80, 117, 110, 107}, // Acid Punk
      new byte[] { 65, 99, 105, 100, 32, 74, 97, 122, 122}, // Acid Jazz
      new byte[] { 80, 111, 108, 107, 97}, // Polka
      new byte[] { 82, 101, 116, 114, 111}, // Retro
      new byte[] { 77, 117, 115, 105, 99, 97, 108}, // Musical
      new byte[] { 82, 111, 99, 107, 32, 38, 32, 82, 111, 108, //
          108}, // Rock & Roll
      new byte[] { 72, 97, 114, 100, 32, 82, 111, 99, 107}, // Hard Rock
      new byte[] { 70, 111, 108, 107}, // Folk
      new byte[] { 70, 111, 108, 107, 45, 82, 111, 99, 107}, // Folk-Rock
      new byte[] { 78, 97, 116, 105, 111, 110, 97, 108, 32, 70, //
          111, 108, 107}, // National Folk
      new byte[] { 83, 119, 105, 110, 103}, // Swing
      new byte[] { 70, 97, 115, 116, 32, 70, 117, 115, 105, 111, //
          110}, // Fast Fusion
      new byte[] { 66, 101, 98, 111, 98}, // Bebob
      new byte[] { 76, 97, 116, 105, 110}, // Latin
      new byte[] { 82, 101, 118, 105, 118, 97, 108}, // Revival
      new byte[] { 67, 101, 108, 116, 105, 99}, // Celtic
      new byte[] { 66, 108, 117, 101, 103, 114, 97, 115, 115}, // Bluegrass
      new byte[] { 65, 118, 97, 110, 116, 103, 97, 114, 100, 101}, // Avantgarde
      new byte[] { 71, 111, 116, 104, 105, 99, 32, 82, 111, 99, //
          107}, // Gothic Rock
      new byte[] { 80, 114, 111, 103, 114, 101, 115, 115, 105, 118, 101, 32,
          82, 111, 99, 107}, // Progressive Rock
      new byte[] { 80, 115, 121, 99, 104, 101, 100, 101, 108, 105, 99, 32, 82,
          111, 99, 107}, // Psychedelic Rock
      new byte[] { 83, 121, 109, 112, 104, 111, 110, 105, 99, 32, 82, 111, 99,
          107}, // Symphonic Rock
      new byte[] { 83, 108, 111, 119, 32, 82, 111, 99, 107}, // Slow Rock
      new byte[] { 66, 105, 103, 32, 66, 97, 110, 100}, // Big Band
      new byte[] { 67, 104, 111, 114, 117, 115}, // Chorus
      new byte[] { 69, 97, 115, 121, 32, 76, 105, 115, 116, 101, 110, 105, 110,
          103}, // Easy Listening
      new byte[] { 65, 99, 111, 117, 115, 116, 105, 99}, // Acoustic
      new byte[] { 72, 117, 109, 111, 117, 114}, // Humour
      new byte[] { 83, 112, 101, 101, 99, 104}, // Speech
      new byte[] { 67, 104, 97, 110, 115, 111, 110}, // Chanson
      new byte[] { 79, 112, 101, 114, 97}, // Opera
      new byte[] { 67, 104, 97, 109, 98, 101, 114, 32, 77, 117, //
          115, 105, 99}, // Chamber Music
      new byte[] { 83, 111, 110, 97, 116, 97}, // Sonata
      new byte[] { 83, 121, 109, 112, 104, 111, 110, 121}, // Symphony
      new byte[] { 66, 111, 111, 116, 121, 32, 66, 114, 97, //
          115, 115}, // Booty Brass
      new byte[] { 80, 114, 105, 109, 117, 115}, // Primus
      new byte[] { 80, 111, 114, 110, 32, 71, 114, 111, 111, //
          118, 101}, // Porn Groove
      new byte[] { 83, 97, 116, 105, 114, 101}, // Satire
      new byte[] { 83, 108, 111, 119, 32, 74, 97, 109}, // Slow Jam
      new byte[] { 67, 108, 117, 98}, // Club
      new byte[] { 84, 97, 110, 103, 111}, // Tango
      new byte[] { 83, 97, 109, 98, 97}, // Samba
      new byte[] { 70, 111, 108, 107, 108, 111, 114, 101}, // Folklore
      new byte[] { 66, 97, 108, 108, 97, 100}, // Ballad
      new byte[] { 80, 111, 119, 101, 101, 114, 32, 66, 97, 108, 108, //
          97, 100}, // Poweer Ballad
      new byte[] { 82, 104, 121, 116, 109, 105, 99, 32, 83, 111, 117, //
          108}, // Rhytmic Soul
      new byte[] { 70, 114, 101, 101, 115, 116, 121, 108, 101}, // Freestyle
      new byte[] { 68, 117, 101, 116}, // Duet
      new byte[] { 80, 117, 110, 107, 32, 82, 111, 99, 107}, // Punk Rock
      new byte[] { 68, 114, 117, 109, 32, 83, 111, 108, 111}, // Drum Solo
      new byte[] { 65, 32, 67, 97, 112, 101, 108, 97}, // A Capela
      new byte[] { 69, 117, 114, 111, 45, 72, 111, 117, 115, 101}, // Euro-House
      new byte[] { 68, 97, 110, 99, 101, 32, 72, 97, 108, 108}, // Dance Hall
      new byte[] { 71, 111, 97}, // Goa
      new byte[] { 68, 114, 117, 109, 32, 38, 32, 66, 97, 115, //
          115}, // Drum & Bass
      new byte[] { 67, 108, 117, 98, 45, 72, 111, 117, 115, 101}, // Club-House
      new byte[] { 72, 97, 114, 100, 99, 111, 114, 101}, // Hardcore
      new byte[] { 84, 101, 114, 114, 111, 114}, // Terror
      new byte[] { 73, 110, 100, 105, 101}, // Indie
      new byte[] { 66, 114, 105, 116, 80, 111, 112}, // BritPop
      new byte[] { 78, 101, 103, 101, 114, 112, 117, 110, 107}, // Negerpunk
      new byte[] { 80, 111, 108, 115, 107, 32, 80, 117, 110, 107}, // Polsk Punk
      new byte[] { 66, 101, 97, 116}, // Beat
      new byte[] { 67, 104, 114, 105, 115, 116, 105, 97, 110, 32, 71, 97, 110,
          103, 115, 116, 97, 32, 82, 97, 112}, // Christian Gangsta Rap
      new byte[] { 72, 101, 97, 118, 121, 32, 77, 101, 116, //
          97, 108}, // Heavy Metal
      new byte[] { 66, 108, 97, 99, 107, 32, 77, 101, 116, 97, //
          108}, // Black Metal
      new byte[] { 67, 114, 111, 115, 115, 111, 118, 101, 114}, // Crossover
      new byte[] { 67, 111, 110, 116, 101, 109, 112, 111, 114, 97, 114, 121,
          32, 67, 104, 114, 105, 115, 116, 105, //
          97, 110}, // Contemporary Christian
      new byte[] { 67, 104, 114, 105, 115, 116, 105, 97, 110, 32, 82, 111, 99,
          107}, // Christian Rock
      new byte[] { 77, 101, 114, 101, 110, 103, 117, 101}, // Merengue
      new byte[] { 83, 97, 108, 115, 97}, // Salsa
      new byte[] { 84, 114, 97, 115, 104, 32, 77, 101, 116, //
          97, 108}, // Trash Metal
      new byte[] { 65, 110, 105, 109, 101}, // Anime
      new byte[] { 74, 112, 111, 112}, // Jpop
      new byte[] { 83, 121, 110, 116, 104, 112, 111, 112}, // Synthpop
  };

  /** All available picture types for APIC frames. */
  private static final String[] PICTURE_TYPE = new String[] { "Other",
    "file icon",
    "Other file icon",
    "Front cover",
    "Back cover",
    "Leaflet page",
    "Media - e.g. label side of CD",
    "Lead artist or lead performer or soloist",
    "Artist or performer",
    "Conductor",
    "Band or Orchestra",
    "Composer",
    "Lyricist or text writer",
    "Recording Location",
    "During recording",
    "During performance",
    "Movie or video screen capture",
    "A bright coloured fish",
    "Illustration",
    "Band or artist logotype",
    "Publisher or Studio logotype"};

  /** Flag for ISO-8859-1 encoding. */
  private static final int ENC_ISO_8859_1 = 0;
  /**
   * Flag for UTF-16 encoding (with BOM).
   * 
   * @see <a href="http://en.wikipedia.org/wiki/UTF-16/UCS-2">Wikipedia</a>
   */
  private static final int ENC_UTF_16_WITH_BOM = 1;
  /**
   * Flag for UTF-16 encoding (without BOM).
   * 
   * @see <a href="http://en.wikipedia.org/wiki/UTF-16/UCS-2">Wikipedia</a>
   */
  private static final int ENC_UTF_16_NO_BOM = 2;
  /** Flag for UTF-8 encoding. */
  private static final int ENC_UTF_8 = 3;

  static {
    NewFSParser.register("mp3", MP3Parser.class);
  }

  // ---------------------------------------------------------------------------
  // ---------------------------------------------------------------------------
  // ---------------------------------------------------------------------------

  /** Standard constructor. */
  public MP3Parser() {
    super(Type.AUDIO, MimeType.MP3);
  }

  // [BL] add support for extended ID3v2 header
  // [BL] add support for extended tag (before ID3v1 tag)

  // ---------------------------------------------------------------------------

  /** The {@link BufferedFileChannel} to read from. */
  BufferedFileChannel bfc;
  /** The {@link NewFSParser} instance to fire events. */
  NewFSParser fsparser;

  /** {@inheritDoc} */
  @Override
  public boolean check(final BufferedFileChannel bufFC) throws IOException {
    bfc = bufFC;
    return checkID3v2() || checkID3v1();
  }

  /** {@inheritDoc} */
  @Override
  public void readMeta(final BufferedFileChannel f, final NewFSParser fsParser)
      throws IOException {
    fsparser = fsParser;
    bfc = f;
    if(checkID3v2()) readMetaID3v2();
    else if(checkID3v1()) readMetaID3v1();
  }

  /** {@inheritDoc} */
  @Override
  public void readContent(final BufferedFileChannel f,
      final NewFSParser fsParser) {
  // no textual representation for mp3 content ...
  }

  // ---------------------------------------------------------------------------
  // ----- ID3v1 methods -------------------------------------------------------
  // ---------------------------------------------------------------------------

  /**
   * Checks if the file contains a ID3v1 tag and sets the file pointer to the
   * beginning of the tag.
   * @return true if the file contains a valid ID3v1 tag.
   * @throws IOException if any error occurs while reading the file.
   */
  private boolean checkID3v1() throws IOException {
    final long size = bfc.size();
    if(size < 128) return false;
    // ID3v1 tags are located at the end of the file (last 128 bytes)
    // The tag begins with the string "TAG" (first three bytes)
    bfc.position(size - 128);
    bfc.buffer(128);
    return bfc.get() == 'T' && bfc.get() == 'A' && bfc.get() == 'G';
  }

  /**
   * Reads the ID3v1 metadata from the file. {@link #checkID3v1()} must be
   * called before (and must return <code>true</code>).
   * @throws IOException if any error occurs while reading the ID3v1 tag.
   */
  private void readMetaID3v1() throws IOException {
    // tag is already buffered by checkID3v1()
    final byte[] array = new byte[30];
    bfc.get(array, 0, 30);
    if(!ws(array)) fsparser.metaEvent(Element.TITLE, DataType.STRING,
        Definition.NONE, null, array);
    bfc.get(array, 0, 30);
    if(!ws(array)) fsparser.metaEvent(Element.CREATOR, DataType.STRING,
        Definition.ARTIST, null, array);
    bfc.get(array, 0, 30);
    if(!ws(array)) fsparser.metaEvent(Element.ALBUM, DataType.STRING,
        Definition.NONE, null, array);
    final byte[] a2 = new byte[4];
    bfc.get(a2, 0, 4);
    if(!ws(array)) fsparser.metaEvent(Element.DATE, DataType.YEAR,
        Definition.RELEASE_TIME, null, ParserUtil.convertYear(a2));
    bfc.get(array, 0, 30);
    if(array[28] == 0) { // detect ID3v1.1, last byte represents track
      if(array[29] != 0) {
        fsparser.metaEvent(Element.TRACK, DataType.INTEGER, Definition.NONE,
            null, token(array[29]));
        array[29] = 0;
      }
    }
    if(!ws(array)) fsparser.metaEvent(Element.COMMENT, DataType.STRING,
        Definition.NONE, null, array);
    final int genreId = bfc.get() & 0xFF;
    if(genreId != 0) {
      fsparser.metaEvent(Element.GENRE, DataType.STRING, Definition.NONE, null,
          getGenre(genreId));
    }
  }

  // ---------------------------------------------------------------------------
  // ----- ID3v2 methods -------------------------------------------------------
  // ---------------------------------------------------------------------------

  /**
   * Checks if the file contains a ID3v2 tag and sets the file pointer to the
   * beginning of the ID3 header fields.
   * @return true if the file contains a ID3v2 tag.
   * @throws IOException if any error occurs while reading the file.
   */
  private boolean checkID3v2() throws IOException {
    final int size = HEADER_LENGTH + MINIMAL_FRAME_SIZE;
    if(bfc.size() < size) return false;
    // ID3v2 tags are usually located at the beginning of the file.
    // The tag begins with the string "ID3" (first three bytes)
    bfc.buffer(size);
    return bfc.get() == 'I' && bfc.get() == 'D' && bfc.get() == '3';
  }

  /**
   * Reads the ID3v2 metadata from the file. The behavior is undefined if there
   * is no ID3v2 tag available, therefore {@link #checkID3v1()} should always be
   * called before.
   * @throws IOException if any error occurs while reading the ID3v2 tag.
   */
  private void readMetaID3v2() throws IOException {
    int size = readID3v2Header();
    // int remainingFrames = Frame.values().length;
    // [BL] there may be multiple APIC frames

    while(size >= MINIMAL_FRAME_SIZE) {
      // abort if all "interesting" frames have been read
      // if(remainingFrames == 0) break;
      final int res = readID3v2Frame();
      if(res > 0) {
        size -= res;
        // remainingFrames--;
      } else {
        size += res;
        if(size < MINIMAL_FRAME_SIZE) break;
        bfc.skip(-res);
      }
    }
  }

  /**
   * Reads the ID3v2 header and returns the header size.
   * @return the size of the ID3v2 header.
   * @throws IOException if any error occurs while reading the file.
   */
  private int readID3v2Header() throws IOException {
    // already buffered by checkID3v2()
    bfc.position(6); // skip tag identifier, ID3 version fields and flags
    return readSynchsafeInt();
  }

  /**
   * Reads the ID3v2 frame at the current buffer position. Afterwards, the
   * buffer position is set to the first byte after this frame.
   * @return the number of bytes read, {@link Integer#MAX_VALUE} if the end of
   *         the header was detected or the number of bytes read (as negative
   *         number) if the frame was not parsed.
   * @throws IOException if any error occurs while reading the file.
   */
  private int readID3v2Frame() throws IOException {
    bfc.buffer(MINIMAL_FRAME_SIZE);
    final byte[] frameId = new byte[4];
    // padding (some 0x00 bytes) marks correct end of frames.
    if((frameId[0] = (byte) bfc.get()) == 0) return Integer.MAX_VALUE;
    bfc.get(frameId, 1, 3);
    final int frameSize = readSynchsafeInt();
    bfc.skip(2); // skip flags
    Frame frame;
    try {
      frame = Frame.valueOf(string(frameId));
      frame.parse(this, frameSize);
      return frameSize;
    } catch(final IllegalArgumentException e) {
      return -frameSize;
    }
  }

  // ---------------------------------------------------------------------------
  // ----- utility methods -----------------------------------------------------
  // ---------------------------------------------------------------------------

  /**
   * Returns the textual representation of the genre with the code
   * <code>b</code>.
   * @param b the "code" of the genre.
   * @return the textual representation of the genre.
   */
  static byte[] getGenre(final int b) {
    return b < GENRES.length && b >= 0 ? GENRES[b] : EMPTY;
  }

  /**
   * Reads a synchsafe integer (4 bytes) from the channel and converts it to a
   * "normal" integer. In ID3 tags, some integers are encoded as "synchsafe"
   * integers to distinguish them from data in other blocks. The most
   * significant bit of each byte is zero, making seven bits out of eight
   * available.
   * @return the integer.
   */
  private int readSynchsafeInt() {
    return (bfc.get() & 0xFF) << 21 //
        | (bfc.get() & 0xFF) << 14 //
        | (bfc.get() & 0xFF) << 7 //
        | bfc.get() & 0xFF;
  }

  /**
   * Skip the text encoding description bytes.
   * @return the number of skipped bytes.
   * @throws IOException if any error occurs while reading from the file.
   */
  int skipEncBytes() throws IOException {
    bfc.buffer(3);
    // [BL] handle different encodings

    // skip text encoding description bytes
    int bytesToSkip = 0;
    if((bfc.get() & 0xFF) <= 0x04) bytesToSkip++;
    if((bfc.get() & 0xFF) >= 0xFE) bytesToSkip++;
    if((bfc.get() & 0xFF) >= 0xFE) bytesToSkip++;
    bfc.skip(bytesToSkip - 3);
    return bytesToSkip;
  }

  /**
   * Reads the text encoding of the following frame from the file channel.
   * Assure that at least one byte is buffered before calling this method.
   * @return a string with the name of the encoding that was detected or
   *         <code>null</code> if an invalid or unsupported encoding was
   *         detected. If no encoding is set, an empty string is returned.
   * @throws IOException if any error occurs while reading from the file.
   */
  String readEncoding() throws IOException {
    final int c = bfc.get();
    switch(c) {
      case ENC_ISO_8859_1:
        return "ISO-8859-1";
      case ENC_UTF_8:
        return "UTF-8";
      case ENC_UTF_16_NO_BOM:
        BaseX.debug(
            "MP3Parser: Unsupported text encoding (UTF-16 without BOM) found "
                + "(%).", bfc.getFileName());
        return null;
      case ENC_UTF_16_WITH_BOM:
        return "UTF-16";
      default: // no encoding specified
        bfc.skip(-1);
        return "";
    }
  }

  /**
   * Reads and parses text from the file. Assure that at least <code>s</code>
   * bytes are buffered before calling this method.
   * @param s number of bytes to read.
   * @return byte array with the text.
   * @throws IOException if any error occurs while reading from the file.
   */
  byte[] readText(final int s) throws IOException {
    return s <= 1 ? EMPTY : readText(s, readEncoding());
  }

  /**
   * Reads and parses text with the given encoding from the file. Assure that at
   * least <code>s</code> bytes are buffered before calling this method.
   * @param s number of bytes to read.
   * @param encoding the encoding of the text.
   * @return byte array with the text.
   * @throws IOException if any error occurs while reading from the file.
   */
  byte[] readText(final int s, final String encoding) throws IOException {
    int size = s;
    if(size <= 1 || encoding == null) return EMPTY;
    if(bfc.get() != 0) bfc.skip(-1); // skip leading zero byte
    else size--;
    if(encoding.length() == 0) { // no encoding specified
      final byte[] array = new byte[size];
      bfc.get(array);
      return ParserUtil.checkAscii(array);
    }
    final byte[] array = new byte[size - 1];
    return token(new String(bfc.get(array), encoding));
  }

  /**
   * Reads and parses the genre from the file.
   * @param s number of bytes to read.
   * @return byte array with the genre.
   * @throws IOException if any error occurs while reading the file.
   */
  byte[] readGenre(final int s) throws IOException {
    final byte[] value = readText(s);
    int id;
    if(!ws(value)) {
      if(value[0] == '(') { // ignore brackets around genre id
        int limit = 1;
        while(value[limit] >= '0' && value[limit] <= '9' && limit < s)
          limit++;
        id = toInt(value, 1, limit);
      } else id = toInt(value);
      return id == Integer.MIN_VALUE ? value : getGenre(id);
    }
    return EMPTY;
  }

  /**
   * Removes all illegal chars from the byte array. ID3 track numbers may be of
   * the form <code>X/Y</code> (X is the track number, Y represents the number
   * of tracks in the whole set). Everything after '/' is deleted.
   * @param s number of bytes to read
   * @return a byte array that contains only ASCII bytes that are valid integer
   *         numbers.
   * @throws IOException if any error occurs while reading the file.
   */
  byte[] readTrack(final int s) throws IOException {
    final byte[] value = readText(s);
    final int size = value.length;
    if(size == 0) return EMPTY;
    int i = 0;
    while(i < size && (value[i] < '0' || value[i] > '9')) {
      value[i++] = 0;
    }
    if(i >= size) return EMPTY;
    while(i < size && value[i] >= '0' && value[i] <= '9')
      i++;
    while(i < size)
      value[i++] = 0;
    return chopNumber(value);
  }

  /**
   * Reads the file suffix of an embedded picture.
   * @return the file suffix.
   * @throws IOException if any error occurs while reading from the file.
   */
  String readPicSuffix() throws IOException {
    bfc.buffer(9);
    skipEncBytes();
    final StringBuilder sb = new StringBuilder();
    int b;
    while((b = bfc.get()) != 0)
      sb.append((char) b);
    String string = sb.toString();
    if(string.startsWith("image/")) {
      string = string.substring(6); // skip "image/"
    }
    if(string.equals("jpeg")) string = "jpg";
    else if(string.length() != 3) {
      return null;
    }
    return string.toLowerCase();
  }

  /**
   * Reads the picture type id from the APIC frame and returns a textual
   * representation that can be used as file name.
   * @return a textual representation of the picture.
   * @throws IOException if any error occurs while reading from the file.
   */
  String getPicName() throws IOException {
    // there may be more than one APIC frame with the same ID in the ID3 tag
    // [BL] avoid duplicate file names
    bfc.buffer(1);
    final int typeId = bfc.get() & 0xFF;
    if(typeId >= 0 && typeId < PICTURE_TYPE.length) {
      return PICTURE_TYPE[typeId];
    }
    return "Unknown";
  }

  /**
   * Skip the picture description.
   * @throws IOException if any error occurs while reading from the file.
   */
  void skipPicDescription() throws IOException {
    while(true) {
      try {
        if(bfc.get() == 0) break;
      } catch(final BufferUnderflowException e) {
        bfc.buffer(1);
      }
    }
  }

  // ---------------------------------------------------------------------------
  // ----- Frame enumeration that fires all the events -------------------------
  // ---------------------------------------------------------------------------

  /**
   * Mapping for ID3 frames to xml elements.
   * @author Bastian Lemke
   */
  private enum Frame {
    /** */
    TIT2 {
      @Override
      void parse(final MP3Parser obj, final int size) throws IOException {
        obj.fsparser.metaEvent(Element.TITLE, DataType.STRING, Definition.NONE,
            null, obj.readText(size));
      }
    },
    /** */
    TPE1 {
      @Override
      void parse(final MP3Parser obj, final int size) throws IOException {
        obj.fsparser.metaEvent(Element.CREATOR, DataType.STRING,
            Definition.ARTIST, null, obj.readText(size));
      }
    },
    /** */
    TALB {
      @Override
      void parse(final MP3Parser obj, final int size) throws IOException {
        obj.fsparser.metaEvent(Element.ALBUM, DataType.STRING, Definition.NONE,
            null, obj.readText(size));
      }
    },
    /** */
    TYER {
      @Override
      void parse(final MP3Parser obj, final int size) throws IOException {
        obj.fsparser.metaEvent(Element.DATE, DataType.YEAR,
            Definition.RELEASE_TIME, null,
            ParserUtil.convertYear(obj.readText(size)));
      }
    },
    /** */
    TCON {
      @Override
      void parse(final MP3Parser obj, final int size) throws IOException {
        final byte[] value = obj.readGenre(size);
        if(!ws(value)) {
          obj.fsparser.metaEvent(Element.GENRE, DataType.STRING,
              Definition.NONE, null, value);
        }
      }
    },
    /** */
    COMM {
      @Override
      void parse(final MP3Parser obj, final int size) throws IOException {
        final String encoding = obj.readEncoding();
        byte[] lang = obj.readText(3, "");
        for(final byte b : lang) {
          if(ws(b) || b == 0) {
            lang = EMPTY;
            break;
          }
        }
        int pos = 4;
        // ignore short content description
        while(obj.bfc.get() != 0 && ++pos < size)
          ;
        if(pos >= size) return;
        if(ws(lang) || lang[0] == 'X') lang = null;
        obj.fsparser.metaEvent(Element.COMMENT, DataType.STRING,
            Definition.NONE, lang, obj.readText(size - pos, encoding));
      }
    },
    /** */
    TRCK {
      @Override
      void parse(final MP3Parser obj, final int size) throws IOException {
        obj.fsparser.metaEvent(Element.TRACK, DataType.INTEGER,
            Definition.NONE, null, obj.readTrack(size));
      }
    },
    /** */
    TLEN {
      @Override
      void parse(final MP3Parser obj, final int size) throws IOException {
        obj.fsparser.metaEvent(Element.DURATION, DataType.DURATION,
            Definition.NONE, null, ParserUtil.toDuration(obj.readText(size),
                true));
      }
    },
    /** */
    APIC {
      @Override
      void parse(final MP3Parser obj, final int s) throws IOException {
        final long position = obj.bfc.position();
        String suffix = obj.readPicSuffix();
        final String name = obj.getPicName();
        obj.skipPicDescription();
        if(suffix == null) {
          // perhaps, MIME type is missing...
          if(obj.fsparser.isParseable(obj.bfc, "png")) suffix = "png";
          else if(obj.fsparser.isParseable(obj.bfc, "jpg")) suffix = "jpg";
          else {
            BaseX.debug(
                "MP3Parser: Illegal or unsupported picture MIME type (%).",
                obj.bfc.getFileName());
            obj.bfc.skip(s - (obj.bfc.position() - position)); // skip frame
            return;
          }
        }
        final int size = s - (int) (obj.bfc.position() - position);
        try {
          obj.fsparser.parseFileFragment(obj.bfc.subChannel(size), name, //
              suffix);
        } catch(final IOException e) {
          if(NewFSParser.VERBOSE) BaseX.debug(
              "MP3Parser: Failed to parse APIC frame (%).",
              e.getMessage() == null ? obj.bfc.getFileName() : e.getMessage());
        }
      }
    };

    // [BL] TDRC frame
    // [BL] TPOS frame?

    /**
     * <p>
     * Frame specific parse method.
     * </p>
     * @param obj {@link MP3Parser} instance to send parser events from.
     * @param size the size of the frame in bytes.
     * @throws IOException if any error occurs while reading the file.
     */
    abstract void parse(final MP3Parser obj, final int size) throws IOException;
  }
}
