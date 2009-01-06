/*
 * This file is part of the Scriba source distribution. This is free, open-source 
 * software. For full licensing information, please see the LicensingInformation file
 * at the root level of the distribution.
 *
 * Copyright (c) 2006-2007 Kobrix Software, Inc.
 */
package seco.notebook.storage;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGHandleFactory;
import org.hypergraphdb.HGIndex;
import org.hypergraphdb.HGLink;
import org.hypergraphdb.HGPersistentHandle;
import org.hypergraphdb.HGQuery;
import org.hypergraphdb.HGRandomAccessResult;
import org.hypergraphdb.HGSearchResult;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.IncidenceSet;
import org.hypergraphdb.indexing.ByPartIndexer;
import org.hypergraphdb.query.AtomTypeCondition;

import seco.U;
import seco.boot.StartMeUp;
import seco.notebook.AppForm;
import seco.notebook.util.RequestProcessor;



public class ClassRepository
{
    // private static final String PATH = AppForm.getConfigDirectory()
    // .getAbsolutePath()
    // + "/.notebook/repository";
    public static final String REPOSITORY_NAME = ".scribaRepository";
    static String repositoryPath = new File(new File(StartMeUp.findUserHome()),
            REPOSITORY_NAME).getAbsolutePath();
    static final HGPersistentHandle JARS_MAP_HANDLE = HGHandleFactory
            .makeHandle("1d3b7df9-f109-11dc-9512-073dfab2b15a");
    private static final String PCK_INDEX = "PackageInfo";
    private static final String PCK_NAME_PROP = "name";
    private static final String PCK_FULL_NAME_PROP = "fullName";
    private static final String CLS_INDEX = "ClassInfo";
    private static final String CLS_NAME_PROP = "name";
    private static final String JAR_INDEX = "JarInfo";
    private static final String JAR_PATH_PROP = "path";
    private static HGHandle[] EMPTY_HANDLE_ARRAY = new HGHandle[0];
    private static ClassRepository instance;
    /* private */HyperGraph hg;
    boolean updateInProgress = false;

    private ClassRepository(final HyperGraph hg)
    {
        this.hg = hg;
//        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
//            public void run()
//            {
//                try
//                {
//                    if (isUpdateInProgress()) System.out
//                            .println("Finishing repository creation."
//                                    + " Please wait.");
//                    while (isUpdateInProgress())
//                        Thread.sleep(1000);
//                    // hg.close();
//                }
//                catch (Throwable t)
//                {
//                    t.printStackTrace(System.err);
//                }
//            }
//        }));
        final String path = System.getProperty("java.home");
        if (path == null) return;
        Thread t = new Thread() {
            public void run()
            {
                updateInProgress = true;
                try
                {
                    createIndexes();
                    addLibDir(
                            new File(new File(path), "lib").getAbsolutePath(),
                            true);
                    addLibDir(new File(AppForm.getConfigDirectory(), "lib")
                            .getAbsolutePath(), false);
                    addJar(new File(AppForm.getConfigDirectory(), "scriba.jar")
                            .getAbsolutePath(), false);
                    System.out.println("Repository creation finished.");
                }
                finally
                {
                    updateInProgress = false;
                }
            }
        };
        RequestProcessor.getDefault().post(t);
    }

    private Map<JarInfo, Boolean> getFinishedJarsMap()
    {
        Map<JarInfo, Boolean> map = (Map<JarInfo, Boolean>) hg
                .get(JARS_MAP_HANDLE);
        if (map == null)
        {
            map = new HashMap<JarInfo, Boolean>();
            hg.define(JARS_MAP_HANDLE, map);
        }
        return map;
    }

    public static void main(String[] args)
    {
        getInstance();
    }

    public void addLibDir(String dir, boolean lib)
    {
        File[] files = new File(dir).listFiles();
        if (files != null) for (int i = 0; i < files.length; i++)
        {
            if (!files[i].isDirectory() && files[i].getName().endsWith(".jar")) addJar(
                    files[i].getAbsolutePath(), lib);
        }
    }

    public void removeJar(String s)
    {
        HGHandle h = lookup(JAR_INDEX, JAR_PATH_PROP, s);
        if (h != null) hg.remove(h);
    }

    private boolean checkJarExistsAndUpToDate(String s)
    {
        HGHandle h = lookup(JAR_INDEX, JAR_PATH_PROP, s);
        if (h != null)
        {
            JarInfo info = (JarInfo) hg.get(h);
            File f = new File(s);
            // System.out.println("Jar: " + s + " present: " +
            // (info.getDate() == f.lastModified()));
            if (info.getDate() != f.lastModified()
                    || !getFinishedJarsMap().containsKey(info))
            {
                ; //hg.remove(h);// TODO: remove the old jar
            }
            return true;
        }
        return false;
    }

