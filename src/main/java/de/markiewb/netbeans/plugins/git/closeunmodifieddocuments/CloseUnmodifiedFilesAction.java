/**
 * Copyright 2015 markiewb
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package de.markiewb.netbeans.plugins.git.closeunmodifieddocuments;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import javax.swing.SwingUtilities;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

@ActionID(
        category = "Editor",
        id = "git.closeunmodifieddocuments.CloseUnmodifiedFilesAction"
)
@ActionRegistration(
        displayName = "#CTL_CloseUnmodifiedFilesAction"
)
@ActionReferences({
    @ActionReference(path = "Editors/TabActions", position = 0, separatorBefore = -50),})
@Messages("CTL_CloseUnmodifiedFilesAction=Close Unmodified Files")
/**
 *
 */
public final class CloseUnmodifiedFilesAction implements ActionListener {

    /**
     * A Boolean specifying if a VCS marks the file as modified or up to date.
     *
     * @since NB 8.1 - https://netbeans.org/bugzilla/show_bug.cgi?id=248811
     */
    private static final String ATTRIBUTE_IS_MODIFIED = "ProvidedExtensions.VCSIsModified";

    @Override
    public void actionPerformed(ActionEvent ev) {

        closedUnchangedDocuments();
    }

    /**
     * Works SVN/HG/GIT since NB8.1. See
     * https://netbeans.org/bugzilla/show_bug.cgi?id=248811
     *
     * @return
     */
    public void closedUnchangedDocuments() {

        // run in EDT to get all current TCs
        final WindowManager wm = WindowManager.getDefault();
        final Collection<TopComponent> topComponents = new ArrayList<>();

        for (TopComponent tc : getCurrentEditors()) {
            if (!wm.isEditorTopComponent(tc)) {
                continue;
            }
            topComponents.add(tc);
        }

        // run outside EDT to get the modified FileObjects behind the TCs
        // https://github.com/markiewb/nb-git-close-unmodified-documents/issues/5
        Runnable getModifiedTcs = new Runnable() {
            @Override
            public void run() {
                final Collection<TopComponent> result = new LinkedHashSet<>();
                for (final TopComponent tc : topComponents) {

                    // check for the format of an unsaved file
                    boolean isUnsaved = null != tc.getLookup().lookup(SaveCookie.class);
                    if (isUnsaved) {
                        continue;
                    }

                    DataObject dob = tc.getLookup().lookup(DataObject.class);
                    if (dob != null) {
                        final FileObject file = dob.getPrimaryFile();
                        if (null == file) {
                            continue;
                        }
                        Object attribute = file.getAttribute(ATTRIBUTE_IS_MODIFIED);
                        if (null != attribute) {
                            if (Boolean.FALSE.equals(attribute)) {
                                result.add(tc);
                            } else {
                        //file is modified, nop
                            }
                        } else {
                    //could not determine status, close this document too
                            result.add(tc);
                        }
                    } else {
                //close diff windows too
                        result.add(tc);
                    }
                }

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        final Collection<TopComponent> tcs = result;

                        // run inside EDT to handle with TCs
                        for (TopComponent tc : tcs) {
                            tc.close();
                        }
                    }
                });
            }
        };
        RequestProcessor.getDefault().post(getModifiedTcs);
    }

    private Collection<TopComponent> getCurrentEditors() {
        final ArrayList<TopComponent> result = new ArrayList<>();
        final WindowManager wm = WindowManager.getDefault();
        for (Mode mode : wm.getModes()) {
            if (wm.isEditorMode(mode)) {
                result.addAll(Arrays.asList(wm.getOpenedTopComponents(mode)));
            }
        }
        return result;
    }

}
