/*******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2012 by Pentaho : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.di.ui.cacheFile.dialog;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.cachefile.CacheFile;
import org.pentaho.di.core.Const;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.core.dialog.EnterSelectionDialog;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.gui.WindowProperty;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

/**
 * 
 * Dialog that allows you to edit the settings of the cluster schema
 * 
 * @see ClusterSchema
 * @author Matt
 * @since 17-11-2006
 *
 */

public class CacheFileDialog extends Dialog 
{
	private static Class<?> PKG = CacheFileDialog.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

	// private static LogWriter log = LogWriter.getInstance();
	
	private CacheFile cacheFile;
	
	private Shell     shell;

    // Name
	private Text     wName;
	private Text wFilePath;

	private Button    wOK, wCancel;
	
    private ModifyListener lsMod;

	private PropsUI     props;

    private int middle;
    private int margin;

    private boolean ok;

    private Text wMemorySize;

    private Button wBTreeIndex;

    private Button wHashIndex;
    
    private Button       wbbFilename;
    
	public CacheFileDialog(Shell par, CacheFile cacheFile)
	{
		super(par, SWT.NONE);
		this.cacheFile = cacheFile;               
		props=PropsUI.getInstance();
        ok=false;
	}
	
	public boolean open() 
	{
		Shell parent = getParent();
		shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN );
 		props.setLook(shell);
		shell.setImage( GUIResource.getInstance().getImageCluster());

		lsMod = new ModifyListener() 
		{
			public void modifyText(ModifyEvent e) 
			{
//				clusterSchema.setChanged();
			}
		};

		middle = props.getMiddlePct();
		margin = Const.MARGIN;

		FormLayout formLayout = new FormLayout ();
		formLayout.marginWidth  = Const.FORM_MARGIN;
		formLayout.marginHeight = Const.FORM_MARGIN;
		
		shell.setText(BaseMessages.getString(PKG, "CacheFileSchemaDialog.Shell.Title")); //$NON-NLS-1$
		shell.setLayout (formLayout);
 		
		// First, add the buttons...
		
		// Buttons
		wOK     = new Button(shell, SWT.PUSH); 
		wOK.setText(BaseMessages.getString(PKG, "System.Button.OK")); //$NON-NLS-1$

		wCancel = new Button(shell, SWT.PUSH); 
		wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel")); //$NON-NLS-1$

		Button[] buttons = new Button[] { wOK, wCancel };
		BaseStepDialog.positionBottomButtons(shell, buttons, margin, null);
		
		// The rest stays above the buttons, so we added those first...
        
        // What's the schema name??
        Label wlName = new Label(shell, SWT.RIGHT); 
        props.setLook(wlName);
        wlName.setText(BaseMessages.getString(PKG, "CacheFileSchemaDialog.Schema.Label")); //$NON-NLS-1$
        FormData fdlName = new FormData();
        fdlName.top   = new FormAttachment(0, 0);
        fdlName.left  = new FormAttachment(0, 0);  // First one in the left top corner
        fdlName.right = new FormAttachment(middle, 0);
        wlName.setLayoutData(fdlName);

        wName = new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
        props.setLook(wName);
        wName.addModifyListener(lsMod);
        FormData fdName = new FormData();
        fdName.top  = new FormAttachment(0, 0);
        fdName.left = new FormAttachment(middle, margin); // To the right of the label
        fdName.right= new FormAttachment(95, 0);
        wName.setLayoutData(fdName);
        
        Label wlFilePath = new Label(shell, SWT.RIGHT); 
        props.setLook(wlFilePath);
        wlFilePath.setText(BaseMessages.getString(PKG, "CacheFileSchemaDialog.FilePath.Label")); //$NON-NLS-1$
        FormData fdlFilePath = new FormData();
        fdlFilePath.top   = new FormAttachment(wName, margin);
        fdlFilePath.left  = new FormAttachment(0, 0);  // First one in the left top corner
        fdlFilePath.right = new FormAttachment(middle, 0);
        wlFilePath.setLayoutData(fdlFilePath);

        wbbFilename=new Button(shell, SWT.PUSH);
        props.setLook(wbbFilename);
        wbbFilename.setText(BaseMessages.getString(PKG, "System.Button.Browse"));
        wbbFilename.setToolTipText(BaseMessages.getString(PKG, "System.Tooltip.BrowseForFileOrDirAndAdd"));
        FormData fdbFilename=new FormData();
        fdbFilename.right= new FormAttachment(95, 0);
        fdbFilename.top  = new FormAttachment(wName, margin);
        wbbFilename.setLayoutData(fdbFilename);
        