    private HGHandle[] findPackage(String s)
    {
        HGSearchResult<HGPersistentHandle> res = null;
        try
        {
            res = lookupAll(PCK_INDEX, PCK_NAME_PROP, s);
            Set<HGHandle> set = new HashSet<HGHandle>();
            while (res.hasNext())
                set.add(res.next());
            return set.toArray(new HGHandle[set.size()]);
        }
        finally
        {
            U.closeNoException(res);
        }
    }

    private HGHandle findPackageByFullName(String s)
    {
        return lookup(PCK_INDEX, PCK_FULL_NAME_PROP, s);
    }

    public JarInfo[] findJars()
    {
        HGHandle[] handles = getJarHandles();
        Set<JarInfo> jars = new HashSet<JarInfo>();
        for (HGHandle h : handles)
            jars.add((JarInfo) hg.get(h));
        return jars.toArray(new JarInfo[jars.size()]);
    }

    private HGHandle[] getJarHandles()
    {
        Set<HGHandle> jars = new HashSet<HGHandle>();
        HGSearchResult<HGPersistentHandle> res = null;
        try
        {
            res = hg.find(new AtomTypeCondition(JarInfo.class));
            while (res.hasNext())
            {
                HGHandle h = (HGHandle) res.next();
                jars.add(h);
            }
            return jars.toArray(new HGHandle[jars.size()]);
        }
        finally
        {
            U.closeNoException(res);
        }
    }

    /**
     * Returns a map of all jars(except J2SE ones) and corresponding javadoc
     * dirs as defined in repository
     */
    public Map<JarInfo, DocInfo> getJavaDocAssoiciations()
    {
        HGHandle[] handles = getJarHandles();
        Map<JarInfo, DocInfo> res = new HashMap<JarInfo, DocInfo>();
        for (HGHandle h : handles)
        {
            HGHandle docH = getDocInfo(h);
            Object info = (docH != null) ? hg.get(docH) : null;
            // if (info != null && (info instanceof DocInfo))
            // System.out.println("DocInfo: " + ((DocInfo) info).getName()
            // + ":" + "jar: " + ((JarInfo) hg.get(h)).getPath());
            if (info == null || !(info instanceof RtDocInfo)) res.put(
                    (JarInfo) hg.get(h), (DocInfo) info);
        }
        return res;
    }

    public DocInfo getDocInfoForClass(Class<?> cl)
    {
        if (cl == null) return null;
        HGHandle h = getClsHandle(cl.getSimpleName(), false);
        if (h == null || cl.getPackage() == null) return null;
        String pack = cl.getPackage().getName();
        HGHandle[] list = parentOfClass(hg, h);
        for (int j = 0; j < list.length; j++)
        {
            String cls = ((PackageInfo) hg.get(list[j])).getFullName();
            if (cls.equalsIgnoreCase(pack)) return getDocInfoForPackage(pack);
        }
        return null;
    }

    public DocInfo getDocInfoForPackage(String fullName)
    {
        HGHandle packH = findPackageByFullName(fullName);
        if (packH == null) return null;
        HGSearchResult<HGHandle> res = hg.find(HGQuery.hg.and(HGQuery.hg
                .type(JarLink.class), HGQuery.hg.link(packH)));
        try
        {
            if (!res.hasNext()) return null;
            JarLink link = (JarLink) hg.get(res.next());
            // System.out.println("Link: " + link);
            HGHandle docH = getDocInfo(link.getTargetAt(0));
            return (docH != null) ? (DocInfo) hg.get(docH) : null;
        }
        finally
        {
            U.closeNoException(res);
        }
    }

    private HGHandle getDocInfo(HGHandle h)
    {
        HGSearchResult<HGHandle> res = hg.find(new AtomTypeCondition(
                DocLink.class));
        try
        {
            while (res.hasNext())
            {
                DocLink link = (DocLink) hg.get((HGHandle) res.next());
                if (link.getTargetAt(0).equals(h)) return link.getTargetAt(1);
            }
        }
        finally
        {
            U.closeNoException(res);
        }
        return null;
    }

    public void setJavaDocForJar(String jar, String doc)
    {
        HGHandle h = lookup(JAR_INDEX, JAR_PATH_PROP, jar);
        if (h != null)
        {
            // System.out.println(hg.get(h));
            HGHandle d = getDocInfo(h);
            if (d != null) hg.replace(d, new DocInfo(doc));
            else
                hg.add(new DocLink(
                        new HGHandle[] { h, hg.add(new DocInfo(doc)) }));
        }
    }

