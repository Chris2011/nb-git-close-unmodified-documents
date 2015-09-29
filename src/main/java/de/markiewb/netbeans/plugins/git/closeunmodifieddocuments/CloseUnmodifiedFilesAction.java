/**
 * Copyright 2014 markiewb
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
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.jgit.api.Git;
import org.netbeans.libs.git.GitClient;
import org.netbeans.libs.git.GitException;
import org.netbeans.libs.git.GitRemoteConfig;
import org.netbeans.libs.git.GitRepository;
import org.netbeans.libs.git.GitRevisionInfo;
import org.netbeans.libs.git.GitStatus;
import org.netbeans.libs.git.GitStatus.Status;
import org.netbeans.libs.git.progress.ProgressMonitor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
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

    private static final EnumSet<Status> modifiedStates = EnumSet.of(Status.STATUS_ADDED, Status.STATUS_MODIFIED);

    /**
     *
     * @param f
     * @param remoteName remote like "origin"
     * @return
     */
    public static Collection<FileObject> getModifiedFiles(final List<FileObject> files) {
        Collection<FileObject> result = new ArrayList<FileObject>();

        Map<FileObject, List<FileObject>> filesPerRepo = groupFileByGitRepo(files);

        Set<FileObject> repoDirs = filesPerRepo.keySet();
        for (FileObject repoDir : repoDirs) {
            if (null == repoDir) {
                continue;
            }
            final File toFile = FileUtil.toFile(repoDir);
            if (null == toFile) {
                continue;
            }
            GitRepository repo = GitRepository.getInstance(toFile);
            if (null == repo) {
                continue;
            }

            FileMapper fm = new FileMapper();
            fm.putAllFileObjects(filesPerRepo.get(repoDir));
            Collection<File> filesInRepo = fm.toFiles().values();
            File[] filesInRepoArray = filesInRepo.toArray(new File[filesInRepo.size()]);
            GitClient client = null;
            try {
                client = repo.createClient();
                Map<File, GitStatus> status = client.getStatus(filesInRepoArray, new ProgressMonitor.DefaultProgressMonitor());
                for (File fileInRepo : filesInRepoArray) {
                    if (null == status) {
                        continue;
                    }
                    final GitStatus state = status.get(fileInRepo);
                    if (null == state) {
                        continue;
                    }
                    boolean isModified = modifiedStates.contains(state.getStatusIndexWC());
                    isModified |= state.isConflict();
                    isModified |= state.isCopied();
                    isModified |= state.isRenamed();
                    if (isModified) {
                        result.add(fm.toFileObjects().get(fileInRepo));
                    }
                }
            } catch (GitException ex) {
                Exceptions.printStackTrace(ex);
            } finally {
                if (null != client) {
                    client.release();
                }
            }
        }
        return result;

    }

    static FileObject getGitRepoDirectory(FileObject file) {
        FileObject currentFile = file;
        while (currentFile != null) {
            if (currentFile.isFolder() && currentFile.getFileObject(".git", "") != null) {
                return currentFile;
            }
            currentFile = currentFile.getParent();
        }
        return null;
    }

    private static Map<FileObject, List<FileObject>> groupFileByGitRepo(final List<FileObject> files) {
        //create map of <repo,requested files per repo>
        Map<FileObject, List<FileObject>> filesPerRepo = new HashMap<FileObject, List<FileObject>>();
        for (FileObject file : files) {

            final FileObject gitRepoDirectory = getGitRepoDirectory(file);
            if (null != gitRepoDirectory) {
                if (!filesPerRepo.containsKey(gitRepoDirectory)) {
                    filesPerRepo.put(gitRepoDirectory, new ArrayList<FileObject>());
                }
                filesPerRepo.get(gitRepoDirectory).add(file);
            }
        }
        return filesPerRepo;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        for (TopComponent tc : getUnchangedDocuments()) {
            tc.close();
        }
    }

    public Collection<TopComponent> getUnchangedDocuments() {

        final WindowManager wm = WindowManager.getDefault();
        final LinkedHashSet<TopComponent> result = new LinkedHashSet<TopComponent>();
        final Collection<TopComponent> currentEditors = getCurrentEditors();
        for (TopComponent tc : currentEditors) {
            if (!wm.isEditorTopComponent(tc)) {
                continue;
            }

            //check for the format of an unsaved file
            boolean isUnsaved = null != tc.getLookup().lookup(SaveCookie.class);
            if (isUnsaved) {
                continue;
            }

            DataObject dob = tc.getLookup().lookup(DataObject.class);
            if (dob != null) {
                final FileObject file = dob.getPrimaryFile();
                Collection<FileObject> modifiedFiles = getModifiedFiles(Arrays.asList(file));
                final boolean isUnmodified = modifiedFiles.isEmpty();
                if (isUnmodified) {
                    result.add(tc);
                }
            } else {
                //close diff windows too
                result.add(tc);
            }
        }
        return result;
    }

    /**
     * Works SVN/HG/GIT since NB8.1. See
     * https://netbeans.org/bugzilla/show_bug.cgi?id=248811
     *
     * @return
     */
    public Collection<TopComponent> getUnchangedDocumentsForNB81() {

        final WindowManager wm = WindowManager.getDefault();
        final LinkedHashSet<TopComponent> result = new LinkedHashSet<TopComponent>();
        for (TopComponent tc : getCurrentEditors()) {
            if (!wm.isEditorTopComponent(tc)) {
                continue;
            }

            //check for the format of an unsaved file
            boolean isUnsaved = null != tc.getLookup().lookup(SaveCookie.class);
            if (isUnsaved) {
                continue;
            }

            DataObject dob = tc.getLookup().lookup(DataObject.class);
            if (dob != null) {
                final FileObject file = dob.getPrimaryFile();
                Object attribute = file.getAttribute("ProvidedExtensions.VCSIsModified");
                if (null != attribute) {
                    if (Boolean.FALSE.equals(attribute)) {
                        result.add(tc);
                    }
                } else {
                    //could not determine status, keep this document
                }
            } else {
                //close diff windows too
                result.add(tc);
            }
        }
        return result;
    }

    private Collection<TopComponent> getCurrentEditors() {
        final ArrayList<TopComponent> result = new ArrayList<TopComponent>();
        final WindowManager wm = WindowManager.getDefault();
        for (Mode mode : wm.getModes()) {
            if (wm.isEditorMode(mode)) {
                result.addAll(Arrays.asList(wm.getOpenedTopComponents(mode)));
            }
        }
        return result;
    }

    static class FileMapper {

        Map<FileObject, File> fo2f = new HashMap<FileObject, File>();
        Map<File, FileObject> f2fo = new HashMap<File, FileObject>();

        void putAllFileObjects(Collection<FileObject> fileObjects) {
            for (FileObject fileObject : fileObjects) {
                put(fileObject);
            }
        }

        void putAllFiles(Collection<File> files) {
            for (File file : files) {
                put(file);
            }
        }

        void put(FileObject fileObject) {
            File file = FileUtil.toFile(fileObject);

            fo2f.put(fileObject, file);
            f2fo.put(file, fileObject);
        }

        void put(File file) {
            FileObject fileObject = FileUtil.toFileObject(FileUtil.normalizeFile(file));

            fo2f.put(fileObject, file);
            f2fo.put(file, fileObject);
        }

        Map<FileObject, File> toFiles() {
            return fo2f;
        }

        Map<File, FileObject> toFileObjects() {
            return f2fo;
        }
    }

}
