/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing
 */
package org.alfresco.web.bean.content;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.alfresco.config.Config;
import org.alfresco.config.ConfigElement;
import org.alfresco.config.ConfigService;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.model.WCMAppModel;
import org.alfresco.model.WCMWorkflowModel;
import org.alfresco.repo.avm.AVMNodeConverter;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.workflow.WorkflowModel;
import org.alfresco.service.cmr.avm.AVMExistsException;
import org.alfresco.service.cmr.avm.AVMNodeDescriptor;
import org.alfresco.service.cmr.avm.AVMService;
import org.alfresco.service.cmr.avm.locking.AVMLock;
import org.alfresco.service.cmr.avmsync.AVMDifference;
import org.alfresco.service.cmr.avmsync.AVMSyncService;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.workflow.WorkflowDefinition;
import org.alfresco.service.cmr.workflow.WorkflowPath;
import org.alfresco.service.cmr.workflow.WorkflowService;
import org.alfresco.service.cmr.workflow.WorkflowTask;
import org.alfresco.service.cmr.workflow.WorkflowTaskState;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.content.BaseContentWizard;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.data.IDataContainer;
import org.alfresco.web.data.QuickSort;
import org.alfresco.web.forms.Form;
import org.alfresco.web.forms.FormInstanceData;
import org.alfresco.web.forms.FormNotFoundException;
import org.alfresco.web.forms.FormProcessor;
import org.alfresco.web.forms.FormsService;
import org.alfresco.web.forms.RenderingEngineTemplate;
import org.alfresco.web.forms.Rendition;
import org.alfresco.web.forms.XMLUtil;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.UIListItem;
import org.alfresco.web.ui.wcm.component.UIUserSandboxes;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

/**
 * Bean implementation for the "Edit Content Wizard" dialog
 */
public class EditContentWizard extends CreateContentWizard
{
   private static final Log LOGGER = LogFactory.getLog(EditContentWizard.class);
   
   private NodeRef nodeRef;
   private Form form;

   // ------------------------------------------------------------------------------
   // Wizard implementation
   
   @Override
   public void init(final Map<String, String> parameters)
   {
      // TODO - currently assumes this is form content
      super.init(parameters);
      Node node = this.navigator.getDispatchContextNode();
      if (node == null)
      {
         throw new IllegalArgumentException("Edit Form wizard requires action node context.");
      }
      this.nodeRef = node.getNodeRef();
      try
      {
         formName = (String)nodeService.getProperty(nodeRef, WCMAppModel.PROP_PARENT_FORM_NAME); // getFormName() ...
         form = formsService.getForm(this.formName);
      }
      catch (FormNotFoundException fnfe)
      {
         Utils.addErrorMessage(fnfe.getMessage(), fnfe);
         throw new IllegalArgumentException(fnfe);
      }

      this.content = this.contentService.getReader(nodeRef, ContentModel.PROP_CONTENT).getContentString();
      
      this.fileName = (String)nodeService.getProperty(nodeRef, ContentModel.PROP_NAME); // getName() ...
      this.mimeType = MimetypeMap.MIMETYPE_XML;
   }

   @Override
   public String back()
   {
      return super.back();
   }
   
   @Override
   protected void saveContent(File fileContent, String strContent) throws Exception
   {
      ContentWriter writer = contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
      writer.putContent(strContent);
   }
   
   @Override
   protected String doPostCommitProcessing(FacesContext context, String outcome)
   {
      return outcome;
   }

   @Override 
   public Form getForm()
   {
      return this.form;
   }
}
