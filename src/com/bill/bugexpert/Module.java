package com.bill.bugexpert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import com.bill.bugexpert.BugExpertModule.SourceFile;
import com.bill.bugexpert.chart.ChartPluginRepo;
import com.bill.bugexpert.doc.Bug;
import com.bill.bugexpert.doc.Chapter;
import com.bill.bugexpert.doc.ChapterParent;
import com.bill.bugexpert.doc.Doc;
import com.bill.bugexpert.doc.DocNode;
import com.bill.bugexpert.doc.Link;
import com.bill.bugexpert.doc.List;
import com.bill.bugexpert.doc.Para;
import com.bill.bugexpert.doc.ReportHeader;
import com.bill.bugexpert.doc.SimpleText;
import com.bill.bugexpert.plugins.extxml.ExtXMLPlugin;
import com.bill.bugexpert.util.LineReader;
import com.bill.bugexpert.util.SaveFile;
import com.bill.bugexpert.util.Util;
import com.bill.bugexpert.util.XMLNode;
import com.bill.bugexpert.webserver.ChkBugReportWebServer;
import com.google.common.io.Resources;

/**
 * A Module processes an input file, using a set of plugins, and produces a
 * report. This baseclass does not know anything about bugreports, for that see
 * {@link BugExpertModule}.
 * 
 * @see BugExpertModule
 */
public abstract class Module implements ChapterParent {

	/** The main version number of the application */
	public static final String VERSION = "V1.0";

	/**
	 * The incremental release number of the application (this number is always
	 * incremented, never reset or decremented)
	 */
	public static final String VERSION_CODE = "C1.0";

	/** The name of the file where the output will be logged */
	private static final String LOG_NAME = "bugexpert_log.txt";

	/** The list of resources which should be copied to the output folder */
	private static final String COMMON_RES[] = { "res/style.css", "res/icons.png", "res/warning.png",
			"res/ftrace-legend-dred.png", "res/ftrace-legend-black.png", "res/ftrace-legend-yellow.png",
			"res/ftrace-legend-red.png", "res/ftrace-legend-cyan.png", "res/ftrace-legend-dcyan.png", "res/ic_help.png",
			"res/ic_new_window.png", "res/ic_pop_out.png", "res/ic_dynamic.png", "res/pcy_p0.png", "res/pcy_p1.png",
			"res/pcy_p2.png", "res/pcy_p3.png", "res/pcy_p4.png", "res/pcy_un.png", "res/pcy_rt.png", "res/pcy_fg.png",
			"res/pcy_bg.png", "res/main.js", "res/jquery.js", "res/jquery.cookie.js", "res/jquery.jstree.js",
			"res/jquery.hotkeys.js", "res/jquery.tablesorter.js", "res/jquery.tablednd.js", "res/jquery.treeTable.js",
			"res/jquery.treeTable.css", "res/jquery.colorhelpers.js", "res/jquery.flot.js",
			"res/jquery.flot.fillbetween.js", "res/jquery.flot.navigate.js", "res/jquery.flot.resize.js",
			"res/jquery.flot.stack.js", "res/jquery.flot.threshold.js", "res/jquery.flot.crosshair.js",
			"res/jquery.flot.image.js", "res/jquery.flot.pie.js", "res/jquery.flot.selection.js",
			"res/jquery.flot.symbol.js", "res/jquery.ui.js", "res/jquery.ui.css",
			"res/images/ui-icons_454545_256x240.png", "res/images/ui-bg_glass_75_dadada_1x400.png",
			"res/images/ui-bg_glass_75_e6e6e6_1x400.png", "res/images/ui-bg_highlight-soft_75_cccccc_1x100.png",
			"res/images/ui-icons_222222_256x240.png", "res/images/ui-icons_2e83ff_256x240.png",
			"res/images/ui-bg_glass_65_ffffff_1x400.png", "res/images/ui-bg_flat_75_ffffff_40x100.png",
			"res/images/ui-icons_888888_256x240.png", "res/images/ui-bg_glass_55_fbf9ee_1x400.png",
			"res/images/ui-bg_flat_0_aaaaaa_40x100.png", "res/images/ui-bg_glass_95_fef1ec_1x400.png",
			"res/images/ui-icons_cd0a0a_256x240.png", "res/colResizable-1.3.source.js", "res/toggle-collapse-dark.png",
			"res/toggle-collapse-light.png", "res/toggle-expand-dark.png", "res/toggle-expand-light.png",
			"res/themes/classic/d.png", "res/themes/classic/dot_for_ie.gif", "res/themes/classic/throbber.gif",
			"res/themes/classic/style.css", "res/themes/blue/desc.gif", "res/themes/blue/bg.gif",
			"res/themes/blue/style.css", "res/themes/blue/asc.gif", };

