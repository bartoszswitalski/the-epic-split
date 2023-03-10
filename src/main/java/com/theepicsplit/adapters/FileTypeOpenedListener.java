package com.theepicsplit.adapters;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectLocator;
import com.intellij.openapi.vfs.VirtualFile;
import com.theepicsplit.models.FilterState;
import com.theepicsplit.services.StateService;
import org.jetbrains.annotations.NotNull;


public class FileTypeOpenedListener implements FileEditorManagerListener {
	private static final int DEFAULT_TAB_INDEX = 0;
	private static final int FILTERED_TAB_INDEX = 1;

	@Override
	public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile openedFile) {
		this._tryMoveFileToTabGroup(openedFile);
	}

	private void _tryMoveFileToTabGroup(VirtualFile openedFile) {
		FileEditorManagerEx fileEditorManager = this._getFileEditorManager(openedFile);
		boolean twoTabsAreOpen = fileEditorManager.getWindows().length == 2;
		FilterState filterState = StateService.getInstance().getState();
		boolean filterIsEnabled = filterState.isFilterEnabled;
		boolean regexIsNotEmpty = filterState.filterRegex != null && !filterState.filterRegex.isEmpty();

		if (filterIsEnabled && twoTabsAreOpen && regexIsNotEmpty) {
			this._moveFileToTargetTabGroup(openedFile);
		}
	}

	private FileEditorManagerEx _getFileEditorManager(VirtualFile openedFile) {
		Project currentProject = ProjectLocator.getInstance().guessProjectForFile(openedFile);

		return FileEditorManagerEx.getInstanceEx(currentProject);
	}

	private void _moveFileToTargetTabGroup(VirtualFile file) {
		FileEditorManagerEx fileEditorManager = this._getFileEditorManager(file);
		EditorWindow[] windowPanes = fileEditorManager.getWindows();
		int targetWindowPaneIndex = this._isFileNameMatchingFilter(file) ? FILTERED_TAB_INDEX : DEFAULT_TAB_INDEX;
		EditorWindow initialWindowPane = fileEditorManager.getCurrentWindow();
		EditorWindow targetWindowPane = windowPanes[targetWindowPaneIndex];

		fileEditorManager.openFileWithProviders(file, true, targetWindowPane);
		this._tryCloseFileInWrongTabGroup(file, initialWindowPane, targetWindowPane);
	}

	private boolean _isFileNameMatchingFilter(VirtualFile file) {
		String filterRegex = StateService.getInstance().getState().filterRegex;

		return file.getName().matches(filterRegex);
	}

	private void _tryCloseFileInWrongTabGroup(VirtualFile file, EditorWindow initialWindowPane, EditorWindow targetWindowPane) {
		if (initialWindowPane == targetWindowPane) {
			return;
		}

		if (!initialWindowPane.isFileOpen(file)) {
			return;
		}

		this._closeFileInTabGroup(file, initialWindowPane);
	}

	private void _closeFileInTabGroup(VirtualFile file, EditorWindow tabGroup) {
		Runnable closeFile = () -> tabGroup.closeFile(file, true, true);

		ApplicationManager.getApplication().invokeLater(closeFile);
	}
}
