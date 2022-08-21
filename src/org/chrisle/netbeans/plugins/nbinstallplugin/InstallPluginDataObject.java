package org.chrisle.netbeans.plugins.nbinstallplugin;

import java.io.IOException;
import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.core.spi.multiview.text.MultiViewEditorElement;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.MIMEResolver;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;

@Messages({
    "LBL_InstallPluginAction_LOADER=Files of InstallPluginAction"
})
@MIMEResolver.ExtensionRegistration(
        displayName = "#LBL_InstallPluginAction_LOADER",
        mimeType = "application/nbm",
        extension = {"nbm", "NBM"}
)
@DataObject.Registration(
        mimeType = "application/nbm",
        iconBase = "org/chrisle/netbeans/plugins/nbinstallplugin/resources/nbm.png",
        displayName = "#LBL_InstallPluginAction_LOADER",
        position = 300
)
@ActionReferences({
    @ActionReference(
            path = "Loaders/application/nbm/Actions",
            id = @ActionID(category = "Edit", id = "org.openide.actions.CutAction"),
            position = 300
    ),
    @ActionReference(
            path = "Loaders/application/nbm/Actions",
            id = @ActionID(category = "Edit", id = "org.openide.actions.CopyAction"),
            position = 400,
            separatorAfter = 500
    ),
    @ActionReference(
            path = "Loaders/application/nbm/Actions",
            id = @ActionID(category = "Edit", id = "org.openide.actions.DeleteAction"),
            position = 600
    ),
    @ActionReference(
            path = "Loaders/application/nbm/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.RenameAction"),
            position = 700,
            separatorAfter = 800
    ),
    @ActionReference(
            path = "Loaders/application/nbm/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.SaveAsTemplateAction"),
            position = 900,
            separatorAfter = 1000
    ),
    @ActionReference(
            path = "Loaders/application/nbm/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.FileSystemAction"),
            position = 1100,
            separatorAfter = 1200
    ),
    @ActionReference(
            path = "Loaders/application/nbm/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.ToolsAction"),
            position = 1300
    ),
    @ActionReference(
            path = "Loaders/application/nbm/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.PropertiesAction"),
            position = 1400
    )
})
public class InstallPluginDataObject extends MultiDataObject {

    public InstallPluginDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
        registerEditor("application/nbm", true);
    }

    @Override
    protected int associateLookup() {
        return 1;
    }

    @MultiViewElement.Registration(
            displayName = "#LBL_InstallPluginAction_EDITOR",
            iconBase = "org/chrisle/netbeans/plugins/nbinstallplugin/resources/nbm.png",
            mimeType = "application/nbm",
            persistenceType = TopComponent.PERSISTENCE_ONLY_OPENED,
            preferredID = "InstallPluginAction",
            position = 1000
    )
    @Messages("LBL_InstallPluginAction_EDITOR=Source")
    public static MultiViewEditorElement createEditor(Lookup lkp) {
        return new MultiViewEditorElement(lkp);
    }
}
