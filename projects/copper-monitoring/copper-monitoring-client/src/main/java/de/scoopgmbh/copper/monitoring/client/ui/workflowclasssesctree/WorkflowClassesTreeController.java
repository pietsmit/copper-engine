/*
 * Copyright 2002-2013 SCOOP Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.scoopgmbh.copper.monitoring.client.ui.workflowclasssesctree;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.util.Callback;
import javafx.util.StringConverter;

import com.google.common.base.Optional;

import de.scoopgmbh.copper.monitoring.client.form.issuereporting.IssueReporter;
import de.scoopgmbh.copper.monitoring.client.util.WorkflowVersion;

public class WorkflowClassesTreeController {
	private final TreeView<DisplayWorkflowClassesModel> treeView;
	private final IssueReporter issueReporter;
	
	public WorkflowClassesTreeController(final TreeView<DisplayWorkflowClassesModel> treeView, IssueReporter issueReporter) {
		super();
		this.treeView = treeView;
		this.issueReporter = issueReporter;
		
		treeView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		treeView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<DisplayWorkflowClassesModel>>() {
			@Override
			public void changed(ObservableValue<? extends TreeItem<DisplayWorkflowClassesModel>> observable,
					TreeItem<DisplayWorkflowClassesModel> oldValue, TreeItem<DisplayWorkflowClassesModel> newValue) {
				
				if (newValue!=null && newValue.getValue()!=null /*&& newValue.getChildren().isEmpty()*/){
					WorkflowVersion workflowClassesModel = newValue.getValue().value;
					selectedItem.set(workflowClassesModel);
					expand(newValue);
				}
				
			}
		});
		
		final ContextMenu contextMenu = new ContextMenu();
		final MenuItem copy = new MenuItem("copy");
		copy.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN));
		copy.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				final Clipboard clipboard = Clipboard.getSystemClipboard();
			    final ClipboardContent content = new ClipboardContent();
			    content.putString(treeView.getSelectionModel().getSelectedItem().getValue().displayname);
			    clipboard.setContent(content);
			}
		});
		copy.disableProperty().bind(treeView.getSelectionModel().selectedItemProperty().isNull());
		contextMenu.getItems().add(copy);
		treeView.setContextMenu(contextMenu);
	}
	
	private void expand(TreeItem<DisplayWorkflowClassesModel> newValue){
		for (TreeItem<DisplayWorkflowClassesModel> item: newValue.getChildren()){
			expand(item);
		}
		newValue.setExpanded(true);
	}
	
	public static class DisplayWorkflowClassesModel{
		public WorkflowVersion value;
		public String displayname;
		public DisplayWorkflowClassesModel(WorkflowVersion value, String displayname) {
			super();
			this.value = value;
			this.displayname = displayname;
		}
	}
	
	
	private Optional<TreeItem<DisplayWorkflowClassesModel>> findMajorItem(WorkflowVersion newWorkflowVersion,List<TreeItem<DisplayWorkflowClassesModel>> result){
		for (TreeItem<DisplayWorkflowClassesModel> classnameItem: result){
			if (newWorkflowVersion.classname.getValue().equals(classnameItem.getValue().value.classname.getValue())){
				for (TreeItem<DisplayWorkflowClassesModel> majorItem: classnameItem.getChildren()){
					if (newWorkflowVersion.versionMajor.getValue().equals(majorItem.getValue().value.versionMajor.getValue())){
						return Optional.of(majorItem);
					}
				}
			}
		}
		return Optional.absent();
	}
	
	public String getMajorVersion(WorkflowVersion workflowVersion){
		return "Major: "+ nullFix(workflowVersion.versionMajor.getValue());
	}
	
	public String getMinorVersion(WorkflowVersion workflowVersion){
		return "Minor: "+nullFix(workflowVersion.versionMinor.getValue())+
				"\nPatchlevel: "+nullFix(workflowVersion.patchlevel.get())+"\nAlias: "+nullFix(workflowVersion.alias.get());
	}
	
	public String nullFix(Object object){
		return object==null?"?": object.toString();
	}
	
	public List<TreeItem<DisplayWorkflowClassesModel>> groupToTreeItem(List<WorkflowVersion> list){
		//from flat List: classname, majorversion, minorversion
		//totree: 
		//classname 
		//	->majorversion
		//		->minorversion		
		
		List<TreeItem<DisplayWorkflowClassesModel>> result = new ArrayList<TreeItem<DisplayWorkflowClassesModel>>();
		for (WorkflowVersion newWorkflowVersion: list){
			Optional<TreeItem<DisplayWorkflowClassesModel>> existingMajorItem=findMajorItem(newWorkflowVersion,result);
			
			TreeItem<DisplayWorkflowClassesModel> majorVersionItemToAdd = null;
			if (existingMajorItem.isPresent()){
				majorVersionItemToAdd = existingMajorItem.get();
			} else {
				TreeItem<DisplayWorkflowClassesModel> classnameItemToAdd = null;
				classnameItemToAdd = new TreeItem<DisplayWorkflowClassesModel>(new DisplayWorkflowClassesModel(newWorkflowVersion, newWorkflowVersion.classname.get()));
				result.add(classnameItemToAdd);
				majorVersionItemToAdd =new TreeItem<DisplayWorkflowClassesModel>(new DisplayWorkflowClassesModel(newWorkflowVersion, getMajorVersion(newWorkflowVersion)));
				classnameItemToAdd.getChildren().add(majorVersionItemToAdd);
			}
			majorVersionItemToAdd.getChildren().add(new TreeItem<DisplayWorkflowClassesModel>(new DisplayWorkflowClassesModel(newWorkflowVersion, getMinorVersion(newWorkflowVersion) )));
		}
		
		return result;
	}

	public void refresh(List<WorkflowVersion> newItems) {
		TreeItem<DisplayWorkflowClassesModel> rootItem = new TreeItem<DisplayWorkflowClassesModel>();
		rootItem.getChildren().addAll(groupToTreeItem(newItems));
		treeView.setRoot(rootItem);

		treeView.setCellFactory(new Callback<TreeView<DisplayWorkflowClassesModel>, TreeCell<DisplayWorkflowClassesModel>>() {
		    @Override
		    public TreeCell<DisplayWorkflowClassesModel> call(TreeView<DisplayWorkflowClassesModel> listView) {
		        return new TextFieldTreeCell<DisplayWorkflowClassesModel>(new StringConverter<DisplayWorkflowClassesModel>() {
					@Override
					public DisplayWorkflowClassesModel fromString(String string) {
						return null;
					}
					@Override
					public String toString(DisplayWorkflowClassesModel object) {
						return object.displayname;
					}
				});
		    }
		});
		
        rootItem.setExpanded(true);
        treeView.setShowRoot(false);
	}
	
	public SimpleObjectProperty<WorkflowVersion> selectedItem = new SimpleObjectProperty<WorkflowVersion>();
	
}