    public void setRtJavaDoc(String doc)
    {
        HGHandle h = getRtDocHandle();
        hg.replace(h, new RtDocInfo(doc));
    }

    private HGHandle findTopPackage(HyperGraph hg, String s)
    {
        HGHandle[] hs = findPackage(s);
        for (HGHandle h : hs)
        {
            PackageInfo p = (PackageInfo) hg.get(h);
            if (p.getFullName().startsWith(p.getName())) return h;
        }
        return null;
    }

    private HGHandle[] findSubPackages(String s)
    {
        if (s.indexOf('.') > -1) return findSubPackages(findPackageByFullName(s));
        return findSubPackages(findTopPackage(hg, s));
    }

    private HGHandle[] findSubPackages(HGHandle h)
    {
        if (h == null) return EMPTY_HANDLE_ARRAY;
        IncidenceSet subs = hg.getIncidenceSet(h);
        // System.out.println(s + ":" + subs.length);
        Set<HGHandle> res = new HashSet<HGHandle>();
        for (HGHandle ih : subs)
        {
            Object o = hg.get(ih);
            // System.out.println(i + ":" + o);
            if (o instanceof ParentOfLink)
            {
                HGHandle par = hg.getPersistentHandle(((ParentOfLink) o)
                        .getTargetAt(1));
                Object parObject = hg.get(par);
                if (!par.equals(h) && parObject instanceof NamedInfo) res
                        .add(par);
            }
        }
        return res.toArray(new HGHandle[res.size()]);
    }

    // return sub-packages and classes
    public NamedInfo[] findSubElements(String _package)
    {
        HGHandle[] hs = findSubPackages(_package);
        NamedInfo[] packs = new NamedInfo[hs.length];
        // System.out.println("ClassRepository - findSubElements: " +
        // hs.length);
        for (int i = 0; i < hs.length; i++)
            packs[i] = ((NamedInfo) hg.get(hs[i]));
        // Arrays.sort(packs);
        return packs;
    }

    public Class<?>[] findClass(String s)
    {
        if (s.indexOf('.') > 0) s = s.substring(s.lastIndexOf('.') + 1);
        HGHandle h = getClsHandle(s, false);
        if (h == null) return new Class[0];
        String clsName = ((ClassInfo) hg.get(h)).getName();
        // System.out.println(clsName + ":" + h);
        HGHandle[] list = parentOfClass(hg, h);
        Set<Class<?>> set = new HashSet<Class<?>>();
        for (int j = 0; j < list.length; j++)
        {
            String cls = ((PackageInfo) hg.get(list[j])).getFullName() + "."
                    + clsName;
            try
            {
                Class<?> thisClass = Thread.currentThread()
                        .getContextClassLoader().loadClass(cls);
                if (thisClass != null) set.add(thisClass);
            }
            catch (Exception ex)
            {
                System.err.println("Unable to load class: " + cls);
            }
        }
        return set.toArray(new Class[set.size()]);
    }

    private static HGHandle[] parentOfClass(HyperGraph hg, HGHandle h)
    {
        Set<HGHandle> res = new HashSet<HGHandle>();
        IncidenceSet set = hg.getIncidenceSet(h);
        for (HGHandle ih : set)
        {
            Object o = hg.get(ih);
            // System.out.println(i + ":" + o);
            if (o instanceof ParentOfLink)
            {
                HGHandle par = hg.getPersistentHandle(((ParentOfLink) o)
                        .getTargetAt(0));
                Object parObject = hg.get(par);
                if (!par.equals(h) && parObject instanceof PackageInfo)
                {
                    // System.out.println("Par:" + hg.get(par) + ":" + par);
                    res.add(par);
                }
            }
        }
        // System.out.println(res.size());
        return res.toArray(new HGHandle[res.size()]);
    }

