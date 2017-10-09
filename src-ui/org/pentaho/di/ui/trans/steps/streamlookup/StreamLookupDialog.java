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

package org.pentaho.di.ui.trans.steps.streamlookup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.cachefile.CacheFactory;
import org.pentaho.di.cachefile.CacheFile;
import org.pentaho.di.cachefile.ICache;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.errorhandling.StreamInterface;
import org.pentaho.di.trans.steps.streamlookup.StreamLookupMeta;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.trans.step.BaseStepDialog;



public class StreamLookupDialog extends BaseStepDialog implements StepDialogInterface
{
	private static Class<?> PKG = StreamLookupMeta.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

	private Label        wlStep;
	private CCombo       wStep;
	private FormData     fdlStep, fdStep;

	private Label        wlKey;
	private TableView    wKey;
	private FormData     fdlKey, fdKey;

	private Label        wlReturn;
	private TableView    wReturn;
	private FormData     fdlReturn, fdReturn;
	
    private Label        wlPreserveMemory;
    private Button       wPreserveMemory;
    private FormData     fdlPreserveMemory, fdPreserveMemory;
    
    private Label        wlFromStep;
    private Button		 wFromStep;
    private FormData	 fdlFromStep,fdFromStep;
    
    private Label        wlUpdateCacheFile;
    private Button		 wUpdateCacheFile;
    private FormData	 fdlUpdateCacheFile,fdUpdateCacheFile;    
    
    private Label		 wlCacheFileName;
    private CCombo		 wUpdateCacheFileCC;
    private FormData	 fdlCacheFileName,fdCacheFileName;
    
    private Label        wlFromCacheFile;
    private Button		 wFromCacheFile;
    private FormData	 fdlFromCacheFile,fdFromCacheFile;   
     
    private Label		 wlFromCacheFileName;
    private CCombo		 wSearchCacheFileNameCC;
    private FormData	 fdlFromCacheFileName,fdFromCacheFileName;    
    
    private Label        wlSortedList;
    private Button       wSortedList;
    private FormData     fdlSortedList, fdSortedList;

    private Label        wlIntegerPair;
    private Button       wIntegerPair;
    private FormData     fdlIntegerPair, fdIntegerPair;

	private StreamLookupMeta input;
	
	private Group stepGroup;
	private Group cacheFileGroup;
	private FormData dStepGroup,dCacheFileGroup;

    private Button       wGetLU;
    private Listener     lsGetLU;
    
    private ColumnInfo[] ciKey;
    
    private ColumnInfo[] ciReturn;
    
	public StreamLookupDialog(Shell parent, Object in, TransMeta transMeta, String sname)
	{
		super(parent, (BaseStepMeta)in, transMeta, sname);
		input=(StreamLookupMeta)in;
	}

	public String open()
	{
		Shell parent = getParent();
		Display display = parent.getDisplay();

		shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN);
 		props.setLook(shell);
        setShellImage(shell, input);

