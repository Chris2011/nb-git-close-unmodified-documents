# nb-git-close-unmodified-documents
NetBeans plugin which allows to close the editor windows, which are unmodified (Git only). Implements https://netbeans.org/bugzilla/show_bug.cgi?id=219850

This plugins adds a new action "<i>Close Unmodified Documents</i>' to the context menu of an editor tab. All editors will be closed except those of files with a VCS status of modified/added/inConflict/renamed/copied.
<p>Supported VCS are Git, Mercurial and Subversion. Editor/documents, which have an unknown or no VCS status, will be closed too.</p>

<h2>Changes</h2>
<ul>
	<li>2.1.0
		<ul>
                    			<li><a href="https://github.com/markiewb/nb-git-close-unmodified-documents/issues/5">Issue 5</a>: Don't block UI, when the closing takes too long</li>
		</ul>
	</li>
	<li>2.0.0
		<ul>
                    			<li><a href="https://github.com/markiewb/nb-git-close-unmodified-documents/issues/3">Issue 3</a>: Support of Hg/SVN (use new API from NB 8.1, which is much faster)</li>
		</ul>
	</li>

	<li>1.0.2
		<ul>
                    			<li><a href="https://github.com/markiewb/nb-git-close-unmodified-documents/issues/2">Issue 2</a>: Fixed NPE</li>
		</ul>
	</li>
	<li>1.0.1
		<ul>
                    			<li><a href="https://github.com/markiewb/nb-git-close-unmodified-documents/issues/1">Issue 1</a>: Performance improvement</li>
		</ul>
	</li>
	<li>1.0.0
		<ul>
                    			<li>Initial version</li>
		</ul>
	</li>

</ul>



<p>Provide defects, request for enhancements and feedback at <a href="https://github.com/markiewb/nb-git-close-unmodified-documents/issues">https://github.com/markiewb/nb-git-close-unmodified-documents/issues</a></p><p>Compatible to NB 8.1+</p>
<p>Legal disclaimer: Code is licensed under Apache 2.0. </p>

<p>
<a href="https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=K4CMP92RZELE2"><img src="https://www.paypalobjects.com/en_US/i/btn/btn_donate_SM.gif" alt="btn_donate_SM.gif"></a>
</p>
