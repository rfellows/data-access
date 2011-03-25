package org.pentaho.platform.dataaccess.datasource.wizard.sources.csv;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.pentaho.platform.dataaccess.datasource.Delimiter;
import org.pentaho.platform.dataaccess.datasource.Enclosure;
import org.pentaho.platform.dataaccess.datasource.wizard.controllers.MessageHandler;
import org.pentaho.platform.dataaccess.datasource.wizard.models.CsvFileInfo;
import org.pentaho.platform.dataaccess.datasource.wizard.service.gwt.ICsvDatasourceServiceAsync;
import org.pentaho.platform.dataaccess.datasource.wizard.steps.AbstractWizardStep;
import org.pentaho.ui.xul.XulComponent;
import org.pentaho.ui.xul.XulDomContainer;
import org.pentaho.ui.xul.XulException;
import org.pentaho.ui.xul.binding.Binding;
import org.pentaho.ui.xul.binding.BindingConvertor;
import org.pentaho.ui.xul.components.XulLabel;
import org.pentaho.ui.xul.components.XulMenuList;
import org.pentaho.ui.xul.components.XulTextbox;
import org.pentaho.ui.xul.containers.XulTree;
import org.pentaho.ui.xul.stereotype.Bindable;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * User: nbaker
 * Date: 3/22/11
 */
public class CsvPhysicalStep extends AbstractWizardStep {

  public static final int DEFAULT_CSV_TABLE_ROW_COUNT = 7;
  private XulMenuList<String> encodingTypeMenuList = null;
  private static final List<String> ENCODINGS = Arrays.asList("", "UTF-8", "UTF-16BE", "UTF-16LE", "UTF-32BE", "UTF-32LE", "Shift_JIS", "ISO-2022-JP", "ISO-2022-CN", "ISO-2022-KR", "GB18030", "Big5", "EUC-JP", "EUC-KR", "ISO-8859-1", "ISO-8859-2", "ISO-8859-5", "ISO-8859-6", "ISO-8859-7", "ISO-8859-8", "windows-1251", "windows-1256", "KOI8-R", "ISO-8859-9");
  private XulTextbox uploadedFileTextBox;
  private XulTree csvDataTable;
  private XulLabel csvTextPreview;

  private ICsvDatasourceServiceAsync csvDatasourceService;

  public CsvPhysicalStep(ICsvDatasourceServiceAsync csvDatasourceService){

    this.csvDatasourceService = csvDatasourceService;
  }
  @Override
  public void init(XulDomContainer mainWizardContainer) throws XulException {
    csvDataTable = (XulTree) getDocument().getElementById("csvDataTable");//$NON-NLS-1$
    uploadedFileTextBox = (XulTextbox) getDocument().getElementById("uploadedFile"); //$NON-NLS-1$
    csvTextPreview = (XulLabel) getDocument().getElementById("csvTextPreview"); //$NON-NLS-1$

    encodingTypeMenuList = (XulMenuList<String>) mainWizardContainer.getDocumentRoot().getElementById("encodingTypeMenuList");
    encodingTypeMenuList.setElements(ENCODINGS);

    super.init(mainWizardContainer);
  }