    public void addJar(String absPath, boolean lib)
    {
        if (checkJarExistsAndUpToDate(absPath)) return;
        n_pack = 0;
        n_cls = 0;
        File f = new File(absPath);
        if (!f.exists())
        {
            System.out.println("*** The file " + absPath + " does not exist.");
            return;
        }
        cache.clear();
        long time = System.currentTimeMillis();
        System.out.println("Creating descriptions for " + absPath);
        try
        {
            JarFile jarFile = new JarFile(absPath);
            JarInfo info = new JarInfo(absPath, f.lastModified());
            HGHandle jarHandle = hg.add(info);
            if (lib)
            {
                hg.add(new DocLink(
                        new HGHandle[] { jarHandle, getRtDocHandle() }));
            }
            String urlName = "jar:file:/" + absPath;
            // to java virtual style :
            String virtualURLname = urlName += "!/";
            URL jarFileURL = new URL(virtualURLname);
            URL[] urlList = new URL[] { jarFileURL };
            URLClassLoader urlClassLoader = new URLClassLoader(urlList);
            Enumeration<JarEntry> jarEntries = jarFile.entries();
            jarEntries = jarFile.entries();
            while (jarEntries.hasMoreElements())
            {
                final JarEntry entry = jarEntries.nextElement();
                if (entry.isDirectory()) continue;
                String entryName = entry.getName();
                if (!entryName.endsWith(".class")) continue;
                if (entryName.indexOf('$') > 0) continue;// TODO:
                HGHandle lastP = processPackage(jarHandle, entryName);
                String vname = entryName.replace('/', '.');
                // remove .class ending :
                final String classDescriptor = vname.substring(0, vname
                        .length() - 6);
                try
                {
                    Class<?> thisClass = urlClassLoader
                            .loadClass(classDescriptor);
                    if (thisClass == null
                            && (thisClass.getModifiers() & Modifier.PUBLIC) == 0) continue;
                    String simple = thisClass.getCanonicalName();
                    if (simple == null) continue;
                    simple = thisClass.getSimpleName();
                    // System.out.println(simple);
                    HGHandle clsH = getClsHandle(simple, true);
                    hg.add(new JarLink(new HGHandle[] { jarHandle, clsH }));
                    if (lastP != null) hg.add(new ParentOfLink(new HGHandle[] {
                            lastP, clsH }));
                    // else
                    // System.out.println("TopLevel class: " +simple);
                }
                catch (Throwable e)
                {
                    // e.printStackTrace();
                }
            }
            System.out.println("Classes: " + n_cls + "(" + jarFile.size()
                    + ") packs: " + n_pack + " time: "
                    + (System.currentTimeMillis() - time) / 1000);
            getFinishedJarsMap().put(info, true);
            hg.update(getFinishedJarsMap());
        }
        catch (Exception ee)
        {
            ee.printStackTrace();
        }
    }

    public RtDocInfo getRtDocInfo()
    {
        return (RtDocInfo) hg.get(getRtDocHandle());
    }

    private HGHandle getRtDocHandle()
    {
        HGSearchResult<HGHandle> res = null;
        try
        {
            res = hg.find(new AtomTypeCondition(RtDocInfo.class));
            if (res.hasNext()) return (HGHandle) res.next();
            return hg.add(new RtDocInfo(""));
        }
        finally
        {
            U.closeNoException(res);
        }
    }

    private static Vector<Set<String>> cache = new Vector<Set<String>>();

    private static boolean isInCache(String s, int level)
    {
        Set<String> set = null;
        if (level < cache.size()) set = cache.get(level);
        if (set != null && set.contains(s)) return true;
        return false;
    }

    private static void putInCache(String s, HGHandle h, int level)
    {
        Set<String> set = null;
        if (level < cache.size()) set = cache.get(level);
        if (set == null)
        {
            set = new HashSet<String>();
            set.add(s);
            cache.add(level, set);
        } else
            set.add(s);// , h);
    }

    private static String fullName(String[] packs, int level)
    {
        String res = "";
        for (int i = 0; i <= level; i++)
        {
            res += packs[i];
            if (i <= level - 1) res += ".";
        }
        return res;
    }

    private HGHandle processPackage(HGHandle jarHandle, String name)
    {
        String[] packs = name.split("/");
        // top-level class, no package
        if (packs.length == 1) return null;
        String[] fpacks = new String[packs.length - 1];
        for (int j = 0; j < packs.length - 1; j++)
            fpacks[j] = fullName(packs, j);
        int n = packs.length;
        for (int i = 0; i < n - 1; i++)
        {
            if (isInCache(fpacks[i], i)) continue;
            HGHandle h = getPckHandle(jarHandle, packs[i], fpacks[i], true);
            if (i == 0)
            {
                hg.add(new ParentOfLink(new HGHandle[] { jarHandle, h }));
                putInCache(fpacks[i], h, i);
                continue;
            }
            HGHandle prev = getPckHandle(jarHandle, packs[i - 1],
                    fpacks[i - 1], true);
            IncidenceSet set = hg.getIncidenceSet(prev);
            // System.out.println("contains: " + pathElement + " in " +
            // prevElement + ":" + i);
            if (!set.contains(h))
            {
                hg.add(new ParentOfLink(new HGHandle[] { prev, h }));
            }
            putInCache(fpacks[i], h, i);
        }
        return getPckHandle(jarHandle, packs[n - 2], fpacks[n - 2], true);
    }

