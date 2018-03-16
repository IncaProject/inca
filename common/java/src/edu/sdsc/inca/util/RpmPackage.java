package edu.sdsc.inca.util;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class RpmPackage {

  /**
   * Instantiates an RpmPackage by reading source.  After instantiation the
   * source stream will point to the gzipped tar file contained within the
   * package.
   *
   * @param source An input stream set to return the bytes of an RPM package.
   */
  public RpmPackage(InputStream source) throws IOException {
    DataInputStream dis = new DataInputStream(source);
    this.lead = new RpmLead(dis);
    this.signature = new RpmHeader(dis);
    this.header = new RpmHeader(dis);
  }

  /**
   * Returns the lead from the RPM package.  The lead is an anachronism that
   * should generally be ignored.
   */
  public RpmLead getLead() {
    return this.lead;
  }

  /**
   * Returns the properties from the package header, which contains most of
   * the package properties of interest.
   */
  public RpmProperty[] getHeader() {
    return this.header.getProperties();
  }

  /**
   * Returns the properties from the package signature.
   */
  public RpmProperty[] getSignature() {
    return this.signature.getProperties();
  }

  /**
   * The lead of an RPM package is a fixed-length section that appears in the
   * first 96 bytes.  Its use has been largely abandoned; the information
   * contained within the lead is now duplicated, more flexibly, in the
   * package header and signature.
   */
  public class RpmLead {
    public RpmLead(DataInputStream dis) throws IOException {
      this.bytes = new byte[LEAD_LEN];
      dis.readFully(this.bytes);
    }
    /* TODO: functions for various portions of lead.  Low priority. */
    private byte[] bytes;
    private static final int LEAD_LEN = 96;
    private static final int LEAD_MAGIC = 0xedabeedb;
    private static final short LEAD_VERSION = 0x0300;
  }

  /**
   * An RPM property is a numeric tag, a type indicator, and associated data.
   * The package header and signature each consist of a set of these.
   */
  public class RpmProperty {

    /**
     * Instantiates a RpmProperty with the enclosed values.
     *
     * @param tag   The property tag (see value enumeration below).
     * @param type  The property type (see value enumeration below).
     * @param value The bytes which contain data for the property value.
     *              The interpretation depends of the property type.
     */
    public RpmProperty(int tag, int type, byte[] value) {
      this.tag = tag;
      this.type = type;
      this.value = value;
    }

    /**
     * Returns the property tag.  See value enumeration below.
     */
    public int getTag() {
      return this.tag;
    }

    /**
     * A convenience function that returns a text name for the tag.
     */
    public String getTagAsString(boolean signatureTag) {
      String[] names = signatureTag ? RPMSIGTAG_NAMES : RPMTAG_NAMES;
      int i = this.tag - RPMTAG_NAME;
      return i < 0 || i >= names.length ?
        new Integer(this.tag).toString() : names[i];
    }

    /**
     * Returns the property type.  See value enumeration below.
     */
    public int getType() {
      return this.type;
    }

    /**
     * A convenience function that returns a text name for the type.
     */
    public String getTypeAsString() {
      int i = this.type - TYPE_CHAR;
      return i < 0 || i >= TYPE_NAMES.length ?
        "" :  TYPE_NAMES[this.type - TYPE_CHAR];
    }

    /**
     * Returns the property value.  The number and interpretation depends on
     * the property type.
     */
    public byte[] getValue() {
      return this.value;
    }

    /**
     * Returns the property value in string form.  Returns integers as an
     * integer image, strings as is, and string arrays as one long, newline-
     * separated string.
     */
    public String getValueAsString() {
      if(this.type == TYPE_STRING ||
         this.type == TYPE_STRING_ARRAY ||
         this.type == TYPE_I18NSTRING) {
        String result = "";
        for(int i = 0, start = 0; i < this.value.length; i++, start = i) {
          while(this.value[i] != 0)
            i++;
          if(result.length() > 0)
            result += "\n";
          result += new String(value, start, i - start);
        }
        return result;
      } else {
        int result = 0;
        for(int i = 0; i < this.value.length; i++)
          result = (result << 8) + this.value[i] + (this.value[i]<0 ? 256 : 0);
        return result + "";
      }
    }

    /**
     * Enumeration of signature property tag values.
     */
    public static final int RPMSIGTAG_SIZE = 1000;
    public static final int RPMSIGTAG_LEMD5_1 = 1001;
    public static final int RPMSIGTAG_PGP = 1002;
    public static final int RPMSIGTAG_LEMD5_2 = 1003;
    public static final int RPMSIGTAG_MD5 = 1004;
    public static final int RPMSIGTAG_GPG = 1005;
    public static final int RPMSIGTAG_PGP5 = 1006;
    public static final int RPMSIGTAG_PAYLOADSIZE = 1007;

    /**
     * Enumeration of header property tag values.
     */
    public static final int RPMTAG_NAME = 1000;
    public static final int RPMTAG_VERSION = 1001;
    public static final int RPMTAG_RELEASE = 1002;
    public static final int RPMTAG_EPOCH = 1003;
    public static final int RPMTAG_SUMMARY = 1004;
    public static final int RPMTAG_DESCRIPTION = 1005;
    public static final int RPMTAG_BUILDTIME = 1006;
    public static final int RPMTAG_BUILDHOST = 1007;
    public static final int RPMTAG_INSTALLTIME = 1008;
    public static final int RPMTAG_SIZE = 1009;
    public static final int RPMTAG_DISTRIBUTION = 1010;
    public static final int RPMTAG_VENDOR = 1011;
    public static final int RPMTAG_GIF = 1012;
    public static final int RPMTAG_XPM = 1013;
    public static final int RPMTAG_LICENSE = 1014;
    public static final int RPMTAG_PACKAGER = 1015;
    public static final int RPMTAG_GROUP = 1016;
    public static final int RPMTAG_CHANGELOG = 1017;
    public static final int RPMTAG_SOURCE = 1018;
    public static final int RPMTAG_URL = 1020;
    public static final int RPMTAG_OS = 1021;
    public static final int RPMTAG_ARCH = 1022;
    public static final int RPMTAG_PREIN = 1023;
    public static final int RPMTAG_POSTIN = 1024;
    public static final int RPMTAG_PREUN = 1025;
    public static final int RPMTAG_POSTUN = 1026;
    public static final int RPMTAG_OLDFILENAMES = 1027;
    public static final int RPMTAG_FILESIZES = 1028;
    public static final int RPMTAG_FILESTATES = 1029;
    public static final int RPMTAG_FILEMODES = 1030;
    public static final int RPMTAG_FILEUIDS = 1031;
    public static final int RPMTAG_FILEGIDS = 1032;
    public static final int RPMTAG_FILERDEVS = 1033;
    public static final int RPMTAG_FILEMTIMES = 1034;
    public static final int RPMTAG_FILEMD5S = 1035;
    public static final int RPMTAG_FILELINKTOS = 1036;
    public static final int RPMTAG_FILEFLAGS = 1037;
    public static final int RPMTAG_ROOT = 1038;
    public static final int RPMTAG_FILEUSERNAME = 1039;
    public static final int RPMTAG_FILEGROUPNAME = 1040;
    public static final int RPMTAG_EXCLUDE = 1041;
    public static final int RPMTAG_EXCLUSIVE = 1042;
    public static final int RPMTAG_ICON = 1043;
    public static final int RPMTAG_SOURCERPM = 1044;
    public static final int RPMTAG_FILEVERIFYFLAGS = 1045;
    public static final int RPMTAG_ARCHIVESIZE = 1046;
    public static final int RPMTAG_PROVIDENAME = 1047;
    public static final int RPMTAG_REQUIREFLAGS = 1048;
    public static final int RPMTAG_REQUIRENAME = 1049;
    public static final int RPMTAG_REQUIREVERSION = 1050;
    public static final int RPMTAG_NOSOURCE = 1051;
    public static final int RPMTAG_NOPATCH = 1052;
    public static final int RPMTAG_CONFLICTFLAGS = 1053;
    public static final int RPMTAG_CONFLICTNAME = 1054;
    public static final int RPMTAG_CONFLICTVERSION = 1055;
    public static final int RPMTAG_DEFAULTPREFIX = 1056;
    public static final int RPMTAG_BUILDROOT = 1057;
    public static final int RPMTAG_INSTALLPREFIX = 1058;
    public static final int RPMTAG_EXCLUDEARCH = 1059;
    public static final int RPMTAG_EXCLUDEOS = 1060;
    public static final int RPMTAG_EXCLUSIVEARCH = 1061;
    public static final int RPMTAG_EXCLUSIVEOS = 1062;
    public static final int RPMTAG_AUTOREQPROV = 1063;
    public static final int RPMTAG_RPMVERSION = 1064;
    public static final int RPMTAG_TRIGGERSCRIPTS = 1065;
    public static final int RPMTAG_TRIGGERNAME = 1066;
    public static final int RPMTAG_TRIGGERVERSION = 1067;
    public static final int RPMTAG_TRIGGERFLAGS = 1068;
    public static final int RPMTAG_TRIGGERINDEX = 1069;
    /* GAP */
    public static final int RPMTAG_VERIFYSCRIPT = 1079;
    public static final int RPMTAG_CHANGELOGTIME = 1080;
    public static final int RPMTAG_CHANGELOGNAME = 1081;
    public static final int RPMTAG_CHANGELOGTEXT = 1082;
    public static final int RPMTAG_BROKENMD5 = 1083;
    public static final int RPMTAG_PREREQ = 1084;
    public static final int RPMTAG_PREINPROG = 1085;
    public static final int RPMTAG_POSTINPROG = 1086;
    public static final int RPMTAG_PREUNPROG = 1087;
    public static final int RPMTAG_POSTUNPROG = 1088;
    public static final int RPMTAG_BUILDARCHS = 1089;
    public static final int RPMTAG_OBSOLETENAME = 1090;
    public static final int RPMTAG_VERIFYSCRIPTPROG = 1091;
    public static final int RPMTAG_TRIGGERSCRIPTPROG = 1092;
    public static final int RPMTAG_DOCDIR = 1093;
    public static final int RPMTAG_COOKIE = 1094;
    public static final int RPMTAG_FILEDEVICES = 1095;
    public static final int RPMTAG_FILEINODES = 1096;
    public static final int RPMTAG_FILELANGS = 1097;
    public static final int RPMTAG_PREFIXES = 1098;
    public static final int RPMTAG_INSTPREFIXES = 1099;
    public static final int RPMTAG_TRIGGERIN = 1100;
    public static final int RPMTAG_TRIGGERUN = 1101;
    public static final int RPMTAG_TRIGGERPOSTUN = 1102;
    public static final int RPMTAG_AUTOREQ = 1103;
    public static final int RPMTAG_AUTOPROV = 1104;
    public static final int RPMTAG_CAPABILITY = 1105;
    public static final int RPMTAG_SOURCEPACKAGE = 1106;
    public static final int RPMTAG_OLDORIGFILENAMES = 1107;
    public static final int RPMTAG_BUILDPREREQ = 1108;
    public static final int RPMTAG_BUILDREQUIRES = 1109;
    public static final int RPMTAG_BUILDCONFLICTS = 1110;
    public static final int RPMTAG_BUILDMACROS = 1111;
    public static final int RPMTAG_PROVIDEFLAGS = 1112;
    public static final int RPMTAG_PROVIDEVERSION = 1113;
    public static final int RPMTAG_OBSOLETEFLAGS = 1114;
    public static final int RPMTAG_OBSOLETEVERSION = 1115;
    public static final int RPMTAG_DIRINDEXES = 1116;
    public static final int RPMTAG_BASENAMES = 1117;
    public static final int RPMTAG_DIRNAMES = 1118;
    public static final int RPMTAG_ORIGDIRINDEXES = 1119;
    public static final int RPMTAG_ORIGBASENAMES = 1120;
    public static final int RPMTAG_ORIGDIRNAMES = 1121;
    public static final int RPMTAG_OPTFLAGS = 1122;
    public static final int RPMTAG_DISTURL = 1123;
    public static final int RPMTAG_PAYLOADFORMAT = 1124;
    public static final int RPMTAG_PAYLOADCOMPRESSOR = 1125;
    public static final int RPMTAG_PAYLOADFLAGS = 1126;
    public static final int RPMTAG_MULTILIBS = 1127;
    public static final int RPMTAG_INSTALLTID = 1128;
    public static final int RPMTAG_REMOVETID = 1129;
    public static final int RPMTAG_SHA1RHN = 1130;
    public static final int RPMTAG_RHNPLATFORM = 1131;
    public static final int RPMTAG_PLATFORM = 1132;
    public static final int RPMTAG_PATCHESNAME = 1133;
    public static final int RPMTAG_PATCHESFLAGS = 1134;
    public static final int RPMTAG_PATCHESVERSION = 1135;
    public static final int RPMTAG_CACHECTIME = 1136;
    public static final int RPMTAG_CACHEPKGPATH = 1137;
    public static final int RPMTAG_CACHEPKGSIZE = 1138;
    public static final int RPMTAG_CACHEPKGMTIME = 1139;
    public static final int RPMTAG_FILECOLORS   = 1140;
    public static final int RPMTAG_FILECLASS    = 1141;
    public static final int RPMTAG_CLASSDICT    = 1142;
    public static final int RPMTAG_FILEDEPENDSX   = 1143;
    public static final int RPMTAG_FILEDEPENDSN   = 1144;
    public static final int RPMTAG_DEPENDSDICT    = 1145;
    public static final int RPMTAG_SOURCEPKGID    = 1146;
    public static final int RPMTAG_FILECONTEXTS   = 1147;
    public static final int RPMTAG_FSCONTEXTS   = 1148;
    public static final int RPMTAG_RECONTEXTS   = 1149;
    public static final int RPMTAG_POLICIES   = 1150;
    public static final int RPMTAG_PRETRANS   = 1151;
    public static final int RPMTAG_POSTTRANS    = 1152;
    public static final int RPMTAG_PRETRANSPROG   = 1153;
    public static final int RPMTAG_POSTTRANSPROG  = 1154;
    public static final int RPMTAG_DISTTAG    = 1155;
    public static final int RPMTAG_SUGGESTSNAME   = 1156;
    public static final int RPMTAG_SUGGESTSVERSION  = 1157;
    public static final int RPMTAG_SUGGESTSFLAGS  = 1158;
    public static final int RPMTAG_ENHANCESNAME   = 1159;
    public static final int RPMTAG_ENHANCESVERSION  = 1160;
    public static final int RPMTAG_ENHANCESFLAGS  = 1161;
    public static final int RPMTAG_PRIORITY   = 1162;
    public static final int RPMTAG_CVSID    = 1163;

    public final String[] RPMSIGTAG_NAMES = {
      "Size", "LeMD5_1", "PGP", "LeMD5_2", "MD5", "GPG", "PGP5", "PayloadSize"
    };

    public final String[] RPMTAG_NAMES = {
      "Name", "Version", "Release", "Epoch", "Summary",
      "Description", "BuildTime", "BuildHost", "InstallTime", "Size",
      "Distribution", "Vendor", "Gif", "Xpm", "License",
      "Packager", "Group", "ChangeLog", "Source", "Patch",
      "Url", "Os", "Arch", "PreIn", "PostIn",
      "PreUn", "PostUn", "OldFileNames", "FileSizes", "FileStates",
      "FileModes", "FileUids", "FileGids", "FileRdevs", "FileMtimes",
      "FileMd5s", "FileLinkTos", "FileFlags", "Root", "FileUserName",
      "FileGroupName", "Exclude", "Exclusive", "Icon", "SourceRpm",
      "FileVerifyFlags", "ArchiveSize", "ProvideName", "RequireFlags", "RequireName",
      "RequireVersion", "NoSource", "NoPatch", "ConflictFlags", "ConflictName",
      "ConflictVersion", "DefaultPrefix", "BuildRoot", "InstallPrefix", "ExcludeArch",
      "ExcludeOs", "ExclusiveArch", "ExclusiveEos", "AutoReqProv", "RpmVersion",
      "TriggerScripts", "TriggerName", "TriggerVersion", "TriggerFlags", "TriggerIndex",
      "", "", "", "", "",
      "", "", "", "", "VerifyScript",
      "ChangeLogTime", "ChangeLogName", "ChangeLogText", "BrokenMd5", "Prereq",
      "PreInProg", "PreUnProg", "PostUnProg", "BuildArchs",
      "ObsoleteName", "VerifyScriptProg", "TriggerScriptProg", "DocDir", "Cookie",
      "FileDevices", "FileInodes", "FileLangs", "Prefixes", "InstPrefixes",
      "TriggerIn", "TriggerUn", "TriggerPostUn", "AutoReq", "AutoProv",
      "Capability", "SourcePackage", "OldOrigFileNames", "BuildPrereq", "BuildRequires",
      "BuildConflicts", "BuildMacros", "ProvideFlags", "ProvideVersion", "ObsoleteFlags",
      "ObsoleteVersion", "DirIndexes", "BaseNames", "DirNames", "OrigDirIndexes",
      "OrigBaseNames", "OrigDirNames", "OptFlags", "DistUrl", "PayloadFormat",
      "PayloadCompressor", "PayloadFlags", "MultiLibs", "InstallTid", "RemoveTid",
      "Sha1Rhn", "RhnPlatform", "Platform", "PatchesName", "PatchesFlags",
      "PatchesVersion", "CacheCtime", "CachePkgPath", "CachePkgSize", "CachePkgMtime",
      "FileColors", "FileClass", "ClassDict", "FileDependsX", "FileDependsN",
      "DependsDict", "SourcePkgId", "FileContexts", "FsContexts", "ReconTexts",
      "Policies", "PreTrans", "PostTrans", "PreTransProg", "PostTransProg",
      "DistTag", "SuggestsName", "SuggestsVersion", "SuggestsFlags", "EnhancesName",
      "EnhancesVersion", "EnhancesFlags", "Priority", "CvsId"
    };

    /**
     * Enumeration of property type values.
     */
    public static final int TYPE_CHAR = 1;
    public static final int TYPE_INT8 = 2;
    public static final int TYPE_INT16 = 3;
    public static final int TYPE_INT32 = 4;
    public static final int TYPE_INT64 = 5;
    public static final int TYPE_STRING = 6;
    public static final int TYPE_BIN = 7;
    public static final int TYPE_STRING_ARRAY = 8;
    public static final int TYPE_I18NSTRING = 9;
    public final String[] TYPE_NAMES = {
      "char", "int8", "int16", "int32", "int64", "string", "bin",
      "string[]", "i18nstring"
    };

    private int tag;
    private int type;
    private byte[] value;

  }

  private RpmHeader header;
  private RpmLead lead;
  private RpmHeader signature;

  /**
   * An internal class for parsing RPM headers (the file signature and header
   * sections).
   */
  private class RpmHeader {

    /**
     * Instantiates a header by reading bytes from source.
     *
     * @param source A stream from which header bytes are read.  After
     *               instantiation the stream will be positioned at the first
     *               byte after the header.
     */
    public RpmHeader(DataInputStream source) throws IOException {

      int entryCount;
      int magic;
      int reserved;
      int storeSize;

      int[] counts;
      int[] offsets;
      int[] tags;
      int[] types;

      byte[] store;
      int[] TYPE_LENS = {0, 1, 1, 2, 4, 8, 0, 1, 0, 0};

      /*
       * The header starts with a sixteen-byte index, containing a magic
       * number, a reserved field, an entry (property) count, and the size of
       * the store (data area).
       */
      do {
        magic = source.readInt();
      } while (magic != HEADER_MAGIC);
      reserved = source.readInt();
      entryCount = source.readInt();
      storeSize = source.readInt();

      /*
       * This is followed by an entry table, each element of which contains the
       * tag, type, offset into the store, and repetition count.
       */
      counts = new int[entryCount];
      offsets = new int[entryCount];
      tags = new int[entryCount];
      types = new int[entryCount];
      for(int i = 0; i < entryCount; i++) {
        tags[i] = source.readInt();
        types[i] = source.readInt();
        offsets[i] = source.readInt();
        counts[i] = source.readInt();
      }

      store = new byte[storeSize];
      source.readFully(store);
      this.props = new RpmProperty[entryCount];

      for(int i = 0; i < entryCount; i++) {
        byte[] data;
        int dataSize;
        int j;
        int k;
        if(types[i] == RpmProperty.TYPE_STRING ||
           types[i] == RpmProperty.TYPE_STRING_ARRAY ||
           types[i] == RpmProperty.TYPE_I18NSTRING) {
          for(j = 0, k = offsets[i]; j < counts[i]; j++) {
            while(store[k] != 0)
              k++;
            k++;
          }
          dataSize = k - offsets[i];
        } else {
          dataSize = counts[i] * TYPE_LENS[types[i]];
        }
        data = new byte[dataSize];
        System.arraycopy(store, offsets[i], data, 0, dataSize);
        this.props[i] = new RpmProperty(tags[i], types[i], data);
      }

    }

    /**
     * Returns the properties contained within the header.
     */
    public RpmProperty[] getProperties() {
      return this.props;
    }

    private static final int HEADER_MAGIC = 0x8eade801; /* Includes version */
    private RpmProperty[] props;

  }

  public static void main(String[] args) {
    for(int i = 0; i < args.length; i++) {
      String rpm = args[i];
      FileInputStream f = null;
      RpmPackage rp = null;
      try {
        f = new FileInputStream(rpm);
      } catch(Exception e) {
        System.err.println("Unable to open '" + rpm + "'");
      }
      if(f != null) {
        try {
          rp = new RpmPackage(f);
        } catch(IOException e) {
          System.err.println("Error while reading '" + rpm + "'");
        }
      }
      if(rp != null) {
        RpmProperty[] props = rp.getHeader();
        System.out.println("Header props:");
        for(int j = 0; j < props.length; j++) {
          String tag = props[j].getTagAsString(false);
          String type = props[j].getTypeAsString();
          String value = props[j].getValueAsString();
          if(value.indexOf("\n") >= 0)
            System.out.println(tag + " (" + type + "):\n  " +
                               value.replaceAll("\n", "\n  "));
          else
            System.out.println(tag + " (" + type + "): " + value);
        }
        props = rp.getSignature();
        System.out.println("Signature props:");
        for(int j = 0; j < props.length; j++) {
          String tag = props[j].getTagAsString(true);
          String type = props[j].getTypeAsString();
          String value = props[j].getValueAsString();
          if(value.indexOf("\n") >= 0)
            System.out.println(tag + " (" + type + "):\n  " +
                               value.replaceAll("\n", "\n  "));
          else
            System.out.println(tag + " (" + type + "): " + value);
        }
      }
    }
  }

}