	private static final int READ_FAILED = 0;
	private static final int READ_PARTS = 1;
	private static final int READ_ALL = 2;

	/**
	 * Contains some global configuration which could affect the module/plugins
	 * behavior
	 */
	private Context mContext;

	/** The list of installed plugins */
	private Vector<Plugin> mPlugins = new Vector<Plugin>();

	/** The resulting document */
	private Doc mDoc;
	/** The header chapter in the document */
	private ReportHeader mHeader;

	private GuessedValue<String> mFileName = new GuessedValue<String>("dummy");

	private Vector<Bug> mBugs = new Vector<Bug>();
	private ChartPluginRepo mChartPluginRepo = new ChartPluginRepo();
	private HashMap<String, Section> mSectionMap = new HashMap<String, Section>();
	private Vector<Section> mSections = new Vector<Section>();
	private HashMap<String, Object> mInfos = new HashMap<String, Object>();
	private boolean mSQLFailed = false;
	private Connection mSQLConnection;
	private boolean mSaveFileFailed = false;
	private SaveFile mSaveFile;
	private int mNextSectionId = 1;
	private HashSet<Plugin> mCrashedPlugins;

	private SourceFile mSource;
	private Vector<SourceFile> mSources = new Vector<BugExpertModule.SourceFile>();

	/**
	 * Creates an instance of the module in order to process the given file.
	 * 
	 * @param context
	 *            A Context instance containing some global configuration.
	 */
	public Module(Context context) {
		mContext = context;
		mDoc = new Doc(context);
		mHeader = new ReportHeader(this);
		mHeader.add(buildLinkToOwnLog());
		mDoc.addChapter(mHeader);

		// Load internal plugins
		loadPlugins();

		// Load external plugins
		loadExternalPlugins();
	}

	private DocNode buildLinkToOwnLog() {
		return new Para().add("BugExpert's log: ").add(new Link(Module.LOG_NAME, Module.LOG_NAME));
	}

	private void loadExternalPlugins() {

		File[] pluginDirs = getPluginDirs();
		for (int i = 0; i < pluginDirs.length; i++) {
			File pluginDir = pluginDirs[i];
			if (pluginDir.exists() && pluginDir.isDirectory()) {
				String files[] = pluginDir.list();
				for (String fn : files) {
					File f = new File(pluginDir, fn);
					try {
						if (fn.endsWith(".jar")) {
							loadExternalJavaPlugin(f);
						}
						if (fn.endsWith(".xml")) {
							loadExternalXmlPlugin(f);
						}
					} catch (Exception e) {
						System.err.println("Error loading external plugin: " + f.getAbsolutePath());
						e.printStackTrace();
					}
				}
			}
		}
	}

	private File[] getPluginDirs() {
		File homeDir = new File(System.getProperty("user.home"));
		File[] dirs = { new File(homeDir, Util.PRIVATE_DIR_NAME), new File(".", Util.EXTERNAL_PLUGINS_ALT_DIR_NAME) };
		return dirs;
	}

	private void loadExternalJavaPlugin(File jar) throws Exception {
		JarInputStream jis = null;
		try {
			jis = new JarInputStream(new FileInputStream(jar));
			Manifest mf = jis.getManifest();
			if (mf != null) {
				String pluginClassName = mf.getMainAttributes().getValue("ChkBugReport-Plugin");
				URL urls[] = { jar.toURI().toURL() };
				URLClassLoader cl = new URLClassLoader(urls, getClass().getClassLoader());
				Class<?> extClass = Class.forName(pluginClassName, true, cl);
				ExternalPlugin ext = (ExternalPlugin) extClass.newInstance();

				// Note: printOut will not work here, since a listener is not set yet
				System.out.println("Loading plugins from: " + jar.getAbsolutePath());
				ext.initExternalPlugin(this);
			}
		} finally {
			jis.close();
		}
	}