    static int n_pack = 0;

    private HGHandle getPckHandle(HGHandle jarHandle, String name,
            String fname, boolean search_db)
    {
        PackageInfo info = new PackageInfo(name, fname);
        HGHandle h = hg.getHandle(info);
        if (h == null && search_db)
        {
            HGRandomAccessResult<HGPersistentHandle> res = null;
            try
            {
                res = lookupAll(PCK_INDEX, PCK_NAME_PROP, name);
                while (res.hasNext())
                {
                    HGHandle resH = (HGHandle) res.next();
                    PackageInfo p = (PackageInfo) hg.get(resH);
                    if (fname.equals(p.getFullName())) return resH;
                }
            }
            finally
            {
                U.closeNoException(res);
            }
        }
        if (h == null)
        {
            h = hg.add(info);
            hg.add(new JarLink(new HGHandle[] { jarHandle, h }));
            // System.out.println("" + n_pack + " Adding package: " + name);
            ++n_pack;
        }
        return h;
    }

    static int n_cls = 0;

    private HGHandle getClsHandle(String name, boolean add_in_db)
    {
        ClassInfo info = new ClassInfo(name);
        HGHandle h = hg.getHandle(info);
        if (h == null)
        {
            h = lookup(CLS_INDEX, CLS_NAME_PROP, name);
            if (h != null) return h;
        }
        if (h == null && add_in_db)
        {
            h = hg.add(info);
            // System.out.println("" + n_cls + " Adding class: " + name);
            ++n_cls;
        }
        return h;
    }

    private HGHandle lookup(String typeAlias, String keyProperty,
            String keyValue)
    {
        // how could this can be null, but such an error was reported....
        HGHandle h = hg.getTypeSystem().getTypeHandle(typeAlias);
        if (h == null) return null;
        HGPersistentHandle typeHandle = hg.getPersistentHandle(h);
        HGIndex<Object, HGPersistentHandle> index = hg.getIndexManager()
                .getIndex(
                        new ByPartIndexer(typeHandle,
                                new String[] { keyProperty }));
        if (index != null) return index.findFirst(keyValue);
        return null;
    }

    private HGRandomAccessResult<HGPersistentHandle> lookupAll(
            String typeAlias, String keyProperty, String keyValue)
    {
        HGPersistentHandle typeHandle = hg.getPersistentHandle(hg
                .getTypeSystem().getTypeHandle(typeAlias));
        HGIndex<Object, HGPersistentHandle> index = hg.getIndexManager()
                .getIndex(
                        new ByPartIndexer(typeHandle,
                                new String[] { keyProperty }));
        if (index != null) return index.find(keyValue);
        return null;
    }

    public static ClassRepository getInstance()
    {
        if (instance == null)
        {
            System.out.println("ClassRepository Path : " + repositoryPath);
            instance = new ClassRepository(new HyperGraph(repositoryPath));
        }
        return instance;
    }

    private void createIndexes()
    {
        HGPersistentHandle typeH = hg.getPersistentHandle(hg.getTypeSystem()
                .getTypeHandle(PackageInfo.class));
        String[] nameArr = new String[] { PCK_NAME_PROP };
        // return if already created
        // if (hg.getIndexManager().getIndex(new ByPartIndexer(typeH, nameArr))
        // != null) return;
        // TODO: getIndex() throw NPE both in the upper or lower code
        // snippets...
        if (!hg.getTypeSystem().findAliases(typeH).isEmpty())
        	return;
        hg.getTypeSystem().addAlias(typeH, PCK_INDEX);
        hg.getIndexManager().register(new ByPartIndexer(typeH, nameArr));
        hg.getIndexManager().register(
                new ByPartIndexer(typeH, new String[] { PCK_FULL_NAME_PROP }));
        typeH = hg.getPersistentHandle(hg.getTypeSystem().getTypeHandle(
                ClassInfo.class));
        hg.getTypeSystem().addAlias(typeH, CLS_INDEX);
        hg.getIndexManager().register(
                new ByPartIndexer(typeH, new String[] { CLS_NAME_PROP }));
        typeH = hg.getPersistentHandle(hg.getTypeSystem().getTypeHandle(
                JarInfo.class));
        hg.getTypeSystem().addAlias(typeH, JAR_INDEX);
        hg.getIndexManager().register(
                new ByPartIndexer(typeH, new String[] { JAR_PATH_PROP }));
    }

    public synchronized boolean isUpdateInProgress()
    {
        return updateInProgress;
    }
}