/**
 * Copyright (C) 2005-2009 Alfresco Software Limited.
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
 
/**
 * Folder actions component.
 * 
 * @namespace Alfresco
 * @class Alfresco.FolderActions
 */
(function()
{
   /**
    * YUI Library aliases
    */
   var Dom = YAHOO.util.Dom;

   /**
    * Alfresco Slingshot aliases
    */
   var $combine = Alfresco.util.combinePaths;
   
   /**
    * FolderActions constructor.
    * 
    * @param {String} htmlId The HTML id of the parent element
    * @return {Alfresco.FolderActions} The new FolderActions instance
    * @constructor
    */
   Alfresco.FolderActions = function(htmlId)
   {
      Alfresco.FolderActions.superclass.constructor.call(this, "Alfresco.FolderActions", htmlId, ["button"]);
      
      /* Decoupled event listeners */
      YAHOO.Bubbling.on("folderDetailsAvailable", this.onFolderDetailsAvailable, this);
      
      return this;
   };
   
   /**
    * Extend Alfresco.component.Base
    */
   YAHOO.extend(Alfresco.FolderActions, Alfresco.component.Base);
   
   /**
    * Augment prototype with Actions module
    */
   YAHOO.lang.augmentProto(Alfresco.FolderActions, Alfresco.doclib.Actions);

   /**
    * Augment prototype with main class implementation, ensuring overwrite is enabled
    */
   YAHOO.lang.augmentObject(Alfresco.FolderActions.prototype,
   {
      /**
       * Object container for initialization options
       *
       * @property options
       * @type object
       */
      options:
      {
         /**
          * Working mode: Site or Repository.
          * Affects how actions operate, e.g. actvities are not posted in Repository mode.
          * 
          * @property workingMode
          * @type number
          * @default Alfresco.doclib.MODE_SITE
          */
         workingMode: Alfresco.doclib.MODE_SITE,

         /**
          * Current siteId.
          * 
          * @property siteId
          * @type string
          */
         siteId: "",
         
         /**
          * ContainerId representing root container
          *
          * @property containerId
          * @type string
          * @default "documentLibrary"
          */
         containerId: "documentLibrary"
      },
      
      /**
       * The data for the folder
       * 
       * @property assetData
       * @type object
       */
      assetData: null,

      /**
       * Metadata returned by doclist data webscript
       *
       * @property doclistMetadata
       * @type object
       * @default null
       */
      doclistMetadata: null,

      /**
       * Path of asset being viewed - used to scope some actions (e.g. copy to, move to)
       * 
       * @property currentPath
       * @type string
       */
      currentPath: null,

      /**
       * The urls to be used when creating links in the action cell
       *
       * @method getActionUrls
       * @return {object} Object literal containing URLs to be substituted in action placeholders
       */
      getActionUrls: function DocumentActions_getActionUrls()
      {
         var nodeRef = this.assetData.nodeRef,
            nodeRefUri = new Alfresco.util.NodeRef(nodeRef).uri;

         return (
         {
            editMetadataUrl: "edit-metadata?nodeRef=" + nodeRef,
            explorerViewUrl: $combine(this.options.repositoryUrl, "/n/showSpaceDetails/", nodeRefUri) + "\" target=\"_blank"
         });
      },
       
      /**
       * Event handler called when the "folderDetailsAvailable" event is received
       */
      onFolderDetailsAvailable: function FolderActions_onFolderDetailsAvailable(layer, args)
      {
         var me = this;
         
         // Asset data passed-in through event arguments
         this.assetData = args[1].folderDetails;
         this.doclistMetadata = args[1].metadata;
         this.currentPath = this.assetData.location.path;
         
         // Copy template into active area
         var actionsContainer = Dom.get(this.id + "-actionSet"),
            actionSet = this.assetData.actionSet,
            clone = Dom.get(this.id + "-actionSet-" + actionSet).cloneNode(true);

         // Token replacement
         clone.innerHTML = YAHOO.lang.substitute(window.unescape(clone.innerHTML), this.getActionUrls());

         // Replace existing actions and assign correct class for icon rendering
         actionsContainer.innerHTML = clone.innerHTML;
         Dom.addClass(actionsContainer, this.assetData.type);

         // Hide actions which have been disallowed through permissions
         if (this.assetData.permissions && this.assetData.permissions.userAccess)
         {
            var userAccess = this.assetData.permissions.userAccess,
               actions = YAHOO.util.Selector.query("div", actionsContainer),
               action, actionPermissions, i, ii, j, jj, actionAllowed;

            // Inject special-case permissions
            if (this.options.repositoryUrl)
            {
               userAccess["repository"] = true;
            }
            
            for (i = 0, ii = actions.length; i < ii; i++)
            {
               action = actions[i];
               actionAllowed = true;
               if (action.firstChild.rel !== "")
               {
                  actionPermissions = action.firstChild.rel.split(",");
                  for (j = 0, jj = actionPermissions.length; j < jj; j++)
                  {
                     if (!userAccess[actionPermissions[j]])
                     {
                        actionAllowed = false;
                        break;
                     }
                  }
               }
               Dom.setStyle(action, "display", actionAllowed ? "block" : "none");
            }
         }
         Dom.setStyle(actionsContainer, "visibility", "visible");
         
         // Hook action events
         var fnActionHandler = function FolderActions_fnActionHandler(layer, args)
         {
            var owner = YAHOO.Bubbling.getOwnerByTagName(args[1].anchor, "div");
            if (owner !== null)
            {
               var action = owner.className;
               if (typeof me[action] == "function")
               {
                  args[1].stop = true;
                  me[action].call(me, me.assetData, owner);
               }
            }
            return true;
         };
         
         YAHOO.Bubbling.addDefaultAction("action-link", fnActionHandler);
         
         // DocLib Actions module
         this.modules.actions = new Alfresco.module.DoclibActions(this.options.workingMode);
      },

      /**
       * Delete Asset confirmed.
       *
       * @override
       * @method _onActionDeleteConfirm
       * @param asset {object} Object literal representing file or folder to be actioned
       * @private
       */
      _onActionDeleteConfirm: function FolderActions__onActionDeleteConfirm(asset)
      {
         var path = asset.location.path,
            fileName = asset.fileName,
            displayName = asset.displayName,
            nodeRef = new Alfresco.util.NodeRef(asset.nodeRef),
            callbackUrl = this.options.workingMode == Alfresco.doclib.MODE_SITE ? "documentlibrary" : "repository",
            encodedPath = path.length > 1 ? "?path=" + window.escape(path) : "";
         
         this.modules.actions.genericAction(
         {
            success:
            {
               callback:
               {
                  fn: function FolderActions_oADC_success(data)
                  {
                     window.location = callbackUrl + encodedPath;
                  }
               }
            },
            failure:
            {
               message: this.msg("message.delete.failure", displayName)
            },
            webscript:
            {
               method: Alfresco.util.Ajax.DELETE,
               name: "file/node/{nodeRef}",
               params:
               {
                  nodeRef: nodeRef.uri
               }
            }
         });
      }
   }, true);
})();