  @Override
  public void activating() {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void deactivate() {
    
  }

  @Override
  public XulComponent getUIComponent() {
    return getDocument().getElementById("csvDeckPanel");
  }

  @Override
  public String getStepName() {
    return "CSV Physical";
  }

  @Override
  public void setBindings() {
    BindingConvertor<Integer, Boolean> isFirstRowHeaderConverter = BindingConvertor.integer2Boolean();
    getBindingFactory().createBinding(
        getDatasourceModel().getModelInfo().getFileInfo(),
        CsvFileInfo.HEADER_ROWS_ATTRIBUTE,
        "isHeaderCheckBox",  //$NON-NLS-1$
        "checked", //$NON-NLS-1$
        isFirstRowHeaderConverter);

    // Binding convertor to between Delimiter and radio group selected value
    BindingConvertor<String, String> delimiterBindingConvertor = new BindingConvertor<String, String>() {
      public String sourceToTarget(String source) {
        Delimiter delimiter = Delimiter.lookupValue(source);
        if (delimiter != null) {
          return Delimiter.lookupValue(source).getName();
        } else {
          return source;
        }
      }
      public String targetToSource(String target) {
        Delimiter delimiter = Delimiter.lookupName(target);
        if (delimiter != null) {
          return delimiter.getValue();
        } else {
          return target;
        }
      }
    };

    // add binding for the Delimiter to it's corresponding radio group
    getBindingFactory().createBinding(
        getDatasourceModel().getModelInfo().getFileInfo(),
        CsvFileInfo.DELIMITER_ATTRIBUTE,
        "delimiterRadioGroup", //$NON-NLS-1$
        "value", //$NON-NLS-1$
        delimiterBindingConvertor);

    // Binding convertor to between Enclosure and radio group selected value
    BindingConvertor<String, String> enclosureBindingConvertor = new BindingConvertor<String, String>() {
      public String sourceToTarget(String source) {
        Enclosure e = Enclosure.lookupValue(source);
        if (e == null) {
          e = Enclosure.NONE;
        }
        return e.getName();
      }
      public String targetToSource(String target) {
        Enclosure e = Enclosure.lookupName(target);
        if (e == Enclosure.NONE) {
          return null;
        } else {
          return e.getValue();
        }
      }
    };

    // add binding for the Enclosure to it's corresponding radio group
    getBindingFactory().createBinding(getDatasourceModel().getModelInfo().getFileInfo(),
        CsvFileInfo.ENCLOSURE_ATTRIBUTE,
        "enclosureRadioGroup", //$NON-NLS-1$
        "value", //$NON-NLS-1$
        enclosureBindingConvertor);

    // when the delimiter changes, we need to refresh the preview
    getDatasourceModel().getModelInfo().getFileInfo().addPropertyChangeListener(CsvFileInfo.DELIMITER_ATTRIBUTE, new RefreshPreviewPropertyChangeListener());

    // when the enclosure changes, we need to refresh the preview
    getDatasourceModel().getModelInfo().getFileInfo().addPropertyChangeListener(CsvFileInfo.ENCLOSURE_ATTRIBUTE, new RefreshPreviewPropertyChangeListener());

    // when the first-row-is-header flag changes, we need to refresh the preview
    getDatasourceModel().getModelInfo().getFileInfo().addPropertyChangeListener(CsvFileInfo.HEADER_ROWS_ATTRIBUTE, new RefreshPreviewPropertyChangeListener());



    uploadedFileTextBox.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("value")) {

          // set the defaults if none already selected
          String delimiter = getDatasourceModel().getModelInfo().getFileInfo().getDelimiter();
          if (delimiter == null || delimiter.equals("")) {
            getDatasourceModel().getModelInfo().getFileInfo().setDelimiter(",");
            getDatasourceModel().getModelInfo().getFileInfo().setHeaderRows(1);
          }
          String enclosure = getDatasourceModel().getModelInfo().getFileInfo().getEnclosure();
          if (enclosure == null || enclosure.equals("")) {
            getDatasourceModel().getModelInfo().getFileInfo().setEnclosure("\"");
          }

          syncModelInfo();
          getDatasourceModel().getGuiStateModel().setDirty(true);
          getDatasourceModel().getModelInfo().validate();
        }
      }
    });


    // binding to set the first-row-is-header checkbox's enabled property based on the selectedItem in the filesList
    getBindingFactory().createBinding(uploadedFileTextBox, "value", "isHeaderCheckBox", "!disabled", BindingConvertor.object2Boolean());
    // binding to set the delimiters enabled property based on the selectedItem in the filesList
    getBindingFactory().createBinding(uploadedFileTextBox, "value", "delimiterRadioGroup", "!disabled", BindingConvertor.object2Boolean());
    // binding to set the enclosures enabled property based on the selectedItem in the filesList
    getBindingFactory().createBinding(uploadedFileTextBox, "value", "enclosureRadioGroup", "!disabled", BindingConvertor.object2Boolean());

    getBindingFactory().createBinding(getDatasourceModel().getModelInfo().getFileInfo(), "friendlyFilename", uploadedFileTextBox, "value"); //$NON-NLS-1$ //$NON-NLS-2$
    getBindingFactory().createBinding(uploadedFileTextBox, "value", "encodingTypeMenuList", "!disabled", BindingConvertor.object2Boolean());

    BindingConvertor<String, String> encodingBindingConvertor = new BindingConvertor<String, String>() {
        public String sourceToTarget(String source) {
        	return source;
        }
        public String targetToSource(String target) {
        	Collection<String> encodings = encodingTypeMenuList.getElements();
        	if(!encodings.contains(target)) {
        		encodings.add(target);
        		encodingTypeMenuList.setElements(encodings);
        	}
        	return target;
        }
    };
    getBindingFactory().setBindingType(Binding.Type.BI_DIRECTIONAL);
    getBindingFactory().createBinding(encodingTypeMenuList, "value", getDatasourceModel().getModelInfo().getFileInfo(), CsvFileInfo.ENCODING, encodingBindingConvertor); //$NON-NLS-1$ //$NON-NLS-2$
    getDatasourceModel().getModelInfo().getFileInfo().addPropertyChangeListener(CsvFileInfo.ENCODING, new RefreshPreviewPropertyChangeListener());
  }

    /**
   * Executes when refresh of preview is required (also effective when dirty flag should be set)
   */
  private class RefreshPreviewPropertyChangeListener implements PropertyChangeListener {
    public void propertyChange(PropertyChangeEvent evt) {
      getDatasourceModel().getGuiStateModel().setDirty(true);
      try {
          if(evt.getPropertyName().equals(CsvFileInfo.ENCODING)) {

        	  csvDatasourceService.getPreviewRows(getDatasourceModel().getModelInfo().getFileInfo().getTmpFilename(),
        			  getDatasourceModel().getModelInfo().getFileInfo().getHeaderRows() > 0,
  		            10, getDatasourceModel().getModelInfo().getFileInfo().getEncoding(),
  		            new AsyncCallback<List<String>>()  {
	    		          public void onSuccess(List<String> lines) {
	    		            try {
	    		            	getDatasourceModel().getModelInfo().getFileInfo().setContents(lines);
	    		            	refreshPreview();
	    		            } catch (Exception e) {
	    		              GWT.log("Had an issue refreshing the data preview", e); //$NON-NLS-1$
	    		            }
	    		          }
	    		          public void onFailure(Throwable th) {
	    		        	  GWT.log(th.toString());
	    		          }
  		        });
          }


          refreshPreview();
      } catch (Exception e) {
    	  GWT.log(e.toString());
      }
      getDatasourceModel().getModelInfo().validate();
      firePropertyChange("valid", !isValidated(), isValidated());

    }
  }

  private boolean isValidated(){

    return datasourceModel.getModelInfo().getStageTableName()!= null &&
        datasourceModel.getModelInfo().getStageTableName().trim().length() > 0 &&
        datasourceModel.getModelInfo().getFileInfo() != null &&
        datasourceModel.getModelInfo().getFileInfo().getTmpFilename() != null &&
        datasourceModel.getModelInfo().getFileInfo().getTmpFilename().length() > 0 &&
        datasourceModel.getModelInfo().getFileInfo().getDelimiter() != null &&
        datasourceModel.getModelInfo().getFileInfo().getDelimiter().length() > 0;
  }

  public void syncModelInfo() {
  	String filename = datasourceModel.getModelInfo().getFileInfo().getFileName();
  	String tmpFilename  = datasourceModel.getModelInfo().getFileInfo().getTmpFilename();

  	if((filename == null) || (!tmpFilename.startsWith(filename))) { //creating a brand new ds || editing a ds having uploaded a new file
  		filename = tmpFilename;
  	} else if(tmpFilename.startsWith(filename)) { // editing a ds without uploading a new file
  		datasourceModel.getModelInfo().getFileInfo().setTmpFilename(filename);
  	}

	if (filename != null) {
    csvDatasourceService.getEncoding(filename, new AsyncCallback<String>()  {
    	public void onSuccess(String encoding) {
    		datasourceModel.getModelInfo().getFileInfo().setEncodingFromServer(encoding);
    		      try {
    		        // go get the file contents of the selected file
    		        csvDatasourceService.getPreviewRows(datasourceModel.getModelInfo().getFileInfo().getTmpFilename(),
    		            datasourceModel.getModelInfo().getFileInfo().getHeaderRows() > 0,
    		            10, encoding,
    		            new AsyncCallback<List<String>>()  {
	    		          public void onSuccess(List<String> lines) {
	    		            try {
	    		              datasourceModel.getModelInfo().getFileInfo().setContents(lines);

	    		              refreshPreview();
	    		            } catch (Exception e) {
	    		              GWT.log("Had an issue refreshing the data preview", e); //$NON-NLS-1$
	    		            }
	    		          }
	    		          public void onFailure(Throwable th) {
	    		            MessageHandler.getInstance().showErrorDialog(th.getMessage());
	    		          }
    		        });

    		      } catch (Exception e) {
    		        MessageHandler.getInstance().showErrorDialog(e.getMessage());
    		      }
    	}
    	public void onFailure(Throwable th) {
    		 GWT.log("Had an issue getting the encoding type", th); //$NON-NLS-1$
    	}
    });

    } else {
	      try {
	        refreshPreview();
	      } catch (Exception e) {
	        GWT.log("Had an issue refreshing the data preview", e);             //$NON-NLS-1$
	      }
	    }


  }


  @Bindable
  public void refreshPreview() throws Exception {
    csvTextPreview.setValue(""); //$NON-NLS-1$
    CsvFileInfo fileInfo = datasourceModel.getModelInfo().getFileInfo();
    csvTextPreview.setValue(fileInfo.formatSampleContents());
  }
}