	private void loadExternalXmlPlugin(File f) throws Exception {
		FileInputStream is = null;
		try {
			is = new FileInputStream(f);
			XMLNode xml = XMLNode.parse(is);
			if (xml != null) {
				addPlugin(new ExtXMLPlugin(xml));
			}
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}

	/**
	 * Load internal plugins. This is the place to load all the internal plugins in
	 * subclasses.
	 */
	protected abstract void loadPlugins();

	/**
	 * Add a new plugin to the module. This method should be called before the
	 * processing begins.
	 * 
	 * @param plugin
	 *            The Plugin instance
	 */
	public void addPlugin(Plugin plugin) {
		mPlugins.add(plugin);
	}

	/**
	 * Locate a plugin with the given name. Plugins are identified by their class
	 * names (without package name).
	 * 
	 * @param pluginName
	 *            The name of the plugin (the base class name)
	 * @return the Plugin instance or null if not found.
	 */
	public Plugin getPlugin(String pluginName) {
		for (Plugin plugin : mPlugins) {
			String name = plugin.getClass().getSimpleName();
			if (name.equals(pluginName)) {
				return plugin;
			}
		}
		return null;
	}

	/**
	 * Returns the context configuration
	 * 
	 * @return the context configuration
	 */
	public Context getContext() {
		return mContext;
	}

	/**
	 * Add a line to the header. The text should be a one liner containing important
	 * information (for example the source log files used read). For rich text use
	 * {@link #addHeaderExtra(DocNode)}
	 * 
	 * @param line
	 *            A short one line message to show in the header
	 */
	public void addHeaderLine(String line) {
		mHeader.addLine(line);
	}

	/**
	 * Add extra info to the header. This is the method to append formatted text or
	 * other data to the header. For simple one line messages regarding the input
	 * file use {@link #addHeaderLine(String)}
	 * 
	 * @param node
	 *            Some extra information which should be appended to the header.
	 */
	public void addHeaderExtra(DocNode node) {
		mHeader.add(node);
	}

	/**
	 * Sets the name of the input file.
	 * 
	 * @param fileName
	 *            The name of the input file
	 */
	public void setFileName(String fileName) {
		setFileName(fileName, 100);
	}

	/* package */ void setFileName(String fileName, int certainty) {
		mFileName.set(fileName, certainty);
	}

	/* package */ String getFileName() {
		return mFileName.get();
	}

	/**
	 * Returns the location of the directory where the report files will be
	 * generated. The generated chapters and chart images will be placed here, as
	 * well as the javascript and css files will be copied here. However files
	 * generated with the purpose of further manual processing are placed in the raw
	 * directory, for example generated SQLite database file, saved sections, etc.
	 * 
	 * @return the location of the directory to save the generated files
	 * @see #getRelRawDir()
	 */
	public final String getBaseDir() {
		return mDoc.getBaseDir();
	}

	/**
	 * Returns the relative location of the raw folder from the output folder
	 * (returned by {@link #getBaseDir()}.
	 * 
	 * @see #getBaseDir()
	 * @return the relative path to the raw directory
	 */
	public String getRelRawDir() {
		return mDoc.getRelRawDir();
	}

	/**
	 * Returns the location of the output directory. Everything generated by
	 * ChkBugReport will be saved here.
	 * 
	 * @return the location of the output directory.
	 */
	public String getOutDir() {
		return mDoc.getOutDir();
	}

	/* package */ String getIndexHtmlFileName() {
		return mDoc.getIndexHtmlFileName();
	}

	/* package */ int allocSectionId() {
		return mNextSectionId++;
	}

	public void logI(String s) {
		mContext.logI(s);
	}

	public void logD(String s) {
		mContext.logD(s);
	}

	public void logW(String s) {
		mContext.logW(s);
	}

	public void logE(String s) {
		mContext.logE(s);
	}

	@Override
	public void addChapter(Chapter ch) {
		mDoc.addChapter(ch);
	}

	@Override
	public int getChapterCount() {
		return mDoc.getChapterCount();
	}

	@Override
	public Chapter getChapter(int idx) {
		return mDoc.getChapter(idx);
	}

	@Override
	public Chapter getChapter(String name) {
		return mDoc.getChapter(name);
	}

	/**
	 * Create a new chapter with the given path, or return an existing one. The
	 * chapter name can contain the slash ('/') character in order to specify the
	 * hierarchy.
	 * 
	 * @param chName
	 *            The name of the chapter (starting from the root of the document)
	 * @return A Chapter instance (either an already existing one or a newly created
	 *         one, but never null)
	 */
	public Chapter findOrCreateChapter(String chName) {
		String[] chPath = chName.split("/");
		ChapterParent parent = this;
		Chapter ch = null;
		for (String s : chPath) {
			ch = parent.getChapter(s);
			if (ch == null) {
				ch = new Chapter(getContext(), s);
				parent.addChapter(ch);
			}
			parent = ch;
		}
		return ch;
	}

	/**
	 * Add an extra file to be generated. These files are not linked directly from
	 * the table of contents, but from other chapters.
	 * 
	 * @param extFile
	 *            The Chapter instance storing the content of the extra file.
	 */
	public void addExtraFile(Chapter extFile) {
		mDoc.addExtraFile(extFile);
	}

	/**
	 * Returns the in-memory representation of the generated report.
	 * 
	 * @return the generated report document in the internal in-memory format.
	 */
	public Doc getDocument() {
		return mDoc;
	}

	/**
	 * Add a new input section. This method can be used when a bugreport is built
	 * from parts (eg. individual logs), or when the kernel log is extracted from
	 * the system log.
	 * 
	 * @param section
	 *            The section content
	 */
	public void addSection(Section section) {
		mSections.add(section);
		mSectionMap.put(section.getShortName(), section);
	}

	/**
	 * Find the Section with the given name,
	 * 
	 * @param name
	 *            The name of the section
	 * @return The Section from the bugreport or null if not found
	 */
	public Section findSection(String name) {
		return mSectionMap.get(name);
	}

	/**
	 * Returns a list of all Sections
	 * 
	 * @return a list of all Sections
	 */
	public Iterable<Section> getSections() {
		return mSections;
	}

	/* package */ int getSectionCount() {
		return mSections.size();
	}

	/**
	 * Stores extra information. This could be used to share processed data or extra
	 * (non text) input with other plugins.
	 * 
	 * @param infoId
	 *            The id (key) of the information
	 * @param obj
	 *            The information object
	 */
	public void addInfo(String infoId, Object obj) {
		mInfos.put(infoId, obj);
	}

	/**
	 * Returns a previously stored extra information
	 * 
	 * @param infoId
	 *            The id (key) of the information
	 * @return The information object
	 */
	public Object getInfo(String infoId) {
		return mInfos.get(infoId);
	}

	public ChartPluginRepo getChartPluginRepo() {
		return mChartPluginRepo;
	}

	/* package */ void setSource(SourceFile sourceFile) {
		mSource = sourceFile;
	}

	/* package */ void addSource(SourceFile sourceFile) {
		mSources.add(sourceFile);
		setFileName(sourceFile.mName, 10);
	}

	/* package */ SourceFile getSource() {
		return mSource;
	}

	/* package */ int getSourceCount() {
		return mSources.size();
	}

	/* package */ SourceFile getSource(int idx) {
		return mSources.get(idx);
	}

	/**
	 * Process the input file and generates the output. The bugreport is already
	 * loaded and split in section. It executes the plugins generating the internal
	 * representation of the report, and then renders the output html files.
	 */
	public final void generate() throws IOException {
		logI("Module:generatie(); Generating...");
		// Make sure it has a name
		mDoc.setFileName(mFileName.get());

		mContext.setLogOutput(getBaseDir() + "/" + LOG_NAME);
		mDoc.begin();

		// Allow sub-classes to pre-process data
		preProcess();

		// 
		logD("Run all the plugins.");
		runPlugins();

		// Collect detected bugs
		logD("Collecting errors...");
		collectBugs();

		// Copy over some builtin resources
		logD("Copying extra resources...");
		copyRes(COMMON_RES);

		// Save each section as raw file
		logD("Saving raw sections");
		saveSections();

		// Allow sub-classes to post-process data and do cleanup
		postProcess();

		// Cleanup plugins
		finish();

		// Render the html files
		mDoc.end();

		logD("DONE!");
	}

	/**
	 * Placeholder for pre-processing data before any plugins are executed
	 */
	protected void preProcess() throws IOException {
		// NOP
	}

	/**
	 * Placeholder for post-processing and cleanup after any plugins are executed
	 */
	protected void postProcess() throws IOException {
		// NOP
	}

	private void finish() {
		// Call finish on each plugin
		// Let's not log this, since this is not used often
		for (Plugin p : mPlugins) {
			if (!mCrashedPlugins.contains(p)) {
				try {
					p.finish(this);
				} catch (Exception e) {
					e.printStackTrace();
					addHeaderLine("Plugin crashed while finishing data: " + p.getClass().getName());
				}
			}
		}
	}

	private void runPlugins() {
		mCrashedPlugins = new HashSet<Plugin>();

		// First, sort the plugins based on prio
		Collections.sort(mPlugins, new Comparator<Plugin>() {
			@Override
			public int compare(Plugin o1, Plugin o2) {
				return o1.getPrio() - o2.getPrio();
			}
		});
		// Resetting and initializing data
		logD("Resetting plugins...");
		for (Plugin p : mPlugins) {
			try {
				p.reset();
			} catch (Throwable e) {
				e.printStackTrace();
				addHeaderLine("Plugin crashed while resetting: " + p.getClass().getName());
				mCrashedPlugins.add(p);
			}
		}
		// Installing hooks
		logD("Installing hooks...");
		for (Plugin p : mPlugins) {
			if (!mCrashedPlugins.contains(p)) {
				try {
					p.hook(this);
				} catch (Throwable e) {
					e.printStackTrace();
					addHeaderLine("Plugin crashed while hooking: " + p.getClass().getName());
					mCrashedPlugins.add(p);
				}
			}
		}
		// Then plugin should process the input data first
		logD("Plugins are loading data...");
		for (Plugin p : mPlugins) {
			if (!mCrashedPlugins.contains(p)) {
				logD("Loading plugin: " + p.getClass().getName() + "...");
				try {
					p.load(this);
				} catch (Throwable e) {
					e.printStackTrace();
					addHeaderLine("Plugin crashed while loading data: " + p.getClass().getName());
					mCrashedPlugins.add(p);
				}
			}
		}
		// Finally, each plugin should save the generated data
		logD("Plugins are generating output...");
		for (Plugin p : mPlugins) {
			if (!mCrashedPlugins.contains(p)) {
				logD("Running (generate) plugin: " + p.getClass().getName() + "...");
				try {
					p.generate(this);
				} catch (Throwable e) {
					e.printStackTrace();
					addHeaderLine("Plugin crashed while generating data: " + p.getClass().getName());
				}
			}
		}
	}

	private void copyRes(String resources[]) throws IOException {
		for (String res : resources) {
			copyRes(res, "data/" + res);
		}
	}

	private void copyRes(String fni, String fno) throws IOException {
		File fniSwitch = new File(fni);
		if (!fniSwitch.exists()) {
			logD(fni + "is not exist!");
		}

		InputStream is = Resources.class.getResourceAsStream(fni);

		if (is == null) {
			logE("Cannot find resource: " + fni);
			return;
		}

		File f = new File(mDoc.getOutDir() + fno);
		f.getParentFile().mkdirs();
		FileOutputStream fo = new FileOutputStream(f);
		byte buff[] = new byte[1024];
		while (true) {
			int read = is.read(buff);
			if (read <= 0)
				break;
			fo.write(buff, 0, read);
		}
		fo.close();
		is.close();
	}

	private void saveSections() throws IOException {
		Chapter ch = new Chapter(getContext(), "Raw data");
		List list = new List();
		ch.add(list);

		for (Section s : mSections) {
			String fn = mDoc.getRelRawDir() + s.getFileName();
			list.add(new Link(mDoc.getRelRawDir() + s.getFileName(), s.getName()));
			FileOutputStream fos = new FileOutputStream(getBaseDir() + fn);
			PrintStream ps = new PrintStream(fos);
			int cnt = s.getLineCount();
			for (int i = 0; i < cnt; i++) {
				ps.println(s.getLine(i));
			}
			ps.close();
			fos.close();
		}

		addChapter(ch);
	}

	/**
	 * Report a detected bug either in the input or while processing it. Plugins
	 * should call this method every time they find and error (or even just
	 * something suspicious) while processing the input data.
	 * 
	 * @param bug
	 *            The detected Bug instance
	 */
	public void addBug(Bug bug) {
		mBugs.add(bug);
	}

	/**
	 * Returns the number of the detected bugs.
	 * 
	 * @return the number of the detected bugs.
	 */
	public int getBugCount() {
		return mBugs.size();
	}

	/**
	 * Returns the detected Bug at the specified index
	 * 
	 * @param idx
	 *            The index of the Bug
	 * @return the detected Bug at the specified index
	 */
	public Bug getBug(int idx) {
		return mBugs.get(idx);
	}

	private void collectBugs() {
		// Sort bugs by priority
		Collections.sort(mBugs, Bug.getComparator());

		// Create error report
		String chapterName = "Errors";
		int count = mBugs.size();
		if (count > 0) {
			chapterName += " (" + count + ")";
		}
		Chapter bugs = new Chapter(getContext(), chapterName);
		if (count == 0) {
			bugs.add(new SimpleText("No errors were detected by BugExperts :-("));
		} else {
			for (Bug bug : mBugs) {
				Chapter ch = new Chapter(getContext(), bug.getName(), bug.getIcon());
				ch.add(bug);
				bugs.addChapter(ch);
			}
		}

		// Insert error report as first chapter
		mDoc.insertChapter(1, bugs); // pos#0 = Header
	}

	/**
	 * Return a new connection to the SQL database. This will return always the same
	 * instance, so if you close it, you're doomed. If connection failed, null will
	 * be returned (which can happen if the jdbc libraries are not found)
	 * 
	 * @return A connection to the database or null.
	 */
	public Connection getSQLConnection() {
		if (mSQLConnection != null)
			return mSQLConnection;
		// Don't try again
		if (mSQLFailed)
			return null;
		try {
			Class.forName("org.sqlite.JDBC");
			String fnBase = "raw/report.db";
			String fn = mDoc.getOutDir() + fnBase;
			File f = new File(fn);
			f.delete(); // We must create a new database every time
			mSQLConnection = DriverManager.getConnection("jdbc:sqlite:" + fn);
			if (mSQLConnection != null) {
				mSQLConnection.setAutoCommit(false);
				addHeaderLine("Note: SQLite report database created as " + fn);
			}
		} catch (Throwable t) {
			logE("Cannot make DB connection: " + t);
			mSQLFailed = true;
		}
		return mSQLConnection;
	}

	/**
	 * Return a SaveFile object which can be used to save user created content. This
	 * will return always the same instance.
	 */
	public SaveFile getSaveFile() {
		if (mSaveFile != null)
			return mSaveFile;
		// Don't try again
		if (mSaveFileFailed)
			return null;
		try {
			String fn = mDoc.getFileName() + ".dat";
			mSaveFile = new SaveFile(fn);
		} catch (Throwable t) {
			logE("Cannot create save file: " + t);
			mSaveFileFailed = true;
		}
		return mSaveFile;
	}

	private int readFile(Section sl, String fileName, InputStream is, int limit) {
		int ret = READ_ALL;
		try {
			if (is == null) {
				// Check file size (only if not reading from stream)
				File f = new File(fileName);
				long size = f.length();
				is = new FileInputStream(f);
				if (size > limit) {
					// Need to seek to "end - limit"
					Util.skip(is, size - limit);
					Util.skipToEol(is);
					logE("File '" + fileName + "' is too long, loading only last " + (limit / Util.MB)
							+ " megabyte(s)...");
					ret = READ_PARTS;
				}
			}

			LineReader br = new LineReader(is);

			String line = null;
			while (null != (line = br.readLine())) {
				sl.addLine(line);
			}
			br.close();
			is.close();
			return ret;
		} catch (IOException e) {
			logE("Error reading file '" + fileName + "' (it will be ignored): " + e);
			return READ_FAILED;
		}
	}

	protected void addSection(String name, String fileName, InputStream is, boolean limitSize) {
		int limit = Integer.MAX_VALUE;
		if (limitSize) {
			limit = mContext.getLimit();
		}
		String headerLine = name + ": " + fileName;
		Section sl = new Section(this, name);
		int ret = readFile(sl, fileName, is, limit);
		if (ret == READ_FAILED) {
			headerLine += "<span style=\"color: #f00;\"> (READ FAILED!)</span>";
		} else if (ret == READ_PARTS) {
			headerLine += "<span style=\"color: #f00;\"> (READ LAST " + (limit / 1024 / 1024) + "MB ONLY!)</span>";
			addSection(sl);
		} else if (ret == READ_ALL) {
			addSection(sl);
		}
		addHeaderLine(headerLine);
		addSource(new SourceFile(fileName, name));
	}

	public boolean addFile(String fileName, String type, boolean limitSize) {
		// Check if any of the plugins would like to handle this
		for (Plugin p : mPlugins) {
			if (p.handleFile(this, fileName, type)) {
				return true;
			}
		}

		// If type is specified, add the section
		if (type != null && Section.isSection(type)) {
			addSection(type, fileName, null, limitSize);
			return true;
		}
		return false;
	}

	protected void autodetect(byte[] buff, int offs, int len, GuessedValue<String> type) {
		for (Plugin p : mPlugins) {
			p.autodetect(this, buff, offs, len, type);
		}
	}

	public boolean isEmpty() {
		return mSections.isEmpty();
	}

	public void setWebServer(ChkBugReportWebServer ws) {
		for (Plugin p : mPlugins) {
			p.setWebServer(ws);
		}
	}

}
