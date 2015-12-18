package org.chrisle.netbeans.plugins.nbinstallplugin.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JOptionPane;
import org.netbeans.api.autoupdate.InstallSupport;
import org.netbeans.api.autoupdate.OperationContainer;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import java.io.File;
import java.io.IOException;
import javax.swing.filechooser.FileFilter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import static org.chrisle.netbeans.plugins.nbinstallplugin.actions.Bundle.CTL_OsgiBundleFilterDescription;
import org.netbeans.api.autoupdate.UpdateManager;
import org.netbeans.api.autoupdate.UpdateUnit;
import org.netbeans.api.autoupdate.UpdateUnitProvider;
import org.netbeans.api.autoupdate.UpdateUnitProviderFactory;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.openide.util.NbPreferences;

@ActionID(
        category = "Tools",
        id = "org.chrisle.netbeans.plugins.nbinstallplugin.actions.InstallPluginAction"
)
@ActionRegistration(
        displayName = "#CTL_InstallPluginAction"
)
@ActionReference(path = "Loaders/application/nbm/Actions", position = 1250)
@Messages("CTL_InstallPluginAction=Install Plugin(s)")
public final class InstallPluginAction implements ActionListener {

    private final List<InstallPluginDataObject> context;
    private static final FileFilter NBM_FILE_FILTER = new NbmFileFilter ();
    private static final FileFilter OSGI_BUNDLE_FILTER = new OsgiBundleFilter ();
    private static final String LOCAL_DOWNLOAD_DIRECTORY_KEY = "local-download-directory"; // NOI18N
    private static final String LOCAL_DOWNLOAD_FILES = "local-download-files"; // NOI18N    
    private static final String LOCAL_DOWNLOAD_CHECKED_FILES = "local-download-checked-files"; // NOI18N        
    private final FileList fileList = new FileList ();
    private static final Logger err = Logger.getLogger (InstallPluginAction.class.getName ());

    public InstallPluginAction(List<InstallPluginDataObject> context) {
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        for (InstallPluginDataObject installPluginDataObject : context) {
            FileObject f = installPluginDataObject.getPrimaryFile();
            
            OperationContainer<InstallSupport> container = OperationContainer.createForInstall();
            InstallSupport support = container.getSupport();

            createUpdateUnitFromNBM(FileUtil.toFile(f), false);
        }
    }
    
    private UpdateUnit createUpdateUnitFromNBM (File nbm, boolean quiet) {
        UpdateUnitProviderFactory factory = UpdateUnitProviderFactory.getDefault ();
        UpdateUnitProvider provider = factory.create (nbm.getName (), new File[] {nbm});
        List<UpdateUnit> units = Collections.emptyList ();
        try {
            units = provider.getUpdateUnits (UpdateManager.TYPE.MODULE);
        } catch (RuntimeException re) {
            if (!quiet) {
                err.log (Level.INFO, re.getMessage (), re);
                DialogDisplayer.getDefault ().notifyLater (new NotifyDescriptor.Exception (re,
                        NbBundle.getMessage(InstallPluginAction.class, "Smth happend.",
                        nbm.getName (),
                        re.getLocalizedMessage ())));
                fileList.removeFile (nbm);
            }
        }
        if (units == null || units.isEmpty()) {
            // skip to another one
            return null;
        }
        assert units.size () == 1 : "Only once UpdateUnit for " + nbm + " but " + units;
        return units.get (0);
    }
    
    private static class FileList {

        private Set<File> allFiles = null;
        private Set<File> checkedFiles = null;

        Set<File> getAllFiles () {
            if (allFiles == null) {
                allFiles = new LinkedHashSet<File> ();
                addFiles (loadPresistentState ());
            }
            return allFiles;
        }

        Set<File> getCheckedFiles () {
            if (checkedFiles == null) {
                checkedFiles = new HashSet<File> ();
                for (File f : getAllFiles ()) {
                    if (isChecked (f)) {
                        checkedFiles.add (f);
                    }
                }
            }
            return checkedFiles;
        }

        void addFiles (File[] files) {
            addFiles (Arrays.asList (files));
        }

        void addFiles (Collection<File> files) {
            getAllFiles ().addAll (files);
            Collection<String> names = new HashSet<String> ();
            for (File f : files) {
                names.add (f.getAbsolutePath ());
            }
            allFiles = stripNoNBMsNorOSGi(stripNotExistingFiles(getAllFiles()));
            makePersistent (allFiles);
            makePersistentCheckedNames (names);
        }

        void removeFile (File file) {
            removeFiles (Collections.singleton (file));
        }

        void removeFiles (Collection<File> files) {
            getAllFiles ().removeAll (files);
            allFiles = stripNoNBMsNorOSGi(stripNotExistingFiles(getAllFiles()));
            makePersistent (allFiles);
            for (File f : files) {
                makePersistentUncheckedFile (f);
            }
        }
        
        private static Preferences getPreferences () {
            return NbPreferences.forModule (InstallPluginAction.class);
        }

        private Set<File> loadPresistentState () {
            Set<File> retval = new HashSet<File> ();
            String files = getPreferences().get (LOCAL_DOWNLOAD_FILES, null);
            if (files != null) {
                String[] fileArray = files.split (","); // NOI18N  
                for (String file : fileArray) {
                    retval.add (new File (file));
                }
            }
            return retval;
        }