		ModifyListener lsMod = new ModifyListener() 
		{
			public void modifyText(ModifyEvent e) 
			{
				input.setChanged();
			}
		};
		SelectionListener lsSelection = new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e) 
			{
				input.setChanged();
				setComboBoxesLookup();
			}
		};
		changed = input.hasChanged();

		FormLayout formLayout = new FormLayout ();
		formLayout.marginWidth  = Const.FORM_MARGIN;
		formLayout.marginHeight = Const.FORM_MARGIN;

		shell.setLayout(formLayout);
		shell.setText(BaseMessages.getString(PKG, "StreamLookupDialog.Shell.Title")); //$NON-NLS-1$
		
		int middle = props.getMiddlePct();
		int margin = Const.MARGIN;

		// Stepname line
		wlStepname=new Label(shell, SWT.RIGHT);
		wlStepname.setText(BaseMessages.getString(PKG, "StreamLookupDialog.Stepname.Label")); //$NON-NLS-1$
 		props.setLook(wlStepname);
		fdlStepname=new FormData();
		fdlStepname.left = new FormAttachment(0, 0);
		fdlStepname.right= new FormAttachment(middle, -margin);
		fdlStepname.top  = new FormAttachment(0, margin);
		wlStepname.setLayoutData(fdlStepname);
		wStepname=new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		wStepname.setText(stepname);
 		props.setLook(wStepname);
		wStepname.addModifyListener(lsMod);
		fdStepname=new FormData();
		fdStepname.left = new FormAttachment(middle, 0);
		fdStepname.top  = new FormAttachment(0, margin);
		fdStepname.right= new FormAttachment(100, 0);
		wStepname.setLayoutData(fdStepname);

		//start of from step group
		stepGroup = new Group(shell,SWT.SHADOW_NONE);
		props.setLook(stepGroup);
		stepGroup.setText(BaseMessages.getString(PKG, "StreamLookupDialog.FromStep.Label"));
		FormLayout stepGroupLayout = new FormLayout();
		stepGroupLayout.marginWidth = 10;
		stepGroupLayout.marginHeight = 10;
		stepGroup.setLayout(stepGroupLayout);
		
		wlFromStep = new Label(stepGroup,SWT.RIGHT);
		wlFromStep.setText(BaseMessages.getString(PKG,"StreamLookupDialog.FromStep.Label"));
 		props.setLook(wlFromStep);
		fdlFromStep=new FormData();
		fdlFromStep.left = new FormAttachment(0, 0);
		fdlFromStep.right= new FormAttachment(middle, -margin);
		fdlFromStep.top  = new FormAttachment(wStepname, margin*2);
		wlFromStep.setLayoutData(fdlFromStep);
		wFromStep = new Button(stepGroup,SWT.CHECK);
        props.setLook(wFromStep);
        fdFromStep= new FormData();
        fdFromStep.top  = new FormAttachment(wStepname, margin*2);
        fdFromStep.left = new FormAttachment(middle, 0); 
        fdFromStep.right= new FormAttachment(100, 0);
        wFromStep.setLayoutData(fdFromStep);	
        wFromStep.addSelectionListener(new SelectionAdapter() 
        {
            public void widgetSelected(SelectionEvent e) 
            {
                wlFromCacheFile.setEnabled(!wFromStep.getSelection());
                wFromCacheFile.setEnabled(!wFromStep.getSelection());
                wlFromCacheFileName.setEnabled(!wFromStep.getSelection());
                wUpdateCacheFile.setSelection(wFromStep.getSelection());
                wSearchCacheFileNameCC.setEnabled(!wFromStep.getSelection());
            }
        });
		
		// Lookup step line...
		wlStep=new Label(stepGroup, SWT.RIGHT);
		wlStep.setText(BaseMessages.getString(PKG, "StreamLookupDialog.LookupStep.Label")); //$NON-NLS-1$
 		props.setLook(wlStep);
		fdlStep=new FormData();
		fdlStep.left = new FormAttachment(0, 0);
		fdlStep.right= new FormAttachment(middle, -margin);
		fdlStep.top  = new FormAttachment(wFromStep, margin*2);
		wlStep.setLayoutData(fdlStep);
		wStep=new CCombo(stepGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
 		props.setLook(wStep);
		
 		List<StepMeta> previousSteps = transMeta.findPreviousSteps(stepMeta, true);
		for (StepMeta previousStep : previousSteps)
		{
			wStep.add(previousStep.getName());
		}
		// transMeta.getInfoStep()
		
		wStep.addModifyListener(lsMod);
		wStep.addSelectionListener(lsSelection);
		
		fdStep=new FormData();
		fdStep.left = new FormAttachment(middle, 0);
		fdStep.top  = new FormAttachment(wFromStep, margin*2);
		fdStep.right= new FormAttachment(100, 0);
		wStep.setLayoutData(fdStep);

		wlUpdateCacheFile = new Label(stepGroup,SWT.RIGHT);
		wlUpdateCacheFile.setText(BaseMessages.getString(PKG,"StreamLookupDialog.UpdateCacheFile.Label"));
 		props.setLook(wlUpdateCacheFile);
		fdlUpdateCacheFile=new FormData();
		fdlUpdateCacheFile.left = new FormAttachment(0, 0);
		fdlUpdateCacheFile.right= new FormAttachment(middle, -margin);
		fdlUpdateCacheFile.top  = new FormAttachment(wStep, margin*2);
		wlUpdateCacheFile.setLayoutData(fdlUpdateCacheFile);
		wUpdateCacheFile = new Button(stepGroup,SWT.CHECK);
        props.setLook(wUpdateCacheFile);
        fdUpdateCacheFile= new FormData();
        fdUpdateCacheFile.top  = new FormAttachment(wStep, margin*2);
        fdUpdateCacheFile.left = new FormAttachment(middle, 0); 
        fdUpdateCacheFile.right= new FormAttachment(100, 0);
        wUpdateCacheFile.setLayoutData(fdUpdateCacheFile);

		wlCacheFileName=new Label(stepGroup, SWT.RIGHT);
		wlCacheFileName.setText(BaseMessages.getString(PKG, "StreamLookupDialog.CacheFile.Label")); //$NON-NLS-1$
 		props.setLook(wlCacheFileName);
		fdlCacheFileName=new FormData();
		fdlCacheFileName.left = new FormAttachment(0, 0);
		fdlCacheFileName.right= new FormAttachment(middle, -margin);
		fdlCacheFileName.top  = new FormAttachment(wUpdateCacheFile, margin*2);
		wlCacheFileName.setLayoutData(fdlCacheFileName);
		wUpdateCacheFileCC=new CCombo(stepGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
 		props.setLook(wUpdateCacheFileCC);	
 		for(CacheFile cacheFile : transMeta.getCacheFile()){
 			wUpdateCacheFileCC.add(cacheFile.getName());
 		}
		
		wUpdateCacheFileCC.addModifyListener(lsMod);
		wUpdateCacheFileCC.addSelectionListener(lsSelection);
		
		fdCacheFileName=new FormData();
		fdCacheFileName.left = new FormAttachment(middle, 0);
		fdCacheFileName.top  = new FormAttachment(wUpdateCacheFile, margin*2);
		fdCacheFileName.right= new FormAttachment(100, 0);
		wUpdateCacheFileCC.setLayoutData(fdCacheFileName);    
		
		dStepGroup = new FormData();
		dStepGroup.left = new FormAttachment(0, margin);
		dStepGroup.top = new FormAttachment(wStepname, margin);
		dStepGroup.right = new FormAttachment(100, -margin);
		stepGroup.setLayoutData(dStepGroup);
		//end of from step group
		
		//start of from cache file group

		cacheFileGroup = new Group(shell,SWT.SHADOW_NONE);
		props.setLook(cacheFileGroup);
		cacheFileGroup.setText(BaseMessages.getString(PKG, "StreamLookupDialog.FromCacheFile.Label"));
		FormLayout cacheFileGroupLayout = new FormLayout();
		cacheFileGroupLayout.marginWidth = 10;
		cacheFileGroupLayout.marginHeight = 10;
		cacheFileGroup.setLayout(cacheFileGroupLayout);
		
		wlFromCacheFile = new Label(cacheFileGroup,SWT.RIGHT);
		wlFromCacheFile.setText(BaseMessages.getString(PKG,"StreamLookupDialog.FromCacheFile.Label"));
 		props.setLook(wlFromCacheFile);
		fdlFromCacheFile=new FormData();
		fdlFromCacheFile.left = new FormAttachment(0, 0);
		fdlFromCacheFile.right= new FormAttachment(middle, -margin);
		fdlFromCacheFile.top  = new FormAttachment(wUpdateCacheFileCC, margin*2);
		wlFromCacheFile.setLayoutData(fdlFromCacheFile);
		wFromCacheFile = new Button(cacheFileGroup,SWT.CHECK);
        props.setLook(wFromCacheFile);
        fdFromCacheFile= new FormData();
        fdFromCacheFile.top  = new FormAttachment(wUpdateCacheFileCC, margin*2);
        fdFromCacheFile.left = new FormAttachment(middle, 0); 
        fdFromCacheFile.right= new FormAttachment(100, 0);
        wFromCacheFile.setLayoutData(fdFromCacheFile);	
        wFromCacheFile.addSelectionListener(new SelectionAdapter() 
        {
            public void widgetSelected(SelectionEvent e) 
            {
                wlFromStep.setEnabled(!wFromCacheFile.getSelection());
                wFromStep.setEnabled(!wFromCacheFile.getSelection());
                wlCacheFileName.setEnabled(!wFromCacheFile.getSelection());
                wUpdateCacheFileCC.setEnabled(!wFromCacheFile.getSelection());
                wlUpdateCacheFile.setEnabled(!wFromCacheFile.getSelection());
                wUpdateCacheFile.setEnabled(!wFromCacheFile.getSelection());
                wlStep.setEnabled(!wFromCacheFile.getSelection());
                wStep.setEnabled(!wFromCacheFile.getSelection());
            }
        });

		wlFromCacheFileName=new Label(cacheFileGroup, SWT.RIGHT);
		wlFromCacheFileName.setText(BaseMessages.getString(PKG, "StreamLookupDialog.CacheFile.Label")); //$NON-NLS-1$
 		props.setLook(wlFromCacheFileName);
		fdlFromCacheFileName=new FormData();
		fdlFromCacheFileName.left = new FormAttachment(0, 0);
		fdlFromCacheFileName.right= new FormAttachment(middle, -margin);
		fdlFromCacheFileName.top  = new FormAttachment(wFromCacheFile, margin*2);
		wlFromCacheFileName.setLayoutData(fdlFromCacheFileName);
		wSearchCacheFileNameCC=new CCombo(cacheFileGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
 		props.setLook(wSearchCacheFileNameCC);	
 		for(CacheFile cacheFile : transMeta.getCacheFile()){
 			wSearchCacheFileNameCC.add((cacheFile.getName()));
 		}
		
		wSearchCacheFileNameCC.addModifyListener(lsMod);
		wSearchCacheFileNameCC.addSelectionListener(lsSelection);
		fdFromCacheFileName=new FormData();
		fdFromCacheFileName.left = new FormAttachment(middle, 0);
		fdFromCacheFileName.top  = new FormAttachment(wFromCacheFile, margin*2);
		fdFromCacheFileName.right= new FormAttachment(100, 0);
		wSearchCacheFileNameCC.setLayoutData(fdFromCacheFileName);  
		
		dCacheFileGroup = new FormData();
		dCacheFileGroup.left = new FormAttachment(0, margin);
		dCacheFileGroup.top = new FormAttachment(stepGroup, margin);
		dCacheFileGroup.right = new FormAttachment(100, -margin);
		cacheFileGroup.setLayoutData(dCacheFileGroup);
		//end of from cache file group
        
		wlKey=new Label(shell, SWT.NONE);
		wlKey.setText(BaseMessages.getString(PKG, "StreamLookupDialog.Key.Label")); //$NON-NLS-1$
 		props.setLook(wlKey);
		fdlKey=new FormData();
		fdlKey.left  = new FormAttachment(0, 0);
		fdlKey.top   = new FormAttachment(cacheFileGroup, margin);
		wlKey.setLayoutData(fdlKey);

		int nrKeyCols=2;
		int nrKeyRows=(input.getKeystream()!=null?input.getKeystream().length:1);
		
		ciKey=new ColumnInfo[nrKeyCols];
		ciKey[0]=new ColumnInfo(BaseMessages.getString(PKG, "StreamLookupDialog.ColumnInfo.Field"),        ColumnInfo.COLUMN_TYPE_CCOMBO, new String[] { "" }, false); //$NON-NLS-1$
		ciKey[1]=new ColumnInfo(BaseMessages.getString(PKG, "StreamLookupDialog.ColumnInfo.LookupField"),  ColumnInfo.COLUMN_TYPE_CCOMBO, new String[] { "" }, false); //$NON-NLS-1$
		
		wKey=new TableView(transMeta, shell, 
						      SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL, 
						      ciKey, 
						      nrKeyRows,  
						      lsMod,
							  props
						      );

		fdKey=new FormData();
		fdKey.left  = new FormAttachment(0, 0);
		fdKey.top   = new FormAttachment(wlKey, margin);
		fdKey.right = new FormAttachment(100, 0);
		fdKey.bottom= new FormAttachment(wlKey, 180);
		wKey.setLayoutData(fdKey);

		// THE UPDATE/INSERT TABLE
		wlReturn=new Label(shell, SWT.NONE);
		wlReturn.setText(BaseMessages.getString(PKG, "StreamLookupDialog.ReturnFields.Label")); //$NON-NLS-1$
 		props.setLook(wlReturn);
		fdlReturn=new FormData();
		fdlReturn.left  = new FormAttachment(0, 0);
		fdlReturn.top   = new FormAttachment(wKey, margin);
		wlReturn.setLayoutData(fdlReturn);
		
		int UpInsCols=4;
		int UpInsRows= (input.getValue()!=null?input.getValue().length:1);
		
		ciReturn=new ColumnInfo[UpInsCols];
		ciReturn[0]=new ColumnInfo(BaseMessages.getString(PKG, "StreamLookupDialog.ColumnInfo.FieldReturn"),    ColumnInfo.COLUMN_TYPE_CCOMBO, new String[] { "" }, false); //$NON-NLS-1$
		ciReturn[1]=new ColumnInfo(BaseMessages.getString(PKG, "StreamLookupDialog.ColumnInfo.NewName"), ColumnInfo.COLUMN_TYPE_TEXT,   false); //$NON-NLS-1$
		ciReturn[2]=new ColumnInfo(BaseMessages.getString(PKG, "StreamLookupDialog.ColumnInfo.Default"),  ColumnInfo.COLUMN_TYPE_TEXT,   false); //$NON-NLS-1$
		ciReturn[3]=new ColumnInfo(BaseMessages.getString(PKG, "StreamLookupDialog.ColumnInfo.Type"),     ColumnInfo.COLUMN_TYPE_CCOMBO, ValueMeta.getTypes() ); //$NON-NLS-1$
		
		wReturn=new TableView(transMeta, shell, 
							  SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL, 
							  ciReturn, 
							  UpInsRows,  
							  lsMod,
							  props
							  );

		fdReturn=new FormData();
		fdReturn.left  = new FormAttachment(0, 0);
		fdReturn.top   = new FormAttachment(wlReturn, margin);
		fdReturn.right = new FormAttachment(100, 0);
		fdReturn.bottom= new FormAttachment(100, -125);
		wReturn.setLayoutData(fdReturn);
        
        wlPreserveMemory=new Label(shell, SWT.RIGHT);
        wlPreserveMemory.setText(BaseMessages.getString(PKG, "StreamLookupDialog.PreserveMemory.Label")); //$NON-NLS-1$
        props.setLook(wlPreserveMemory);
        fdlPreserveMemory=new FormData();
        fdlPreserveMemory.left = new FormAttachment(0, 0);
        fdlPreserveMemory.top  = new FormAttachment(wReturn, margin);
        fdlPreserveMemory.right= new FormAttachment(middle, -margin);
        wlPreserveMemory.setLayoutData(fdlPreserveMemory);
        wPreserveMemory=new Button(shell, SWT.CHECK );
        props.setLook(wPreserveMemory);
        fdPreserveMemory=new FormData();
        fdPreserveMemory.left = new FormAttachment(middle, 0);
        fdPreserveMemory.top  = new FormAttachment(wReturn, margin);
        fdPreserveMemory.right= new FormAttachment(100, 0);
        wPreserveMemory.setLayoutData(fdPreserveMemory);
        wPreserveMemory.addSelectionListener(new SelectionAdapter() 
            {
                public void widgetSelected(SelectionEvent e) 
                {
                    input.setChanged();
                }
            }
        );

        wlIntegerPair=new Label(shell, SWT.RIGHT);
        wlIntegerPair.setText(BaseMessages.getString(PKG, "StreamLookupDialog.IntegerPair.Label")); //$NON-NLS-1$
        props.setLook(wlIntegerPair);
        fdlIntegerPair=new FormData();
        fdlIntegerPair.left = new FormAttachment(0, 0);
        fdlIntegerPair.top  = new FormAttachment(wPreserveMemory, margin);
        fdlIntegerPair.right= new FormAttachment(middle, -margin);
        wlIntegerPair.setLayoutData(fdlIntegerPair);
        wIntegerPair=new Button(shell, SWT.CHECK );
        props.setLook(wIntegerPair);
        fdIntegerPair=new FormData();
        fdIntegerPair.left = new FormAttachment(middle, 0);
        fdIntegerPair.top  = new FormAttachment(wPreserveMemory, margin);
        fdIntegerPair.right= new FormAttachment(100, 0);
        wIntegerPair.setLayoutData(fdIntegerPair);
        wIntegerPair.addSelectionListener(new SelectionAdapter() 
            {
                public void widgetSelected(SelectionEvent e) 
                {
                    input.setChanged();
                }
            }
        );
        
        wlSortedList=new Label(shell, SWT.RIGHT);
        wlSortedList.setText(BaseMessages.getString(PKG, "StreamLookupDialog.SortedList.Label")); //$NON-NLS-1$
        props.setLook(wlSortedList);
        fdlSortedList=new FormData();
        fdlSortedList.left = new FormAttachment(0, 0);
        fdlSortedList.top  = new FormAttachment(wIntegerPair, margin);
        fdlSortedList.right= new FormAttachment(middle, -margin);
        wlSortedList.setLayoutData(fdlSortedList);
        wSortedList=new Button(shell, SWT.CHECK );
        props.setLook(wSortedList);
        fdSortedList=new FormData();
        fdSortedList.left = new FormAttachment(middle, 0);
        fdSortedList.top  = new FormAttachment(wIntegerPair, margin);
        fdSortedList.right= new FormAttachment(100, 0);
        wSortedList.setLayoutData(fdSortedList);
        wSortedList.addSelectionListener(new SelectionAdapter() 
            {
                public void widgetSelected(SelectionEvent e) 
                {
                    input.setChanged();
                }
            }
        );

        
		// THE BUTTONS
		wOK=new Button(shell, SWT.PUSH);
		wOK.setText(BaseMessages.getString(PKG, "System.Button.OK")); //$NON-NLS-1$
		wGet=new Button(shell, SWT.PUSH);
		wGet.setText(BaseMessages.getString(PKG, "StreamLookupDialog.GetFields.Button")); //$NON-NLS-1$
		wGetLU=new Button(shell, SWT.PUSH);
		wGetLU.setText(BaseMessages.getString(PKG, "StreamLookupDialog.GetLookupFields.Button")); //$NON-NLS-1$
		wCancel=new Button(shell, SWT.PUSH);
		wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel")); //$NON-NLS-1$

		setButtonPositions(new Button[] { wOK, wCancel , wGet, wGetLU }, margin, null);

		// Add listeners
		lsOK       = new Listener() { public void handleEvent(Event e) { ok();        } };
		lsGet      = new Listener() { public void handleEvent(Event e) { get();       } };
		lsGetLU    = new Listener() { public void handleEvent(Event e) { getlookup(); } };
		lsCancel   = new Listener() { public void handleEvent(Event e) { cancel();    } };
		
		wOK.addListener    (SWT.Selection, lsOK    );
		wGet.addListener   (SWT.Selection, lsGet   );
		wGetLU.addListener (SWT.Selection, lsGetLU );
		wCancel.addListener(SWT.Selection, lsCancel);
		
		lsDef=new SelectionAdapter() { public void widgetDefaultSelected(SelectionEvent e) { ok(); } };
		
		wStepname.addSelectionListener( lsDef );
		
		// Detect X or ALT-F4 or something that kills this window...
		shell.addShellListener(	new ShellAdapter() { public void shellClosed(ShellEvent e) { cancel(); } } );

		// Set the shell size, based upon previous time...
		setSize();
		
		getData();

		setComboBoxes();
        setComboBoxesLookup();
		input.setChanged(changed);
		checkPriviledges();		
		shell.open();
		while (!shell.isDisposed())
		{
				if (!display.readAndDispatch()) display.sleep();
		}
		return stepname;
	}

	protected void setComboBoxes()
    {
        // 
        // Search the fields in the background
        //
        
        final Runnable runnable = new Runnable()
        {
            public void run()
            {
                StepMeta stepMeta = transMeta.findStep(stepname);
                if (stepMeta!=null)
                {
                    try
                    {
                        RowMetaInterface row = transMeta.getPrevStepFields(stepMeta);
                        Map<String, Integer> prevFields= new HashMap<String, Integer>();
                        // Remember these fields...
                        for (int i=0;i<row.size();i++)
                        {
                            prevFields.put(row.getValueMeta(i).getName(), Integer.valueOf(i));
                        }
                        
                        // Something was changed in the row.
                        //
                		final Map<String, Integer> fields = new HashMap<String, Integer>();;
                        
                        // Add the currentMeta fields...
                        fields.putAll(prevFields);
                        
                        Set<String> keySet = fields.keySet();
                        List<String> entries = new ArrayList<String>(keySet);
                        
                        String[] fieldNames= (String[]) entries.toArray(new String[entries.size()]);
                        Const.sortStrings(fieldNames);
                        // return fields
                        ciKey[0].setComboValues(fieldNames);
                    }
                    catch(KettleException e)
                    {
                    	logError(BaseMessages.getString(PKG, "System.Dialog.GetFieldsFailed.Message"));
                    }
                }
            }
        };
        new Thread(runnable).start();
        
    }
	
	protected void setComboBoxesLookup()
    {
		Runnable fieldLoader = new Runnable() {
			public void run() {
		        StepMeta lookupStepMeta = transMeta.findStep(wStep.getText());
                if (lookupStepMeta!=null)
                {
                    try
                    {
                        RowMetaInterface row = transMeta.getStepFields(lookupStepMeta);
                        Map<String, Integer> lookupFields =new HashMap<String, Integer>();
                        // Remember these fields...
                        for (int i=0;i<row.size();i++)
                        {
                            lookupFields.put(row.getValueMeta(i).getName(), Integer.valueOf(i));
                        }
                        
                        // Something was changed in the row.
        		        //
        				final Map<String, Integer> fields = new HashMap<String, Integer>();
        		        
        		        // Add the currentMeta fields...
        		        fields.putAll(lookupFields);
        		        
        		        Set<String> keySet = fields.keySet();
        		        List<String> entries = new ArrayList<String>(keySet);
        		        
        		        String[] fieldNames= (String[]) entries.toArray(new String[entries.size()]);
        		        Const.sortStrings(fieldNames);
        		        // return fields
        		        ciReturn[0].setComboValues(fieldNames);
        		        ciKey[1].setComboValues(fieldNames);
                    }
                    catch(KettleException e)
                    {
                    	 logError("It was not possible to retrieve the list of fields for step [" + wStep.getText() + "]!");
                    }
                }
			}
		};
		shell.getDisplay().asyncExec(fieldLoader);
    }

	/**
	 * Copy information from the meta-data input to the dialog fields.
	 */ 
	public void getData()
	{
		if(log.isDebug()) logDebug(BaseMessages.getString(PKG, "StreamLookupDialog.Log.GettingKeyInfo")); //$NON-NLS-1$
		
		if (input.getKeystream()!=null)
		for (int i=0;i<input.getKeystream().length;i++)
		{
			TableItem item = wKey.table.getItem(i);
			if (input.getKeystream()[i]     !=null) item.setText(1, input.getKeystream()[i]);
			if (input.getKeylookup()[i]!=null) item.setText(2, input.getKeylookup()[i]);
		}
		
		if (input.getValue()!=null)
		for (int i=0;i<input.getValue().length;i++)
		{
			TableItem item = wReturn.table.getItem(i);
			if (input.getValue()[i]!=null     ) item.setText(1, input.getValue()[i]);
			if (input.getValueName()[i]!=null && !input.getValueName()[i].equals(input.getValue()[i]))
				item.setText(2, input.getValueName()[i]);
			if (input.getValueDefault()[i]!=null  ) item.setText(3, input.getValueDefault()[i]);
			item.setText(4, ValueMeta.getTypeDesc(input.getValueDefaultType()[i]));
		}
		
		StreamInterface infoStream = input.getStepIOMeta().getInfoStreams().get(0);
		wStep.setText( Const.NVL(infoStream.getStepname(), "") );
		wPreserveMemory.setSelection(input.isMemoryPreservationActive());
        wSortedList.setSelection(input.isUsingSortedList());
        wIntegerPair.setSelection(input.isUsingIntegerPair());
        wFromCacheFile.setSelection(input.isFromCacheFile());
        wUpdateCacheFile.setSelection(input.isUpdateCacheFile());
        wFromStep.setSelection(input.isFromStep());
    	List<CacheFile> cacheFiles=transMeta.getCacheFile();
        if(input.getSearchCacheFileName()!=null){
        	for(int i=0;i<cacheFiles.size();i++){
        		if(input.getSearchCacheFileName().equals(cacheFiles.get(i).getName()))
        			wSearchCacheFileNameCC.select(i);
        	}
        }
        if(input.getUpdateCacheFileName()!=null){
        	for(int i=0;i<cacheFiles.size();i++){
        		if(input.getUpdateCacheFileName().equals(cacheFiles.get(i).getName()))
        			wUpdateCacheFileCC.select(i);
        	}
        }
        wStepname.selectAll();
		wKey.setRowNums();
		wKey.optWidth(true);
		wReturn.setRowNums();
		wReturn.optWidth(true);		
		
        wlFromStep.setEnabled(!wFromCacheFile.getSelection());
        wFromStep.setEnabled(!wFromCacheFile.getSelection());
        wlCacheFileName.setEnabled(!wFromCacheFile.getSelection());
        wUpdateCacheFileCC.setEnabled(!wFromCacheFile.getSelection());
        wlUpdateCacheFile.setEnabled(!wFromCacheFile.getSelection());
        wUpdateCacheFile.setEnabled(!wFromCacheFile.getSelection());
        wlStep.setEnabled(!wFromCacheFile.getSelection());
        wStep.setEnabled(!wFromCacheFile.getSelection());
        wlFromCacheFile.setEnabled(!wFromStep.getSelection());
        wFromCacheFile.setEnabled(!wFromStep.getSelection());
        wlFromCacheFileName.setEnabled(!wFromStep.getSelection());
        wSearchCacheFileNameCC.setEnabled(!wFromStep.getSelection());
        
	}
	
	private void cancel()
	{
		stepname=null;
		input.setChanged(changed);
		dispose();
	}
	
	private void ok()
	{
		if (Const.isEmpty(wStepname.getText())) return;

		int nrkeys             = wKey.nrNonEmpty();
		int nrvalues           = wReturn.nrNonEmpty();
		input.allocate(nrkeys, nrvalues);
		input.setMemoryPreservationActive(wPreserveMemory.getSelection());
		input.setUsingSortedList(wSortedList.getSelection());
        input.setUsingIntegerPair(wIntegerPair.getSelection());
        input.setFromCacheFile(wFromCacheFile.getSelection());
        input.setFromStep(wFromStep.getSelection());
        input.setUpdateCacheFile(wUpdateCacheFile.getSelection());
        input.setSearchCacheFileName(wSearchCacheFileNameCC.getText());
        input.setUpdateCacheFileName(wUpdateCacheFileCC.getText());
        
        
        if(log.isDebug()) logDebug(BaseMessages.getString(PKG, "StreamLookupDialog.Log.FoundKeys",nrkeys+"")); //$NON-NLS-1$ //$NON-NLS-2$
		for (int i=0;i<nrkeys;i++)
		{
			TableItem item     = wKey.getNonEmpty(i);
			input.getKeystream()[i]       = item.getText(1);
			input.getKeylookup()[i] = item.getText(2);
		}
		
		if(log.isDebug()) logDebug(BaseMessages.getString(PKG, "StreamLookupDialog.Log.FoundFields",nrvalues+"")); //$NON-NLS-1$ //$NON-NLS-2$
		for (int i=0;i<nrvalues;i++)
		{
			TableItem item        = wReturn.getNonEmpty(i);
			input.getValue()[i]        = item.getText(1);
			input.getValueName()[i]    = item.getText(2);
			if (input.getValueName()[i]==null || input.getValueName()[i].length()==0)
				input.getValueName()[i] = input.getValue()[i];
			input.getValueDefault()[i]     = item.getText(3);
			input.getValueDefaultType()[i] = ValueMeta.getType(item.getText(4));
		}
		
		StreamInterface infoStream = input.getStepIOMeta().getInfoStreams().get(0);
		infoStream.setStepMeta( transMeta.findStep( wStep.getText() ) );
		if ((infoStream.getStepMeta()==null&&wFromStep.getSelection())||("".equals(wSearchCacheFileNameCC.getText())&&wFromCacheFile.getSelection())
				||(!wFromCacheFile.getSelection()&&!wFromStep.getSelection()))
		{
			MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR );
			mb.setMessage(BaseMessages.getString(PKG, "StreamLookupDialog.NotStepSpecified.DialogMessage",wStep.getText())); 	
			mb.setText(BaseMessages.getString(PKG, "StreamLookupDialog.StepCanNotFound.DialogTitle")); //$NON-NLS-1$
			mb.open(); 
		}

		stepname = wStepname.getText(); // return value
		
		dispose();
	}

	private void get()
	{
		if (transMeta.findStep(wStep.getText())==null) {
			MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR );
			mb.setMessage(BaseMessages.getString(PKG, "StreamLookupDialog.PleaseSelectAStepToReadFrom.DialogMessage")); //$NON-NLS-1$
			mb.setText(BaseMessages.getString(PKG, "StreamLookupDialog.PleaseSelectAStepToReadFrom.DialogTitle")); //$NON-NLS-1$
			mb.open(); 
			return;
		}
		
		try
		{
			RowMetaInterface r = transMeta.getPrevStepFields(stepname);
			if (r!=null && !r.isEmpty())
			{
                BaseStepDialog.getFieldsFromPrevious(r, wKey, 1, new int[] { 1, 2}, new int[] {}, -1, -1, null);
			}
			else
			{
				String stepFrom = wStep.getText();
				if (!Const.isEmpty(stepFrom))
				{
					r = transMeta.getStepFields(stepFrom);
					if (r!=null)
					{
	                    BaseStepDialog.getFieldsFromPrevious(r, wKey, 2, new int[] { 1, 2 }, new int[] {}, -1, -1, null);
					}
					else
					{
						MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR );
						mb.setMessage(BaseMessages.getString(PKG, "StreamLookupDialog.CouldNotFindFields.DialogMessage")); //$NON-NLS-1$
						mb.setText(BaseMessages.getString(PKG, "StreamLookupDialog.CouldNotFindFields.DialogTitle")); //$NON-NLS-1$
						mb.open(); 
					}
				}
				else
				{
					MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR );
					mb.setMessage(BaseMessages.getString(PKG, "StreamLookupDialog.StepNameRequired.DialogMessage")); //$NON-NLS-1$
					mb.setText(BaseMessages.getString(PKG, "StreamLookupDialog.StepNameRequired.DialogTitle")); //$NON-NLS-1$
					mb.open(); 
				}
			}
		}
		catch(KettleException ke)
		{
			new ErrorDialog(shell, BaseMessages.getString(PKG, "StreamLookupDialog.FailedToGetFields.DialogTitle"), BaseMessages.getString(PKG, "StreamLookupDialog.FailedToGetFields.DialogMessage"), ke); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private void getlookup()
	{
		try
		{
			String stepFrom = wStep.getText();
			if (!Const.isEmpty(stepFrom))
			{
				RowMetaInterface r = null;
				if(wFromStep.getSelection())//jjchu 2012-08
					r = transMeta.getStepFields(stepFrom);
				else
				{
					ICache cache = CacheFactory.load(transMeta.getCacheFilePath(wSearchCacheFileNameCC.getText()).getFilePath());
					r = cache.getInfoFields();
				} 
				if (r!=null && !r.isEmpty())
				{
                    BaseStepDialog.getFieldsFromPrevious(r, wReturn, 1, new int[] { 1 }, new int[] { 4 }, -1, -1, null);
				}
				else
				{
					MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR );
					mb.setMessage(BaseMessages.getString(PKG, "StreamLookupDialog.CouldNotFindFields.DialogMessage")); //$NON-NLS-1$
					mb.setText(BaseMessages.getString(PKG, "StreamLookupDialog.CouldNotFindFields.DialogTitle")); //$NON-NLS-1$
					mb.open(); 
				}
			}
			else
			{
				MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR );
				mb.setMessage(BaseMessages.getString(PKG, "StreamLookupDialog.StepNameRequired.DialogMessage")); //$NON-NLS-1$
				mb.setText(BaseMessages.getString(PKG, "StreamLookupDialog.StepNameRequired.DialogTitle")); //$NON-NLS-1$
				mb.open(); 
			}
		}
		catch(KettleException ke)
		{
			new ErrorDialog(shell, BaseMessages.getString(PKG, "StreamLookupDialog.FailedToGetFields.DialogTitle"), BaseMessages.getString(PKG, "StreamLookupDialog.FailedToGetFields.DialogMessage"), ke); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
}