        wFilePath = new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
        props.setLook(wFilePath);
        wFilePath.addModifyListener(lsMod);
        FormData fdFilePath = new FormData();
        fdFilePath.top  = new FormAttachment(wName, margin);
        fdFilePath.left = new FormAttachment(middle, 3); // To the right of the label
        fdFilePath.right= new FormAttachment(wbbFilename, -margin);
        wFilePath.setLayoutData(fdFilePath);       
        wbbFilename.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				FileDialog dialog = new FileDialog(shell, SWT.OPEN);
//				dialog.setFilterExtensions(new String[] { "*.txt", "*.csv", "*" });
				if (dialog.open() != null)
				{
					String str = dialog.getFilterPath()+System.getProperty("file.separator")+dialog.getFileName();
					wFilePath.setText(str);
				}
			}
		});
        
        Label wlMemorySize = new Label(shell, SWT.RIGHT); 
        props.setLook(wlMemorySize);
        wlMemorySize.setText(BaseMessages.getString(PKG, "CacheFileSchemaDialog.MemorySize.Label")); //$NON-NLS-1$
        FormData fdlMemorySize = new FormData();
        fdlMemorySize.top   = new FormAttachment(wFilePath, margin);
        fdlMemorySize.left  = new FormAttachment(0, 0);  // First one in the left top corner
        fdlMemorySize.right = new FormAttachment(middle, 0);
        wlMemorySize.setLayoutData(fdlMemorySize);

        
        wMemorySize = new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
        props.setLook(wMemorySize);
        wMemorySize.addModifyListener(lsMod);
        FormData fdMemorySize = new FormData();
        fdMemorySize.top  = new FormAttachment(wFilePath, margin);
        fdMemorySize.left = new FormAttachment(middle, margin); // To the right of the label
        fdMemorySize.right= new FormAttachment(95, 0);
        wMemorySize.setLayoutData(fdMemorySize);


        // use BTree Index
        Label wlBTreeIndex = new Label(shell, SWT.RIGHT); 
        props.setLook(wlBTreeIndex);
        wlBTreeIndex.setText(BaseMessages.getString(PKG, "CacheFileSchemaDialog.BTreeIndex.Label")); //$NON-NLS-1$
        FormData fdlBTreeIndex = new FormData();
        fdlBTreeIndex.top   = new FormAttachment(wMemorySize, margin);
        fdlBTreeIndex.left  = new FormAttachment(0, 0);  // First one in the left top corner
        fdlBTreeIndex.right = new FormAttachment(middle, 0);
        wlBTreeIndex.setLayoutData(fdlBTreeIndex);

        wBTreeIndex = new Button(shell, SWT.RADIO);
        props.setLook(wBTreeIndex);
        FormData fdBTreeIndex = new FormData();
        fdBTreeIndex.top  = new FormAttachment(wMemorySize, margin);
        fdBTreeIndex.left = new FormAttachment(middle, margin); // To the right of the label
        fdBTreeIndex.right= new FormAttachment(95, 0);
        wBTreeIndex.setLayoutData(fdBTreeIndex);

        // use Hash Index
        Label wlHashIndex = new Label(shell, SWT.RIGHT); 
        props.setLook(wlHashIndex);
        wlHashIndex.setText(BaseMessages.getString(PKG, "CacheFileSchemaDialog.HashIndex.Label")); //$NON-NLS-1$
        FormData fdlHashIndex = new FormData();
        fdlHashIndex.top   = new FormAttachment(wBTreeIndex, margin);
        fdlHashIndex.left  = new FormAttachment(0, 0);  // First one in the left top corner
        fdlHashIndex.right = new FormAttachment(middle, 0);
        wlHashIndex.setLayoutData(fdlHashIndex);

        wHashIndex = new Button(shell, SWT.RADIO);
        props.setLook(wHashIndex);
        FormData fdHashIndex = new FormData();
        fdHashIndex.top  = new FormAttachment(wBTreeIndex, margin);
        fdHashIndex.left = new FormAttachment(middle, margin); // To the right of the label
        fdHashIndex.right= new FormAttachment(95, 0);
        wHashIndex.setLayoutData(fdHashIndex);
        		
		// Add listeners
		wOK.addListener(SWT.Selection, new Listener () { public void handleEvent (Event e) { ok(); } } );
        wCancel.addListener(SWT.Selection, new Listener () { public void handleEvent (Event e) { cancel(); } } );
		

		// Detect X or ALT-F4 or something that kills this window...
		shell.addShellListener(	new ShellAdapter() { public void shellClosed(ShellEvent e) { cancel(); } } );
	
		getData();

		BaseStepDialog.setSize(shell,300,200,true);
		
		shell.open();
		Display display = parent.getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) display.sleep();
		}
		return ok;
	}
	


    public void dispose()
	{
		props.setScreen(new WindowProperty(shell));
		shell.dispose();
	}
    
    public void getData()
	{	if(cacheFile.getObjectId()!=null)
			wName.setEnabled(false);
    	wName.setText(cacheFile.getName()==null?"":cacheFile.getName());
    	wFilePath.setText(cacheFile.getFilePath()==null?"":cacheFile.getFilePath());
    	wMemorySize.setText(Integer.toString(cacheFile.getMemorySize()));
    	if(cacheFile.getIndexType()==CacheFile.USE_BTREE_INDEX)
    		wBTreeIndex.setSelection(true);
    	else wHashIndex.setSelection(true);	
	}
    

    private void cancel()
	{
    	dispose();
	}
	
	public void ok()
	{   
		cacheFile.setName(wName.getText());
		cacheFile.setFilePath(wFilePath.getText());
		cacheFile.setMemorySize(Integer.parseInt(wMemorySize.getText()==null?"16":wMemorySize.getText()));
		if(wBTreeIndex.getSelection())
			cacheFile.setIndexType(CacheFile.USE_BTREE_INDEX);
		else 
			cacheFile.setIndexType(CacheFile.USE_HASH_INDEX);
        ok=true;
        dispose();
	}   
	
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}