        private boolean isChecked (File f) {
            return getCheckedPaths ().contains (f.getAbsolutePath ());
        }

        private Collection<String> getCheckedPaths () {
            Set<String> res = new HashSet<String> ();
            String names = getPreferences ().get (LOCAL_DOWNLOAD_CHECKED_FILES, null);
            if (names != null) {
                StringTokenizer st = new StringTokenizer (names, ",");
                while (st.hasMoreTokens ()) {
                    res.add (st.nextToken ().trim ());
                }
            }
            return res;
        }

        private void makePersistentUncheckedFile (File f) {
            if (isChecked (f)) {
                Collection<String> newNames = getCheckedPaths ();
                newNames.remove (f.getAbsolutePath ());
                makePersistentCheckedNames (newNames);
            }
        }

        private void makePersistentCheckedFile (File f) {
            if (!isChecked (f)) {
                Collection<String> newNames = getCheckedPaths ();
                newNames.add (f.getAbsolutePath ());
                makePersistentCheckedNames (newNames);
            }
        }

        private void makePersistentCheckedNames (Collection<String> names) {
            StringBuilder sb = null;
            if (!names.isEmpty ()) {
                for (String s : names) {
                    if (sb == null) {
                        sb = new StringBuilder (s);
                    } else {
                        sb.append (", ").append (s); // NOI18N
                    }
                }
            }
            if (sb == null) {
                getPreferences ().remove (LOCAL_DOWNLOAD_CHECKED_FILES);
            } else {
                getPreferences ().put (LOCAL_DOWNLOAD_CHECKED_FILES, sb.toString ());
            }
        }

        private static void makePersistent (Set<File> files) {
            StringBuilder sb = null;
            if (!files.isEmpty ()) {
                for (File file : files) {
                    if (sb == null) {
                        sb = new StringBuilder (file.getAbsolutePath ());
                    } else {
                        sb.append (',').append (file.getAbsolutePath ()); // NOI18N
                    }
                }
            }
            if (sb == null) {
                getPreferences ().remove (LOCAL_DOWNLOAD_FILES);
            } else {
                getPreferences ().put (LOCAL_DOWNLOAD_FILES, sb.toString ());
            }
        }

        private static Set<File> stripNotExistingFiles (Set<File> files) {
            Set<File> retval = new HashSet<File> ();
            for (File file : files) {
                if (file.exists ()) {
                    retval.add (file);
                }
            }
            return retval;
        }

        private static Set<File> stripNoNBMsNorOSGi(Set<File> files) {
            Set<File> retval = new HashSet<File> ();
            for (File file : files) {
                if (NBM_FILE_FILTER.accept (file)) {
                    retval.add (file);
                } else if (OSGI_BUNDLE_FILTER.accept(file)) {
                    retval.add(file);
                }
            }
            return retval;
        }
    }
    
    private static class NbmFileFilter extends FileFilter {

        @Override
        public boolean accept (File f) {
            return f.isDirectory () || f.getName ().toLowerCase ().endsWith (".nbm"); // NOI18N
        }

        @Override
        public String getDescription () {
            return NbBundle.getMessage(InstallPluginAction.class, "CTL_FileFilterDescription"); // NOI18N
        }
    }

    private static class OsgiBundleFilter extends FileFilter {

        @Override
        public boolean accept(File f) {
            return f.isDirectory() || (f.getName().toLowerCase().endsWith(".jar") && isOSGiBundle(f)); // NOI18N
        }

        @Override
        @Messages("CTL_OsgiBundleFilterDescription=OSGi Bundle files (*.jar)")
        public String getDescription() {
            return CTL_OsgiBundleFilterDescription(); // NOI18N
        }
    }
    
    private static boolean isOSGiBundle(File jarFile) {
        try {
            JarFile jar = new JarFile(jarFile);
            Manifest mf = jar.getManifest();
            return mf != null && mf.getMainAttributes().getValue("Bundle-SymbolicName") != null; // NOI18N
        } catch (IOException ioe) {
            err.log(Level.INFO, ioe.getLocalizedMessage(), ioe);
        }
        return false;
    }

//    static Restarter doInstall(InstallSupport support, Installer installer) throws OperationException {
//        final String displayName = "Installing plugin...";
//        System.out.println(displayName);
//        ProgressHandle installHandle = ProgressHandleFactory.createHandle(
//                displayName,
//                new Cancellable() {
//                    @Override
//                    public boolean cancel() {
//                        return true;
//                    }
//                }
//        );
//
//        return support.doInstall(installer, installHandle);
//    }
//
//    static Installer doVerify(InstallSupport support, Validator validator) throws OperationException {
//        final String displayName = "Validating Gradle plugin...";
//        System.out.println(displayName);
//        ProgressHandle validateHandle = ProgressHandleFactory.createHandle(
//                displayName,
//                new Cancellable() {
//                    @Override
//                    public boolean cancel() {
//                        return true;
//                    }
//                }
//        );
//
//        Installer installer = support.doValidate(validator, validateHandle);
//        return installer;
//    }
}
